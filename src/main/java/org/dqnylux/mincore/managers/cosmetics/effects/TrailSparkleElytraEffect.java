package org.dqnylux.mincore.managers.cosmetics.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.managers.cosmetics.EffectUtils;
import org.dqnylux.mincore.managers.cosmetics.api.ElytraEffect;

public class TrailSparkleElytraEffect implements ElytraEffect {

    @Override
    public String getId() {
        return "trail_sparkle";
    }

    @Override
    public void play(Mincore plugin, Player player, CosmeticItem item) {
        Location location = player.getLocation();
        EffectUtils.spawnParticle(location, item, Particle.END_ROD, 3, 0.2, 0.2, 0.2, 0.01);
    }
}
