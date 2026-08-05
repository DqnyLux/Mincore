package org.dqnylux.mincore.managers.cosmetics;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.dqnylux.mincore.config.models.CosmeticItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Toda resolución de partícula/material pasa por XSeries (XParticle/XMaterial -
 * ya era dependencia del proyecto, y es exactamente lo que usaba v1 en
 * TrailManager/WingManager) antes de caer a Particle.valueOf/Material.valueOf
 * crudo: un nombre en YAML resuelve igual sin importar qué tan vieja o nueva
 * sea la versión de Minecraft del servidor (los nombres cambiaron varias
 * veces - ej. REDSTONE -> DUST en 1.20.5 - XParticle/XMaterial conocen los
 * alias históricos, el enum crudo de Bukkit no).
 */
public final class EffectUtils {

    private EffectUtils() {
    }

    /** Resuelve por XParticle primero (soporta alias históricos entre versiones), con Particle.valueOf como respaldo. */
    public static Particle parseParticle(String name, Particle fallback) {
        if (name == null || name.isBlank()) return fallback;

        Optional<XParticle> xParticle = XParticle.of(name.trim());
        if (xParticle.isPresent()) return xParticle.get().get();

        try {
            return Particle.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Resuelve por XMaterial primero (soporta alias históricos entre versiones), con Material.valueOf como respaldo. */
    public static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) return fallback;

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(name.trim());
        if (xMaterial.isPresent()) {
            Material resolved = xMaterial.get().get();
            if (resolved != null) return resolved;
        }

        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static int resolveCount(CosmeticItem item, int fallback) {
        return item.count >= 0 ? item.count : fallback;
    }

    public static int resolvePointCount(CosmeticItem item, int fallback) {
        return item.pointCount >= 0 ? item.pointCount : fallback;
    }

    /** Uno al azar de item.materials, o el material dado como respaldo si la lista está vacía o algún nombre no es válido. */
    public static Material randomMaterial(CosmeticItem item, Material fallback) {
        if (item.materials.isEmpty()) return fallback;

        List<Material> parsed = new ArrayList<>();
        for (String name : item.materials) {
            Material material = parseMaterial(name, null);
            if (material != null) parsed.add(material);
        }
        if (parsed.isEmpty()) return fallback;
        return parsed.get(ThreadLocalRandom.current().nextInt(parsed.size()));
    }

    /**
     * Punto único de spawn de partículas de cosméticos: aplica particle/color/
     * speed del CosmeticItem (sección 17 - las entradas visuales vienen del
     * YAML, no del código del efecto). El efecto solo aporta sus valores por
     * defecto (partícula y velocidad) para cuando el YAML no los define.
     * Construido sobre ParticleDisplay (XSeries) en vez de World#spawnParticle
     * crudo - igual técnica que usaba v1 en TrailManager/WingManager.
     */
    public static void spawnParticle(Location location, CosmeticItem item, Particle fallbackParticle,
                                      int count, double spreadX, double spreadY, double spreadZ, double fallbackSpeed) {
        Particle particle = parseParticle(item.particle, fallbackParticle);
        double speed = item.speed >= 0 ? item.speed : fallbackSpeed;

        ParticleDisplay display = ParticleDisplay.of(particle)
                .withCount(count)
                .offset(spreadX, spreadY, spreadZ)
                .withExtra(speed);

        if (particle == Particle.DUST && item.color != null && !item.color.isBlank()) {
            Color color = parseColor(item.color, Color.WHITE);
            display.withColor(new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue()), 1.0f);
        } else if (particle.getDataType() == Float.class) {
            // Desde el cambio de partículas de 1.20.5+, algunas (DRAGON_BREATH,
            // SCULK_CHARGE...) EXIGEN un Float como dato - antes no llevaban
            // ninguno. Sin esto, World#spawnParticle lanza
            // "IllegalArgumentException: missing required data class
            // java.lang.Float" apenas se invoca (no es un problema de count/
            // offset/extra, es que falta el dato obligatorio). 1.0f es un
            // valor neutro razonable para ambas.
            display.withRawData(1.0f);
        } else if (particle.getDataType() == org.bukkit.Color.class) {
            // Mismo caso que el Float de arriba, pero para FLASH (exige un
            // org.bukkit.Color, no un java.awt.Color como DUST) - usa
            // item.color si está definido, blanco si no.
            Color color = parseColor(item.color, Color.WHITE);
            display.withRawData(color);
        }

        display.spawn(location);
    }

    /**
     * Para las pocas llamadas que van directo a World#spawnParticle en vez de
     * pasar por spawnParticle() de arriba (necesitan una partícula concreta
     * sin color de CosmeticItem, ej. DRAGON_BREATH/FLASH en animaciones con
     * varias capas) - mismo chequeo dinámico de particle.getDataType() que
     * arriba, para no tener que hardcodear 1.0f/Color.WHITE a mano en cada
     * sitio (y no romperse si una versión futura deja de exigir el dato, o
     * empieza a exigirlo en una partícula que hoy no lo pide).
     */
    public static void spawnRaw(Location location, Particle particle, int count,
                                 double spreadX, double spreadY, double spreadZ, double extra) {
        if (particle.getDataType() == Float.class) {
            location.getWorld().spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, extra, 1.0f);
        } else if (particle.getDataType() == org.bukkit.Color.class) {
            location.getWorld().spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, extra, Color.WHITE);
        } else {
            location.getWorld().spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, extra);
        }
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

    /**
     * Brillo coloreado para entidades invocadas por un efecto (allays,
     * guardianes, pollos...) - no jugadores, así que un equipo de scoreboard
     * real es seguro acá (a diferencia de GlowManager, que evita equipos
     * reales en jugadores por el conflicto con TAB - sección de GlowManager).
     * Estos mobs son temporales y no gestionados por TAB.
     */
    public static void applyEntityGlow(Entity entity, String colorName) {
        entity.setGlowing(true);
        if (colorName == null || colorName.isBlank()) return;

        ChatColor color;
        try {
            color = ChatColor.valueOf(colorName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "coreec_mobglow_" + color.name();
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setColor(color);
        }
        team.addEntry(entity.getUniqueId().toString());
    }
}
