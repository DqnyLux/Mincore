package org.dqnylux.mincore.managers.cosmetics.api;

import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;

/** play() se invoca una vez por tick mientras el jugador planea con élitros - no se auto-programa. */
public interface ElytraEffect {
    String getId();

    void play(Mincore plugin, Player player, CosmeticItem item);
}
