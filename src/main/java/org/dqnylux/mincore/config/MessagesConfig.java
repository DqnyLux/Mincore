package org.dqnylux.mincore.config;

import java.util.Arrays;
import java.util.List;

public class MessagesConfig extends MincoreConfig {

    public String prefix = "<#888888>[<#55FFFF>Mincore<#888888>] <white>";
    public Console console = new Console();
    public Commands commands = new Commands();
    public Chat chat = new Chat();
    public PrivateMessages privateMessages = new PrivateMessages();
    public Death death = new Death();
    public Cosmetics cosmetics = new Cosmetics();
    public Menus menus = new Menus();
    public JoinQuit joinQuit = new JoinQuit();

    public static class Cosmetics extends MincoreConfig {
        public String noAccess = "<red>No tienes acceso a este cosmético.";
        public String equipped = "<green>Cosmético equipado.";
        public String unequipped = "<yellow>Cosmético desequipado.";
        public String categoryDisabled = "<red>Esta categoría de cosméticos está desactivada.";
        public String statusEquipped = "<green>Equipado - clic para desequipar";
        public String statusClickToEquip = "<yellow>Clic para equipar";
        public String statusLocked = "<red>Bloqueado";
    }

    public static class Menus extends MincoreConfig {
        public String mainTitle = "<bold><gradient:#ff5e62:#ff9966>Mincore Principal</gradient></bold>";
        public String categoriesTitle = "<bold>Mincore <#888888>» <white>Categorías";
        public String categoryTitle = "<bold>Mincore <#888888>» <white>%category%";
        public String pagePrevious = "<white>« Anterior";
        public String pageNext = "<white>Siguiente »";
        public String categoryUnlockedLore = "<#888888>Desbloqueados: <white>%unlocked%/%total%";
        public String categoryEquippedLore = "<#888888>Equipado: <green>%equipped%";
        public String categoryNothingEquipped = "<#888888>Nada equipado";

        public String cosmeticsButtonName = "<#55FFFF>Cosméticos";
        public String cosmeticsButtonLore = "<#888888>Abre el menú de categorías.";
        public String profileHeadName = "<#55FFFF><b>%player%";
        public String profileHeadCoinsLore = "<#888888>Monedas: <gold>%coins%";
        public String toggleGlobalChatName = "<#FF66B2>Chat global";
        public String toggleMessagesName = "<#FF66B2>Mensajes privados";
        public String toggleOn = "<green>Activado <#888888>- clic para desactivar";
        public String toggleOff = "<red>Desactivado <#888888>- clic para activar";
    }

    public static class JoinQuit extends MincoreConfig {
        public String joinMessage = "<#888888>[<green>+<#888888>] <white>%player%";
        public String quitMessage = "<#888888>[<red>-<#888888>] <white>%player%";
        public List<String> joinMotd = Arrays.asList(
                "",
                "<center><#55FFFF><b>Bienvenido a Mincore</b></center>",
                "<center><#888888>Usa <#FF66B2>/cosmetics <#888888>para personalizar tu perfil.</center>",
                ""
        );
    }

    public static class Death extends MincoreConfig {
        public String trackPrompt = "<yellow>Moriste. Clic aquí para rastrear el lugar (ID: %id%)";
        public String trackingStarted = "<green>Rastreo iniciado.";
        public String trackingNotFound = "<red>Ese rastreo no existe o expiró.";
        public String trackingArrived = "<green>¡Has llegado al lugar de tu muerte!";
        public String bossbarDistance = "<yellow>Distancia: <white>%distance%m";
    }

    public static class PrivateMessages extends MincoreConfig {
        public String playerOffline = "<red>Ese jugador no está conectado.";
        public String cannotMessageSelf = "<red>No puedes enviarte un mensaje a ti mismo.";
        public String targetDisabled = "<red>Ese jugador tiene los mensajes privados desactivados.";
        public String noReplyTarget = "<red>No tienes a quién responder.";
        public String senderFormat = "<gray>Tú -> %target%: <white>";
        public String targetFormat = "<gray>%sender% -> Tú: <white>";
    }

    public static class Chat extends MincoreConfig {
        public String filterSpam = "<red>Estás enviando mensajes muy rápido.";
        public String filterRepetition = "<red>No repitas el mismo mensaje.";
        public String filterBadWord = "<red>Tu mensaje contiene palabras no permitidas.";
        public String filterAds = "<red>No está permitido enviar enlaces o publicidad no autorizada.";
        public String injectionBlocked = "<red>%player% intentó inyectar código en el chat: <white>%message%";
        public String staffAlert = "<yellow>[Filtro] <white>%player% <yellow>tuvo un mensaje bloqueado en el chat.";
        public String warningActionbar = "<yellow>Aviso <white>%current%/%max% <yellow>por lenguaje inapropiado.";
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
                "<#888888>  • <white>Autor: <#FF66B2>%author%",
                "<#888888>  • <white>Descripción: <#FF66B2>%description%",
                "<#888888>  • <white>Versión: <#55FFFF>%version%",
                "",
                "<#888888>» <#55FFFF>Información del Servidor:",
                "<#888888>  • <white>Motor: <#FF66B2>%fork% <#888888>(%server_version%)",
                "<#888888>  • <white>Java: <#FF66B2>%java%",
                "<#888888>  • <white>Soporte: %supported_versions%",
                "",
                "<#888888>» <#55FFFF>Motores y Conexiones:",
                "<#888888>  • <white>Base de Datos: %database%",
                "<#888888>  • <white>Redis (Sync): %redis%",
                "",
                "<#888888>» <#55FFFF>Hooks y Dependencias:",
                "<#888888>  • <white>PlaceholderAPI: %hook_papi%",
                "",
                "<#888888>» <green>Mincore cargado exitosamente en %time%ms."
        );

        public String shutdownMessage = "<#FF66B2>Apagando el Mega Core... ¡Hasta pronto!";
        public String errorPrefix = "<red>ERROR: ";
        public String configUpdated = "<yellow>El archivo <white>%file% <yellow>estaba desactualizado (v%old%). Se inyectaron los datos faltantes automáticamente (v%new%).";
        public String configDowngraded = "<red>El archivo <white>%file% <red>es más moderno (v%old%) que el plugin (v%new%). ¡Podría generar errores!";
    }

    public static class Commands extends MincoreConfig {
        public String reloadSuccess = "<green>Configuraciones recargadas en <white>%ms%ms<green>.";
        public String noPermission = "<red>No tienes permiso para usar este comando.";
        public String playersOnly = "<red>Este comando solo puede ser ejecutado por un jugador.";
        public String cosmeticsDisabled = "<red>Los cosméticos están desactivados.";
        public String profileDisabled = "<red>El menú de perfil está desactivado.";
        public List<String> help = Arrays.asList(
                "",
                "<#55FFFF><b>Mincore</b> <#888888>| <white>Lista de Comandos",
                "<#888888>» <#FF66B2>/mincore reload <white>Recarga las configuraciones.",
                "<#888888>» <#FF66B2>/mincore help <white>Muestra este mensaje.",
                "<#888888>» <#FF66B2>/mincore eco <give|take|set> <jugador> <cantidad> <white>Administra monedas.",
                ""
        );

        public String ecoPlayerOffline = "<red>Ese jugador no está conectado.";
        public String ecoInvalidAmount = "<red>La cantidad debe ser un número positivo.";
        public String ecoGiveSuccess = "<green>Le diste <white>%amount% <green>monedas a <white>%player%<green>.";
        public String ecoTakeSuccess = "<green>Le quitaste <white>%amount% <green>monedas a <white>%player%<green>.";
        public String ecoSetSuccess = "<green>Estableciste el saldo de <white>%player% <green>en <white>%amount%<green>.";

        public String flyEnabled = "<green>Vuelo activado.";
        public String flyDisabled = "<red>Vuelo desactivado.";
        public String gamemodeChanged = "<green>Modo de juego cambiado a <white>%gamemode%<green>.";
        public String syncPush = "<green>Sincronización (push) de catálogo y configuración iniciada.";
        public String syncPull = "<green>Sincronización (pull) de catálogo y configuración iniciada.";
        public String clearchatDone = "<green>El chat ha sido limpiado por <white>%player%<green>.";
    }
}