package org.dqnylux.mincore.tasks;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.WingsConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.config.models.WingCosmetic;
import org.dqnylux.mincore.model.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trails y wings, cada 3 ticks (sección 13.2). Un EntityScheduler por
 * jugador en vez de un único bucle global iterando a todos - necesario para
 * Folia (cada jugador puede estar en el hilo de una región distinta) y
 * corrige de raíz el bug de la nota 6 (nunca corre async llamando a
 * World.spawnParticle).
 */
public class ActiveCosmeticsTask {

    private final Mincore plugin;
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public ActiveCosmeticsTask(Mincore plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        stop(player);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduled -> tick(player), () -> {
        }, 3L, 3L);
        tasks.put(player.getUniqueId(), task);
    }

    public void stop(Player player) {
        stop(player.getUniqueId());
    }

    public void stop(UUID uuid) {
        ScheduledTask task = tasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void stopAll() {
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    private void tick(Player player) {
        if (!player.isOnline()) return;
        PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
        if (data == null) return;

        String trailId = data.getActiveCosmetic("trails");
        if (trailId != null) {
            CosmeticItem item = plugin.getCosmeticConfigManager().getItem("trails", trailId);
            if (item != null) plugin.getTrailManager().drawTrail(player, item);
        }

        String wingsId = data.getActiveCosmetic("wings");
        if (wingsId != null) {
            WingsConfig wingsConfig = plugin.getCosmeticConfigManager().getWings();
            WingCosmetic wingItem = wingsConfig.items.get(wingsId);
            if (wingItem != null) plugin.getWingManager().render(player, wingItem);
        }
    }
}
