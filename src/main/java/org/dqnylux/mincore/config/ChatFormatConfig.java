package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Include(MincoreConfig.class)
public class ChatFormatConfig extends MincoreConfig {

    public Chat chat = new Chat();
    public InteractiveItem interactiveItem = new InteractiveItem();
    public Mentions mentions = new Mentions();
    public DeletionButton deletionButton = new DeletionButton();

    @Comment("Reemplazo automático de texto simple por emojis en el CUERPO del mensaje (ej. escribir <3 muestra ❤) - se aplica antes de todo lo demás, así que de paso un shortcode con forma de tag roto (como <3) nunca llega a MiniMessage. Agregá/sacá entradas libremente, no hace falta reiniciar (/coreec reload alcanza).")
    public boolean emoticonsEnabled = true;
    public Map<String, String> emoticons = defaultEmoticons();

    private static Map<String, String> defaultEmoticons() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("<3", "❤");
        map.put("</3", "💔");
        map.put(":)", "🙂");
        map.put(":(", "🙁");
        map.put(":D", "😄");
        map.put(":P", "😛");
        map.put(";)", "😉");
        map.put(":'(", "😢");
        map.put("xd", "😂");
        map.put("XD", "😂");
        return map;
    }

    public static class Chat extends MincoreConfig {
        @Comment("Texto insertado ENTRE cada parte (prefix/name/icon/arrow/message) del formato de chat. Vacío = las partes van pegadas, sin separador propio (la mayoría de partes ya trae su propio espaciado, ej. arrow).")
        public String partSeparator = "";

        public Parts parts = new Parts();
    }

    public static class Parts extends MincoreConfig {
        @Comment({"Placeholders externos (economía, kills, clan...) antes del prefijo real.",
                "%luckperms_prefix% requiere LuckPerms+PlaceholderAPI - es SIEMPRE el prefijo real del rango, nunca lo pisa un cosmético.",
                "%coreec_prefix% es el \"tag\" cosmético equipado (categoría \"prefixes\", si tiene uno) - se agrega DESPUÉS del prefijo de rango, nunca lo reemplaza (antes el cosmético escribía directo en LuckPerms y tapaba el prefijo real en cualquier otro plugin que leyera ese mismo placeholder, TAB Reborn incluido)."})
        public Part prefix = new Part("&#FF2A2A💵&#FF2A2A$%vault_eco_balance_formatted%&8| &#FF4D4D⚔&#FF4D4D%statistic_player_kills%&8| &#8B0000⛺%justteams_team_name% %luckperms_prefix%%coreec_prefix%");

        public NamePart name = new NamePart();

        @Comment("%luckperms_suffix% requiere LuckPerms+PlaceholderAPI - es SIEMPRE el sufijo real del rango. %coreec_icon% es el ícono cosmético equipado (categoría \"icons\", si tiene uno) - se agrega DESPUÉS, nunca reemplaza el sufijo real (mismo motivo que %coreec_prefix% arriba).")
        public Part icon = new Part("%luckperms_suffix%%coreec_icon%");

        public Part arrow = new Part(" <#555555>» ");

        @Comment("Color aplicado al cuerpo del mensaje. %chatcolor% toma el cosmético equipado en \"chatcolors\". El texto del mensaje en sí siempre se agrega después, nunca aquí (protección anti-inyección).")
        public Part message = new Part("%chatcolor%");
    }

    /** Parte simple de un solo campo (icon/arrow/message/prefix) - envuelta en objeto para dejar espacio a futuros sub-campos sin romper el YAML otra vez. */
    public static class Part extends MincoreConfig {
        public String text;

        public Part() {
        }

        public Part(String text) {
            this.text = text;
        }
    }

    /** El nombre es la única parte con hover/clic - por eso tiene su propia clase en vez de reusar Part. */
    public static class NamePart extends MincoreConfig {
        @Comment("%player_name% y %namecolor% los resuelve Mincore directamente (no requieren PlaceholderAPI). %namecolor% toma el valor del cosmético equipado en la categoría \"namecolors\".")
        public String text = "%namecolor%%player_name%";

        @Comment("Tooltip al pasar el mouse sobre el nombre del jugador en el chat - una línea por entrada, soporta placeholders de PAPI. Lista vacía = sin hover.")
        public List<String> hover = Arrays.asList(
                "           <gradient:#FF2A2A:#8B0000><bold>✦ RIFT MYSTIC ✦</bold></gradient>",
                " ",
                "&#FF2A2Aℹ <#EAEAEA>Iɴꜰᴏʀᴍᴀᴄɪóɴ&f:",
                "&8 • &7Nᴏᴍʙʀᴇ&f: &#FF4D4D%player_name%",
                "&8 • &7Cʟᴀɴ&f: &8[&#8B0000%justteams_team_name%&8]",
                " ",
                "&#FF2A2A🕐 <#EAEAEA>Eꜱᴛᴀᴅíꜱᴛɪᴄᴀꜱ&f:",
                "&8 • &#FF4D4D⏳ &7Hᴏʀᴀꜱ&f: &#EAEAEA%statistic_hours_played%h",
                "&8 • &#FF2A2A⚔ &7Kɪʟʟꜱ&f: &#FF2A2A%statistic_player_kills%",
                "&8 • &#8B0000💀 &7Mᴜᴇʀᴛᴇꜱ&f: &#8B0000%statistic_deaths%",
                " ",
                "<#FF2A2A>💰 <#EAEAEA>Eᴄᴏɴᴏᴍíᴀ&f",
                "&8 • &#FF4D4D💵 &7Dɪɴᴇʀᴏ&f: &#FF4D4D$%vault_eco_balance_formatted%",
                "&8 • &#FF2A2A💎 &7Gᴇᴍᴀꜱ&f: &#FF2A2A%playerpoints_points_formatted%",
                "&8 • &#8B0000🎫 &7Pᴀꜱᴇ ᴅᴇ B.&f: &#8B0000Nivel %battlepass_tier%",
                " ",
                "<gradient:#FF2A2A:#8B0000>✉ Clic para enviar mensaje privado ✉</gradient>"
        );

        @Comment("Comando que se autocompleta en la barra de chat al hacer clic sobre el nombre. Vacío = desactivado.")
        public String suggest = "/msg %player_name% ";
    }

    public static class InteractiveItem extends MincoreConfig {
        @Comment("Literal que, en cualquier mensaje de chat, se reemplaza por un componente hoverable mostrando el ítem que el jugador tiene en la mano. Vacío = desactivado.")
        public String trigger = "[item]";
        public String emptyHand = "<#888888>[Mano Vacía]</#888888>";
    }

    public static class Mentions extends MincoreConfig {
        @Comment("¿Activar la detección y resaltado de menciones? No hace falta escribir @, con el nombre del jugador alcanza (aunque @nombre también funciona).")
        public boolean enabled = true;

        @Comment("Color/nombre plano (sin < >, ej. #FFD700 o yellow) - el código arma <color:VALOR>...</color> alrededor de cada mención resaltada, para poder anidarla sin cortar el color del chat que la rodea (ver ChatFormatHandler#highlightMentions).")
        public String highlightColor = "#FF2A2A";

        @Comment("Sonido reproducido al jugador mencionado - acepta el nombre vanilla (ej. block.note_block.pling) o el de Bukkit/XSound. Vacío = sin sonido.")
        public String sound = "block.note_block.pling";

        public Actionbar actionbar = new Actionbar();
        public TitleSection title = new TitleSection();
        public BossbarSection bossbar = new BossbarSection();
        public ToastSection toast = new ToastSection();
    }

    public static class Actionbar extends MincoreConfig {
        @Comment("¿Mostrar un actionbar al jugador mencionado?")
        public boolean enabled = true;
        public String text = "<#555555>» <#FF2A2A>¡<#FFFFFF>%player% <#FF2A2A>te ha mencionado en el chat! <#555555>«";
    }

    public static class TitleSection extends MincoreConfig {
        @Comment("¿Mostrar un título/subtítulo en pantalla al jugador mencionado?")
        public boolean enabled = true;
        public String main = "<gradient:#FF2A2A:#8B0000><bold>¡MENCIÓN ABISAL!</bold></gradient>";
        public String sub = "<#888888>El invocador <#FFFFFF>%player% <#888888>te ha nombrado";
        public int fadeInTicks = 10;
        public int stayTicks = 40;
        public int fadeOutTicks = 10;
    }

    public static class BossbarSection extends MincoreConfig {
        @Comment("¿Mostrar una bossbar temporal al jugador mencionado?")
        public boolean enabled = true;
        public String text = "<gradient:#FF2A2A:#8B0000>Mención del Abismo: <#FFFFFF>%player%</gradient>";
        @Comment("PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE.")
        public String color = "RED";
        @Comment("PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20.")
        public String overlay = "PROGRESS";
        @Comment("Duración en segundos.")
        public int durationSeconds = 5;
    }

    public static class ToastSection extends MincoreConfig {
        @Comment({
                "¿Mostrar un toast (aviso estilo logro) al jugador mencionado?",
                "NOTA: por una limitación de Minecraft, icon/frame/message solo se",
                "aplican al arrancar el servidor (cambiarlos en caliente exigiría recargar",
                "todo el datapack) - si los editas, reinicia el servidor."
        })
        public boolean enabled = true;
        public String icon = "BELL";
        @Comment("TASK, GOAL o CHALLENGE.")
        public String frame = "GOAL";
        public String message = "<#FF2A2A>¡Invocado por %player% en el chat!";
    }

    public static class DeletionButton extends MincoreConfig {
        @Comment("¿Habilitar el botón interactivo de eliminación de mensajes para el staff? (Estilo LPC Pro)")
        public boolean enabled = true;

        @Comment("Permiso requerido para ver y usar el botón de borrado.")
        public String permission = "mincore.chat.delete";

        @Comment("Texto/formato del botón interactivo. %id% es el ID de 8 caracteres del mensaje.")
        public String format = "<#8B0000>[<#FF2A2A>✕<#8B0000>]</#8B0000> ";

        @Comment("Tooltip que aparece al pasar el cursor sobre el botón [✕].")
        public String hover = "<#FF2A2A>✦ <#EAEAEA>Clic para eliminar este mensaje del chat <#555555>(ID: #%id%)";

        @Comment("Comando que se ejecuta al hacer clic sobre el botón. %id% se reemplaza por el ID del mensaje.")
        public String command = "/deletemsg %id%";

        @Comment("Límite de mensajes por jugador guardados en el búfer histórico (por defecto 100).")
        public int historyLimit = 100;

        @Comment("Líneas en blanco enviadas al cliente para limpiar la pantalla antes de re-renderizar el historial intacto.")
        public int clearLinesCount = 100;
    }
}
