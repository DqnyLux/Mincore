package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BotsConfig extends MincoreConfig {

    @Comment("Respuestas automáticas: si un jugador escribe uno de los 'triggers', el bot responde.")
    public Map<String, AutoResponder> autoResponders = defaultResponders();

    @Comment({
            "",
            "Comandos dinámicos: se registran como comandos reales (ej. /discord), no requieren",
            "estar escritos en el chat. El primer alias es el nombre principal del comando."
    })
    public Map<String, CustomCommand> customCommands = defaultCustomCommands();

    public static class AutoResponder extends MincoreConfig {
        @Comment("Frases o patrones (si regex=true) que activan esta respuesta.")
        public List<String> triggers = new ArrayList<>();

        @Comment("Si es true, cada trigger se interpreta como una expresión regular en vez de texto exacto.")
        public boolean regex = false;

        @Comment("¿Cancelar el mensaje original del jugador que activó el trigger?")
        public boolean cancelOriginal = false;

        public List<String> response = new ArrayList<>();
    }

    public static class CustomCommand extends MincoreConfig {
        public boolean enabled = true;
        public List<String> aliases = new ArrayList<>();
        public List<String> response = new ArrayList<>();
    }

    private static Map<String, AutoResponder> defaultResponders() {
        Map<String, AutoResponder> map = new LinkedHashMap<>();

        AutoResponder saludo = new AutoResponder();
        saludo.triggers = new ArrayList<>(Arrays.asList("hola bot", "hello bot"));
        saludo.regex = false;
        saludo.cancelOriginal = false;
        saludo.response = new ArrayList<>(List.of("<#55FFFF>¡Hola! Soy el bot de Mincore."));
        map.put("saludo", saludo);

        return map;
    }

    private static Map<String, CustomCommand> defaultCustomCommands() {
        Map<String, CustomCommand> map = new LinkedHashMap<>();

        CustomCommand discord = new CustomCommand();
        discord.aliases = new ArrayList<>(List.of("discord"));
        discord.response = new ArrayList<>(List.of("<#5865F2>Únete a nuestro Discord: <white>discord.gg/minecuador"));
        map.put("discord", discord);

        return map;
    }
}
