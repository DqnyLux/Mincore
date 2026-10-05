package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Include(MincoreConfig.class)
public class AnnouncementsConfig extends MincoreConfig {

    public Settings settings = new Settings();

    @Comment("Placeholders reutilizables: %%clave%% en cualquier línea o campo de anuncio se reemplaza por el valor.")
    public Map<String, String> placeholders = defaultPlaceholders();

    public Map<String, AnnouncementEntry> announcements = defaultAnnouncements();

    public static class Settings extends MincoreConfig {
        @Comment("Si es true, cada anuncio usa su propio 'intervalSeconds'. Si es false, hay un único temporizador que rota entre todos los anuncios activos.")
        public boolean perAnnouncementInterval = false;

        @Comment("Intervalo (segundos) del temporizador global. Solo aplica si perAnnouncementInterval=false.")
        public int globalIntervalSeconds = 180;

        @Comment("¿Rotar en orden o aleatoriamente? Solo aplica al modo de intervalo global.")
        public boolean random = false;
    }

    public static class AnnouncementEntry extends MincoreConfig {
        public boolean enabled = true;

        @Comment("¿Enviar el mensaje en el chat?")
        public boolean chatEnabled = true;

        @Comment({
                "Soporta MiniMessage completo, centrado con <center>...</center>, hover y click:",
                "<hover:show_text:'<#AAAAAA>Texto al pasar el mouse'>texto</hover>",
                "<click:open_url:'https://ejemplo.com'>texto</click>",
                "<click:run_command:'/comando'>texto</click>",
                "<click:suggest_command:'/comando '>texto</click>"
        })
        public List<String> lines = new ArrayList<>();

        @Comment("¿Reproducir sonido al enviar este anuncio?")
        public boolean soundEnabled = true;

        @Comment("Nombre de sonido XSound (ej. ENTITY_EXPERIENCE_ORB_PICKUP). Vacío para no reproducir ninguno.")
        public String sound = "";

        @Comment("Intervalo (segundos) propio de este anuncio. Solo se usa si perAnnouncementInterval=true.")
        public int intervalSeconds = 180;

        @Comment("¿Mostrar título animado en pantalla (Title/Subtitle)?")
        public boolean titleEnabled = true;

        @Comment("Título animado en pantalla (Title). Dejar vacío si no se desea mostrar.")
        public String title = "";

        @Comment("Subtítulo animado en pantalla (Subtitle). Dejar vacío si no se desea mostrar.")
        public String subtitle = "";

        @Comment("Ticks de animación de aparición (Fade In) del Title. (20 ticks = 1 segundo)")
        public int titleFadeIn = 10;

        @Comment("Ticks de permanencia (Stay) del Title en pantalla.")
        public int titleStay = 50;

        @Comment("Ticks de desvanecimiento (Fade Out) del Title.")
        public int titleFadeOut = 15;

        @Comment("¿Mostrar mensaje en la barra de acción (Action Bar)?")
        public boolean actionBarEnabled = true;

        @Comment("Mensaje en la barra de acción (Action Bar). Dejar vacío para desactivar.")
        public String actionBar = "";

        @Comment("¿Mostrar notificación tipo logro emergente (Toast)?")
        public boolean toastEnabled = false;

        @Comment("Título superior de la notificación Toast.")
        public String toastTitle = "";

        @Comment("Descripción o texto inferior de la notificación Toast.")
        public String toastDescription = "";

        @Comment("Ítem / Icono del Toast (ej. BEACON, NETHER_STAR, ENDER_EYE, GOLDEN_APPLE, BOOK).")
        public String toastIcon = "BEACON";

        @Comment("Estilo del marco del Toast: GOAL (redondeado), TASK (cuadrado normal), CHALLENGE (puntiagudo).")
        public String toastFrame = "GOAL";
    }

    private static Map<String, String> defaultPlaceholders() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Discord", "discord.gg/riftmystic");
        map.put("Web", "riftmystic.lat");
        map.put("Tienda", "tienda.riftmystic.lat");
        return map;
    }

    private static Map<String, AnnouncementEntry> defaultAnnouncements() {
        Map<String, AnnouncementEntry> map = new LinkedHashMap<>();

        // 1. Discord Oficial
        AnnouncementEntry discord = new AnnouncementEntry();
        discord.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#8B0000:#FF2A2A:#8B0000><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#FF2A2A:#FF6B6B><bold>RIFTMYSTIC COMMUNITY</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Únete a nuestro aquelarre y participa en</center>",
                "<center><#EAEAEA>eventos arcanos y sorteos exclusivos.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para abrir el portal a Discord'><click:open_url:'https://%%Discord%%'><gradient:#FF4D4D:#FF2A2A><bold>• ¡Entra a %%Discord%%! •</bold></gradient></click></hover></center>",
                "<center><gradient:#8B0000:#FF2A2A:#8B0000><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        discord.sound = "ENTITY_EXPERIENCE_ORB_PICKUP";
        discord.intervalSeconds = 180;
        discord.title = "<gradient:#FF2A2A:#8B0000><bold>RIFTMYSTIC</bold></gradient>";
        discord.subtitle = "<#EAEAEA>¡Únete a nuestra comunidad en Discord!";
        discord.titleFadeIn = 10;
        discord.titleStay = 50;
        discord.titleFadeOut = 15;
        discord.actionBar = "<#888888>» <#FF4D4D>Discord: <#FFFFFF>%%Discord%% <#888888>«";
        discord.toastEnabled = true;
        discord.toastTitle = "<gradient:#FF2A2A:#8B0000><bold>Discord Oficial</bold></gradient>";
        discord.toastDescription = "<#EAEAEA>%%Discord%%";
        discord.toastIcon = "ENDER_EYE";
        discord.toastFrame = "GOAL";
        map.put("Discord", discord);

        // 2. Tienda Oficial
        AnnouncementEntry tienda = new AnnouncementEntry();
        tienda.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#8B0000:#FFD700:#8B0000><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#FFD700:#FFF099><bold>TIENDA OFICIAL</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Adquiere rangos exclusivos, cosméticos únicos</center>",
                "<center><#EAEAEA>y llaves arcanas para potenciar tu aventura.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para explorar la tienda web'><click:open_url:'https://%%Tienda%%'><gradient:#FFD700:#FFA500><bold>• ¡Visita %%Tienda%%! •</bold></gradient></click></hover></center>",
                "<center><gradient:#8B0000:#FFD700:#8B0000><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        tienda.sound = "UI_TOAST_CHALLENGE_COMPLETE";
        tienda.intervalSeconds = 180;
        tienda.title = "<gradient:#FFD700:#FF8C00><bold>TIENDA VIP</bold></gradient>";
        tienda.subtitle = "<#EAEAEA>Mejora tu experiencia en <#FFD700>%%Tienda%%";
        tienda.titleFadeIn = 10;
        tienda.titleStay = 50;
        tienda.titleFadeOut = 15;
        tienda.actionBar = "<#888888>» <#FFD700>Tienda Web: <#FFFFFF>%%Tienda%% <#888888>«";
        tienda.toastEnabled = true;
        tienda.toastTitle = "<gradient:#FFD700:#FF8C00><bold>Tienda Web</bold></gradient>";
        tienda.toastDescription = "<#EAEAEA>%%Tienda%%";
        tienda.toastIcon = "NETHER_STAR";
        tienda.toastFrame = "CHALLENGE";
        map.put("Tienda", tienda);

        // 3. Voto por el Servidor
        AnnouncementEntry voto = new AnnouncementEntry();
        voto.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#1B5E20:#5CE65C:#1B5E20><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#5CE65C:#A3FFA3><bold>VOTA POR EL SERVIDOR</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Vota a diario para recibir recompensas</center>",
                "<center><#EAEAEA>místicas, llaves y sucres adicionales.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para ejecutar /vote'><click:run_command:'/vote'><gradient:#5CE65C:#2E8B22><bold>• ¡Usa /vote para votar! •</bold></gradient></click></hover></center>",
                "<center><gradient:#1B5E20:#5CE65C:#1B5E20><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        voto.sound = "ENTITY_PLAYER_LEVELUP";
        voto.intervalSeconds = 180;
        voto.title = "<gradient:#5CE65C:#2E8B22><bold>¡VOTA Y GANA!</bold></gradient>";
        voto.subtitle = "<#EAEAEA>Usa <#5CE65C>/vote <#EAEAEA>para reclamar recompensas";
        voto.titleFadeIn = 10;
        voto.titleStay = 50;
        voto.titleFadeOut = 15;
        voto.actionBar = "<#888888>» <#5CE65C>Vota diariamente con <#FFFFFF>/vote <#888888>«";
        voto.toastEnabled = true;
        voto.toastTitle = "<gradient:#5CE65C:#2E8B22><bold>Recompensas Diarias</bold></gradient>";
        voto.toastDescription = "<#EAEAEA>Ejecuta /vote ahora";
        voto.toastIcon = "GOLDEN_APPLE";
        voto.toastFrame = "TASK";
        map.put("Voto", voto);

        // 4. Normas del Reino
        AnnouncementEntry normas = new AnnouncementEntry();
        normas.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#8B0000:#FF2A2A:#8B0000><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#FF2A2A:#8B0000><bold>NORMAS DEL REINO</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Mantén el respeto y la armonía dentro</center>",
                "<center><#EAEAEA>de las tierras de nuestro reino.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para consultar las normas'><click:run_command:'/reglas'><gradient:#FF4D4D:#8B0000><bold>• ¡Usa /reglas para leerlas! •</bold></gradient></click></hover></center>",
                "<center><gradient:#8B0000:#FF2A2A:#8B0000><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        normas.sound = "ITEM_BOOK_PAGE_TURN";
        normas.intervalSeconds = 180;
        normas.title = "<gradient:#FF4D4D:#8B0000><bold>NORMAS</bold></gradient>";
        normas.subtitle = "<#EAEAEA>Respeta a los demás jugadores";
        normas.titleFadeIn = 10;
        normas.titleStay = 50;
        normas.titleFadeOut = 15;
        normas.actionBar = "<#888888>» <#FF4D4D>Consulta las normas con <#FFFFFF>/reglas <#888888>«";
        normas.toastEnabled = true;
        normas.toastTitle = "<gradient:#FF4D4D:#8B0000><bold>Reglamento</bold></gradient>";
        normas.toastDescription = "<#EAEAEA>Usa /reglas para leerlas";
        normas.toastIcon = "WRITABLE_BOOK";
        normas.toastFrame = "TASK";
        map.put("Normas", normas);

        // 5. Asistencia & Reportes
        AnnouncementEntry soporte = new AnnouncementEntry();
        soporte.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#006666:#55FFFF:#006666><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#55FFFF:#00B8B8><bold>ASISTENCIA & REPORTES</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>¿Necesitas auxilio del equipo de moderación</center>",
                "<center><#EAEAEA>o encontraste a un infractor de las reglas?</center>",
                "",
                "<center><hover:show_text:'<#888888>Pedir ayuda al staff'><click:suggest_command:'/helpop '><#55FFFF><bold>[ /helpop ]</bold></click></hover> <#555555>• <hover:show_text:'<#888888>Reportar a un infractor'><click:suggest_command:'/report '><#FF4D4D><bold>[ /report ]</bold></click></hover></center>",
                "<center><gradient:#006666:#55FFFF:#006666><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        soporte.sound = "BLOCK_BEACON_ACTIVATE";
        soporte.intervalSeconds = 180;
        soporte.title = "<gradient:#55FFFF:#00B8B8><bold>SOPORTE</bold></gradient>";
        soporte.subtitle = "<#EAEAEA>Usa <#55FFFF>/helpop <#EAEAEA>o <#FF4D4D>/report";
        soporte.titleFadeIn = 10;
        soporte.titleStay = 50;
        soporte.titleFadeOut = 15;
        soporte.actionBar = "<#888888>» <#55FFFF>¿Dudas o reportes? Usa <#FFFFFF>/helpop <#888888>«";
        soporte.toastEnabled = true;
        soporte.toastTitle = "<gradient:#55FFFF:#00B8B8><bold>Asistencia Staff</bold></gradient>";
        soporte.toastDescription = "<#EAEAEA>Usa /helpop o /report";
        soporte.toastIcon = "BEACON";
        soporte.toastFrame = "GOAL";
        map.put("Soporte", soporte);

        // 6. Renta de Terrenos
        AnnouncementEntry renta = new AnnouncementEntry();
        renta.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#7A3800:#FF8C00:#7A3800><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#FF8C00:#FFB84D><bold>RENTA TU TERRENO</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Protege tu parcela del reino y evita que</center>",
                "<center><#EAEAEA>sea eliminada o embargada por falta de pago.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para pagar la renta de tu terreno'><click:suggest_command:'/renta pagar'><gradient:#FF8C00:#FF4500><bold>• ¡Usa /renta pagar a tiempo! •</bold></gradient></click></hover></center>",
                "<center><gradient:#7A3800:#FF8C00:#7A3800><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        renta.sound = "BLOCK_ANVIL_LAND";
        renta.intervalSeconds = 180;
        renta.title = "<gradient:#FF8C00:#FF4500><bold>RENTA DE TERRENOS</bold></gradient>";
        renta.subtitle = "<#EAEAEA>Mantén tu terreno protegido en el reino";
        renta.titleFadeIn = 10;
        renta.titleStay = 50;
        renta.titleFadeOut = 15;
        renta.actionBar = "<#888888>» <#FF8C00>Paga tu renta con <#FFFFFF>/renta pagar <#888888>«";
        renta.toastEnabled = true;
        renta.toastTitle = "<gradient:#FF8C00:#FF4500><bold>Renta de Terreno</bold></gradient>";
        renta.toastDescription = "<#EAEAEA>Usa /renta info";
        renta.toastIcon = "IRON_DOOR";
        renta.toastFrame = "TASK";
        map.put("Renta", renta);

        // 7. Trabajos del Reino
        AnnouncementEntry trabajos = new AnnouncementEntry();
        trabajos.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#1B5E20:#5CE65C:#1B5E20><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#5CE65C:#99FF99><bold>TRABAJOS DEL REINO</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Consigue sucres y monedas de forma rápida</center>",
                "<center><#EAEAEA>ejerciendo los oficios de tu preferencia.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para abrir el menú de trabajos'><click:run_command:'/trabajos'><gradient:#5CE65C:#00AA00><bold>• ¡Usa /trabajos ahora! •</bold></gradient></click></hover></center>",
                "<center><gradient:#1B5E20:#5CE65C:#1B5E20><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        trabajos.sound = "ENTITY_EXPERIENCE_ORB_PICKUP";
        trabajos.intervalSeconds = 180;
        trabajos.title = "<gradient:#5CE65C:#00AA00><bold>TRABAJOS</bold></gradient>";
        trabajos.subtitle = "<#EAEAEA>Gana sucres con <#5CE65C>/trabajos";
        trabajos.titleFadeIn = 10;
        trabajos.titleStay = 50;
        trabajos.titleFadeOut = 15;
        trabajos.actionBar = "<#888888>» <#5CE65C>Elige tu oficio con <#FFFFFF>/trabajos <#888888>«";
        trabajos.toastEnabled = true;
        trabajos.toastTitle = "<gradient:#5CE65C:#00AA00><bold>Trabajos del Reino</bold></gradient>";
        trabajos.toastDescription = "<#EAEAEA>Usa /trabajos";
        trabajos.toastIcon = "IRON_PICKAXE";
        trabajos.toastFrame = "TASK";
        map.put("Trabajos", trabajos);

        // 8. Mercado de Jugadores
        AnnouncementEntry mercado = new AnnouncementEntry();
        mercado.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#6B5300:#FFD700:#6B5300><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#FFD700:#FFE066><bold>MERCADO DE JUGADORES</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Compra y vende objetos arcanos, recursos y</center>",
                "<center><#EAEAEA>equipamiento con otros aventureros del reino.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para abrir la casa de subastas / mercado'><click:run_command:'/ah'><gradient:#FFD700:#E6C200><bold>• ¡Usa /ah para comerciar! •</bold></gradient></click></hover></center>",
                "<center><gradient:#6B5300:#FFD700:#6B5300><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        mercado.sound = "ENTITY_VILLAGER_TRADE";
        mercado.intervalSeconds = 180;
        mercado.title = "<gradient:#FFD700:#E6C200><bold>MERCADO</bold></gradient>";
        mercado.subtitle = "<#EAEAEA>Comercia en <#FFD700>/ah";
        mercado.titleFadeIn = 10;
        mercado.titleStay = 50;
        mercado.titleFadeOut = 15;
        mercado.actionBar = "<#888888>» <#FFD700>Compra y vende con <#FFFFFF>/ah <#888888>«";
        mercado.toastEnabled = true;
        mercado.toastTitle = "<gradient:#FFD700:#E6C200><bold>Mercado de Jugadores</bold></gradient>";
        mercado.toastDescription = "<#EAEAEA>Usa /ah";
        mercado.toastIcon = "EMERALD";
        mercado.toastFrame = "TASK";
        map.put("Mercado", mercado);

        // 9. Pesca Mística
        AnnouncementEntry pesca = new AnnouncementEntry();
        pesca.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#004D40:#00B8B8:#004D40><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#00B8B8:#80DEEA><bold>PESCA MÍSTICA</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Pesca criaturas raras y tesoros sumergidos</center>",
                "<center><#EAEAEA>para ganar recompensas místicas del reino.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para ver la tabla de líderes de pesca'><click:run_command:'/fishtop'><gradient:#00B8B8:#00838F><bold>• ¡Usa /fishtop para competir! •</bold></gradient></click></hover></center>",
                "<center><gradient:#004D40:#00B8B8:#004D40><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        pesca.sound = "ITEM_TRIDENT_RIPTIDE_1";
        pesca.intervalSeconds = 180;
        pesca.title = "<gradient:#00B8B8:#00838F><bold>PESCA</bold></gradient>";
        pesca.subtitle = "<#EAEAEA>Compite con <#00B8B8>/fishtop";
        pesca.titleFadeIn = 10;
        pesca.titleStay = 50;
        pesca.titleFadeOut = 15;
        pesca.actionBar = "<#888888>» <#00B8B8>Compite en la pesca con <#FFFFFF>/fishtop <#888888>«";
        pesca.toastEnabled = true;
        pesca.toastTitle = "<gradient:#00B8B8:#00838F><bold>Pesca Mística</bold></gradient>";
        pesca.toastDescription = "<#EAEAEA>Usa /fishtop";
        pesca.toastIcon = "FISHING_ROD";
        pesca.toastFrame = "GOAL";
        map.put("Pesca", pesca);

        // 10. Recompensas Diarias (Loot)
        AnnouncementEntry loot = new AnnouncementEntry();
        loot.lines = new ArrayList<>(List.of(
                " ",
                "<center><gradient:#5A0000:#8B0000:#5A0000><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                "<center><gradient:#FF2A2A:#8B0000><bold>RECOMPENSA DIARIA</bold></gradient></center>",
                "<center><#777777><i>@ʀɪꜰᴛᴍʏsᴛɪᴄ ɴᴇᴛᴡᴏʀᴋ</i></center>",
                "",
                "<center><#EAEAEA>Reclama tu botín y dones diarios del abismo</center>",
                "<center><#EAEAEA>antes de que el ciclo se reinicie.</center>",
                "",
                "<center><hover:show_text:'<#888888>Haz clic para reclamar recompensas'><click:run_command:'/rewards'><gradient:#FF2A2A:#8B0000><bold>• ¡Usa /rewards para reclamar! •</bold></gradient></click></hover></center>",
                "<center><gradient:#5A0000:#8B0000:#5A0000><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></gradient></center>",
                " "
        ));
        loot.sound = "ENTITY_PLAYER_LEVELUP";
        loot.intervalSeconds = 180;
        loot.title = "<gradient:#8B0000:#FF2A2A><bold>RECOMPENSAS</bold></gradient>";
        loot.subtitle = "<#EAEAEA>Reclama con <#8B0000>/rewards";
        loot.titleFadeIn = 10;
        loot.titleStay = 50;
        loot.titleFadeOut = 15;
        loot.actionBar = "<#888888>» <#8B0000>Reclama tu recompensa con <#FFFFFF>/rewards <#888888>«";
        loot.toastEnabled = true;
        loot.toastTitle = "<gradient:#8B0000:#FF2A2A><bold>Recompensa Diaria</bold></gradient>";
        loot.toastDescription = "<#EAEAEA>Usa /rewards";
        loot.toastIcon = "CHEST";
        loot.toastFrame = "CHALLENGE";
        map.put("Loot", loot);

        return map;
    }
}

