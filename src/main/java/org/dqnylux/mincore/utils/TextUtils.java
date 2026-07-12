package org.dqnylux.mincore.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern CENTER_PATTERN = Pattern.compile("<center>(.*?)</center>", Pattern.CASE_INSENSITIVE);

    private static final int CHAT_CENTER_PIXELS = 154;

    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        Matcher centerMatcher = CENTER_PATTERN.matcher(text);
        StringBuilder centerSb = new StringBuilder();
        while (centerMatcher.find()) {
            centerMatcher.appendReplacement(centerSb, centerText(centerMatcher.group(1)));
        }
        centerMatcher.appendTail(centerSb);
        text = centerSb.toString();

        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuilder hexSb = new StringBuilder();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexSb, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(hexSb);
        text = hexSb.toString();

        text = text.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>").replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");

        if (!text.endsWith("<reset>")) {
            text += "<reset>";
        }

        return MINI_MESSAGE.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static Component formatSafeChat(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        MiniMessage safeParser = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.color())
                        .resolver(StandardTags.decorations())
                        .resolver(StandardTags.gradient())
                        .resolver(StandardTags.rainbow())
                        .resolver(StandardTags.reset())
                        .build())
                .build();

        return safeParser.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static Component format(Player player, String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        String parsedText = text;
        if (player != null && org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            parsedText = PlaceholderAPI.setPlaceholders(player, parsedText);
        }
        return format(parsedText);
    }

    public static String formatLegacy(String text) {
        if (text == null || text.isEmpty()) return "";
        Component comp = format(text);
        return LegacyComponentSerializer.legacySection().serialize(comp);
    }

    public static String stripColors(String text) {
        if (text == null) return "";
        return MINI_MESSAGE.stripTags(text).replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }

    private static String centerText(String text) {
        boolean isBold = text.toLowerCase().contains("<b>") || text.toLowerCase().contains("<bold>")
                || text.contains("&l") || text.contains("§l");

        String raw = stripColors(text);
        int messagePxSize = 0;

        for (char c : raw.toCharArray()) {
            DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
            messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
            messagePxSize++;
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CHAT_CENTER_PIXELS - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;

        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }

        return sb.toString() + text;
    }

    private enum DefaultFontInfo {
        A('A', 5), a('a', 5), B('B', 5), b('b', 5), C('C', 5), c('c', 5), D('D', 5), d('d', 5),
        E('E', 5), e('e', 5), F('F', 5), f('f', 4), G('G', 5), g('g', 5), H('H', 5), h('h', 5),
        I('I', 3), i('i', 1), J('J', 5), j('j', 5), K('K', 5), k('k', 4), L('L', 5), l('l', 1),
        M('M', 5), m('m', 5), N('N', 5), n('n', 5), O('O', 5), o('o', 5), P('P', 5), p('p', 5),
        Q('Q', 5), q('q', 5), R('R', 5), r('r', 5), S('S', 5), s('s', 5), T('T', 5), t('t', 4),
        U('U', 5), u('u', 5), V('V', 5), v('v', 5), W('W', 5), w('w', 5), X('X', 5), x('x', 5),
        Y('Y', 5), y('y', 5), Z('Z', 5), z('z', 5), NUM_1('1', 5), NUM_2('2', 5), NUM_3('3', 5),
        NUM_4('4', 5), NUM_5('5', 5), NUM_6('6', 5), NUM_7('7', 5), NUM_8('8', 5), NUM_9('9', 5),
        NUM_0('0', 5), EXCLAMATION_POINT('!', 1), AT_SYMBOL('@', 6), NUM_SIGN('#', 5), DOLLAR_SIGN('$', 5),
        PERCENT('%', 5), UP_ARROW('^', 5), AMPERSAND('&', 5), ASTERISK('*', 5), LEFT_PARENTHESIS('(', 4),
        RIGHT_PARENTHESIS(')', 4), MINUS('-', 5), UNDERSCORE('_', 5), PLUS_SIGN('+', 5), EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 4), RIGHT_CURL_BRACE('}', 4), LEFT_BRACKET('[', 3), RIGHT_BRACKET(']', 3),
        COLON(':', 1), SEMI_COLON(';', 1), DOUBLE_QUOTE('"', 3), SINGLE_QUOTE('\'', 1), LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4), QUESTION_MARK('?', 5), SLASH('/', 5), BACK_SLASH('\\', 5), LINE('|', 1),
        TILDE('~', 5), TICK('`', 2), PERIOD('.', 1), COMMA(',', 1), SPACE(' ', 3), DEFAULT('a', 4);

        private final char character;
        private final int length;

        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }

        public char getCharacter() { return this.character; }
        public int getLength() { return this.length; }
        public int getBoldLength() {
            if (this == SPACE) return this.getLength();
            return this.length + 1;
        }

        public static DefaultFontInfo getDefaultFontInfo(char c) {
            for (DefaultFontInfo dFI : DefaultFontInfo.values()) {
                if (dFI.getCharacter() == c) return dFI;
            }
            return DefaultFontInfo.DEFAULT;
        }
    }

}