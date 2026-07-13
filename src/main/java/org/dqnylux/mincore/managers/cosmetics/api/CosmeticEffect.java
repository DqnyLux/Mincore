package org.dqnylux.mincore.managers.cosmetics.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;

/**
 * Usado por join-effects, kill-effects y death-effects: la misma interfaz,
 * la categoría la decide el YAML, no la clase Java. Recibe el CosmeticItem
 * para leer sus parámetros visuales - a diferencia del prompt original, que
 * no lo pasaba y forzaba a hardcodear partícula/color/radio en cada clase.
 */
public interface CosmeticEffect {
    String getId();

    void play(Mincore plugin, Player player, Location location, CosmeticItem item);
}
