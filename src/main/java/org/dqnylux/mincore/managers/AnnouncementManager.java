package org.dqnylux.mincore.managers;

import com.cryptomorin.xseries.XSound;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.AnnouncementsConfig;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AnnouncementManager {

    private final Mincore plugin;
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private int rotationIndex = 0;

    public AnnouncementManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        AnnouncementsConfig config = plugin.getConfigManager().getAnnouncementsConfig();

        if (config.settings.perAnnouncementInterval) {
            startPerAnnouncementMode(config);
        } else {
            startGlobalRotationMode(config);
        }
    }

    public void stop() {
        for (ScheduledTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }

    private void startPerAnnouncementMode(AnnouncementsConfig config) {
        for (AnnouncementsConfig.AnnouncementEntry entry : config.announcements.values()) {
            if (!entry.enabled) continue;
            long ticks = Math.max(1, entry.intervalSeconds * 20L);
            tasks.add(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> broadcast(entry), ticks, ticks));
        }
    }

    private void startGlobalRotationMode(AnnouncementsConfig config) {
        List<AnnouncementsConfig.AnnouncementEntry> enabled = config.announcements.values().stream()
                .filter(entry -> entry.enabled)
                .collect(Collectors.toList());
        if (enabled.isEmpty()) return;

        long ticks = Math.max(1, config.settings.globalIntervalSeconds * 20L);
        tasks.add(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            AnnouncementsConfig.AnnouncementEntry entry = config.settings.random
                    ? enabled.get(ThreadLocalRandom.current().nextInt(enabled.size()))
                    : enabled.get(rotationIndex++ % enabled.size());
            broadcast(entry);
        }, ticks, ticks));
    }

    private void broadcast(AnnouncementsConfig.AnnouncementEntry entry) {
        AnnouncementsConfig config = plugin.getConfigManager().getAnnouncementsConfig();

        for (String line : entry.lines) {
            String resolved = line;
            for (Map.Entry<String, String> placeholder : config.placeholders.entrySet()) {
                resolved = resolved.replace("%%" + placeholder.getKey() + "%%", placeholder.getValue());
            }
            Bukkit.broadcast(TextUtils.format(resolved));
        }

        if (entry.sound != null && !entry.sound.isBlank()) {
            XSound.matchXSound(entry.sound).ifPresent(sound ->
                    Bukkit.getOnlinePlayers().forEach(sound::play));
        }
    }
}
