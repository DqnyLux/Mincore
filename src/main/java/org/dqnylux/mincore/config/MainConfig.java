package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Include(MincoreConfig.class)
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


    public static class Modules extends MincoreConfig {
        @Comment("¿Activar el menú de perfil (/perfil)?")
        public boolean profileMenu = true;

        @Comment({
                "¿Ocultar la información extra que Minecraft agrega solo a los ítems de los menús",
                "(atributos, irrompible, brillo de encantamiento...) para que solo se vea el lore",
                "configurado? Desactívalo si quieres que esa información vanilla sí aparezca."
        })
        public boolean hideItemAttributes = true;

        @Comment("¿Activar el MOTD personalizado que se envía al jugador al entrar? (líneas en messages.yml -> join-quit.join-motd)")
        public boolean motd = true;

        @Comment({
                "¿Descargar y renderizar la cara de la skin del jugador como arte de bloques de color",
                "(%skinmotd_1% .. %skinmotd_8%, una fila de 8x8 por línea) en el join-motd? Requiere que",
                "el servidor tenga salida a internet (crafatar.com/mc-heads.net/minotar.net). Si es false,",
                "esos placeholders se rellenan con bloques grises fijos, sin hacer ninguna petición."
        })
        public boolean motdSkinFace = true;

        @Comment("¿Reemplazar los mensajes vanilla de entrada/salida por los personalizados de messages.yml -> join-quit?")
        public boolean customJoinQuitMessages = true;

        @Comment({
                "¿Bloquear TODOS los logros (advancements) vanilla del servidor, incluidos",
                "los que añaden datapacks? El gamerule announceAdvancements NO alcanza -",
                "solo evita el mensaje de chat, pero el toast (la notificación emergente)",
                "sigue saliendo igual si viene de un datapack. Esto cancela el logro antes",
                "de que se complete, así que ni el toast ni el mensaje llegan a aparecer."
        })
        public boolean blockAdvancements = false;

        @Comment({
                "",
                "¿Filtrar en qué mundos se reproducen los efectos de cosméticos",
                "(join/kill/death/elytra/proyectil/trails, incluida su previsualización)?",
                "No aplica a namecolors/chatcolors/prefixes/icons/glows - esos son solo",
                "texto o scoreboard, no partículas ni entidades, así que no causan los",
                "problemas de rendimiento/interferencia que este filtro busca evitar",
                "(lobbies, minijuegos, survival compartiendo el mismo plugin)."
        })
        public Worlds worlds = new Worlds();

        public static class Worlds extends MincoreConfig {
            @Comment({
                    "BLACKLIST = los efectos se reproducen en todos los mundos EXCEPTO",
                    "los listados en world-list. WHITELIST = SOLO se reproducen en los",
                    "mundos listados en world-list."
            })
            public String mode = "BLACKLIST";

            @Comment("Nombres exactos de mundo (los que devuelve World#getName(), ej. \"world_the_end\").")
            public List<String> worldList = new ArrayList<>();
        }

        public Cosmetics cosmetics = new Cosmetics();

        public Skins skins = new Skins();

        public static class Skins extends MincoreConfig {
            @Comment({
                    "",
                    "Zona de previsualización de skins - DOS puntos configurables por",
                    "separado con /coreec setskinpreviewzone:",
                    "  /coreec setskinpreviewzone npc       -> punto EXACTO del maniquí",
                    "                                            (dónde nace, parándote ahí",
                    "                                            y mirando hacia donde quieres",
                    "                                            que aparezca).",
                    "  /coreec setskinpreviewzone spectator -> punto del ESPECTADOR",
                    "                                            (parado ahí, inmóvil, viendo",
                    "                                            el maniquí). Si dejas este",
                    "                                            vacío, el espectador se coloca",
                    "                                            solo 3 bloques detrás del",
                    "                                            maniquí.",
                    "Si el punto del maniquí (npc) queda con world vacío, /skin avisa",
                    "que no hay zona configurada."
            })
            public PreviewZone npcZone = new PreviewZone();
            public PreviewZone spectatorZone = new PreviewZone();

            @Comment("Cooldown en segundos entre usos de /skin (anti-spam Mojang API).")
            public int cooldownSeconds = 30;

            public static class PreviewZone extends MincoreConfig {
                public String world = "";
                public double x;
                public double y;
                public double z;
                public float yaw;
                public float pitch;
            }
        }

        public static class Cosmetics extends MincoreConfig {
            @Comment("¿Activar el sistema de cosméticos por completo?")
            public boolean enabled = true;

            @Comment({
                    "¿Permitir previsualizar un cosmético con clic derecho en el menú",
                    "(sin equiparlo ni gastar monedas)? Desactívalo si el plugin corre",
                    "en un lobby/minijuego/survival compartido donde disparar un efecto",
                    "de muestra (fuegos artificiales, explosiones, mobs...) fuera de",
                    "contexto podría causar bugs o interferir con otros jugadores."
            })
            public boolean previewEnabled = true;

            @Comment({
                    "Cuántos ticks se cierra el menú al previsualizar con clic derecho",
                    "(para que el jugador vea el efecto en el mundo en vez de tapado por",
                    "el inventario) antes de reabrirlo automáticamente en la misma",
                    "categoría. Si hay zona de previsualización configurada, se usa en",
                    "cambio previewZone.sessionDurationTicks (dura lo mismo que la sesión",
                    "del NPC). 100 = 5 segundos."
            })
            public int previewMenuCloseTicks = 100;

            @Comment({
                    "Zona de previsualización: en vez de reproducir el efecto encima del",
                    "jugador donde sea que esté parado (lobby, arena de minijuego...), lo",
                    "manda un momento a este punto a ver un NPC (jugador falso, solo",
                    "visible para él) haciendo la demostración, y lo regresa exactamente",
                    "a donde estaba al terminar. Se configura parándote en el punto exacto",
                    "y mirando hacia donde quieres que el NPC aparezca, con",
                    "/mincore setpreviewzone. Si world queda vacío, la previsualización",
                    "vuelve a reproducirse directamente sobre el jugador (comportamiento",
                    "anterior)."
            })
            public PreviewZone previewZone = new PreviewZone();

            public static class PreviewZone extends MincoreConfig {
                public String world = "";
                public double x;
                public double y;
                public double z;
                public float yaw;
                public float pitch;

                @Comment("Radio (bloques) que el jugador puede moverse durante la previsualización antes de que se le regrese al punto - no puede alejarse ni deambular.")
                public double movementRadius = 3.0;

                @Comment("Cuántos ticks dura la sesión completa (NPC + teletransporte) antes de devolver al jugador a donde estaba. 140 = 7 segundos.")
                public int sessionDurationTicks = 140;
            }

            @Comment("Activa o desactiva cada categoría de cosmético de forma independiente.")
            public Map<String, Boolean> categories = defaultCategories();

            private static Map<String, Boolean> defaultCategories() {
                Map<String, Boolean> map = new LinkedHashMap<>();
                String[] names = {
                        "namecolors", "chatcolors", "prefixes", "icons", "glows", "formats",
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
        public String mincore = "coreec";

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
        @Comment("Comando del menú de ajustes (toggles de chat, vuelo, menciones).")
        public String settings = "ajustes";
        public String message = "msg";
        public String reply = "reply";
        public String track = "trackcore";
        public String staffMode = "staffmode";
        public String vanish = "vanish";
        public String helpOp = "helpop";
        public String report = "report";
        public String invsee = "invsee";
        public String enderchest = "enderchest";
        public String afk = "afk";

        @Comment("Comandos de la economía dual: coins (Vault) y sucres (moneda del servidor).")
        public String balance = "balance";
        public String pay = "pay";
        public String eco = "eco";
        public String sucre = "sucre";
        public String skin = "skin";

        @Comment("Cantidad de líneas vacías que envía /mincore clearchat.")
        public int clearchatLines = 100;

        @Comment({
                "",
                "¿Ocultar del autocompletado \"/\" los comandos de Mincore cuyo permiso",
                "el jugador no tiene? Sin esto, cualquiera ve sugerido /staffmode,",
                "/vanish, /freeze, etc. aunque el servidor los rechace al presionar",
                "Enter. Los OP siempre ven todo, sin importar este ajuste."
        })
        public boolean hideUnauthorizedCommands = true;
    }

    public static class Permissions extends MincoreConfig {
        @Comment("Permiso base de administración (/mincore y subcomandos).")
        public String admin = "coreec.admin";

        @Comment("Permiso del subcomando /mincore clearchat.")
        public String clearchat = "coreec.admin.clearchat";

        @Comment("Permiso del comando de vuelo.")
        public String fly = "coreec.command.fly";

        @Comment("Permiso de los comandos de modo de juego.")
        public String gamemode = "coreec.command.gamemode";

        @Comment("Permiso para saltarse el toggle de mensajes privados del destinatario.")
        public String messageBypass = "coreec.admin.bypass";

        @Comment("Permiso del comando /skin.")
        public String skin = "coreec.command.skin";
    }

}
