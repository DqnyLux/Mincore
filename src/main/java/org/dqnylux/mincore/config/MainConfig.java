package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainConfig extends MincoreConfig {

    @Comment({
            "=======================================================",
            " MÓDULOS",
            " Activa o desactiva bloques completos de funcionalidad",
            " sin tener que quitar/reinstalar el plugin.",
            "======================================================="
    })
    public Modules modules = new Modules();

    @Comment({
            "",
            "=======================================================",
            " RASTREO DE MUERTES (GPS)",
            "======================================================="
    })
    public DeathTracking deathTracking = new DeathTracking();

    @Comment({
            "",
            "=======================================================",
            " COMANDOS",
            " Nombre de cada comando raíz. Cambiar aquí no requiere",
            " recompilar el plugin ni tocar código.",
            "======================================================="
    })
    public Commands commands = new Commands();

    @Comment({
            "",
            "=======================================================",
            " PERMISOS",
            " Nodo de permiso de cada función. Sección 17 del prompt:",
            " ningún permiso no-estructural debe vivir fijo en Java.",
            "======================================================="
    })
    public Permissions permissions = new Permissions();

    @Comment({
            "",
            "=======================================================",
            " LUCKPERMS",
            "======================================================="
    })
    public LuckPermsIntegration luckPerms = new LuckPermsIntegration();

    public static class Modules extends MincoreConfig {
        @Comment("¿Activar el menú de perfil (/perfil)?")
        public boolean profileMenu = true;

        @Comment("¿Activar el MOTD personalizado que se envía al jugador al entrar? (líneas en messages.yml -> join-quit.join-motd)")
        public boolean motd = true;

        @Comment("¿Reemplazar los mensajes vanilla de entrada/salida por los personalizados de messages.yml -> join-quit?")
        public boolean customJoinQuitMessages = true;

        public Cosmetics cosmetics = new Cosmetics();

        public static class Cosmetics extends MincoreConfig {
            @Comment("¿Activar el sistema de cosméticos por completo?")
            public boolean enabled = true;

            @Comment("Activa o desactiva cada categoría de cosmético de forma independiente.")
            public Map<String, Boolean> categories = defaultCategories();

            private static Map<String, Boolean> defaultCategories() {
                Map<String, Boolean> map = new LinkedHashMap<>();
                String[] names = {
                        "namecolors", "chatcolors", "prefixes", "icons", "glows",
                        "join-messages", "join-effects", "projectile-effects", "kill-effects",
                        "death-effects", "kill-messages", "death-messages", "elytra-effects",
                        "trails", "wings"
                };
                for (String name : names) {
                    map.put(name, true);
                }
                return map;
            }
        }
    }

    public static class DeathTracking extends MincoreConfig {
        @Comment("¿Activar el rastreo GPS de muertes?")
        public boolean enabled = true;

        @Comment("Minutos antes de que expire un rastreo de muerte sin reclamar.")
        public int expirationMinutes = 10;

        @Comment("¿Mostrar el diálogo automático de rastreo a jugadores Bedrock al respawnear?")
        public boolean bedrockAutoMenu = true;
    }

    public static class Commands extends MincoreConfig {
        public String mincore = "mincore";

        @Comment("¿Activar el comando de vuelo?")
        public boolean flyEnabled = true;
        public String fly = "fly";

        @Comment("¿Activar los comandos de modo de juego?")
        public boolean gamemodesEnabled = true;
        public String gamemodeCreative = "gmc";
        public String gamemodeSurvival = "gms";
        public String gamemodeAdventure = "gma";
        public String gamemodeSpectator = "gmsp";

        public String cosmetics = "cosmetics";
        public String profile = "perfil";
        public String message = "msg";
        public String reply = "reply";
        public String track = "trackcore";

        @Comment("Cantidad de líneas vacías que envía /mincore clearchat.")
        public int clearchatLines = 100;
    }

    public static class Permissions extends MincoreConfig {
        @Comment("Permiso base de administración (/mincore y subcomandos).")
        public String admin = "mincore.admin";

        @Comment("Permiso del subcomando /mincore clearchat.")
        public String clearchat = "mincore.admin.clearchat";

        @Comment("Permiso del comando de vuelo.")
        public String fly = "mincore.command.fly";

        @Comment("Permiso de los comandos de modo de juego.")
        public String gamemode = "mincore.command.gamemode";

        @Comment("Permiso para saltarse el toggle de mensajes privados del destinatario.")
        public String messageBypass = "mincore.admin.bypass";
    }

    public static class LuckPermsIntegration extends MincoreConfig {
        @Comment({
                "Prioridad del nodo de prefijo/sufijo que Mincore añade al equipar",
                "un cosmético de prefixes/icons. Debe ser mayor que la prioridad de",
                "cualquier prefijo/sufijo de rango para que siempre gane mientras",
                "esté equipado - pero NO se borra el nodo del rango, solo se le",
                "superpone: al desequipar, Mincore únicamente quita el nodo con",
                "esta prioridad exacta, y el prefijo/sufijo del rango del jugador",
                "reaparece solo (nunca se tocó)."
        })
        public int prefixPriority = 1000;
        public int suffixPriority = 1000;
    }
}
