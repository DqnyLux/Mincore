package org.dqnylux.mincore.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern RAW_HEX_PATTERN = Pattern.compile("(?<![<&#§:/])#([a-fA-F0-9]{6})");
    private static final Pattern CENTER_PATTERN = Pattern.compile("<center>(.*?)</center>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACEHOLDER_TOKEN = Pattern.compile("%[A-Za-z0-9_]+%");

    /** &-code + &#RRGGBB hex (mismo formato que HEX_PATTERN ya sabe leer) - lo que esperan plugins externos (LuckPerms, TAB, Vault) que no entienden MiniMessage. */
    private static final LegacyComponentSerializer LEGACY_AMPERSAND_HEX = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final int CHAT_CENTER_PIXELS = 154;

    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        if (text.indexOf("<center>") != -1 || text.indexOf("<CENTER>") != -1) {
            Matcher centerMatcher = CENTER_PATTERN.matcher(text);
            StringBuilder centerSb = new StringBuilder();
            while (centerMatcher.find()) {
                centerMatcher.appendReplacement(centerSb, centerText(centerMatcher.group(1)));
            }
            centerMatcher.appendTail(centerSb);
            text = centerSb.toString();
        }

        text = legacyToMiniMessageTags(text);

        if (!text.endsWith("<reset>")) {
            text += "<reset>";
        }

        return safeDeserialize(MINI_MESSAGE, text);
    }

    /**
     * BUG REAL encontrado (causa raíz más probable de "de la nada no puedo
     * mandar mensajes de chat", sin ningún error visible): MiniMessage.
     * deserialize() tira ParsingException (RuntimeException sin capturar en
     * ningún lado del pipeline) ante CUALQUIER "<...>" que no reconozca como
     * tag válido - y esto corre sobre texto que el JUGADOR escribe libremente
     * en el chat (formatSafeChat, ver más abajo), donde cosas de lo más
     * normales (emoticones "<3"/">.<", flechas ASCII "<--", "<->", pegar código/
     * markup, mencionar "<player>" como placeholder de ejemplo) matchean el
     * patrón de un tag sin serlo. Como nada en ChatListener.onChat() envolvía
     * esta llamada, la excepción se escapaba a mitad del pipeline (antes de
     * event.renderer(...)) - Bukkit la loguea a consola, pero de forma tan
     * genérica ("Could not pass event AsyncChatEvent to CoreEC") que se
     * perdía entre el resto del log, y el jugador nunca veía ningún aviso.
     * Fallback: se muestra el texto TAL CUAL lo escribió, sin ningún tag
     * interpretado, en vez de perder el mensaje/la función completa.
     */
    private static Component safeDeserialize(MiniMessage parser, String text) {
        try {
            return parser.deserialize(text).decoration(TextDecoration.ITALIC, false);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CoreEC] No se pudo interpretar como MiniMessage (se muestra tal cual): \"" + text + "\" - " + e.getMessage());
            return Component.text(text);
        }
    }

    /**
     * Convierte códigos legacy (&0-&f, &#RRGGBB, &l/&o/&n/&m/&k, &r) a tags
     * MiniMessage - sin tocar &lt;center&gt; ni resolver placeholders, eso lo
     * hace format() antes/después de llamar a esto. Expuesto públicamente
     * (no solo usado internamente por format()) para que el chat pueda
     * convertir el mensaje CRUDO de un jugador con permiso antes de pasarlo
     * por formatSafeChat() - ese parser solo entiende tags MiniMessage
     * nativos (&lt;red&gt;, &lt;#RRGGBB&gt;), nunca "&amp;a"/"&amp;#RRGGBB"
     * crudos, así que sin esta conversión un jugador con coreec.chat.color/
     * coreec.chat.format vería sus propios códigos como texto literal en vez
     * de color real.
     *
     * Vanilla legacy formatting: elegir un color SIEMPRE apaga la negrita/
     * cursiva/subrayado/tachado/obfuscado activos (a diferencia de
     * MiniMessage, donde &lt;color&gt; y &lt;bold&gt; son tags independientes
     * que no se cancelan entre sí). Por eso cada código de color legacy
     * -incluido hex- se traduce anteponiendo &lt;reset&gt; antes del propio
     * tag de color: &lt;reset&gt;&lt;color&gt; primero limpia decoraciones
     * activas y LUEGO fija el color nuevo, replicando el comportamiento real
     * de vanilla sin depender de que quien escribe el texto (ej. un prefix
     * de LuckPerms, o un jugador en el chat) cierre sus propios códigos con &r.
     */
    /** Paleta RGB propia del proyecto para los códigos &0-&f (índice = valor hex del carácter: 0-9,a-f) - conserva los colores pastel que &1/&a/etc ya daban antes. */
    private static final String[] LEGACY_COLORS = {
            "black", "#2A3B99", "#2E8B22", "#00B8B8", "#B22222", "#8A2BE2",
            "#FFD700", "#AAAAAA", "#555555", "#4C6FFF", "#5CE65C", "#55FFFF",
            "#FF4C4C", "#FF6EFF", "#FFEB3B", "#FFFFFF"
    };

    /** &x&F&F&4&C&4&C - la otra notación RGB de vanilla/BungeeCord (un & por cada dígito hex, tras el &x). Se expande a &#RRGGBB antes de tocar los códigos de un solo &. */
    private static final Pattern HEX_X_PATTERN = Pattern.compile("(?i)&x(?:&[0-9a-f]){6}");

    public static String legacyToMiniMessageTags(String text) {
        if (text == null || text.isEmpty()) return text;

        // Unificar § a & para soportar codificación nativa de Minecraft y clientes
        text = text.replace('§', '&');

        // &x&R&R... -> &#RRGGBB PRIMERO (cualquier minúscula/mayúscula), para
        // que el bloque de abajo lo convierta a tag MiniMessage en el mismo paso.
        text = HEX_X_PATTERN.matcher(text).replaceAll(match -> "&#" + match.group().replaceAll("(?i)&x|&", ""));

        if (text.indexOf("&#") != -1) {
            Matcher hexMatcher = HEX_PATTERN.matcher(text);
            StringBuilder hexSb = new StringBuilder();
            while (hexMatcher.find()) {
                hexMatcher.appendReplacement(hexSb, "<reset><#" + hexMatcher.group(1) + ">");
            }
            hexMatcher.appendTail(hexSb);
            text = hexSb.toString();
        }

        if (text.indexOf('#') != -1) {
            Matcher rawHexMatcher = RAW_HEX_PATTERN.matcher(text);
            if (rawHexMatcher.find()) {
                text = RAW_HEX_PATTERN.matcher(text).replaceAll("<reset><#$1>");
            }
        }

        // Recorrido carácter a carácter (a diferencia de la cadena vieja de
        // .replace()): los códigos se reconocen case-insensitive (&A = &a), y
        // un & que no abre ningún código (ej. "100% & ventas") se conserva
        // literal en vez de comerse el carácter que le sigue.
        StringBuilder result = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                int digit = Character.digit(code, 16); // 0-15 para 0-9 y a-f
                if (digit >= 0) {
                    result.append("<reset><").append(LEGACY_COLORS[digit]).append(">");
                    i += 2;
                    continue;
                }
                switch (code) {
                    case 'l': result.append("<bold>"); i += 2; continue;
                    case 'o': result.append("<italic>"); i += 2; continue;
                    case 'n': result.append("<underlined>"); i += 2; continue;
                    case 'm': result.append("<strikethrough>"); i += 2; continue;
                    case 'k': result.append("<obfuscated>"); i += 2; continue;
                    case 'r': result.append("<reset>"); i += 2; continue;
                    default: break;
                }
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    /**
     * Variante para el CUERPO del mensaje de chat que el jugador escribe: el
     * color de chat lo decide EXCLUSIVAMENTE el chatcolor cosmético equipado
     * (o el default si no tiene ninguno) - nunca lo que el jugador teclee a
     * mano, aunque tenga coreec.chat.color. A diferencia de
     * legacyToMiniMessageTags(), los códigos de color (&0-&f, &#RRGGBB) se
     * DESCARTAN sin dejar ningún tag (ni siquiera &lt;reset&gt;), porque el
     * cuerpo del mensaje va envuelto en el tag de color/gradiente del
     * cosmético (ver ChatFormatHandler#buildFinalChat) y un &lt;reset&gt; ahí
     * en medio cortaría ese gradiente para el resto del mensaje - bug real
     * reportado por el usuario. Los códigos de FORMATO (&l/&o/&n/&m/&k) sí
     * se mantienen, para que coreec.chat.format siga siendo útil. &r solo
     * apaga el formato activo (no el color, que igual nunca tocamos acá) -
     * se cierran los 5 tags de decoración explícitamente en vez de
     * &lt;reset&gt;, que si cortaría el chatcolor exterior.
     */
    public static String legacyFormatOnlyToMiniMessageTags(String text) {
        if (text == null || text.isEmpty()) return text;

        text = HEX_PATTERN.matcher(text).replaceAll("");
        text = text.replaceAll("&[0-9a-fA-F]", "");

        // &r solo puede cerrar las decoraciones REALMENTE abiertas hasta acá -
        // MiniMessage no ignora en silencio un &lt;/tag&gt; de cierre sin su
        // apertura correspondiente, lo muestra como texto literal (confirmado
        // con una prueba aparte). Por eso esto recorre carácter a carácter en
        // vez de encadenar .replace(), llevando registro de qué decoraciones
        // están activas para cerrar solo esas.
        StringBuilder result = new StringBuilder(text.length());
        boolean bold = false, italic = false, underlined = false, strikethrough = false, obfuscated = false;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                switch (code) {
                    case 'l': result.append("<bold>"); bold = true; i += 2; continue;
                    case 'o': result.append("<italic>"); italic = true; i += 2; continue;
                    case 'n': result.append("<underlined>"); underlined = true; i += 2; continue;
                    case 'm': result.append("<strikethrough>"); strikethrough = true; i += 2; continue;
                    case 'k': result.append("<obfuscated>"); obfuscated = true; i += 2; continue;
                    case 'r':
                        if (bold) result.append("</bold>");
                        if (italic) result.append("</italic>");
                        if (underlined) result.append("</underlined>");
                        if (strikethrough) result.append("</strikethrough>");
                        if (obfuscated) result.append("</obfuscated>");
                        bold = italic = underlined = strikethrough = obfuscated = false;
                        i += 2;
                        continue;
                    default:
                        break;
                }
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    // Antes se reconstruía este MiniMessage.builder() entero en CADA mensaje
    // de chat de CADA jugador - estático una sola vez, mismo resultado.
    private static final MiniMessage SAFE_CHAT_MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.rainbow())
                    .resolver(StandardTags.reset())
                    .build())
            .build();

    /**
     * Chat (cuerpo de mensaje del jugador) y /msg: parser restringido a tags
     * MiniMessage de color/decoración/gradiente/rainbow/reset (sin tags
     * peligrosos del chat). Ahora también convierte códigos legacy (&a,
     * &#RRGGBB, &x..., case-insensitive) ANTES de parsear - en el cuerpo del
     * chat normal los códigos de color ya se descartaron en
     * legacyFormatOnlyToMiniMessageTags (el color lo decide el chatcolor
     * cosmético), así que acá el único efecto visible es en /msg y en
     * cualquier config que apunte a este parser con colores legacy.
     */
    public static Component formatSafeChat(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return safeDeserialize(SAFE_CHAT_MINI_MESSAGE, legacyToMiniMessageTags(text));
    }

    public static Component format(Player player, String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        String parsedText = text;
        if (player != null && parsedText.indexOf('%') != -1
                && org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            // Doble pasada (misma referencia real, UnlimitedNametags ->
            // PAPIManager): el VALOR de un placeholder externo (ej. un
            // prefix de LuckPerms armado con meta que a su vez inserta OTRO
            // %placeholder%) puede traer un token sin resolver todavía -
            // una sola pasada lo deja literal en pantalla.
            parsedText = PlaceholderAPI.setPlaceholders(player, parsedText);
            parsedText = PlaceholderAPI.setPlaceholders(player, parsedText);
        }
        return format(parsedText);
    }

    /**
     * Envuelve cada "%placeholder%" que quede literal en el string (osea,
     * TODAVÍA sin resolver por PlaceholderAPI) en su propio &lt;reset&gt;
     * ...&lt;reset&gt;, ANTES de pasarlo a format(Player, String). El VALOR
     * de un placeholder externo (ej. %luckperms_prefix%) es texto que NO
     * controlamos - lo escribe el admin de LuckPerms/otro plugin, no el de
     * este proyecto - y muy seguido viene con códigos de formato legado sin
     * cerrar (un prefix "&6&l[VIP]" casi nunca trae un &r al final, porque
     * LuckPerms no lo exige). Sin aislar cada placeholder así, un formato
     * abierto (&l, &n, etc.) dentro del VALOR se filtraba a TODO lo que
     * viniera después en el mismo string una vez parseado como MiniMessage -
     * bug real confirmado tanto en el nametag propio como en cualquier
     * config que concatene varios placeholders en una sola línea/parte de
     * chat. Los propios %namecolor%/%player_name% (que si controlamos, ya
     * resueltos por string-replace antes de llegar acá) no matchean este
     * patrón porque ya no tienen forma de "%token%" para cuando se llama a
     * esto - solo protege lo que sigue siendo un placeholder literal.
     */
    public static String isolatePlaceholders(String text) {
        if (text == null || text.isEmpty()) return text;
        // Solo <reset> DESPUÉS del token, nunca antes: un <reset> previo
        // pisaba cualquier color puesto a propósito justo delante de un
        // placeholder (ej. "&#FF4C4C%statistic_player_kills%" - color rojo
        // pegado al placeholder, patrón intencional y común en chat_format.yml)
        // antes de que llegara a pintar nada - bug real reportado por el
        // usuario. El <reset> de DESPUÉS sigue siendo necesario (evita que el
        // VALOR del placeholder, ej. un prefix de LuckPerms con &l sin cerrar,
        // filtre formato hacia lo que sigue en la misma línea).
        return PLACEHOLDER_TOKEN.matcher(text).replaceAll(match -> match.group() + "<reset>");
    }

    public static String formatLegacy(String text) {
        if (text == null || text.isEmpty()) return "";
        Component comp = format(text);
        return LegacyComponentSerializer.legacySection().serialize(comp);
    }

    /**
     * Convierte MiniMessage a texto legacy con &-codes (y &#RRGGBB para hex) -
     * usado para que plugins externos que consumen prefijos/sufijos de
     * LuckPerms (tab list, nametags, Vault) muestren el color configurado
     * en vez del tag MiniMessage crudo. Mincore vuelve a leer ese mismo
     * &#RRGGBB al re-parsear vía PAPI (%luckperms_prefix%), así que el color
     * se mantiene también en el propio chat.
     */
    public static String toLegacyAmpersand(String text) {
        if (text == null || text.isEmpty()) return "";
        return LEGACY_AMPERSAND_HEX.serialize(format(text));
    }

    public static String stripColors(String text) {
        if (text == null) return "";
        return MINI_MESSAGE.stripTags(text).replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }

    private static final java.text.DecimalFormat AMOUNT_FORMAT = new java.text.DecimalFormat("#,##0.##", new java.text.DecimalFormatSymbols(java.util.Locale.US));

    public static String formatAmount(double amount) {
        if (!Double.isFinite(amount)) return "0";
        synchronized (AMOUNT_FORMAT) {
            return AMOUNT_FORMAT.format(amount);
        }
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