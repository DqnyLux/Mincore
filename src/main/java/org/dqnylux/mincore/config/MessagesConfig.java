package org.dqnylux.mincore.config;

import java.util.Arrays;
import java.util.List;

public class MessagesConfig extends MincoreConfig {

    public String prefix = "<#888888>[<#55FFFF>Mincore<#888888>] <white>";
    public Console console = new Console();
    public Commands commands = new Commands();

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
        public List<String> help = Arrays.asList(
                "",
                "<#55FFFF><b>Mincore</b> <#888888>| <white>Lista de Comandos",
                "<#888888>» <#FF66B2>/mincore reload <white>Recarga las configuraciones.",
                "<#888888>» <#FF66B2>/mincore help <white>Muestra este mensaje.",
                ""
        );
    }
}