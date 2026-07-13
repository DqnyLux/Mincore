package org.dqnylux.mincore.managers.cosmetics;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.config.models.WingCosmetic;

import java.util.List;

/**
 * Renderiza el layout ASCII-art de wings.yml con aleteo sinusoidal simple.
 * Versión simplificada del original (sin distinguir forma completa en reposo
 * vs. simplificada en movimiento) - suficiente para probar el framework.
 */
public class WingManager {

    public void render(Player player, WingCosmetic wings) {
        List<String> layout = wings.wings.layout;
        if (layout.isEmpty()) return;

        Location base = player.getLocation();
        double yaw = Math.toRadians(base.getYaw());
        double flap = Math.sin(System.currentTimeMillis() / 200.0 * wings.wings.flapSpeed) * Math.toRadians(wings.wings.maxAngle);

        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                if (c == ' ') continue;

                Particle particle = EffectUtils.parseParticle(wings.wings.particleMap.get(String.valueOf(c)), Particle.CLOUD);

                double localX = (col - line.length() / 2.0) * 0.2;
                double localY = -row * 0.2 + 1.2;
                double localZ = 0.3 + Math.sin(flap) * 0.1;

                double worldX = localX * Math.cos(yaw) - localZ * Math.sin(yaw);
                double worldZ = localX * Math.sin(yaw) + localZ * Math.cos(yaw);

                base.getWorld().spawnParticle(particle, base.clone().add(worldX, localY, worldZ), 1, 0, 0, 0, 0);
            }
        }
    }
}
