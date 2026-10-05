package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;

import java.util.Arrays;
import java.util.List;

@Include(MincoreConfig.class)
public class MessagesConfig extends MincoreConfig {

    public String prefix = "&#FF5050&lSURVIVAL &#555555» &#EAEAEA";
    public Console console = new Console();
    public Commands commands = new Commands();
    public Chat chat = new Chat();
    public PrivateMessages privateMessages = new PrivateMessages();
    public Cosmetics cosmetics = new Cosmetics();
    public Menus menus = new Menus();
    public JoinQuit joinQuit = new JoinQuit();
    public Skins skins = new Skins();

    public static class Cosmetics extends MincoreConfig {
        public String noAccess = "<#FF4C4C>No tienes acceso a este cosmético.";
        @Comment("%cosmetic% = displayName del cosmético que se acaba de equipar (ej. \"Aura Carmesí\").")
        public String equipped = "<#FF2A2A>✦ <#EAEAEA>Equipaste <#FFFFFF>%cosmetic%<#EAEAEA>.";
        @Comment("%cosmetic% = displayName del cosmético que se acaba de desequipar.")
        public String unequipped = "<#8B0000>✦ <#EAEAEA>Desequipaste <#888888>%cosmetic%<#EAEAEA>.";
        public String categoryDisabled = "<#FF4C4C>Esta categoría de cosméticos está desactivada.";
        public List<String> statusEquipped = Arrays.asList(" ",
                "<#333333>▪ <#888888>Estado: <#FF2A2A><bold>Eǫᴜɪᴘᴀᴅᴏ</bold>", " ", "<#FF2A2A>✦ ¡Ya lo llevas puesto!");
        public List<String> statusClickToEquip = Arrays.asList(" ",
                "<#333333>▪ <#888888>Estado: <#EAEAEA><bold>Dᴇꜱʙʟᴏǫᴜᴇᴀᴅᴏ</bold>", " ", "<#FF4D4D>▶ Clic Izq. para equipar");
        public String priceLore = "<#666666>Precio: <#FF2A2A>%price%";

        @Comment("Lore dinámico de un cosmético bloqueado: varía según si al jugador le faltan monedas o ya puede comprarlo (%missing%/%price%).")
        public List<String> statusLockedMissingCoins = Arrays.asList(" ",
                "<#333333>▪ <#888888>Estado: <#444444><bold>Bʟᴏǫᴜᴇᴀᴅᴏ</bold>",
                "<#333333>▪ <#888888>Te faltan: <#FF2A2A>%missing%</#FF2A2A> monedas", " ", "<#8B0000>✖ No tienes suficiente saldo");
        public List<String> statusLockedCanBuy = Arrays.asList(" ",
                "<#333333>▪ <#888888>Estado: <#444444><bold>Bʟᴏǫᴜᴇᴀᴅᴏ</bold>",
                "<#333333>▪ <#888888>Precio: <#FF2A2A>$%price%", " ", "<#FF4D4D>▶ Clic Izq. para comprar");

        @Comment("Lore de un cosmético con permiso EXTRA (cosmetic.permission en el YAML, ej. formats.yml) que el jugador no tiene - a diferencia de statusLockedCanBuy/MissingCoins, esto NO es cuestión de plata, no hay nada que comprar.")
        public List<String> statusLockedNoPermission = Arrays.asList(" ",
                "<#333333>▪ <#888888>Estado: <#444444><bold>Bʟᴏǫᴜᴇᴀᴅᴏ</bold>",
                " ", "<#8B0000>✖ Necesitas un rango místico");
        public String noCoins = "<#FF4C4C>No tienes suficientes monedas. Te faltan <#FF2A2A>%missing%</#FF2A2A>.";
        @Comment("%cosmetic% = displayName del cosmético comprado.")
        public String purchased = "<#FF2A2A>✦ <#EAEAEA>Compraste y equipaste <#FFFFFF>%cosmetic%<#EAEAEA>.";

        @Comment("Color del NOMBRE del ítem (no del lore) en la fila de formats.yml (bold/cursiva/...) según esté activo o no - independiente entre namecolors (nombre) y chatcolors (chat).")
        public String formatActiveColor = "#FF2A2A";
        public String formatInactiveColor = "#555555";

        @Comment("Material del ítem de la fila de formats según su estado (verde prendido / gris apagado) - dejar vacío para usar siempre el material fijo de formats.yml.")
        public String formatActiveMaterial = "RED_DYE";
        public String formatInactiveMaterial = "GRAY_DYE";

        @Comment("Ícono genérico para un cosmético BLOQUEADO (aún no comprado/desbloqueado) - un \"tinte gris\" en vez del ítem real, que solo se muestra una vez obtenido. Dejar vacío para desactivar y mostrar siempre el material real del YAML.")
        public String lockedMaterial = "GRAY_DYE";

        @Comment({
                "Previsualización (clic derecho en un cosmético del menú, sin equiparlo",
                "ni gastar monedas). glows/prefixes/icons quedan fuera - dependen de",
                "estado externo persistente (LuckPerms/scoreboard) que no conviene",
                "activar y desactivar solo por unos segundos."
        })
        public String previewHint = "<#FF4D4D>▶ Clic derecho para previsualizar";
        public String cannotPreview = "<#FF4C4C>Este cosmético es visual, equípalo para verlo.";
        public String previewingEffect = "<#FF4D4D>✦ Previsualizando efecto...";
        public String previewingMessage = "<#FF4D4D>✦ Previsualizando mensaje de entrada:";
        @Comment("Antes se reusaba previewingMessage (\"...de entrada\") también para kill/death - quedaba mal etiquetado al previsualizar una muerte/baja.")
        public String previewingKillMessage = "<#FF4D4D>✦ Previsualizando mensaje de baja:";
        public String previewingDeathMessage = "<#FF4D4D>✦ Previsualizando mensaje de muerte:";
        public String previewNamecolorLine = "%color%%player% <#444444>» <#EAEAEA>¡Así se vería mi nombre en el chat!";
        public String previewChatcolorLine = "<#FFFFFF>%player% <#444444>» %color%¡Así se vería mi mensaje en el chat!";
        public String previewZoneEntering = "<#FF2A2A>✦ Transportándote a la grieta de previsualización...";
    }

    public static class Menus extends MincoreConfig {
        public String mainTitle = "<gradient:#FF2A2A:#8B0000>Ajustes Personales</gradient>";
        public String categoriesTitle = "<gradient:#FF2A2A:#8B0000>Cosméticos</gradient> <#444444>» <#EAEAEA>Categorías";
        public String categoryTitle = "<gradient:#FF2A2A:#8B0000>Cosméticos</gradient> <#444444>» <#EAEAEA>%category%";
        public String categoryUnlockedLore = "<#666666>Desbloqueados: <#EAEAEA>%unlocked%<#666666>/<#EAEAEA>%total%";
        public String categoryEquippedLore = "<#666666>Equipado: <#FF2A2A>%equipped%";
        public String categoryNothingEquipped = "<#555555>Ninguno";

        @Comment("Nombre/lore/material de cada botón fijo del menú (perfil, cosméticos, paginación, volver...) ahora viven en su propio menus/*.yml, no aquí - esto solo queda para el estado ON/OFF de los toggles, que es dinámico.")
        public String toggleOn = "<#55FF55>✔ Activado <#333333>▪ <#888888>Clic para desactivar";
        public String toggleOff = "<#FF5555>✘ Desactivado <#333333>▪ <#888888>Clic para activar";
        public String toggleLocked = "<#888888>✖ Bloqueado <#555555>(Requiere Rango)";
    }

    public static class JoinQuit extends MincoreConfig {
        public String joinMessage = "<#333333>[<#FF2A2A>+<#333333>] <#EAEAEA>%player% <#555555>emergió de la grieta.";
        public String quitMessage = "<#333333>[<#8B0000>-<#333333>] <#666666>%player% <#444444>regresó a la oscuridad.";

        @Comment({
                "%skinmotd_1% .. %skinmotd_8% = las 8 filas de la cara de la skin del jugador",
                "renderizada en bloques de color (ver modules.motdSkinFace en config.yml).",
                "%player% = nombre del jugador que entra."
        })
        public List<String> joinMotd = Arrays.asList(
                "<#333333><strikethrough>                                                  </strikethrough>",
                " ",
                "   %skinmotd_1%   <gradient:#FF2A2A:#8B0000><b>RIFT MYSTIC NETWORK</b></gradient>",
                "   %skinmotd_2%   <#888888>Bienvenido a las profundidades, <#EAEAEA>%player%",
                "   %skinmotd_3% ",
                "   %skinmotd_4%   <#333333>▪ <#888888>Personaliza tu perfil con <#FF2A2A>/cosmetics",
                "   %skinmotd_5%   <#333333>▪ <#888888>Consulta tus estadísticas con <#FF2A2A>/perfil",
                "   %skinmotd_6% ",
                "   %skinmotd_7% ",
                "   %skinmotd_8% ",
                " ",
                "<#333333><strikethrough>                                                  </strikethrough>"
        );
    }

    public static class PrivateMessages extends MincoreConfig {
        public String playerOffline = "<#FF4C4C>Ese jugador no está conectado.";
        public String cannotMessageSelf = "<#FF4C4C>No puedes enviarte un mensaje a ti mismo.";
        public String targetDisabled = "<#FF4C4C>Ese jugador tiene los mensajes privados desactivados.";
        public String noReplyTarget = "<#FF4C4C>No tienes a quién responder.";
        public String senderFormat = "<#888888>[<#FF2A2A>MSG<#888888>] <#EAEAEA>Tú <#444444>» <#FF4D4D>%target%<#444444>: <#FFFFFF>";
        public String targetFormat = "<#888888>[<#FF2A2A>MSG<#888888>] <#FF4D4D>%sender% <#444444>» <#EAEAEA>Tú<#444444>: <#FFFFFF>";
    }

    public static class Chat extends MincoreConfig {
        public String filterSpam = "<#FF4C4C>Estás enviando mensajes muy rápido.";
        public String filterFlood = "<#FF4C4C>Aguarda antes de enviar otro mensaje al abismo.";
        public String filterTooLong = "<#FF4C4C>Tu mensaje es demasiado largo.";
        public String filterRepetition = "<#FF4C4C>No repitas el mismo mensaje.";
        @Comment("%word% = la palabra/frase concreta que disparó el bloqueo (sin censurar, tal cual la escribió el jugador).")
        public String filterBadWord = "<#FF4C4C>Tu mensaje contiene una palabra prohibida: <#FFFFFF>%word%";
        public String filterAds = "<#FF4C4C>No está permitido difundir enlaces o publicidad externa.";
        public String filterMuted = "<#FF4C4C>Tu voz ha sido silenciada por el abismo, no puedes hablar.";
        public String aiMessageRemoved = "<#FF4C4C>Tu mensaje fue purgado por los centinelas por toxicidad.";
        @Comment("%player% = quién lo escribió, %message% = el mensaje borrado.")
        public String aiStaffAlert = "<gradient:#FF2A2A:#8B0000>[IA-Abismo]</gradient> <#FFFFFF>Mensaje purgado de %player%: <#888888>%message%";
        public String messageDeleted = "<#FF2A2A>✔ <#EAEAEA>El mensaje <#FFFFFF>#%id% <#EAEAEA>ha sido eliminado del chat.";
        public String messageNotFound = "<#FF4C4C>No se encontró el mensaje o ya expiró.";
        public String deleteNoPermission = "<#FF4C4C>No tienes permiso para eliminar mensajes.";
        public String injectionBlocked = "<#FF4C4C>%player% intentó inyectar código en el chat: <#FFFFFF>%message%";
        @Comment("%message% = el mensaje original que escribió el jugador (sin censurar), la razón concreta ya la ve el jugador en su propio aviso (ej. filterBadWord -> %word%).")
        public String staffAlert = "<gradient:#FF2A2A:#8B0000>[Filtro]</gradient> <#FFFFFF>%player% <#888888>bloqueado en chat: <#EAEAEA>%message%";
        public String warningActionbar = "<#FF4D4D>Aviso <#FFFFFF>%current%/%max% <#FF4D4D>por conducta inapropiada.";
    }

    public static class Console extends MincoreConfig {
        public List<String> startupLogo = Arrays.asList(
                "",
                "<gradient:#FF2A2A:#8B0000>  ██████╗ ██╗███████╗████████╗███╗   ███╗██╗   ██╗███████╗████████╗██╗ ██████╗ </gradient>",
                "<gradient:#FF2A2A:#8B0000>  ██╔══██╗██║██╔════╝╚══██╔══╝████╗ ████║╚██╗ ██╔╝██╔════╝╚══██╔══╝██║██╔════╝ </gradient>",
                "<gradient:#FF4D4D:#7A0000>  ██████╔╝██║█████╗     ██║   ██╔████╔██║ ╚████╔╝ ███████╗   ██║   ██║██║      </gradient>",
                "<gradient:#FF4D4D:#7A0000>  ██╔══██╗██║██╔══╝     ██║   ██║╚██╔╝██║  ╚██╔╝  ╚════██║   ██║   ██║██║      </gradient>",
                "<gradient:#8B0000:#330000>  ██║  ██║██║██║        ██║   ██║ ╚═╝ ██║   ██║   ███████║   ██║   ██║╚██████╗ </gradient>",
                "<gradient:#8B0000:#330000>  ╚═╝  ╚═╝╚═╝╚═╝        ╚═╝   ╚═╝     ╚═╝   ╚═╝   ╚══════╝   ╚═╝   ╚═╝ ╚═════╝ </gradient>",
                ""
        );

        public List<String> startupInfo = Arrays.asList(
                "<gradient:#FF2A2A:#8B0000>✦ Información de RiftMystic Core:</gradient>",
                "<#555555>  • <#888888>Autor: <#FF4D4D>%author%",
                "<#555555>  • <#888888>Descripción: <#EAEAEA>%description%",
                "<#555555>  • <#888888>Versión: <#FF2A2A>%version%",
                "",
                "<gradient:#FF2A2A:#8B0000>✦ Información del Servidor:</gradient>",
                "<#555555>  • <#888888>Motor: <#FF4D4D>%fork% <#555555>(%server_version%)",
                "<#555555>  • <#888888>Java: <#FF4D4D>%java%",
                "<#555555>  • <#888888>Soporte: %supported_versions%",
                "",
                "<gradient:#FF2A2A:#8B0000>✦ Motores y Conexiones:</gradient>",
                "<#555555>  • <#888888>Base de Datos: %database%",
                "<#555555>  • <#888888>Redis (Sync): %redis%",
                "",
                "<gradient:#FF2A2A:#8B0000>✦ Hooks y Dependencias:</gradient>",
                "<#555555>  • <#888888>PlaceholderAPI: %hook_papi%",
                "",
                "<#333333>» <#FF2A2A>RiftMystic cargado exitosamente en <#FFFFFF>%time%ms<#FF2A2A>."
        );

        public String shutdownMessage = "<#8B0000>Apagando RiftMystic Core... retornando al abismo.";
        public String errorPrefix = "<#FF4C4C>ERROR: ";
        public String configUpdated = "<#FFEB3B>El archivo <#FFFFFF>%file% <#FFEB3B>estaba desactualizado (v%old%). Se inyectaron los datos faltantes automáticamente (v%new%).";
        public String configDowngraded = "<#FF4C4C>El archivo <#FFFFFF>%file% <#FF4C4C>es más moderno (v%old%) que el plugin (v%new%). ¡Podría generar errores!";
    }

    public static class Commands extends MincoreConfig {
        public String reloadSuccess = "<#FF2A2A>✔ <#EAEAEA>Configuraciones de RiftMystic recargadas en <#FFFFFF>%ms%ms<#EAEAEA>.";
        @Comment("Respuesta de /mincore reload catalog - sobrescribe los YAML de cosméticos en el dataFolder con el catálogo embebido en el jar, incluso si el admin ya los había editado (útil en desarrollo; en producción borra ediciones manuales de esos archivos).")
        public String resetCatalogSuccess = "<#FF2A2A>✔ <#EAEAEA>Catálogo de cosméticos restaurado desde el jar en <#FFFFFF>%ms%ms<#EAEAEA>.";
        @Comment("Respuesta de /mincore commandblocker reload.")
        public String commandBlockerReloadSuccess = "<#FF2A2A>✔ <#EAEAEA>commandblocker.yml recargado en <#FFFFFF>%ms%ms<#EAEAEA>.";
        public String noPermission = "<#FF4C4C>No tienes permiso para usar este comando.";
        public String playersOnly = "<#FF4C4C>Este comando solo puede ser ejecutado por un jugador.";
        public String playerNotFound = "<#FF4C4C>Ese jugador no está conectado.";
        public String cosmeticsDisabled = "<#FF4C4C>Los cosméticos están desactivados.";
        public String profileDisabled = "<#FF4C4C>El menú de perfil está desactivado.";
        public List<String> help = Arrays.asList(
                "",
                "<gradient:#FF2A2A:#8B0000><b>RiftMystic Core</b></gradient> <#444444>| <#888888>Lista de Comandos",
                "<#FF2A2A>» <#EAEAEA>/coreec reload <#555555>- <#888888>Recarga las configuraciones.",
                "<#FF2A2A>» <#EAEAEA>/coreec reload catalog <#555555>- <#888888>Restaura los YAML de cosméticos desde el jar.",
                "<#FF2A2A>» <#EAEAEA>/coreec help <#555555>- <#888888>Muestra este mensaje.",
                "<#FF2A2A>» <#EAEAEA>/coreec menu <#555555>- <#888888>Abre el menú principal.",
                "<#FF2A2A>» <#EAEAEA>/coreec eco <give|take|set> <jugador> <cantidad> <#555555>- <#888888>Administra monedas.",
                "<#FF2A2A>» <#EAEAEA>/coreec sync <push|pull> <#555555>- <#888888>Sincroniza catálogo y configuración con la red.",
                "<#FF2A2A>» <#EAEAEA>/coreec commandblocker reload <#555555>- <#888888>Recarga el bloqueador de comandos.",
                "<#FF2A2A>» <#EAEAEA>/coreec ai <refreshwords|refreshbadwords|addbadword|pending|block|allow> <#555555>- <#888888>Filtro de chat con IA.",
                "<#FF2A2A>» <#EAEAEA>/coreec clearchat <#555555>- <#888888>Limpia el chat.",
                "<#FF2A2A>» <#EAEAEA>/coreec setpreviewzone <#555555>- <#888888>Fija la zona de previsualización de cosméticos.",
                "",
                "<#555555>Sistemas: <#888888>/pozomillonario, /sanction, /reports, /staffmode, /vanish, /invsee, /enderchest, /disguise, /cosmetics, /perfil",
                "<#555555>Essentials: <#888888>/air, /heal, /god, /tp, /tphere, /spawn, /setspawn, /speed, /near, /playerinfo, /staff y más.",
                ""
        );

        @Comment({
                "Mensajes de sintaxis inválida de Lamp (MissingArgumentException, EnumNotFoundException, etc.) traducidos - ver MincoreExceptionHandler.",
                "%usage% = sintaxis exacta del comando (ej. '/tp <jugador> [destino]')",
                "%parameter% = nombre del parámetro puntual que falló",
                "%example% = ejemplo práctico autogenerado con valores realistas (ej. '/tp Steve Alex')"
        })
        public String invalidUsage = "<#FF4C4C>Uso correcto: <#EAEAEA>%usage%\n<#888888>Ejemplo:\n<#FF4D4D>%example%";
        public String invalidChoice = "<#8B0000>[<#FF2A2A>Sintaxis<#8B0000>] <#FF4C4C>Opción inválida: <#FFFFFF>%input%<#FF4C4C>.";
        public String invalidNumber = "<#8B0000>[<#FF2A2A>Sintaxis<#8B0000>] <#FF4C4C>Número inválido: <#FFFFFF>%input%<#FF4C4C>.";
        public String invalidBoolean = "<#8B0000>[<#FF2A2A>Sintaxis<#8B0000>] <#FF4C4C>Se esperaba <#FFFFFF>true<#FF4C4C> o <#FFFFFF>false<#FF4C4C>, no <#FFFFFF>%input%<#FF4C4C>.";
        public String numberNotInRange = "<#8B0000>[<#FF2A2A>Sintaxis<#8B0000>] <#FF4C4C>El valor <#FFFFFF>%input%<#FF4C4C> está fuera de rango (<#FFFFFF>%min%<#FF4C4C>-<#FFFFFF>%max%<#FF4C4C>).";
        public String unknownCommand = "<#8B0000>[<#FF2A2A>Error<#8B0000>] <#FF4C4C>Comando desconocido: <#FFFFFF>%input%";
        public String expectedLiteral = "<#8B0000>[<#FF2A2A>Sintaxis<#8B0000>] <#FF4C4C>Se esperaba <#FFFFFF>%expected%<#FF4C4C>, no <#FFFFFF>%input%<#FF4C4C>.";

        public String ecoPlayerOffline = "<#FF4C4C>Ese jugador no está conectado.";
        public String ecoInvalidAmount = "<#FF4C4C>La cantidad debe ser un número positivo.";
        public String ecoGiveSuccess = "<#FF2A2A>✦ <#EAEAEA>Le diste <#FFFFFF>%amount% <#EAEAEA>monedas a <#FFFFFF>%player%<#EAEAEA>.";
        public String ecoTakeSuccess = "<#8B0000>✦ <#EAEAEA>Le quitaste <#FFFFFF>%amount% <#EAEAEA>monedas a <#FFFFFF>%player%<#EAEAEA>.";
        public String ecoSetSuccess = "<#FF2A2A>✦ <#EAEAEA>Estableciste el saldo de <#FFFFFF>%player% <#EAEAEA>en <#FFFFFF>%amount%<#EAEAEA>.";

        @Comment({
                "Economía dual: %balance% = cantidad formateada. Los mensajes de pay",
                "son comunes a coins y sucres (%currency% = \"Coins\"/\"Sucres\")."
        })
        public String balanceCoins = "<#555555>» <#888888>Monedas: <#FF2A2A>%balance%";
        public String balanceCoinsOther = "<#555555>» <#888888>Monedas de %player%: <#FF2A2A>%balance%";
        public String balanceSucres = "<#555555>» <#888888>Sucres: <#FF4D4D>%balance%";
        public String balanceSucresOther = "<#555555>» <#888888>Sucres de %player%: <#FF4D4D>%balance%";
        public String baltopHeader = "<gradient:#FF2A2A:#8B0000><bold>✦ Top 10 Jugadores Más Ricos (Coins) ✦</bold></gradient>";
        public String baltopEntry = "<#333333>#%pos% <#EAEAEA>%player% <#555555>▪ <#FF2A2A>$%balance%";
        public String baltopEmpty = "<#888888>No hay datos de economía registrados.";
        public String insufficientFunds = "<#FF4C4C>No tienes suficientes fondos.";
        public String cannotPaySelf = "<#FF4C4C>No puedes pagarte a ti mismo.";
        public String paySent = "<#FF2A2A>✦ <#EAEAEA>Le enviaste <#FFFFFF>%amount% %currency% <#EAEAEA>a <#FFFFFF>%target%<#EAEAEA>.";
        public String payReceived = "<#FF2A2A>✦ <#EAEAEA>Recibiste <#FFFFFF>%amount% %currency% <#EAEAEA>de <#FFFFFF>%sender%<#EAEAEA>.";
        public String payFailed = "<#FF4C4C>No se pudo completar la transferencia.";
        public String sucreGiveSuccess = "<#FF2A2A>✦ <#EAEAEA>Le diste <#FFFFFF>%amount% <#EAEAEA>sucres a <#FFFFFF>%player%<#EAEAEA>.";
        public String sucreTakeSuccess = "<#8B0000>✦ <#EAEAEA>Le quitaste <#FFFFFF>%amount% <#EAEAEA>sucres a <#FFFFFF>%player%<#EAEAEA>.";
        public String sucreSetSuccess = "<#FF2A2A>✦ <#EAEAEA>Estableciste el saldo de sucres de <#FFFFFF>%player% <#EAEAEA>en <#FFFFFF>%amount%<#EAEAEA>.";

        public String flyEnabled = "<#FF2A2A>✔ <#EAEAEA>Vuelo activado.";
        public String flyDisabled = "<#8B0000>✘ <#EAEAEA>Vuelo desactivado.";
        @Comment("Confirmación para quien ejecuta /fly sobre OTRO jugador - sin esto el admin no se entera si funcionó, porque el mensaje de flyEnabled/flyDisabled solo se le manda al jugador afectado.")
        public String flyEnabledOther = "<#FF2A2A>✔ <#EAEAEA>Le activaste el vuelo a <#FFFFFF>%player%<#EAEAEA>.";
        public String flyDisabledOther = "<#8B0000>✘ <#EAEAEA>Le desactivaste el vuelo a <#FFFFFF>%player%<#EAEAEA>.";
        public String gamemodeChanged = "<#FF2A2A>✦ <#EAEAEA>Modo de juego cambiado a <#FFFFFF>%gamemode%<#EAEAEA>.";
        @Comment("Confirmación para quien ejecuta /gmc, /gms, /gma o /gmsp sobre OTRO jugador - mismo motivo que flyEnabledOther/flyDisabledOther.")
        public String gamemodeChangedOther = "<#FF2A2A>✦ <#EAEAEA>Cambiaste el modo de juego de <#FFFFFF>%player% <#EAEAEA>a <#FFFFFF>%gamemode%<#EAEAEA>.";
        public String syncPush = "<#FF2A2A>✦ <#EAEAEA>Sincronización (push) de catálogo iniciada.";
        public String syncPull = "<#FF2A2A>✦ <#EAEAEA>Sincronización (pull) de catálogo iniciada.";
        public String clearchatDone = "<#FF2A2A>✦ <#EAEAEA>El chat ha sido purgado por <#FFFFFF>%player%<#EAEAEA>.";
        public String previewZoneSet = "<#FF2A2A>✦ <#EAEAEA>Zona de previsualización fijada en tu posición actual.";

        public String aiPendingEmpty = "<#888888>No hay patrones pendientes de revisión.";
        public String aiPendingEntry = "<#FF4D4D>#%id% <#888888>(%player%: \"%message%\") <#EAEAEA>%regex%";
        public String aiBlockSuccess = "<#FF2A2A>✔ <#EAEAEA>Patrón #%id% bloqueado: <#FFFFFF>%regex%";
        public String aiBlockNotFound = "<#FF4C4C>No hay ningún patrón pendiente con el ID %id%.";
        public String aiAllowSuccess = "<#888888>Patrón #%id% permitido.";
        public String aiAllowNotFound = "<#FF4C4C>No hay ningún patrón pendiente con el ID %id%.";
        public String aiReviewWordsGenerating = "<#FF4C4C>Ya hay una generación de reviewWords en curso, espera a que termine.";
        public String aiReviewWordsFailed = "<#FF4C4C>No se pudo generar la lista - revisa la consola.";
        public String aiReviewWordsNone = "<#888888>La IA no encontró palabras nuevas.";
        public String aiReviewWordsAdded = "<#FF2A2A>✦ <#EAEAEA>Se agregaron %added% palabras nuevas: <#FFFFFF>%words%";
        public String aiBadWordsGenerating = "<#FF4C4C>Ya hay una generación de badWords en curso, espera a que termine.";
        public String aiBadWordsFailed = "<#FF4C4C>No se pudo generar el blocklist - revisa la consola.";
        public String aiBadWordsSummary = "<#FF2A2A>✦ <#EAEAEA>La IA devolvió %total% regex, %added% eran nuevas.";
        public String aiAddBadWordPrompt = "<#FF4C4C>Escribe la palabra que quieres agregar.";
        public String aiAddBadWordGenerating = "<#FF4C4C>Generando badWords...";
        public String aiAddBadWordSuccess = "<#FF2A2A>✔ <#EAEAEA>Se agregó \"%word%\" al blocklist.";
        public String aiAddBadWordAlreadyExists = "<#888888>La regex para \"%word%\" ya existía.";
        public String aiAddBadWordFailed = "<#FF4C4C>No se pudo generar una regex para \"%word%\".";
    }

    public static class Skins extends MincoreConfig {
        @Comment("Mensajes del sistema de previsualización de skins (maniquí rotatorio / cabezas de confirmación).")
        public String alreadyInSession = "<#8B0000>✖ <#EAEAEA>Ya tienes una previsualización de skin activa.";
        @Comment("%skin% = nombre de la skin que se está buscando.")
        public String fetching = "<#FF4D4D>✦ <#EAEAEA>Invocando apariencia de <#FFFFFF>%skin%<#EAEAEA>...";
        @Comment("%skin% = nombre de la skin que no se encontró o falló al obtener.")
        public String fetchFailed = "<#8B0000>✖ <#EAEAEA>No se pudo manifestar la skin <#FF4D4D>%skin%<#EAEAEA>. Verifica el nombre.";
        public String inputPrompt = "<#FF2A2A>✦ <#EAEAEA>Escribe en el chat el nombre de la skin a invocar:";
        public String inputCancelled = "<#888888>Previsualización cancelada.";
        public String noActiveSession = "<#8B0000>✖ <#EAEAEA>No tienes una previsualización de skin activa.";
        @Comment("%skin% = nombre de la skin aplicada.")
        public String applied = "<#FF2A2A>✔ <#EAEAEA>¡Apariencia <#FFFFFF>%skin% <#EAEAEA>vinculada a tu alma!";
        public String discarded = "<#888888>Cambios descartados. Conservas tu apariencia anterior.";
        public String resetSuccess = "<#FF2A2A>✔ <#EAEAEA>Apariencia restablecida a tu forma original.";
        @Comment("Se muestra cuando /skin se usa sin haber configurado el punto del maniquí (config.yml -> modules.skins.npcZone).")
        public String noZoneSet = "<#8B0000>✖ <#EAEAEA>No hay punto del maniquí configurado. Un admin debe usar <#FF4D4D>/coreec setskinpreviewzone npc<#EAEAEA>.";
        public String zoneUsage = "<#8B0000>✖ <#EAEAEA>Uso: <#FF4D4D>/coreec setskinpreviewzone <npc|spectator>";
        @Comment("Se muestra al configurar el punto del maniquí (npc).")
        public String npcZoneSet = "<#FF2A2A>✔ <#EAEAEA>Punto del maniquí establecido en tu posición actual.";
        @Comment("Se muestra al configurar el punto del espectador (parado ahí verás el maniquí).")
        public String spectatorZoneSet = "<#FF2A2A>✔ <#EAEAEA>Punto del espectador establecido en tu posición actual.";
        @Comment("%skin% = nombre de la skin que se acaba de cargar en el maniquí de la previsualización.")
        public String previewUpdated = "<#FF2A2A>✦ <#EAEAEA>Maniquí actualizado: <#FFFFFF>%skin%<#EAEAEA>. Escribe otro nombre o toca una calavera.";
        @Comment("Se envía cada vez que el jugador escribe un nombre de skin mientras la previsualización está activa (anti-spam de fetchs en el chat mundial).")
        public String inputCaptured = "<#888888>Rastreando apariencia <#FFFFFF>%skin%<#888888> en la grieta...";
        @Comment("%seconds% = segundos de cooldown restantes antes de poder usar /skin otra vez.")
        public String cooldownExceeded = "<#8B0000>✖ <#EAEAEA>Debes esperar %seconds% segundos antes de volver a usar /skin.";

        @Comment("Banner que se muestra al iniciar la previsualización. %skin% = nombre de la skin a previsualizar.")
        public List<String> previewHeader = Arrays.asList(" ",
                "<gradient:#FF2A2A:#8B0000><bold>» PREVISUALIZACIÓN DE SKIN: <#FFFFFF>%skin% <gradient:#FF2A2A:#8B0000>«</bold></gradient>",
                "<#888888>Interactúa con las calaveras flotantes para decidir",
                "<#FF2A2A>✔ Confirmar   <#8B0000>✖ Cancelar",
                " ");

        @Comment("Nombre visible de las cabezas de decisión flotantes a los lados del maniquí.")
        public String headConfirm = "<#FF2A2A>✔ <bold>CONFIRMAR</bold>";
        public String headCancel = "<#8B0000>✖ <bold>CANCELAR</bold>";

        @Comment("URL de la textura Base64 de la cabeza de confirmación.")
        public String headConfirmTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDMxMmNhNDYzMmRlZjVmZmFmMmViMGQ5ZDdjYzdiNTVhNTBjNGUzOTIwZDkwMzcyYWFiMTQwNzgxZjVkZmJjNCJ9fX0=";

        @Comment("URL de la textura Base64 de la cabeza de cancelación.")
        public String headCancelTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTljZGI5YWYzOGNmNDFkYWE1M2JjOGNkYTc2NjVjNTA5NjMyZDE0ZTY3OGYwZjE5ZjI2M2Y0NmU1NDFkOGEzMCJ9fX0=";

        @Comment("Distancia lateral de las cabezas de confirmación/cancelación respecto al NPC.")
        public double headSideDistance = 1.8;

        @Comment("Desplazamiento hacia delante (+) o atrás (-) de las cabezas respecto al NPC.")
        public double headForwardOffset = 0.0;

        @Comment("Altura relativa de las cabezas de confirmación/cancelación respecto al cuerpo del NPC.")
        public double headHeightOffset = 1.5;

        @Comment("Offset vertical de la primera línea de holograma.")
        public double hologramLine1YOffset = 2.6;

        @Comment("Offset vertical de la segunda línea de holograma.")
        public double hologramLine2YOffset = 2.3;

        @Comment("Hologramas sobre el NPC en el modo de previsualización. %skin% se reemplaza.")
        public String previewHologramLine1 = "<#888888>Apariencia invocada:";
        public String previewHologramLine2 = "<#FF2A2A>%skin%";
    }
}