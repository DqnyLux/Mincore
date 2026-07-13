package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FiltersConfig extends MincoreConfig {

    @Comment("Permiso que exime a un jugador de TODOS los filtros de chat.")
    public String bypassPermission = "mincore.chat.bypass";

    public AntiSpam antiSpam = new AntiSpam();
    public Caps caps = new Caps();
    public Repetition repetition = new Repetition();
    public BadWords badWords = new BadWords();
    public Ads ads = new Ads();
    public Punishment punishment = new Punishment();

    public static class AntiSpam extends MincoreConfig {
        @Comment("¿Activar el filtro anti-spam por ráfaga de mensajes?")
        public boolean enabled = true;

        @Comment("Ventana de tiempo (segundos) en la que se cuentan los mensajes.")
        public int delaySeconds = 3;

        @Comment("Mensajes permitidos dentro de la ventana antes de cancelar.")
        public int maxMessages = 3;
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

        @Comment("Cantidad de caracteres idénticos consecutivos a partir de la cual se colapsan (ej. 'hahahaha' -> 'haha').")
        public int repeatedCharThreshold = 4;
    }

    public static class BadWords extends MincoreConfig {
        @Comment("¿Activar el filtro de palabras prohibidas?")
        public boolean enabled = true;

        @Comment("Si es true, censura la palabra con asteriscos en vez de cancelar el mensaje completo.")
        public boolean replaceWords = true;

        @Comment("Lista de palabras prohibidas. Tolera espaciado y leetspeak básico (ej. 'p4l4br4').")
        public List<String> words = new ArrayList<>(Arrays.asList("ejemplo1", "ejemplo2"));
    }

    public static class Ads extends MincoreConfig {
        @Comment("¿Activar el filtro de anuncios/enlaces no autorizados?")
        public boolean enabled = true;

        @Comment("Dominios permitidos pese a que el filtro detecte un enlace.")
        public List<String> whitelist = new ArrayList<>(Arrays.asList("minecuador.lat", "discord.gg"));
    }

    public static class Punishment extends MincoreConfig {
        @Comment("Avisos acumulados antes de ejecutar el comando de castigo.")
        public int maxWarnings = 3;

        @Comment("Comando ejecutado desde consola al alcanzar el máximo de avisos. %player% se reemplaza por el nombre.")
        public String punishCommand = "mute %player% 10m Lenguaje inapropiado";

        @Comment("Permiso que reciben las alertas de staff cuando se cancela un mensaje.")
        public String staffAlertPermission = "mincore.staff.alerts";
    }
}
