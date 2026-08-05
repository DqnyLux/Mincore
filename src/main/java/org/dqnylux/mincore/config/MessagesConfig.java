package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;

import java.util.Arrays;
import java.util.List;

@Include(MincoreConfig.class)
public class MessagesConfig extends MincoreConfig {

    public String prefix = "<#888888>[<#55FFFF>Mincore<#888888>] <#FFFFFF>";
    public Console console = new Console();
    public Commands commands = new Commands();
    public Chat chat = new Chat();
    public PrivateMessages privateMessages = new PrivateMessages();
    public Cosmetics cosmetics = new Cosmetics();
    public Menus menus = new Menus();
    public JoinQuit joinQuit = new JoinQuit();

    public static class Cosmetics extends MincoreConfig {
        public String noAccess = "<#FF4C4C>No tienes acceso a este cosmético.";
        @Comment("%cosmetic% = displayName del cosmético que se acaba de equipar (ej. \"Aura Carmesí\").")
        public String equipped = "<#5CE65C>Equipaste <#FFFFFF>%cosmetic%<#5CE65C>.";
        @Comment("%cosmetic% = displayName del cosmético que se acaba de desequipar.")
        public String unequipped = "<#FFEB3B>Desequipaste <#FFFFFF>%cosmetic%<#FFEB3B>.";
        public String categoryDisabled = "<#FF4C4C>Esta categoría de cosméticos está desactivada.";
        public List<String> statusEquipped = Arrays.asList(" ",
                "<#555555>▪ <#AAAAAA>Estado: <#5CE65C><bold>Eǫᴜɪᴘᴀᴅᴏ</bold>", " ", "<#5CE65C>♦ ¡Ya lo llevas puesto!");
        public List<String> statusClickToEquip = Arrays.asList(" ",
                "<#555555>▪ <#AAAAAA>Estado: <#FFD976><bold>Dᴇꜱʙʟᴏǫᴜᴇᴀᴅᴏ</bold>", " ", "<#FFD976>▶ Clic Izq. para equipar");
        public String priceLore = "<#888888>Precio: <#FFD700>%price%";

        @Comment("Lore dinámico de un cosmético bloqueado: varía según si al jugador le faltan monedas o ya puede comprarlo (%missing%/%price%).")
        public List<String> statusLockedMissingCoins = Arrays.asList(" ",
                "<#555555>▪ <#AAAAAA>Estado: <#FF4C4C><bold>Bʟᴏǫᴜᴇᴀᴅᴏ</bold>",
                "<#555555>▪ <#AAAAAA>Te faltan: <#FFD700>%missing%</#FFD700> monedas", " ", "<#FF4C4C>✖ No tienes suficiente saldo");
        public List<String> statusLockedCanBuy = Arrays.asList(" ",
                "<#555555>▪ <#AAAAAA>Estado: <#FF4C4C><bold>Bʟᴏǫᴜᴇᴀᴅᴏ</bold>",
                "<#555555>▪ <#AAAAAA>Precio: <#FFD700>$%price%", " ", "<#FF4C4C>▶ Clic Izq. para comprar");

        @Comment("Lore de un cosmético con permiso EXTRA (cosmetic.permission en el YAML, ej. formats.yml) que el jugador no tiene - a diferencia de statusLockedCanBuy/MissingCoins, esto NO es cuestión de plata, no hay nada que comprar.")
        public List<String> statusLockedNoPermission = Arrays.asList(" ",
                "<#555555>▪ <#AAAAAA>Estado: <#FF4C4C><bold>Bʟᴏǫᴜᴇᴀᴅᴏ</bold>",
                " ", "<#FF4C4C>✖ Necesitas un permiso especial");
        public String noCoins = "<#FF4C4C>No tienes suficientes monedas. Te faltan <#FFD700>%missing%</#FFD700>.";
        @Comment("%cosmetic% = displayName del cosmético comprado.")
        public String purchased = "<#5CE65C>Compraste y equipaste <#FFFFFF>%cosmetic%<#5CE65C>.";

        @Comment("Color del NOMBRE del ítem (no del lore) en la fila de formats.yml (bold/cursiva/...) según esté activo o no - independiente entre namecolors (nombre) y chatcolors (chat).")
        public String formatActiveColor = "#5CE65C";
        public String formatInactiveColor = "#888888";

        @Comment("Material del ítem de la fila de formats según su estado (verde prendido / gris apagado) - dejar vacío para usar siempre el material fijo de formats.yml.")
        public String formatActiveMaterial = "LIME_DYE";
        public String formatInactiveMaterial = "GRAY_DYE";

        @Comment("Ícono genérico para un cosmético BLOQUEADO (aún no comprado/desbloqueado) - un \"tinte gris\" en vez del ítem real, que solo se muestra una vez obtenido. Dejar vacío para desactivar y mostrar siempre el material real del YAML.")
        public String lockedMaterial = "GRAY_DYE";

        @Comment({
                "Previsualización (clic derecho en un cosmético del menú, sin equiparlo",
                "ni gastar monedas). glows/prefixes/icons quedan fuera - dependen de",
                "estado externo persistente (LuckPerms/scoreboard) que no conviene",
                "activar y desactivar solo por unos segundos."
        })
        public String previewHint = "<#00FBFF>▶ Clic derecho para previsualizar";
        public String cannotPreview = "<#FF4C4C>Este cosmético es visual, equípalo para verlo.";
        public String previewingEffect = "<#00FBFF>Previsualizando efecto...";
        public String previewingMessage = "<#00FBFF>Previsualizando mensaje de entrada:";
        public String previewNamecolorLine = "%color%%player% <#888888>» <#FFFFFF>¡Así se vería mi nombre en el chat!";
        public String previewChatcolorLine = "<#FFFFFF>%player% <#888888>» %color%¡Así se vería mi mensaje en el chat!";
        public String previewZoneEntering = "<#00FBFF>Llevándote a la zona de previsualización...";
    }

    public static class Menus extends MincoreConfig {
        public String mainTitle = "<bold><gradient:#ff5e62:#ff9966>Mincore Principal</gradient></bold>";
        public String categoriesTitle = "<bold>Mincore <#888888>» <#FFFFFF>Categorías";
        public String categoryTitle = "<bold>Mincore <#888888>» <#FFFFFF>%category%";
        public String categoryUnlockedLore = "<#888888>Desbloqueados: <#FFFFFF>%unlocked%/%total%";
        public String categoryEquippedLore = "<#888888>Equipado: <#5CE65C>%equipped%";
        public String categoryNothingEquipped = "<#888888>Nada equipado";

        @Comment("Nombre/lore/material de cada botón fijo del menú (perfil, cosméticos, paginación, volver...) ahora viven en su propio menus/*.yml, no aquí - esto solo queda para el estado ON/OFF de los toggles, que es dinámico.")
        public String toggleOn = "<#5CE65C>✔ Aᴄᴛɪᴠᴀᴅᴏ <#555555>▪ <#AAAAAA>Clic para apagar";
        public String toggleOff = "<#FF4C4C>✘ Dᴇꜱᴀᴄᴛɪᴠᴀᴅᴏ <#555555>▪ <#AAAAAA>Clic para encender";
        public String toggleLocked = "<#FF4C4C>✖ Bʟᴏǫᴜᴇᴀᴅᴏ <#555555>(Rango Requerido)";
    }

    public static class JoinQuit extends MincoreConfig {
        public String joinMessage = "<#888888>[<#5CE65C>+<#888888>] <#FFFFFF>%player%";
        public String quitMessage = "<#888888>[<#FF4C4C>-<#888888>] <#FFFFFF>%player%";

        @Comment({
                "%skinmotd_1% .. %skinmotd_8% = las 8 filas de la cara de la skin del jugador",
                "renderizada en bloques de color (ver modules.motdSkinFace en config.yml).",
                "%player% = nombre del jugador que entra."
        })
        public List<String> joinMotd = Arrays.asList(
                "<#555555><strikethrough>                                                  </strikethrough>",
                " ",
                "   %skinmotd_1%   <#55FFFF><b>Bienvenido a Mincore</b>",
                "   %skinmotd_2%   <#FFFFFF>Hola de nuevo, %player%",
                "   %skinmotd_3% ",
                "   %skinmotd_4%   <#888888>Usa <#FF66B2>/cosmetics <#888888>para personalizar tu perfil.",
                "   %skinmotd_5% ",
                "   %skinmotd_6% ",
                "   %skinmotd_7% ",
                "   %skinmotd_8% ",
                " ",
                "<#555555><strikethrough>                                                  </strikethrough>"
        );
    }

    public static class PrivateMessages extends MincoreConfig {
        public String playerOffline = "<#FF4C4C>Ese jugador no está conectado.";
        public String cannotMessageSelf = "<#FF4C4C>No puedes enviarte un mensaje a ti mismo.";
        public String targetDisabled = "<#FF4C4C>Ese jugador tiene los mensajes privados desactivados.";
        public String noReplyTarget = "<#FF4C4C>No tienes a quién responder.";
        public String senderFormat = "<#AAAAAA>Tú -> %target%: <#FFFFFF>";
        public String targetFormat = "<#AAAAAA>%sender% -> Tú: <#FFFFFF>";
    }

    public static class Chat extends MincoreConfig {
        public String filterSpam = "<#FF4C4C>Estás enviando mensajes muy rápido.";
        public String filterFlood = "<#FF4C4C>Esperá un poco antes de enviar otro mensaje.";
        public String filterTooLong = "<#FF4C4C>Tu mensaje es demasiado largo.";
        public String filterRepetition = "<#FF4C4C>No repitas el mismo mensaje.";
        public String filterBadWord = "<#FF4C4C>Tu mensaje contiene palabras no permitidas.";
        public String filterAds = "<#FF4C4C>No está permitido enviar enlaces o publicidad no autorizada.";
        public String aiMessageRemoved = "<#FF4C4C>Tu mensaje fue borrado por contenido tóxico.";
        @Comment("%player% = quién lo escribió, %message% = el mensaje borrado.")
        public String aiStaffAlert = "<#FFEB3B>[IA] <#FFFFFF>Se borró un mensaje de %player% por contenido tóxico: <#FFFFFF>%message%";
        public String injectionBlocked = "<#FF4C4C>%player% intentó inyectar código en el chat: <#FFFFFF>%message%";
        public String staffAlert = "<#FFEB3B>[Filtro] <#FFFFFF>%player% <#FFEB3B>tuvo un mensaje bloqueado en el chat.";
        public String warningActionbar = "<#FFEB3B>Aviso <#FFFFFF>%current%/%max% <#FFEB3B>por lenguaje inapropiado.";
    }

    public static class Console extends MincoreConfig {
        public List<String> startupLogo = Arrays.asList(
                "",
                "<#55FFFF>  __    __  __    __  __    ______   ______   ______   ______   ",
                "<#55FFFF> /\\ \"-./  \\ /\\ \\ /\\ \"-.\\ \\ /\\  ___\\ /\\  __ \\ /\\  == \\ /\\  ___\\  ",
                "<#FF66B2> \\ \\ \\-./\\ \\\\ \\ \\\\ \\ \\-.  \\\\ \\ \\____\\ \\ \\/\\ \\\\ \\  __< \\ \\  __\\  ",
                "<#FF66B2>  \\ \\_\\ \\ \\_\\\\ \\_\\\\ \\_\\\\\"\\_\\\\ \\_____\\\\ \\_____\\\\ \\_\\ \\_\\\\ \\_____\\ ",
                "<#FF66B2>   \\/_/  \\/_/ \\/_/ \\/_/ \\/_/ \\/_____/ \\/_____/ \\/_/ /_/ \\/_____/ ",
                ""
        );

        public List<String> startupInfo = Arrays.asList(
                "<#888888>» <#55FFFF>Información del Plugin:",
                "<#888888>  • <#FFFFFF>Autor: <#FF66B2>%author%",
                "<#888888>  • <#FFFFFF>Descripción: <#FF66B2>%description%",
                "<#888888>  • <#FFFFFF>Versión: <#55FFFF>%version%",
                "",
                "<#888888>» <#55FFFF>Información del Servidor:",
                "<#888888>  • <#FFFFFF>Motor: <#FF66B2>%fork% <#888888>(%server_version%)",
                "<#888888>  • <#FFFFFF>Java: <#FF66B2>%java%",
                "<#888888>  • <#FFFFFF>Soporte: %supported_versions%",
                "",
                "<#888888>» <#55FFFF>Motores y Conexiones:",
                "<#888888>  • <#FFFFFF>Base de Datos: %database%",
                "<#888888>  • <#FFFFFF>Redis (Sync): %redis%",
                "",
                "<#888888>» <#55FFFF>Hooks y Dependencias:",
                "<#888888>  • <#FFFFFF>PlaceholderAPI: %hook_papi%",
                "",
                "<#888888>» <#5CE65C>Mincore cargado exitosamente en %time%ms."
        );

        public String shutdownMessage = "<#FF66B2>Apagando el Mega Core... ¡Hasta pronto!";
        public String errorPrefix = "<#FF4C4C>ERROR: ";
        public String configUpdated = "<#FFEB3B>El archivo <#FFFFFF>%file% <#FFEB3B>estaba desactualizado (v%old%). Se inyectaron los datos faltantes automáticamente (v%new%).";
        public String configDowngraded = "<#FF4C4C>El archivo <#FFFFFF>%file% <#FF4C4C>es más moderno (v%old%) que el plugin (v%new%). ¡Podría generar errores!";
    }

    public static class Commands extends MincoreConfig {
        public String reloadSuccess = "<#5CE65C>Configuraciones recargadas en <#FFFFFF>%ms%ms<#5CE65C>.";
        @Comment("Respuesta de /mincore reload catalog - sobrescribe los YAML de cosméticos en el dataFolder con el catálogo embebido en el jar, incluso si el admin ya los había editado (útil en desarrollo; en producción borra ediciones manuales de esos archivos).")
        public String resetCatalogSuccess = "<#5CE65C>Catálogo de cosméticos restaurado desde el jar en <#FFFFFF>%ms%ms<#5CE65C>.";
        @Comment("Respuesta de /mincore commandblocker reload.")
        public String commandBlockerReloadSuccess = "<#5CE65C>commandblocker.yml recargado en <#FFFFFF>%ms%ms<#5CE65C>.";
        public String noPermission = "<#FF4C4C>No tienes permiso para usar este comando.";
        public String playersOnly = "<#FF4C4C>Este comando solo puede ser ejecutado por un jugador.";
        public String playerNotFound = "<#FF4C4C>Ese jugador no está conectado.";
        public String cosmeticsDisabled = "<#FF4C4C>Los cosméticos están desactivados.";
        public String profileDisabled = "<#FF4C4C>El menú de perfil está desactivado.";
        public List<String> help = Arrays.asList(
                "",
                "<#55FFFF><b>CoreEC</b> <#888888>| <#FFFFFF>Lista de Comandos",
                "<#888888>» <#FF66B2>/coreec reload <#FFFFFF>Recarga las configuraciones.",
                "<#888888>» <#FF66B2>/coreec reload catalog <#FFFFFF>Restaura los YAML de cosméticos desde el jar.",
                "<#888888>» <#FF66B2>/coreec help <#FFFFFF>Muestra este mensaje.",
                "<#888888>» <#FF66B2>/coreec eco <give|take|set> <jugador> <cantidad> <#FFFFFF>Administra monedas.",
                ""
        );

        public String ecoPlayerOffline = "<#FF4C4C>Ese jugador no está conectado.";
        public String ecoInvalidAmount = "<#FF4C4C>La cantidad debe ser un número positivo.";
        public String ecoGiveSuccess = "<#5CE65C>Le diste <#FFFFFF>%amount% <#5CE65C>monedas a <#FFFFFF>%player%<#5CE65C>.";
        public String ecoTakeSuccess = "<#5CE65C>Le quitaste <#FFFFFF>%amount% <#5CE65C>monedas a <#FFFFFF>%player%<#5CE65C>.";
        public String ecoSetSuccess = "<#5CE65C>Estableciste el saldo de <#FFFFFF>%player% <#5CE65C>en <#FFFFFF>%amount%<#5CE65C>.";

        public String flyEnabled = "<#5CE65C>Vuelo activado.";
        public String flyDisabled = "<#FF4C4C>Vuelo desactivado.";
        public String gamemodeChanged = "<#5CE65C>Modo de juego cambiado a <#FFFFFF>%gamemode%<#5CE65C>.";
        public String syncPush = "<#5CE65C>Sincronización (push) de catálogo y configuración iniciada.";
        public String syncPull = "<#5CE65C>Sincronización (pull) de catálogo y configuración iniciada.";
        public String clearchatDone = "<#5CE65C>El chat ha sido limpiado por <#FFFFFF>%player%<#5CE65C>.";
        public String previewZoneSet = "<#5CE65C>Zona de previsualización establecida en tu posición actual.";
    }
}