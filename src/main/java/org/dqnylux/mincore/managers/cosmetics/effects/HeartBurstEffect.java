package org.dqnylux.mincore.managers.cosmetics.effects;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.managers.cosmetics.EffectUtils;
import org.dqnylux.mincore.managers.cosmetics.api.CosmeticEffect;

public class HeartBurstEffect implements CosmeticEffect {

    @Override
    public String getId() {
        return "heart_burst";
    }

    @Override
    public void play(Mincore plugin, Player player, Location location, CosmeticItem item) {
        int count = Math.max(1, (int) (item.radius * 8));
        EffectUtils.spawnParticle(location.clone().add(0, 1, 0), item, Particle.HEART, count, 0.5, 0.5, 0.5, 0.05);
        EffectUtils.playSound(player, item.sound);
    }
}
