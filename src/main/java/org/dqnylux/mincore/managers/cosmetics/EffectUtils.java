package org.dqnylux.mincore.managers.cosmetics;

import com.cryptomorin.xseries.XSound;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.dqnylux.mincore.config.models.CosmeticItem;

public final class EffectUtils {

    private EffectUtils() {
    }

    public static Particle parseParticle(String name, Particle fallback) {
        if (name == null || name.isBlank()) return fallback;
        try {
            return Particle.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Punto único de spawn de partículas de cosméticos: aplica particle/color/
     * speed del CosmeticItem (sección 17 - las entradas visuales vienen del
     * YAML, no del código del efecto). El efecto solo aporta sus valores por
     * defecto (partícula y velocidad) para cuando el YAML no los define.
     */
    public static void spawnParticle(Location location, CosmeticItem item, Particle fallbackParticle,
                                     int count, double spreadX, double spreadY, double spreadZ, double fallbackSpeed) {
        Particle particle = parseParticle(item.particle, fallbackParticle);
        double speed = item.speed >= 0 ? item.speed : fallbackSpeed;

        Object data = null;
        if (particle == Particle.DUST && item.color != null && !item.color.isBlank()) {
            data = new Particle.DustOptions(parseColor(item.color, Color.WHITE), 1.0f);
        }

        location.getWorld().spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, speed, data);
    }

    /** Acepta #RRGGBB o nombres de org.bukkit.Color (RED, AQUA, LIME...). */
    public static Color parseColor(String value, Color fallback) {
        if (value == null || value.isBlank()) return fallback;
        String trimmed = value.trim();

        if (trimmed.startsWith("#") && trimmed.length() == 7) {
            try {
                return Color.fromRGB(Integer.parseInt(trimmed.substring(1), 16));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        try {
            java.lang.reflect.Field field = Color.class.getField(trimmed.toUpperCase());
            return (Color) field.get(null);
        } catch (ReflectiveOperationException e) {
            return fallback;
        }
    }

    public static void playSound(Entity entity, String soundName) {
        if (soundName == null || soundName.isBlank()) return;
        XSound.matchXSound(soundName).ifPresent(sound -> sound.play(entity));
    }
}
