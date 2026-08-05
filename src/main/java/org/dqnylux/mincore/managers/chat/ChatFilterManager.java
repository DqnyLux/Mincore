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

    public record FilterResult(String message, boolean cancelled, CancelReason reason, boolean infraction) {

        static FilterResult allowed(String message, boolean infraction) {
            return new FilterResult(message, false, null, infraction);
        }

        static FilterResult cancelled(CancelReason reason) {
            return new FilterResult(null, true, reason, false);
        }
    }

    private static final Pattern URL_PATTERN = Pattern.compile(
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
            Bukkit.getLogger().warning("[CoreEC] " + fileName + " no existe todavía - el blocklist de regex de badWords está vacío hasta que se genere (automático al arrancar con apiKey configurada) o lo crees a mano.");
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
                    patterns.add(Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE));
                } catch (java.util.regex.PatternSyntaxException e) {
                    ignoredLines++;
                    Bukkit.getLogger().warning("[CoreEC] Regex inválida en " + fileName + ", se ignora esa línea: " + trimmed + " (" + e.getMessage() + ")");
                }
            }
            // Log explícito de cuántas regex quedaron activas - para poder
            // confirmar desde consola que el archivo se leyó COMPLETO (todas
            // las líneas no vacías/no comentario) y no solo una parte.
            Bukkit.getLogger().info("[CoreEC] " + fileName + " cargado: " + patterns.size() + " regex activas"
                    + (ignoredLines > 0 ? " (" + ignoredLines + " líneas con error de sintaxis, ver warnings arriba)" : "") + ".");
            return patterns;
        } catch (java.io.IOException e) {
            Bukkit.getLogger().warning("[CoreEC] No se pudo leer " + fileName + ": " + e.getMessage());
            return java.util.List.of();
        }
    }

    /** Si aiModeration está activo y reviewWords sigue vacía (nadie la llenó a mano), le pide a Gemini que la genere UNA vez al arrancar - no hace falta que el dueño del server escriba la lista. */
    private void maybeAutoGenerateReviewWords() {
        FiltersConfig.AiModeration config = plugin.getConfigManager().getFiltersConfig().aiModeration;
        if (!config.enabled || !config.reviewWords.isEmpty()) return;
        if (config.apiKey == null || config.apiKey.isBlank()) return;
        generateReviewWordsAsync(null);
    }

    /**
     * Genera reviewWords vía Gemini y las guarda en filters.yml - usado tanto
     * por la generación automática al arrancar como por /mincore ai refreshwords.
     * requester puede ser null (disparo automático, sin nadie a quien avisar).
     */
    public void generateReviewWordsAsync(org.bukkit.command.CommandSender requester) {
        if (!generatingReviewWords.compareAndSet(false, true)) {
            if (requester != null) {
                requester.sendMessage(TextUtils.format("<red>Ya hay una generación de reviewWords en curso, esperá a que termine."));
            }
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            java.util.List<String> words;
            try {
                words = aiModerationClient.generateReviewWords();
            } finally {
                generatingReviewWords.set(false);
            }

            java.util.List<String> finalWords = words;
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                if (finalWords.isEmpty()) {
                    Bukkit.getLogger().warning("[CoreEC] [IA] No se pudo generar reviewWords (ver warnings de Gemini arriba).");
                    if (requester != null) {
                        requester.sendMessage(TextUtils.format("<red>No se pudo generar la lista - revisá la consola."));
                    }
                    return;
                }

                FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
                filters.aiModeration.reviewWords = new java.util.ArrayList<>(finalWords);
                filters.save();
                reload();

                Bukkit.getLogger().info("[CoreEC] [IA] reviewWords generadas automáticamente (" + finalWords.size() + " palabras) y guardadas en filters.yml.");
                if (requester != null) {
                    requester.sendMessage(TextUtils.format("<green>Se generaron " + finalWords.size() + " palabras de revisión y se guardaron en filters.yml."));
                }
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
                requester.sendMessage(TextUtils.format("<red>Ya hay una generación de badWords en curso, esperá a que termine."));
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
                if (finalNew.isEmpty()) {
                    Bukkit.getLogger().warning("[CoreEC] [IA] No se pudo generar el blocklist de regex (ver warnings de Gemini arriba).");
                    if (requester != null) {
                        requester.sendMessage(TextUtils.format("<red>No se pudo generar el blocklist - revisá la consola."));
                    }
                    return;
                }

                FiltersConfig filters = plugin.getConfigManager().getFiltersConfig();
                java.io.File file = new java.io.File(plugin.getDataFolder(), filters.badWords.regexFile);
                int added = appendRegexLines(file, finalNew);
                reload();

                String summary = "Gemini devolvió " + finalNew.size() + " regex, " + added + " eran nuevas (el resto ya estaban) - "
                        + badWordsRegexList.size() + " en total en " + filters.badWords.regexFile + ".";
                Bukkit.getLogger().info("[CoreEC] [IA] " + summary);
                if (requester != null) {
                    requester.sendMessage(TextUtils.format("<green>" + summary));
                }
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
            Bukkit.getLogger().warning("[CoreEC] No se pudo escribir " + file.getName() + ": " + e.getMessage());
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

        return combined == null ? null : Pattern.compile("\\b(?:" + combined + ")\\b", Pattern.CASE_INSENSITIVE);
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
            return FilterResult.cancelled(CancelReason.TOO_LONG);
        }

        if (filters.floodProtection.enabled && isFlooding(uuid, filters.floodProtection)) {
            return FilterResult.cancelled(CancelReason.FLOOD);
        }

        if (filters.antiSpam.enabled && isSpamming(uuid, filters.antiSpam)) {
            return FilterResult.cancelled(CancelReason.SPAM);
        }

        String message = rawMessage;

        if (filters.caps.enabled) {
            message = applyCapsFilter(message, filters.caps);
        }

        if (filters.repetition.enabled) {
            if (isRepetition(uuid, message, filters.repetition)) {
                return FilterResult.cancelled(CancelReason.REPETITION);
            }
            message = collapseRepeatedChars(message, filters.repetition.repeatedCharThreshold, customCharLimits);
        }

        boolean infraction = false;
        Pattern pattern = badWordsPattern;
        if (filters.badWords.enabled && pattern != null) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                if (!filters.badWords.replaceWords) {
                    return FilterResult.cancelled(CancelReason.BAD_WORD);
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
                String replaced = regex.matcher(message).replaceAll(result -> {
                    if (whitelistWordsLower.contains(result.group().toLowerCase())) return result.group();
                    hit[0] = true;
                    return "*".repeat(result.group().length());
                });
                if (!hit[0]) continue;

                if (!filters.badWords.replaceWords) {
                    return FilterResult.cancelled(CancelReason.BAD_WORD);
                }
                message = replaced;
                infraction = true;
            }
        }

        if (filters.ads.enabled && containsDisallowedLink(message, filters.ads)) {
            return FilterResult.cancelled(CancelReason.ADS);
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
        java.util.Set<String> words = reviewWordsNormalized;
        if (words.isEmpty()) return false;

        String normalized = normalizeChatSentinelStyle(message);
        for (String token : normalized.split(" ")) {
            if (words.contains(token)) return true;
        }
        return false;
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
     * El mensaje ya se mostró (o está por mostrarse) normalmente; si Gemini
     * confirma que es tóxico, se borra de la pantalla de todos, se avisa al
     * infractor y se alerta al staff. signedMessage puede ser null (cliente
     * sin firma de chat) - en ese caso no hay forma de borrar el mensaje, así
     * que se omite la revisión entera.
     */
    public void reviewAsync(Player player, String message, SignedMessage signedMessage) {
        FiltersConfig.AiModeration config = plugin.getConfigManager().getFiltersConfig().aiModeration;
        if (!config.enabled || !matchesReviewWord(message)) return;

        // Caso silencioso que costaba diagnosticar: si el cliente no manda
        // chat firmado (proxy sin forward de firma, cliente modificado,
        // etc.) no hay forma de borrar el mensaje después - antes esto
        // simplemente no hacía nada sin ningún rastro en consola.
        if (signedMessage == null) {
            Bukkit.getLogger().warning("[CoreEC] " + player.getName() + " escribió una palabra de revisión pero su cliente no mandó chat firmado - no se puede revisar/borrar ese mensaje.");
            return;
        }

        java.util.UUID uuid = player.getUniqueId();
        if (aiModerationClient.isOnCooldown(uuid)) return;

        java.util.List<String> context = recentContext(uuid, config.contextMessages);
        if (config.apiKey == null || config.apiKey.isBlank()) {
            Bukkit.getLogger().warning("[CoreEC] filters.yml -> aiModeration.enabled está en true pero apiKey está vacía - no se puede consultar a Gemini.");
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            boolean toxic = aiModerationClient.isToxic(player, message, context);
            if (!toxic) return;

            Bukkit.getGlobalRegionScheduler().run(plugin, t -> handleToxicMessage(player, message, signedMessage));
        });
    }

    private void handleToxicMessage(Player player, String message, SignedMessage signedMessage) {
        Bukkit.getServer().deleteMessage(signedMessage);

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
