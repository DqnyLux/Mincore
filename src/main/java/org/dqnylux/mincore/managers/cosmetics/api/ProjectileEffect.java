package org.dqnylux.mincore.managers.cosmetics.api;

import org.bukkit.entity.Projectile;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;

/** play() se llama una vez al lanzar el proyectil; el propio efecto se auto-programa si necesita seguirlo. */
public interface ProjectileEffect {
    String getId();

    void play(Mincore plugin, Projectile projectile, CosmeticItem item);
}
