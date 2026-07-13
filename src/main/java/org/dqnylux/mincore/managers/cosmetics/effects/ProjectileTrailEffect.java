package org.dqnylux.mincore.managers.cosmetics.effects;

import org.bukkit.Particle;
import org.bukkit.entity.Projectile;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.managers.cosmetics.EffectUtils;
import org.dqnylux.mincore.managers.cosmetics.api.ProjectileEffect;

/**
 * Patrón "auto-seguimiento de entidad viva" (sección 13.1): se programa a sí
 * mismo con el EntityScheduler del propio proyectil (Folia-safe) hasta que
 * aterriza o se invalida - nunca con BukkitRunnable/runTaskTimer.
 */
public class ProjectileTrailEffect implements ProjectileEffect {

    @Override
    public String getId() {
        return "projectile_trail";
    }

    @Override
    public void play(Mincore plugin, Projectile projectile, CosmeticItem item) {
        long[] ticksAlive = {0};

        projectile.getScheduler().runAtFixedRate(plugin, task -> {
            ticksAlive[0]++;
            boolean expired = item.durationTicks > 0 && ticksAlive[0] > item.durationTicks;
            if (!projectile.isValid() || projectile.isDead() || expired) {
                task.cancel();
                return;
            }
            EffectUtils.spawnParticle(projectile.getLocation(), item, Particle.CRIT, 2, 0.05, 0.05, 0.05, 0);
        }, () -> {
        }, 1L, 1L);
    }
}
