package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Include(MincoreConfig.class)
public class FiltersConfig extends MincoreConfig {

    @Comment("Permiso que exime a un jugador de TODOS los filtros de chat.")
    public String bypassPermission = "coreec.chat.bypass";

    public AntiSpam antiSpam = new AntiSpam();
    public FloodProtection floodProtection = new FloodProtection();
    public MessageLength messageLength = new MessageLength();
    public Caps caps = new Caps();
    public Repetition repetition = new Repetition();
    public BadWords badWords = new BadWords();
    public Ads ads = new Ads();
    public Punishment punishment = new Punishment();
    public AiModeration aiModeration = new AiModeration();

    public static class AntiSpam extends MincoreConfig {
        @Comment("¿Activar el filtro anti-spam por ráfaga de mensajes?")
        public boolean enabled = true;

        @Comment("Ventana de tiempo (segundos) en la que se cuentan los mensajes.")
        public int delaySeconds = 3;

        @Comment("Mensajes permitidos dentro de la ventana antes de cancelar.")
        public int maxMessages = 3;
    }

    /**
     * Distinto de AntiSpam (que cuenta mensajes por ráfaga en una ventana):
     * esto exige un tiempo MÍNIMO entre un mensaje y el siguiente sin
     * importar cuántos van en una ventana - útil contra bots/macros que
     * mandan mensajes muy seguido pero por debajo del umbral de ráfaga.
     */
    public static class FloodProtection extends MincoreConfig {
        @Comment("¿Activar la protección anti-flood por tiempo mínimo entre mensajes?")
        public boolean enabled = false;

        @Comment("Milisegundos mínimos que debe esperar un jugador entre un mensaje y el siguiente.")
        public int minMillisBetweenMessages = 800;
    }

    public static class MessageLength extends MincoreConfig {
        @Comment("¿Bloquear mensajes que superen la longitud máxima?")
        public boolean enabled = true;

        @Comment("Cantidad máxima de caracteres permitidos en un mensaje.")
        public int maxCharacters = 256;
    }

    public static class Caps extends MincoreConfig {
        @Comment("¿Convertir a minúsculas los mensajes con demasiadas mayúsculas? (no cancela)")
        public boolean enabled = true;

        @Comment("Longitud mínima del mensaje para aplicar este filtro.")
        public int minLength = 5;

        @Comment("Porcentaje de mayúsculas (0.0-1.0) a partir del cual se convierte a minúsculas.")
        public double maxPercentage = 0.6;
    }

    public static class Repetition extends MincoreConfig {
        @Comment("¿Activar la detección de mensajes repetidos?")
        public boolean enabled = true;

        @Comment("Longitud máxima para considerar un mensaje 'corto'.")
        public int shortMessageMaxLength = 5;

        @Comment("Repeticiones idénticas consecutivas permitidas para mensajes cortos antes de cancelar.")
        public int shortMessageMaxRepeats = 3;

        @Comment("Similitud (0.0-1.0, distancia de Levenshtein) a partir de la cual un mensaje largo se considera repetido.")
        public double longMessageSimilarityThreshold = 0.85;

        @Comment("Cantidad de caracteres idénticos consecutivos a partir de la cual se colapsan (ej. 'hahahaha' -> 'haha') - límite POR DEFECTO, ver customCharacterLimits para excepciones por carácter.")
        public int repeatedCharThreshold = 4;

        @Comment({"Límites específicos por carácter que sobreescriben repeatedCharThreshold para",
                "ese carácter puntual - formato \"<CARACTER>;;<LIMITE>\" (mismo formato que",
                "custom-limits de ShieldChat, para poder migrar valores directo).",
                "Ejemplo: \"!;;10\" permite hasta 10 \"!\" seguidos antes de colapsar."})
        public List<String> customCharacterLimits = new ArrayList<>(Arrays.asList("!;;10", ".;;10"));

        @Comment({"Contra cuántos de los últimos mensajes del jugador se compara la similitud",
                "(no solo el último) - evita evadir el filtro alternando entre 2-3 mensajes",
                "parecidos en vez de repetir siempre el mismo (idea de ChatSentinel)."})
        public int compareLastMessages = 3;

        @Comment({"Antes de medir similitud, ¿se normaliza el texto (colores/formato, acentos,",
                "caracteres especiales, letras repetidas colapsadas a una sola) para que",
                "'HOLA!!!' y 'hola' cuenten como el mismo mensaje? Desactivarlo vuelve la",
                "comparación literal (más estricta, más fácil de evadir con puntuación extra)."})
        public boolean stripAccents = true;
        public boolean stripSpecialCharacters = true;
        public boolean collapseRepeatedCharactersForComparison = true;
    }

    public static class BadWords extends MincoreConfig {
        @Comment("¿Activar el filtro de palabras prohibidas?")
        public boolean enabled = true;

        @Comment("Si es true, censura la palabra con asteriscos en vez de cancelar el mensaje completo.")
        public boolean replaceWords = true;

        @Comment({"Lista de palabras prohibidas escritas a mano. Cada palabra se expande sola a un",
                "regex tolerante a espaciado y leetspeak (ej. \"puta\" también bloquea \"p.u.t.a\", \"pvt4\").",
                "El blocklist generado por IA (regex real, más completo) vive aparte en",
                "plugins/CoreEC/badwords_regex.txt - ver regexGenerationPrompt más abajo."})
        public List<String> words = new ArrayList<>();

        @Comment({"Nombre del archivo .txt (dentro de la carpeta del plugin) donde vive el blocklist",
                "de regex generado por IA - una expresión regular de Java por línea, líneas que",
                "empiezan con # se ignoran. Si el archivo no existe o está vacío al arrancar (y",
                "enabled=true con una apiKey de Gemini configurada en aiModeration), se genera",
                "solo. /coreec ai refreshbadwords AGREGA patrones nuevos sin borrar los que ya",
                "estén - podés además editar el archivo a mano, una regex por línea."})
        public String regexFile = "badwords_regex.txt";

        @Comment("Instrucción usada para que Gemini genere el archivo de arriba (regexFile).")
        public String regexGenerationPrompt = "Generá una lista extensa (60-100 líneas) de EXPRESIONES REGULARES "
                + "de Java (java.util.regex) que detecten insultos, groserías, términos discriminatorios y "
                + "lenguaje sexual explícito, en ESPAÑOL e INGLÉS mezclados, para el blocklist duro de un "
                + "servidor de Minecraft. Cada línea es UN regex independiente. Seguí ESTE estilo exacto (son "
                + "ejemplos reales ya en uso, imitá la estructura): "
                + "\"(^|[^\\\\wñ])b[0o]l[uú]d([0o]|[i1]t[0o]|[i1]n)(s)*([^\\\\wñ]|$)\" para \"boludo\", "
                + "\"(^|[^\\\\wñ])[\\\\w]?sh(i|1|!)t(head)*(s)*[\\\\w]?([^\\\\wñ]|$)\" para \"shit\". "
                + "Patrón general: \"(^|[^\\\\wñ])\" al inicio y \"([^\\\\wñ]|$)\" al final para exigir un límite "
                + "de palabra real (así no matchea adentro de otra palabra más larga), y entre letras de la "
                + "palabra base usá clases de leetspeak como [a4@], [e3], [i1!], [o0], [s5$] en vez de la letra "
                + "sola cuando tenga sentido. NO agrupes varias palabras distintas en una sola línea con | salvo "
                + "que sean la misma raíz - una línea por concepto. A diferencia de palabras ambiguas por "
                + "contexto (como 'negro' o 'puto' usados como color/apellido), esto tiene que capturar SIEMPRE "
                + "contenido ofensivo, no palabras con doble sentido. Respondé ÚNICAMENTE con los regex, uno por "
                + "línea, sin numerar, sin explicación y sin comillas ni backticks alrededor de cada uno.";

        @Comment({"Palabras que NUNCA se bloquean, aunque coincidan (o se parezcan) a una de la",
                "lista de arriba o al blocklist de regex - para casos donde algo normal genera",
                "falsos positivos. Si una palabra está acá, gana esta (nunca se bloquea)."})
        public List<String> whitelistWords = new ArrayList<>();
    }

    public static class Ads extends MincoreConfig {
        @Comment("¿Activar el filtro de anuncios/enlaces no autorizados?")
        public boolean enabled = true;

        @Comment("Dominios permitidos pese a que el filtro detecte un enlace.")
        public List<String> whitelist = new ArrayList<>(Arrays.asList("minecuador.lat", "discord.gg"));

        @Comment("¿Detectar URLs separadas con espacios? Ej. \"s h i e l d . n e t\" - se colapsan tandas de 4+ \"palabras\" de un solo carácter antes de buscar un enlace, sin tocar oraciones normales.")
        public boolean detectSpacedUrls = true;

        @Comment("¿Normalizar unicode (acentos, letras 'fancy' matemáticas/fraktur que imitan al alfabeto normal, etc.) antes de buscar un enlace? Evita bypasses con caracteres que se ven iguales pero no lo son.")
        public boolean detectUnicodeUrls = true;
    }

    public static class Punishment extends MincoreConfig {
        @Comment("Avisos acumulados antes de ejecutar el comando de castigo.")
        public int maxWarnings = 3;

        @Comment("Comando ejecutado desde consola al alcanzar el máximo de avisos. %player% se reemplaza por el nombre.")
        public String punishCommand = "mute %player% 10m Lenguaje inapropiado";

        @Comment("Permiso que reciben las alertas de staff cuando se cancela un mensaje.")
        public String staffAlertPermission = "coreec.staff.alerts";

        @Comment({
                "¿Mostrar un toast (aviso estilo logro) al jugador cuando recibe un aviso de chat?",
                "NOTA: por una limitación de Minecraft, icon/frame/title/description solo se aplican",
                "al arrancar el servidor - si los editas, reinicia el servidor."
        })
        public boolean toast = true;
        public String toastIcon = "BARRIER";
        @Comment("TASK, GOAL o CHALLENGE.")
        public String toastFrame = "GOAL";
        public String toastTitle = "<#FF4C4C>¡Lenguaje!";
        public String toastDescription = "<#AAAAAA>Revisa tu lenguaje en el chat.";
    }

    /**
     * Revisión por IA (Gemini) para palabras ambiguas por contexto (ej. "negro"
     * como color de un bloque vs. como insulto) que el blocklist fuzzy de
     * BadWords no puede distinguir. Desactivado por defecto - no hace nada
     * hasta que el dueño del server ponga su propia API key y al menos una
     * palabra en reviewWords. La llamada HTTP es BLOQUEANTE a propósito
     * (ver AiModerationClient) - AsyncChatEvent ya corre fuera del hilo
     * principal, así que bloquear ahí no congela el server.
     */
    public static class AiModeration extends MincoreConfig {
        @Comment("¿Activar la revisión por IA? Requiere apiKey propia y al menos una palabra en reviewWords.")
        public boolean enabled = false;

        @Comment("API key de Google Gemini (https://ai.google.dev).")
        public String apiKey = "";

        @Comment("Modelo de Gemini a usar - configurable por si Google lo renombra/retira.")
        public String model = "gemini-3.6-flash";

        @Comment("Timeout de la llamada HTTP en milisegundos - corto a propósito, mejor fallar rápido que trabar el chat.")
        public int timeoutMillis = 1500;

        @Comment("Si la IA falla, tarda más del timeout, o responde algo inesperado, ¿se permite el mensaje (true) o se bloquea (false)? Recomendado: true - nunca bloquear sin un veredicto real.")
        public boolean failOpen = true;

        @Comment("Segundos de cooldown por jugador entre consultas a la IA - evita gastar cuota si spamea palabras de revisión.")
        public int cooldownSeconds = 5;

        @Comment({"¿Loguear cada llamada a Gemini (prompt enviado, respuesta cruda, veredicto) en la consola?",
                "Útil para diagnosticar por qué la IA no está actuando - activalo temporalmente si algo no anda."})
        public boolean debugLogging = false;

        @Comment("Cuántos de los mensajes anteriores del mismo jugador se mandan como contexto en el prompt.")
        public int contextMessages = 2;

        @Comment({"Palabras ambiguas por contexto que disparan la revisión por IA (no confundir con",
                "badWords.words, que siempre bloquea sin excepción). Vacía por defecto - si enabled",
                "es true y esta lista sigue vacía, el propio Gemini la genera UNA VEZ al arrancar",
                "(con wordGenerationPrompt) y la guarda acá - no hace falta escribirla a mano.",
                "/mincore ai refreshwords la regenera cuando quieras."})
        public List<String> reviewWords = new ArrayList<>();

        @Comment("Instrucción usada para que Gemini genere la lista de arriba (solo se usa cuando reviewWords está vacía o al correr /mincore ai refreshwords).")
        public String wordGenerationPrompt = "Generá una lista de 30 palabras en español (podés sumar alguna en "
                + "inglés si es muy común en chats de Minecraft) que tengan DOBLE SENTIDO: pueden ser "
                + "ofensivas o tóxicas usadas de cierta forma, pero también tener un uso completamente "
                + "inocente según el contexto (un color, un objeto, una expresión común, etc.). Son "
                + "palabras que un moderador debería revisar CON CONTEXTO antes de decidir, no palabras "
                + "que se bloquean siempre. Respondé ÚNICAMENTE con las palabras separadas por comas, "
                + "todo en minúscula, sin numerar y sin ninguna explicación.";

        @Comment("Instrucción para el modelo. %context% = mensajes previos del jugador, %message% = mensaje a evaluar.")
        public String systemPrompt = "Sos un moderador de chat de un servidor de Minecraft. Evaluá si el "
                + "mensaje del jugador es tóxico, insultante o discriminatorio, teniendo en cuenta el "
                + "CONTEXTO - la palabra marcada puede referirse a un color, objeto u otra cosa inocente "
                + "en vez de un insulto. Mensajes previos del jugador (más viejo primero):\n%context%\n"
                + "Mensaje a evaluar: \"%message%\"\nRespondé con una sola palabra, sin explicación: TOXICO u OK.";
    }
}
