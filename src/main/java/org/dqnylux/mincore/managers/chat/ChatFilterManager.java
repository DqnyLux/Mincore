package org.dqnylux.mincore.managers.chat;

import net.kyori.adventure.chat.SignedMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.FiltersConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.utils.TextUtils;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline de filtros de chat: anti-spam, mayúsculas, repetición, palabras
 * prohibidas y anuncios/enlaces. Se salta por completo si el jugador tiene
 * mincore.chat.bypass (verificado por el llamador, no aquí).
 *
 * La revisión por IA (palabras ambiguas por contexto) NO es parte de este
 * pipeline síncrono - a diferencia del resto, el mensaje se muestra siempre
 * al instante (cero demora perceptible, nunca se retiene) y {@link #reviewAsync}
 * corre la consulta a Gemini en un hilo aparte DESPUÉS de que el chat ya se
 * mostró; si confirma que era tóxico, borra el mensaje de la pantalla de
 * todos con el paquete nativo de borrado de mensajes firmados de Minecraft
 * (Bukkit.getServer().deleteMessage) y avisa al infractor y al staff.
 */
public class ChatFilterManager {

    public enum CancelReason {
        SPAM, FLOOD, TOO_LONG, REPETITION, BAD_WORD, ADS
    }

    /**
     * message: para un resultado cancelado, es el mensaje ORIGINAL tal cual
     * lo escribió el jugador (rawMessage, sin censurar) - lo usa el aviso de
     * staff (messages.chat.staffAlert -> %message%) para mostrar CUÁL
     * mensaje disparó el filtro, no solo que "algo" se bloqueó.
     * blockedWord: SOLO se llena para CancelReason.BAD_WORD - la palabra/
     * frase literal que hizo matchear el blocklist, para el aviso al propio
     * jugador (messages.chat.filterBadWord -> %word%). Null para el resto.
     */
    public record FilterResult(String message, boolean cancelled, CancelReason reason, boolean infraction, String blockedWord) {

        static FilterResult allowed(String message, boolean infraction) {
            return new FilterResult(message, false, null, infraction, null);
        }

        static FilterResult cancelled(CancelReason reason, String message) {
            return new FilterResult(message, true, reason, false, null);
        }

        static FilterResult cancelled(CancelReason reason, String message, String blockedWord) {
            return new FilterResult(message, true, reason, false, blockedWord);
        }
    }

    /** Detector de URLs del filtro de anuncios, compartido con ChatFormatHandler para que los enlaces clickeables usen EXACTAMENTE la misma definición de "qué es una URL". */
    static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?([a-zA-Z0-9-]+\\.[a-zA-Z]{2,})(?:[/:][^\\s]*)?|\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<Character, String> LEETSPEAK = Map.of(
            'a', "[a4@]", 'e', "[e3]", 'i', "[i1!|]", 'o', "[o0]", 's', "[s5$z]",
            't', "[t7+]", 'b', "[b8]", 'g', "[g9]"
    );

    private final Mincore plugin;
    private final Map<java.util.UUID, Deque<Long>> messageTimestamps = new ConcurrentHashMap<>();
    /** Último mensaje de cada jugador (epoch millis) - solo para floodProtection (tiempo MÍNIMO entre mensajes, distinto de antiSpam que cuenta por ráfaga). */
    private final Map<java.util.UUID, Long> lastMessageAt = new ConcurrentHashMap<>();
    /** Últimos N mensajes SIN normalizar (se normalizan al comparar) - acotado a filters.repetition.compareLastMessages en cada uso. */
    private final Map<java.util.UUID, Deque<String>> recentMessages = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Integer> lastMessageRepeats = new ConcurrentHashMap<>();
    private final AiModerationClient aiModerationClient;
    private final java.util.concurrent.atomic.AtomicBoolean generatingReviewWords = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean generatingBadWords = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile Pattern badWordsPattern;
    private volatile java.util.List<Pattern> badWordsRegexList = java.util.List.of();
    private volatile java.util.Set<String> whitelistWordsLower = java.util.Set.of();
    private volatile java.util.Set<String> reviewWordsNormalized = java.util.Set.of();
    private volatile Map<Character, Integer> customCharLimits = Map.of();

    public ChatFilterManager(Mincore plugin) {
        this.plugin = plugin;
        this.aiModerationClient = new AiModerationClient(plugin);
        ensureBadWordsRegexResource();
        reload();
        maybeAutoGenerateReviewWords();
        maybeAutoGenerateBadWordsRegex();
    }

    /** Si badWords.regexFile todavía no existe, lo siembra con el blocklist de regex que trae el jar (src/main/resources/badwords_regex.txt) - así hay una base real desde el primer arranque, sin depender de la IA. /coreec ai refreshbadwords AGREGA sobre esto, nunca lo pisa. No hace nada si el admin ya tiene el archivo (aunque esté vacío a propósito) o si cambió regexFile a un nombre sin recurso embebido. */
    private void ensureBadWordsRegexResource() {
        String fileName = plugin.getConfigManager().getFiltersConfig().badWords.regexFile;
        java.io.File file = new java.io.File(plugin.getDataFolder(), fileName);
        if (file.exists()) return;
        if (plugin.getResource(fileName) == null) return;
        plugin.saveResource(fileName, false);
    }

    public void reload() {
        FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
        this.badWordsPattern = buildFuzzyPattern(withoutWhitelisted(filters.badWords.words, filters.badWords.whitelistWords));
        this.badWordsRegexList = loadRegexBlocklist(filters.badWords.regexFile);
        this.whitelistWordsLower = filters.badWords.whitelistWords.stream()
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.reviewWordsNormalized = filters.aiModeration.reviewWords.stream()
                .map(this::normalizeChatSentinelStyle)
                .filter(w -> !w.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.customCharLimits = parseCustomCharLimits(filters.repetition.customCharacterLimits);
    }

    /** Lee badWords.regexFile (una regex de Java por línea, "#" = comentario) - líneas mal formadas se ignoran con un warning, no rompen la carga del resto. */
    private java.util.List<Pattern> loadRegexBlocklist(String fileName) {
        java.io.File file = new java.io.File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            Bukkit.getLogger().warning("[Mincore] " + fileName + " no existe todavía - el blocklist de regex de badWords está vacío hasta que se genere (automático al arrancar con apiKey configurada) o lo crees a mano.");
            return java.util.List.of();
        }

        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            java.util.List<Pattern> patterns = new java.util.ArrayList<>();
            int ignoredLines = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                try {
                    // UNICODE_CHARACTER_CLASS: sin este flag, \w (y por lo
                    // tanto [^\wñ] usado como "límite de palabra" en TODO el
                    // blocklist) es ASCII-only en Java - trata á/é/í/ó/ú como
                    // si fueran un espacio, partiendo palabras normales en
                    // español en fragmentos y disparando falsos positivos en
                    // los patrones más laxos. Con el flag, \w reconoce
                    // cualquier letra Unicode (tildes incluidas) como
                    // carácter de palabra real.
                    patterns.add(Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS));
                } catch (java.util.regex.PatternSyntaxException e) {
                    ignoredLines++;
                    Bukkit.getLogger().warning("[Mincore] Regex inválida en " + fileName + ", se ignora esa línea: " + trimmed + " (" + e.getMessage() + ")");
                }
            }
            // CRÍTICO: se ordena por longitud de regex DESCENDENTE antes de
            // devolver. En process(), cada regex de esta lista se aplica en
            // orden sobre el MISMO "message" que va mutando (reemplazando lo
            // que matchea con ***) - si un patrón corto y genérico (ej. una
            // sola palabra como "puta") corre ANTES que uno largo/específico
            // (ej. la frase completa "maricon...negro...hijo de puta" que
            // aprobó un staff), el corto ya destruye el texto literal que el
            // patrón largo necesita para matchear, y el patrón largo NUNCA
            // llega a activarse aunque esté cargado y guardado bien en el
            // archivo - bug real reportado por el usuario ("bloqueé el
            // patrón pero el mensaje se sigue pudiendo repetir igual"). Las
            // frases completas que aprende la IA (learnedPhrasePrompt/
            // regexGenerationPrompt) son consistentemente más largas que los
            // patrones de una sola palabra, así que ordenar por longitud es
            // un proxy barato y efectivo de "más específico primero".
            patterns.sort((a, b) -> Integer.compare(b.pattern().length(), a.pattern().length()));

            // Log explícito de cuántas regex quedaron activas - para poder
            // confirmar desde consola que el archivo se leyó COMPLETO (todas
            // las líneas no vacías/no comentario) y no solo una parte.
            Bukkit.getLogger().info("[Mincore] " + fileName + " cargado: " + patterns.size() + " regex activas"
                    + (ignoredLines > 0 ? " (" + ignoredLines + " líneas con error de sintaxis, ver warnings arriba)" : "") + ".");
            return patterns;
        } catch (java.io.IOException e) {
            Bukkit.getLogger().warning("[Mincore] No se pudo leer " + fileName + ": " + e.getMessage());
            return java.util.List.of();
        }
    }

    /** Si aiModeration está activo y reviewWords sigue vacía (nadie la llenó a mano), le pide a Gemini que la genere UNA vez al arrancar - no hace falta que el dueño del server escriba la lista. */
    private void maybeAutoGenerateReviewWords() {
        FiltersConfig.AiModeration config = plugin.getConfigManager().getFiltersConfig().aiModeration;
        if (!config.enabled || !config.reviewWords.isEmpty()) return;
        // apiKey solo es obligatoria con provider=gemini - mismo motivo que
        // en reviewAsync (ver ese comentario): con provider=openai muchos
        // servidores locales (OmniRoute, LM Studio, etc.) no piden ninguna.
        if ("gemini".equalsIgnoreCase(config.provider) && (config.apiKey == null || config.apiKey.isBlank())) return;
        generateReviewWordsAsync(null);
    }

    /** Resultado de un refresh de reviewWords - separado en nuevas (recién agregadas) y ya existentes (la IA las repitió pero ya estaban en la lista), para poder mostrar el diff tanto en el juego como en Discord. */
    public record ReviewWordsRefreshResult(java.util.List<String> added, java.util.List<String> alreadyPresent) {}

    /**
     * Le pide a la IA una lista de reviewWords y la FUSIONA con la que ya
     * había (nunca reemplaza, mismo "agregar sin borrar" que ya usa
     * appendRegexLines para badWords) - antes esto SÍ reemplazaba la lista
     * entera en cada corrida, perdiendo cualquier palabra agregada a mano
     * que la IA no volviera a sugerir. @return null si la IA no devolvió
     * nada usable, o el diff (agregadas/ya existentes) si funcionó.
     */
    private ReviewWordsRefreshResult refreshReviewWordsSync() {
        java.util.List<String> generated = aiModerationClient.generateReviewWords();
        if (generated.isEmpty()) return null;

        FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        for (String w : filters.aiModeration.reviewWords) {
            String lw = w.toLowerCase(java.util.Locale.ROOT).trim();
            if (!lw.isEmpty()) merged.add(lw);
        }

        java.util.List<String> added = new java.util.ArrayList<>();
        java.util.List<String> alreadyPresent = new java.util.ArrayList<>();
        for (String w : generated) {
            String lw = w.toLowerCase(java.util.Locale.ROOT).trim();
            if (lw.isEmpty()) continue;
            if (merged.add(lw)) added.add(lw); else alreadyPresent.add(lw);
        }

        filters.aiModeration.reviewWords = new java.util.ArrayList<>(merged);
        filters.save();
        reload();

        return new ReviewWordsRefreshResult(added, alreadyPresent);
    }

    /**
     * Genera reviewWords vía IA y las agrega a filters.yml - usado tanto por
     * la generación automática al arrancar como por /coreec ai refreshwords
     * y el comando equivalente de Discord. requester puede ser null (disparo
     * automático, sin nadie a quien avisar).
     */
    public void generateReviewWordsAsync(org.bukkit.command.CommandSender requester) {
        generateReviewWordsAsync(requester, result -> {});
    }

    /** Variante con callback (result=null si falló) - la usa el bot de Discord para armar su propio embed con el diff en vez del mensaje de chat de Bukkit. El callback SIEMPRE corre en la región global (Bukkit-safe). */
    public void generateReviewWordsAsync(org.bukkit.command.CommandSender requester, java.util.function.Consumer<ReviewWordsRefreshResult> callback) {
        if (!generatingReviewWords.compareAndSet(false, true)) {
            if (requester != null) {
                var messages = plugin.getConfigManager().getMessagesConfig();
                requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiReviewWordsGenerating));
            }
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            ReviewWordsRefreshResult result;
            try {
                result = refreshReviewWordsSync();
            } finally {
                generatingReviewWords.set(false);
            }

            ReviewWordsRefreshResult finalResult = result;
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                var messages = plugin.getConfigManager().getMessagesConfig();
                if (finalResult == null) {
                    Bukkit.getLogger().warning("[Mincore] [IA] No se pudo generar reviewWords (ver warnings de Gemini arriba).");
                    if (requester != null) {
                        requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiReviewWordsFailed));
                    }
                    callback.accept(null);
                    return;
                }

                Bukkit.getLogger().info("[Mincore] [IA] reviewWords: " + finalResult.added().size() + " nuevas, "
                        + finalResult.alreadyPresent().size() + " ya estaban - guardadas en filters.yml.");
                if (requester != null) {
                    String summary = finalResult.added().isEmpty()
                            ? messages.commands.aiReviewWordsNone.replace("%count%", String.valueOf(finalResult.alreadyPresent().size()))
                            : messages.commands.aiReviewWordsAdded
                                    .replace("%added%", String.valueOf(finalResult.added().size()))
                                    .replace("%words%", String.join(", ", finalResult.added()))
                                    .replace("%already%", String.valueOf(finalResult.alreadyPresent().size()));
                    requester.sendMessage(TextUtils.format(messages.prefix + summary));
                }
                callback.accept(finalResult);
            });
        });
    }

    /** Si badWords está activo y badWords.regexFile no existe o está vacío (nadie lo generó/escribió a mano), le pide a Gemini que lo genere UNA vez al arrancar - reusa la apiKey de aiModeration, badWords no tiene la suya propia. */
    private void maybeAutoGenerateBadWordsRegex() {
        FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
        if (!filters.badWords.enabled) return;
        String apiKey = filters.aiModeration.apiKey;
        if (apiKey == null || apiKey.isBlank()) return;

        java.io.File file = new java.io.File(plugin.getDataFolder(), filters.badWords.regexFile);
        if (file.exists() && file.length() > 0) return;
        generateBadWordsAsync(null);
    }

    /**
     * Genera regex nuevas vía Gemini y las AGREGA a badWords.regexFile (nunca
     * reemplaza lo que ya había, ni lo generado antes ni lo que el dueño del
     * server haya escrito a mano) - usado tanto por la generación automática
     * al arrancar como por /coreec ai refreshbadwords. requester puede ser
     * null (disparo automático, sin nadie a quien avisar).
     */
    public void generateBadWordsAsync(org.bukkit.command.CommandSender requester) {
        if (!generatingBadWords.compareAndSet(false, true)) {
            if (requester != null) {
                var messages = plugin.getConfigManager().getMessagesConfig();
                requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiBadWordsGenerating));
            }
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            java.util.List<String> newPatterns;
            try {
                newPatterns = aiModerationClient.generateBadWordsRegex();
            } finally {
                generatingBadWords.set(false);
            }

            java.util.List<String> finalNew = newPatterns;
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                var messages = plugin.getConfigManager().getMessagesConfig();
                if (finalNew.isEmpty()) {
                    Bukkit.getLogger().warning("[Mincore] [IA] No se pudo generar el blocklist de regex (ver warnings de Gemini arriba).");
                    if (requester != null) {
                        requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiBadWordsFailed));
                    }
                    return;
                }

                FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
                java.io.File file = new java.io.File(plugin.getDataFolder(), filters.badWords.regexFile);
                int added = appendRegexLines(file, finalNew);
                reload();

                String summary = messages.commands.aiBadWordsSummary
                        .replace("%total%", String.valueOf(finalNew.size()))
                        .replace("%added%", String.valueOf(added))
                        .replace("%file_total%", String.valueOf(badWordsRegexList.size()))
                        .replace("%file%", filters.badWords.regexFile);
                Bukkit.getLogger().info("[Mincore] [IA] " + summary);
                if (requester != null) {
                    requester.sendMessage(TextUtils.format(messages.prefix + summary));
                }
            });
        });
    }

    /**
     * /coreec ai addbadword <palabra> - a diferencia de generateBadWordsAsync
     * (que regenera una TANDA entera de regex desde cero), esto le pide a
     * Gemini UNA sola regex nueva para UNA palabra puntual que un admin
     * quiere agregar en caliente (ej. un insulto local/regional que el
     * blocklist todavía no cubre) y la AGREGA sin tocar el resto del archivo
     * - mismo mecanismo de "nunca reemplazar" que ya usa appendRegexLines.
     * Reusa el mismo AtomicBoolean que refreshbadwords porque ambos escriben
     * el mismo archivo - dos generaciones simultáneas podrían pisarse la
     * lectura-modificación-escritura entre sí.
     */
    public void addBadWordAsync(org.bukkit.command.CommandSender requester, String word) {
        var messages = plugin.getConfigManager().getMessagesConfig();
        if (word == null || word.isBlank()) {
            requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiAddBadWordPrompt));
            return;
        }

        if (!generatingBadWords.compareAndSet(false, true)) {
            requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiAddBadWordGenerating));
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String pattern;
            try {
                pattern = aiModerationClient.generateRegexForWord(word);
            } finally {
                generatingBadWords.set(false);
            }

            String finalPattern = pattern;
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                if (finalPattern == null) {
                    Bukkit.getLogger().warning("[Mincore] [IA] No se pudo generar una regex para \"" + word + "\" (ver warnings de Gemini arriba).");
                    requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiAddBadWordFailed.replace("%word%", word)));
                    return;
                }

                FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
                java.io.File file = new java.io.File(plugin.getDataFolder(), filters.badWords.regexFile);
                int added = appendRegexLines(file, java.util.List.of(finalPattern));
                reload();

                if (added == 0) {
                    requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiAddBadWordAlreadyExists
                            .replace("%word%", word)
                            .replace("%file%", filters.badWords.regexFile)));
                    return;
                }

                Bukkit.getLogger().info("[Mincore] [IA] Se agregó una regex nueva para \"" + word + "\" a " + filters.badWords.regexFile + ": " + finalPattern);
                requester.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiAddBadWordSuccess
                        .replace("%word%", word)
                        .replace("%count%", String.valueOf(badWordsRegexList.size()))));
            });
        });
    }

    /**
     * Fusiona newLines con el contenido existente del archivo (deduplicado
     * exacto, sin normalizar) y reescribe TODO el archivo con el resultado -
     * el efecto neto es "agregar", nunca "reemplazar": lo que ya estaba
     * siempre sobrevive. @return cuántas líneas eran realmente nuevas.
     */
    private int appendRegexLines(java.io.File file, java.util.List<String> newLines) {
        try {
            plugin.getDataFolder().mkdirs();
            java.util.LinkedHashSet<String> existing = new java.util.LinkedHashSet<>();
            if (file.exists()) {
                for (String line : java.nio.file.Files.readAllLines(file.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) existing.add(trimmed);
                }
            }

            int before = existing.size();
            for (String line : newLines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) existing.add(trimmed);
            }
            int added = existing.size() - before;

            StringBuilder sb = new StringBuilder();
            sb.append("# Generado/ampliado por IA (Gemini) - una expresion regular de Java por linea.\n");
            sb.append("# /coreec ai refreshbadwords AGREGA lineas nuevas, nunca borra estas - tambien podes editar a mano.\n");
            for (String line : existing) sb.append(line).append('\n');
            java.nio.file.Files.writeString(file.toPath(), sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
            return added;
        } catch (java.io.IOException e) {
            Bukkit.getLogger().warning("[Mincore] No se pudo escribir " + file.getName() + ": " + e.getMessage());
            return 0;
        }
    }

    /** Resta (case-insensitive) las palabras de whitelistWords de la lista de prohibidas - si una palabra está en las dos, gana la whitelist y nunca se bloquea. */
    private java.util.List<String> withoutWhitelisted(java.util.List<String> words, java.util.List<String> whitelistWords) {
        if (whitelistWords.isEmpty()) return words;

        java.util.Set<String> whitelistLower = new java.util.HashSet<>();
        for (String w : whitelistWords) whitelistLower.add(w.toLowerCase());

        return words.stream().filter(w -> !whitelistLower.contains(w.toLowerCase())).toList();
    }

    /** "<CARACTER>;;<LIMITE>" (ej. "!;;10") -> mapa carácter->límite. Entradas mal formadas se ignoran (no revientan la carga del resto). */
    private Map<Character, Integer> parseCustomCharLimits(java.util.List<String> entries) {
        Map<Character, Integer> result = new java.util.HashMap<>();
        for (String entry : entries) {
            String[] parts = entry.split(";;", 2);
            if (parts.length != 2 || parts[0].isEmpty()) continue;
            try {
                result.put(parts[0].charAt(0), Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private Pattern buildFuzzyPattern(java.util.List<String> words) {
        if (words.isEmpty()) return null;

        String combined = words.stream()
                .map(this::toFuzzyRegex)
                .reduce((a, b) -> a + "|" + b)
                .orElse(null);

        // Mismo motivo que en loadRegexBlocklist: \b es ASCII-only sin este
        // flag, así que sin él una palabra con tilde (café, así, etc.)
        // quedaría "cortada" en el límite y el fuzzy regex podría no
        // reconocerla como palabra completa (o matchear un fragmento).
        return combined == null ? null : Pattern.compile("\\b(?:" + combined + ")\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    }

    private static final String FUZZY_SEPARATOR = "[\\s\\-_.]*";

    private String toFuzzyRegex(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toLowerCase().toCharArray()) {
            sb.append(LEETSPEAK.getOrDefault(c, Pattern.quote(String.valueOf(c))));
            sb.append(FUZZY_SEPARATOR);
        }
        if (sb.length() >= FUZZY_SEPARATOR.length()) {
            sb.setLength(sb.length() - FUZZY_SEPARATOR.length()); // quita el último separador opcional sobrante
        }
        return sb.toString();
    }

    public FilterResult process(Player player, String rawMessage) {
        FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
        java.util.UUID uuid = player.getUniqueId();

        if (filters.messageLength.enabled && rawMessage.length() > filters.messageLength.maxCharacters) {
            return FilterResult.cancelled(CancelReason.TOO_LONG, rawMessage);
        }

        if (filters.floodProtection.enabled && isFlooding(uuid, filters.floodProtection)) {
            return FilterResult.cancelled(CancelReason.FLOOD, rawMessage);
        }

        if (filters.antiSpam.enabled && isSpamming(uuid, filters.antiSpam)) {
            return FilterResult.cancelled(CancelReason.SPAM, rawMessage);
        }

        String message = rawMessage;

        if (filters.caps.enabled) {
            message = applyCapsFilter(message, filters.caps);
        }

        if (filters.repetition.enabled) {
            if (isRepetition(uuid, message, filters.repetition)) {
                return FilterResult.cancelled(CancelReason.REPETITION, rawMessage);
            }
            message = collapseRepeatedChars(message, filters.repetition.repeatedCharThreshold, customCharLimits);
        }

        boolean infraction = false;
        Pattern pattern = badWordsPattern;
        if (filters.badWords.enabled && pattern != null) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                if (!filters.badWords.replaceWords) {
                    return FilterResult.cancelled(CancelReason.BAD_WORD, rawMessage, matcher.group());
                }
                message = matcher.replaceAll(result -> "*".repeat(result.group().length()));
                infraction = true;
            }
        }

        if (filters.badWords.enabled && !badWordsRegexList.isEmpty()) {
            for (Pattern regex : badWordsRegexList) {
                Matcher matcher = regex.matcher(message);
                if (!matcher.find()) continue;

                // whitelistWords también aplica acá: por-match (no se puede
                // "restar" una palabra de una regex arbitraria como se hace
                // con badWords.words antes de compilarla) - un match que cae
                // justo en una palabra de la whitelist se deja tal cual.
                boolean[] hit = {false};
                String[] hitWord = {null};
                String replaced = regex.matcher(message).replaceAll(result -> {
                    if (whitelistWordsLower.contains(result.group().toLowerCase())) return result.group();
                    hit[0] = true;
                    hitWord[0] = result.group();
                    return "*".repeat(result.group().length());
                });
                if (!hit[0]) continue;

                if (!filters.badWords.replaceWords) {
                    return FilterResult.cancelled(CancelReason.BAD_WORD, rawMessage, hitWord[0]);
                }
                message = replaced;
                infraction = true;
            }
        }

        if (filters.ads.enabled && containsDisallowedLink(message, filters.ads)) {
            return FilterResult.cancelled(CancelReason.ADS, rawMessage);
        }

        rememberMessage(uuid, message, filters.repetition);
        return FilterResult.allowed(message, infraction);
    }

    /**
     * @return true si el mensaje contiene alguna palabra de filters.aiModeration.reviewWords
     * - el llamador decide si vale la pena revisar. Compara por token, normalizando ambos
     * lados (colores, acentos, caracteres especiales, letras repetidas colapsadas - mismo
     * formato que ChatSentinel) en vez del regex leetspeak que usa badWords, así "n3grooo"
     * o "négro!!!" matchean igual que "negro" sin tener que listar cada variante a mano.
     */
    public boolean matchesReviewWord(String message) {
        return findMatchedReviewWord(message) != null;
    }

    /**
     * Igual que matchesReviewWord() pero devuelve la palabra puntual que
     * matcheó (normalizada, ej. "negro") en vez de solo true/false - se la
     * pasamos a la IA al pedirle la regex aprendida (generateRegexForMessage)
     * para que sepa EXACTAMENTE qué palabra revisar en vez de tener que
     * adivinar cuál, de toda la oración, es la parte ambigua. @return null
     * si ninguna coincide.
     */
    public String findMatchedReviewWord(String message) {
        java.util.Set<String> words = reviewWordsNormalized;
        if (words.isEmpty()) return null;

        String normalized = normalizeChatSentinelStyle(message);
        for (String token : normalized.split(" ")) {
            if (words.contains(token)) return token;
        }
        return null;
    }

    /** Colores/formato, acentos, caracteres especiales y letras repetidas colapsadas a una sola - mismo formato que usa ChatSentinel para evadir bypasses por espaciado/leetspeak visual, reusado acá para reviewWords en vez del regex fuzzy que usa badWords. */
    private String normalizeChatSentinelStyle(String input) {
        if (input == null) return "";
        String value = input.replaceAll("(?i)[&§][0-9a-fk-orx]", "").trim().toLowerCase();
        value = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        value = value.replaceAll("[^\\p{L}\\p{N}\\s]+", " ").trim();
        value = value.replaceAll("\\s+", " ");
        return collapseAdjacentDuplicates(value);
    }

    /**
     * Dispara la revisión por IA en un hilo aparte - NO bloquea al llamador.
     * El mensaje ya se mostró (o está por mostrarse) normalmente; si la IA
     * confirma que es tóxico, se borra de la pantalla de todos, se avisa al
     * infractor y se alerta al staff. signedMessage puede ser null (cliente
     * sin firma de chat) - en ese caso no hay forma de borrar el mensaje, así
     * que se omite la revisión entera.
     *
     * censoredMessage es el texto YA pasado por el blocklist duro (con ***
     * donde matcheó badWords) - el gate de abajo revisa si la palabra de
     * revisión SIGUE VISIBLE ahí, no en rawMessage. Si el blocklist duro ya
     * censuró justo esa palabra, no hace falta gastar otra consulta a la IA
     * sobre algo que ya quedó oculto/resuelto; pero si el mensaje tiene OTRA
     * palabra ambigua sin relación (ej. "hijo de puta" censurado por el
     * blocklist Y "negro" sin tocar en el mismo mensaje), esa sigue visible
     * en censoredMessage y la revisión de IA sigue disparando igual - a
     * diferencia de un gate más tosco basado en "¿hubo CUALQUIER censura?",
     * que apagaba la revisión entera aunque la palabra ambigua nunca se
     * hubiera tocado.
     */
    public void reviewAsync(Player player, String rawMessage, String censoredMessage, SignedMessage signedMessage) {
        reviewAsync(player, rawMessage, censoredMessage, signedMessage, null);
    }

    public void reviewAsync(Player player, String rawMessage, String censoredMessage, SignedMessage signedMessage, String messageId) {
        FiltersConfig.AiModeration config = plugin.getConfigManager().getFiltersConfig().aiModeration;
        String matchedWord = findMatchedReviewWord(censoredMessage);
        if (!config.enabled || matchedWord == null) return;

        java.util.UUID uuid = player.getUniqueId();
        if (aiModerationClient.isOnCooldown(uuid)) return;

        java.util.List<String> context = recentContext(uuid, config.contextMessages);
        // apiKey solo es obligatoria con provider=gemini - muchos servidores
        // openai-compatible locales (OmniRoute, LM Studio, etc.) no piden
        // ninguna, así que exigirla siempre bloqueaba la revisión entera
        // aunque el servidor local funcionara perfecto sin key.
        if ("gemini".equalsIgnoreCase(config.provider) && (config.apiKey == null || config.apiKey.isBlank())) {
            Bukkit.getLogger().warning("[Mincore] filters.yml -> aiModeration.enabled está en true pero apiKey está vacía - no se puede consultar a Gemini.");
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            boolean toxic = aiModerationClient.isToxic(player, rawMessage, context);
            if (!toxic) return;

            // Antes de gastar OTRA llamada a la IA pidiendo una regex nueva,
            // reviso si algún patrón YA PENDIENTE (todavía sin aprobar/
            // rechazar) matchea este mismo mensaje - sin esto, repetir el
            // mismo insulto un par de veces (ej. copy-paste) generaba un
            // patrón pendiente DISTINTO cada vez para el mismo texto,
            // ensuciando la cola de aprobación con duplicados.
            Integer duplicateOf = findPendingPatternMatch(rawMessage);
            if (duplicateOf != null) {
                if (plugin.getConfigManager().getFiltersConfig().aiModeration.debugLogging) {
                    Bukkit.getLogger().info("[Mincore] [IA] Mensaje ya cubierto por el patrón pendiente #" + duplicateOf + " - no se genera otro.");
                }
                Bukkit.getGlobalRegionScheduler().run(plugin, t -> handleToxicMessage(player, rawMessage, signedMessage, messageId));
                return;
            }

            // Se pide la regex de la frase ACÁ, todavía en el hilo async -
            // es otra llamada HTTP bloqueante, no puede correr en el hilo
            // principal (ver handleToxicMessage/queuePendingPattern abajo).
            // Se le pasa matchedWord (la palabra puntual de reviewWords que
            // disparó todo esto) además del mensaje completo, para que la IA
            // no tenga que ADIVINAR cuál de todas las palabras de la oración
            // es la parte ambigua a revisar - se lo decimos directo.
            String learnedRegex = aiModerationClient.generateRegexForMessage(rawMessage, matchedWord);

            Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                handleToxicMessage(player, rawMessage, signedMessage, messageId);
                if (learnedRegex != null) queuePendingPattern(player, rawMessage, learnedRegex);
            });
        });
    }

    /**
     * Patrón aprendido de un mensaje YA confirmado tóxico por la IA, a la
     * espera de que un staff lo bloquee con /coreec ai block <id> (o lo
     * permita con /coreec ai allow <id>) antes de agregarse al blocklist
     * real - nunca se agrega solo. Esto evita que un
     * jugador fuerce a la IA a marcar una frase inocente como tóxica para
     * "envenenar" el blocklist con una entrada que después bloquee esa frase
     * para todo el mundo sin que nadie lo revise.
     */
    public record PendingPattern(int id, String regex, String playerName, String message, long timestamp) {}

    /** @return el ID del primer patrón pendiente cuya regex ya matchea rawMessage, o null si ninguno lo cubre todavía - evita generarle a la IA un patrón nuevo para un mensaje que ya está esperando aprobación. Ignora silenciosamente una regex que no compile (no debería pasar, ya se validó al generarla, pero no vale la pena tirar la revisión entera por eso). */
    private Integer findPendingPatternMatch(String rawMessage) {
        for (PendingPattern pending : pendingPatterns.values()) {
            try {
                if (Pattern.compile(pending.regex(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS).matcher(rawMessage).find()) {
                    return pending.id();
                }
            } catch (java.util.regex.PatternSyntaxException ignored) {
            }
        }
        return null;
    }

    private final Map<Integer, PendingPattern> pendingPatterns = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger pendingPatternIdSeq = new java.util.concurrent.atomic.AtomicInteger(1);

    private void queuePendingPattern(Player player, String message, String regex) {
        int id = pendingPatternIdSeq.getAndIncrement();
        PendingPattern pending = new PendingPattern(id, regex, player.getName(), message, System.currentTimeMillis());
        pendingPatterns.put(id, pending);

        String staffAlertPermission = plugin.getConfigManager().getFiltersConfig().punishment.staffAlertPermission;
        var alert = TextUtils.format("<yellow>[IA] Patrón nuevo pendiente de revisión <gray>(#" + id + ") <yellow>de "
                + player.getName() + ": <white>" + regex
                + " <gray>- /coreec ai block " + id + " o /coreec ai allow " + id);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(staffAlertPermission)) {
                staff.sendMessage(alert);
            }
        }
        Bukkit.getLogger().info("[Mincore] [IA] Patrón pendiente #" + id + " de " + player.getName() + " (\"" + message + "\"): " + regex);

        // No-op silencioso si discordApproval.enabled=false o el bot no
        // llegó a conectar - el bloqueo en el juego (/coreec ai block)
        // sigue funcionando igual sin importar esto.
        plugin.getDiscordApprovalBot().postPendingPattern(pending);
    }

    /** @return la regex aprobada y ya agregada al blocklist, o null si no existía ese ID (ya se aprobó/rechazó/nunca existió). */
    public String approvePendingPattern(int id) {
        PendingPattern pending = pendingPatterns.remove(id);
        if (pending == null) return null;

        FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
        java.io.File file = new java.io.File(plugin.getDataFolder(), filters.badWords.regexFile);
        appendRegexLines(file, java.util.List.of(pending.regex()));
        reload();

        Bukkit.getLogger().info("[Mincore] [IA] Patrón #" + id + " aprobado y agregado a " + filters.badWords.regexFile + ": " + pending.regex());
        return pending.regex();
    }

    /** @return true si había un patrón pendiente con ese ID (y se descartó), false si no existía. */
    public boolean rejectPendingPattern(int id) {
        return pendingPatterns.remove(id) != null;
    }

    public java.util.List<PendingPattern> listPendingPatterns() {
        return pendingPatterns.values().stream()
                .sorted(java.util.Comparator.comparingInt(PendingPattern::id))
                .toList();
    }

    private void handleToxicMessage(Player player, String message, SignedMessage signedMessage, String messageId) {
        if (messageId != null) {
            plugin.getMessageDeletionManager().deleteMessage(messageId, null);
        } else if (signedMessage != null) {
            try {
                Bukkit.getServer().deleteMessage(signedMessage);
            } catch (Throwable ignored) {
            }
        }

        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        if (player.isOnline()) {
            player.sendMessage(TextUtils.format(messages.prefix + messages.chat.aiMessageRemoved));
        }

        String staffAlertPermission = plugin.getConfigManager().getFiltersConfig().punishment.staffAlertPermission;
        var staffAlert = TextUtils.format(messages.prefix + messages.chat.aiStaffAlert
                .replace("%player%", player.getName())
                .replace("%message%", message));
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(staffAlertPermission)) {
                staff.sendMessage(staffAlert);
            }
        }
    }

    /**
     * Últimos "max" mensajes SIN normalizar del jugador ANTERIORES al actual,
     * más viejo primero - para el prompt de contexto de la IA. reviewAsync()
     * siempre corre DESPUÉS de process() (ver ChatListener.onChat), y
     * process() ya llamó rememberMessage() con el mensaje actual antes de
     * devolver el control - o sea que el mensaje que se está por evaluar YA
     * es el último elemento del historial acá. Sin descartarlo, la IA recibía
     * el mensaje actual duplicado (una vez como "contexto previo" sin marcar,
     * otra vez como "mensaje a evaluar"), robándole un lugar real de contexto
     * genuino a contextMessages.
     */
    private java.util.List<String> recentContext(java.util.UUID uuid, int max) {
        Deque<String> history = recentMessages.get(uuid);
        if (history == null || max <= 0) return java.util.List.of();

        synchronized (history) {
            java.util.List<String> snapshot = new java.util.ArrayList<>(history);
            if (!snapshot.isEmpty()) snapshot = snapshot.subList(0, snapshot.size() - 1);
            int from = Math.max(0, snapshot.size() - max);
            return snapshot.subList(from, snapshot.size());
        }
    }

    private void rememberMessage(java.util.UUID uuid, String message, FiltersConfig.Repetition config) {
        Deque<String> history = recentMessages.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(message);
            int max = Math.max(1, config.compareLastMessages);
            while (history.size() > max) history.removeFirst();
        }
    }

    private boolean isSpamming(java.util.UUID uuid, FiltersConfig.AntiSpam config) {
        long now = System.currentTimeMillis();
        long windowMillis = config.delaySeconds * 1000L;

        Deque<Long> timestamps = messageTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            timestamps.removeIf(timestamp -> now - timestamp > 10_000L);
            long recentCount = timestamps.stream().filter(timestamp -> now - timestamp <= windowMillis).count();
            timestamps.addLast(now);

            return recentCount + 1 >= config.maxMessages;
        }
    }

    /** Tiempo MÍNIMO entre un mensaje y el siguiente, sin importar cuántos van en una ventana - distinto de isSpamming (que cuenta por ráfaga). Útil contra bots/macros por debajo del umbral de ráfaga. */
    private boolean isFlooding(java.util.UUID uuid, FiltersConfig.FloodProtection config) {
        long now = System.currentTimeMillis();
        Long last = lastMessageAt.put(uuid, now);
        return last != null && now - last < config.minMillisBetweenMessages;
    }

    private String applyCapsFilter(String message, FiltersConfig.Caps config) {
        if (message.length() < config.minLength) return message;

        long upperCount = message.chars().filter(Character::isUpperCase).count();
        long letterCount = message.chars().filter(Character::isLetter).count();
        if (letterCount == 0) return message;

        double ratio = (double) upperCount / letterCount;
        return ratio >= config.maxPercentage ? message.toLowerCase() : message;
    }

    private boolean isRepetition(java.util.UUID uuid, String message, FiltersConfig.Repetition config) {
        Deque<String> history = recentMessages.get(uuid);
        if (history == null || history.isEmpty()) return false;

        String normalized = normalizeForComparison(message, config);

        if (normalized.length() <= config.shortMessageMaxLength) {
            String previousNormalized;
            synchronized (history) {
                previousNormalized = normalizeForComparison(history.peekLast(), config);
            }
            if (normalized.equals(previousNormalized)) {
                int repeats = lastMessageRepeats.merge(uuid, 1, Integer::sum);
                return repeats >= config.shortMessageMaxRepeats - 1;
            }
            lastMessageRepeats.put(uuid, 0);
            return false;
        }

        // Mensajes largos: comparados contra las últimas N (no solo la
        // anterior) - evita evadir el filtro alternando entre un puñado de
        // variantes parecidas en vez de repetir siempre la misma.
        java.util.List<String> snapshot;
        synchronized (history) {
            snapshot = new java.util.ArrayList<>(history);
        }
        Iterator<String> it = snapshot.iterator();
        int compared = 0;
        while (it.hasNext() && compared < config.compareLastMessages) {
            String previousNormalized = normalizeForComparison(it.next(), config);
            compared++;
            if (previousNormalized.isEmpty()) continue;
            if (similarity(normalized, previousNormalized) >= config.longMessageSimilarityThreshold) return true;
        }
        return false;
    }

    /** Colores/formato, acentos, caracteres especiales y letras repetidas colapsadas - así "HOLA!!!" y "hola" comparan igual (idea de ChatSentinel). */
    private String normalizeForComparison(String input, FiltersConfig.Repetition config) {
        if (input == null) return "";
        String value = input.replaceAll("(?i)[&§][0-9a-fk-orx]", "").trim().toLowerCase();
        if (config.stripAccents) {
            value = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        }
        if (config.stripSpecialCharacters) {
            value = value.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        }
        value = value.replaceAll("\\s+", " ");
        if (config.collapseRepeatedCharactersForComparison) {
            value = collapseAdjacentDuplicates(value);
        }
        return value;
    }

    private String collapseAdjacentDuplicates(String input) {
        if (input.isEmpty()) return input;
        StringBuilder result = new StringBuilder(input.length());
        char last = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i == 0 || c != last) result.append(c);
            last = c;
        }
        return result.toString();
    }

    /** Colapsa cada tanda de un mismo carácter repetido a 2 copias si su largo llega al límite - por carácter (customLimits) o el default si ese carácter no tiene uno propio. */
    private String collapseRepeatedChars(String message, int defaultThreshold, Map<Character, Integer> customLimits) {
        if (message.isEmpty()) return message;

        StringBuilder result = new StringBuilder(message.length());
        int i = 0;
        while (i < message.length()) {
            char c = message.charAt(i);
            int runLength = 1;
            while (i + runLength < message.length() && message.charAt(i + runLength) == c) runLength++;

            int limit = customLimits.getOrDefault(c, defaultThreshold);
            int keep = (limit >= 2 && runLength >= limit) ? 2 : runLength;
            for (int k = 0; k < keep; k++) result.append(c);
            i += runLength;
        }
        return result.toString();
    }

    /** Colapsa tandas de 4+ "palabras" de un solo carácter separadas por un espacio simple (ej. "s h i e l d . n e t" -> "shield.net") - no toca oraciones normales, donde las palabras casi nunca son de 1 solo carácter. */
    private static final Pattern SPACED_TOKEN_RUN = Pattern.compile("(?<!\\S)\\S(?: \\S){3,}(?!\\S)");

    private boolean containsDisallowedLink(String message, FiltersConfig.Ads config) {
        if (hasDisallowedLinkIn(message, config.whitelist)) return true;

        if (config.detectUnicodeUrls) {
            String normalized = Normalizer.normalize(message, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "");
            if (!normalized.equals(message) && hasDisallowedLinkIn(normalized, config.whitelist)) return true;
        }

        if (config.detectSpacedUrls) {
            String collapsed = SPACED_TOKEN_RUN.matcher(message).replaceAll(match -> match.group().replace(" ", ""));
            if (!collapsed.equals(message) && hasDisallowedLinkIn(collapsed, config.whitelist)) return true;
        }

        return false;
    }

    private boolean hasDisallowedLinkIn(String message, java.util.List<String> whitelist) {
        Matcher matcher = URL_PATTERN.matcher(message);
        while (matcher.find()) {
            String domain = matcher.group(1);
            if (domain == null) return true; // coincidió como IP

            String lowerDomain = domain.toLowerCase();
            boolean allowed = whitelist.stream().anyMatch(allowedDomain -> lowerDomain.endsWith(allowedDomain.toLowerCase()));
            if (!allowed) return true;
        }
        return false;
    }

    private double similarity(String a, String b) {
        int distance = levenshtein(a, b);
        int maxLength = Math.max(a.length(), b.length());
        if (maxLength == 0) return 1.0;
        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) previous[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            System.arraycopy(current, 0, previous, 0, current.length);
        }

        return previous[b.length()];
    }
}
