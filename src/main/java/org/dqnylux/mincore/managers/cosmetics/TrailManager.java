package org.dqnylux.mincore.managers.cosmetics;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.config.models.CosmeticItem;

/**
 * Sin estado propio - drawTrail() se llama cada tick por el task del jugador.
 * Solo 3 de los 10 estilos originales por ahora (STEPS/ORBIT/SPARKS); añadir
 * el resto es repetir el mismo patrón de switch. Los parámetros visuales
 * (partícula/color/velocidad/radio) vienen del CosmeticItem (sección 17).
 */
public class TrailManager {

    public void drawTrail(Player player, CosmeticItem item) {
        String style = item.value == null ? "" : item.value.toUpperCase();
        Location location = player.getLocation();

        switch (style) {
            case "ORBIT" -> drawOrbit(location, item);
            case "SPARKS" -> drawSparks(location, item);
            default -> drawSteps(location, item);
        }
    }

    private void drawSteps(Location location, CosmeticItem item) {
        EffectUtils.spawnParticle(location.clone().add(0, 0.1, 0), item, Particle.CLOUD, 1, 0, 0, 0, 0);
    }

    private void drawOrbit(Location location, CosmeticItem item) {
        double radius = item.radius > 0 ? item.radius : 0.8;
        double angle = (System.currentTimeMillis() / 50 % 360) * Math.PI / 180;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        EffectUtils.spawnParticle(location.clone().add(x, 1, z), item, Particle.CLOUD, 1, 0, 0, 0, 0);
    }

    private void drawSparks(Location location, CosmeticItem item) {
        EffectUtils.spawnParticle(location.clone().add(0, 0.5, 0), item, Particle.CLOUD, 3, 0.3, 0.3, 0.3, 0.02);
    }
}
