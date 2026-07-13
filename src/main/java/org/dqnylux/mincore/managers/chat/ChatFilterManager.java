package org.dqnylux.mincore.managers.chat;

import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.FiltersConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline de filtros de chat: anti-spam, mayúsculas, repetición, palabras
 * prohibidas y anuncios/enlaces. Se salta por completo si el jugador tiene
 * mincore.chat.bypass (verificado por el llamador, no aquí).
 */
public class ChatFilterManager {

    public enum CancelReason {
        SPAM, REPETITION, BAD_WORD, ADS
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
            'a', "[a4@]", 'e', "[e3]", 'i', "[i1!]", 'o', "[o0]", 's', "[s5$]"
    );

    private final Mincore plugin;
    private final Map<java.util.UUID, Deque<Long>> messageTimestamps = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, String> lastMessage = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Integer> lastMessageRepeats = new ConcurrentHashMap<>();
    private volatile Pattern badWordsPattern;

    public ChatFilterManager(Mincore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FiltersConfig.BadWords badWords = plugin.getConfigManager().getFiltersConfig().badWords;
        if (badWords.words.isEmpty()) {
            this.badWordsPattern = null;
            return;
        }

        String combined = badWords.words.stream()
                .map(this::toFuzzyRegex)
                .reduce((a, b) -> a + "|" + b)
                .orElse(null);

        this.badWordsPattern = combined == null ? null : Pattern.compile("\\b(?:" + combined + ")\\b", Pattern.CASE_INSENSITIVE);
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
            message = collapseRepeatedChars(message, filters.repetition.repeatedCharThreshold);
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

        if (filters.ads.enabled && containsDisallowedLink(message, filters.ads.whitelist)) {
            return FilterResult.cancelled(CancelReason.ADS);
        }

        lastMessage.put(uuid, message);
        return FilterResult.allowed(message, infraction);
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

    private String applyCapsFilter(String message, FiltersConfig.Caps config) {
        if (message.length() < config.minLength) return message;

        long upperCount = message.chars().filter(Character::isUpperCase).count();
        long letterCount = message.chars().filter(Character::isLetter).count();
        if (letterCount == 0) return message;

        double ratio = (double) upperCount / letterCount;
        return ratio >= config.maxPercentage ? message.toLowerCase() : message;
    }

    private boolean isRepetition(java.util.UUID uuid, String message, FiltersConfig.Repetition config) {
        String previous = lastMessage.get(uuid);
        if (previous == null) return false;

        String normalized = message.trim().toLowerCase();
        String previousNormalized = previous.trim().toLowerCase();

        if (normalized.length() <= config.shortMessageMaxLength) {
            if (normalized.equals(previousNormalized)) {
                int repeats = lastMessageRepeats.merge(uuid, 1, Integer::sum);
                return repeats >= config.shortMessageMaxRepeats - 1;
            }
            lastMessageRepeats.put(uuid, 0);
            return false;
        }

        double similarity = similarity(normalized, previousNormalized);
        return similarity >= config.longMessageSimilarityThreshold;
    }

    private String collapseRepeatedChars(String message, int threshold) {
        if (threshold < 2) return message;
        Pattern repeated = Pattern.compile("(.)\\1{" + (threshold - 1) + ",}");
        return repeated.matcher(message).replaceAll(result -> result.group(1).repeat(2));
    }

    private boolean containsDisallowedLink(String message, java.util.List<String> whitelist) {
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
