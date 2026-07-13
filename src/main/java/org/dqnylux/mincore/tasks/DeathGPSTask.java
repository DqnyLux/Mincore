package org.dqnylux.mincore.tasks;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Versión simplificada del original: bossbar con distancia + un EnderCrystal
 * invulnerable como baliza visual (visible solo para quien rastrea). Se
 * omite el holograma TextDisplay y la flecha direccional de 8 posiciones del
 * prompt original para acotar el alcance de esta fase - la baliza + bossbar
 * ya cubren la función principal (guiar al jugador hasta el punto).
 */
public class DeathGPSTask {

    private static final double ARRIVAL_DISTANCE = 5.0;

    private final Mincore plugin;
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, EnderCrystal> beacons = new ConcurrentHashMap<>();

    public DeathGPSTask(Mincore plugin) {
        this.plugin = plugin;
    }

    public void start(Player player, Location target, int deathId) {
        stop(player);

        BossBar bossBar = BossBar.bossBar(Component.text("Rastreando..."), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);
        bossBars.put(player.getUniqueId(), bossBar);

        EnderCrystal crystal = target.getWorld().spawn(target.clone().add(0.5, 1, 0.5), EnderCrystal.class, c -> {
            c.setInvulnerable(true);
            c.setPersistent(false);
            c.setShowingBottom(false);
        });
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) other.hideEntity(plugin, crystal);
        }
        beacons.put(player.getUniqueId(), crystal);

        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin,
                scheduled -> tick(player, target, deathId), () -> cleanup(player.getUniqueId()), 2L, 2L);
        tasks.put(player.getUniqueId(), task);
    }

    private void tick(Player player, Location target, int deathId) {
        if (!player.isOnline()) {
            stop(player.getUniqueId());
            return;
        }

        double distance = player.getLocation().distance(target);
        BossBar bossBar = bossBars.get(player.getUniqueId());
        if (bossBar != null) {
            String template = plugin.getConfigManager().getMessagesConfig().death.bossbarDistance;
            bossBar.name(TextUtils.format(template.replace("%distance%", String.valueOf(Math.round(distance)))));
            bossBar.progress(Math.max(0f, Math.min(1f, (float) (1 - (distance / 200.0)))));
        }

        if (distance <= ARRIVAL_DISTANCE) {
            player.sendMessage(TextUtils.format(plugin.getConfigManager().getMessagesConfig().death.trackingArrived));
            plugin.getDeathTrackingManager().remove(deathId);
            stop(player.getUniqueId());
        }
    }

    public boolean isTracking(UUID uuid) {
        return tasks.containsKey(uuid);
    }

    public void stop(Player player) {
        stop(player.getUniqueId());
    }

    public void stop(UUID uuid) {
        ScheduledTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
        } else {
            cleanup(uuid);
        }
    }

    private void cleanup(UUID uuid) {
        BossBar bossBar = bossBars.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (bossBar != null && player != null) player.hideBossBar(bossBar);

        EnderCrystal crystal = beacons.remove(uuid);
        if (crystal != null && !crystal.isDead()) crystal.remove();
    }

    public void stopAll() {
        for (UUID uuid : new ArrayList<>(tasks.keySet())) {
            stop(uuid);
        }
    }
}
