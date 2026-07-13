package org.dqnylux.mincore.managers.cosmetics.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.managers.cosmetics.EffectUtils;
import org.dqnylux.mincore.managers.cosmetics.api.CosmeticEffect;

public class FlameRingEffect implements CosmeticEffect {

    private static final int POINTS = 24;

    @Override
    public String getId() {
        return "flame_ring";
    }

    @Override
    public void play(Mincore plugin, Player player, Location location, CosmeticItem item) {
        double radius = item.radius > 0 ? item.radius : 1.0;

        for (int i = 0; i < POINTS; i++) {
            double angle = 2 * Math.PI * i / POINTS;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            EffectUtils.spawnParticle(location.clone().add(x, 0.1, z), item, Particle.FLAME, 1, 0, 0, 0, 0);
        }

        EffectUtils.playSound(player, item.sound);
    }
}
