package org.dqnylux.mincore.tasks;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.model.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ElytraCosmeticsTask {

    private final Mincore plugin;
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public ElytraCosmeticsTask(Mincore plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        stop(player);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduled -> tick(player), () -> {
        }, 1L, 1L);
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
        if (!player.isOnline() || !player.isGliding()) return;

        PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
        if (data == null) return;

        String elytraId = data.getActiveCosmetic("elytra-effects");
        if (elytraId == null) return;

        CosmeticItem item = plugin.getCosmeticConfigManager().getItem("elytra-effects", elytraId);
        if (item != null) plugin.getEffectRegistry().playElytraEffect(plugin, player, item);
    }
}
