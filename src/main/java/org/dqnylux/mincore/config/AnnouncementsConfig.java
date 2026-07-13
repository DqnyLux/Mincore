package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnnouncementsConfig extends MincoreConfig {

    public Settings settings = new Settings();

    @Comment("Placeholders reutilizables: %%clave%% en cualquier línea de anuncio se reemplaza por el valor.")
    public Map<String, String> placeholders = defaultPlaceholders();

    public Map<String, AnnouncementEntry> announcements = defaultAnnouncements();

    public static class Settings extends MincoreConfig {
        @Comment("Si es true, cada anuncio usa su propio 'intervalSeconds'. Si es false, hay un único temporizador que rota entre todos los anuncios activos.")
        public boolean perAnnouncementInterval = false;

        @Comment("Intervalo (segundos) del temporizador global. Solo aplica si perAnnouncementInterval=false.")
        public int globalIntervalSeconds = 300;

        @Comment("¿Rotar en orden o aleatoriamente? Solo aplica al modo de intervalo global.")
        public boolean random = false;
    }

    public static class AnnouncementEntry extends MincoreConfig {
        public boolean enabled = true;
        public List<String> lines = new ArrayList<>();

        @Comment("Nombre de sonido (ej. ENTITY_EXPERIENCE_ORB_PICKUP). Vacío para no reproducir ninguno.")
        public String sound = "";

        @Comment("Intervalo (segundos) propio de este anuncio. Solo se usa si perAnnouncementInterval=true.")
        public int intervalSeconds = 300;
    }

    private static Map<String, String> defaultPlaceholders() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("discord", "discord.gg/minecuador");
        map.put("web", "minecuador.lat");
        return map;
    }

    private static Map<String, AnnouncementEntry> defaultAnnouncements() {
        Map<String, AnnouncementEntry> map = new LinkedHashMap<>();

        AnnouncementEntry welcome = new AnnouncementEntry();
        welcome.lines = new ArrayList<>(List.of("<#55FFFF>¡Bienvenido a Mincore! <white>Únete a nuestro Discord: <#5865F2>%%discord%%"));
        welcome.sound = "ENTITY_EXPERIENCE_ORB_PICKUP";
        map.put("bienvenida", welcome);

        return map;
    }
}
