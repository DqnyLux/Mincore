package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;

import java.util.Arrays;
import java.util.List;

@Include(MincoreConfig.class)
public class ChatFormatConfig extends MincoreConfig {

    public Chat chat = new Chat();
    public InteractiveItem interactiveItem = new InteractiveItem();
    public Mentions mentions = new Mentions();

    public static class Chat extends MincoreConfig {
        @Comment("Texto insertado ENTRE cada parte (prefix/name/icon/arrow/message) del formato de chat. Vacío = las partes van pegadas, sin separador propio (la mayoría de partes ya trae su propio espaciado, ej. arrow).")
        public String partSeparator = "";

        public Parts parts = new Parts();
    }

    public static class Parts extends MincoreConfig {
        @Comment({"Placeholders externos (economía, kills, clan...) antes del prefijo real.",
                "%luckperms_prefix% requiere LuckPerms+PlaceholderAPI - el cosmético de \"prefixes\" ya escribe ahí vía LuckPerms si está instalado."})
        public Part prefix = new Part("&#a8ff78💵&#a8ff78$%vault_eco_balance_formatted%&8| &#FF4C4C⚔&#FF4C4C%statistic_player_kills%&8| &#00c6ff⛺%justteams_team_name% %luckperms_prefix%");

        public NamePart name = new NamePart();

        @Comment("%luckperms_suffix% requiere LuckPerms+PlaceholderAPI - el cosmético de \"icons\" ya escribe ahí vía LuckPerms si está instalado.")
        public Part icon = new Part("%luckperms_suffix%");

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
                "           &#a8ff78✦ Sᴜʀᴠɪᴠᴀʟ ✦",
                " ",
                "&#55FF55ℹ Iɴꜰᴏʀᴍᴀᴄɪóɴ&f:",
                "&8 • &7Nᴏᴍʙʀᴇ&f: &#00c6ff%player_name%",
                "&8 • &7Cʟᴀɴ&f: &8[&#00c6ff%justteams_team_name%&8]",
                " ",
                "&#55FF55🕐 Eꜱᴛᴀᴅíꜱᴛɪᴄᴀꜱ&f:",
                "&8 • &#FFD976⏳ &7Hᴏʀᴀꜱ&f: &#00c6ff%statistic_hours_played%h",
                "&8 • &#FF4C4C⚔ &7Kɪʟʟꜱ&f: &#FF4C4C%statistic_player_kills%",
                "&8 • &#a8caba💀 &7Mᴜᴇʀᴛᴇꜱ&f: &#a8caba%statistic_deaths%",
                " ",
                "<#55FF55>💰 Eᴄᴏɴᴏᴍíᴀ&f",
                "&8 • &#a8ff78💵 &7Dɪɴᴇʀᴏ&f: &#a8ff78$%vault_eco_balance_formatted%",
                "&8 • &#00c6ff💎 &7Gᴇᴍᴀꜱ&f: &#00c6ff%playerpoints_points_formatted%",
                "&8 • &#FFD700🎫 &7Pᴀꜱᴇ ᴅᴇ B.&f: &#FFD700Nivel %battlepass_tier%",
                " ",
                "&#55FF55✉ Eɴᴠíᴀ ᴜɴ ᴍᴇɴꜱᴀᴊᴇ ᴘʀɪᴠᴀᴅᴏ ✉"
        );

        @Comment("Comando que se autocompleta en la barra de chat al hacer clic sobre el nombre. Vacío = desactivado.")
        public String suggest = "/msg %player_name% ";
    }

    public static class InteractiveItem extends MincoreConfig {
        @Comment("Literal que, en cualquier mensaje de chat, se reemplaza por un componente hoverable mostrando el ítem que el jugador tiene en la mano. Vacío = desactivado.")
        public String trigger = "[item]";
        public String emptyHand = "<#AAAAAA>[Mano Vacía]</#AAAAAA>";
    }

    public static class Mentions extends MincoreConfig {
        @Comment("¿Activar la detección y resaltado de menciones? No hace falta escribir @, con el nombre del jugador alcanza (aunque @nombre también funciona).")
        public boolean enabled = true;

        @Comment("Color/nombre plano (sin < >, ej. #FFD700 o yellow) - el código arma <color:VALOR>...</color> alrededor de cada mención resaltada, para poder anidarla sin cortar el color del chat que la rodea (ver ChatFormatHandler#highlightMentions).")
        public String highlightColor = "#FFD700";

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
        public String text = "<#555555>» <#FFD976>¡<#FFFFFF>%player% <#FFD976>te ha mencionado en el chat! <#555555>«";
    }

    public static class TitleSection extends MincoreConfig {
        @Comment("¿Mostrar un título/subtítulo en pantalla al jugador mencionado?")
        public boolean enabled = true;
        public String main = "<#FFD700>¡Mᴇɴᴄɪᴏɴᴀᴅᴏ!";
        public String sub = "<#AAAAAA>El jugador <#FFFFFF>%player% <#AAAAAA>te está buscando";
        public int fadeInTicks = 10;
        public int stayTicks = 40;
        public int fadeOutTicks = 10;
    }

    public static class BossbarSection extends MincoreConfig {
        @Comment("¿Mostrar una bossbar temporal al jugador mencionado?")
        public boolean enabled = true;
        public String text = "<#FFD976>Mᴇɴᴄɪᴏɴ ᴅᴇ: <#FFFFFF>%player%";
        @Comment("PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE.")
        public String color = "YELLOW";
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
        public String message = "<#FFD976>¡Alguien te mencionó en el chat!";
    }
}
