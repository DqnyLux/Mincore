package org.dqnylux.mincore.managers;

import com.cryptomorin.xseries.XSound;
import com.google.gson.JsonObject;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.AnnouncementsConfig;
import org.dqnylux.mincore.utils.TextUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AnnouncementManager {

    private static final NamespacedKey ROOT_KEY = new NamespacedKey("coreec", "announcements_root");
    private static final String CRITERIA = "announcement_view";

    private final Mincore plugin;
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private final Map<String, NamespacedKey> toastKeys = new ConcurrentHashMap<>();
    private int rotationIndex = 0;
    private boolean toastsAvailable = false;

    public AnnouncementManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        initToasts();
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

    private void initToasts() {
        toastKeys.clear();
        AnnouncementsConfig config = plugin.getConfigManager().getAnnouncementsConfig();

        try {
            if (Bukkit.getAdvancement(ROOT_KEY) == null) {
                Bukkit.getUnsafe().loadAdvancement(ROOT_KEY, buildRootJson());
            }

            for (Map.Entry<String, AnnouncementsConfig.AnnouncementEntry> entry : config.announcements.entrySet()) {
                String key = entry.getKey().toLowerCase();
                AnnouncementsConfig.AnnouncementEntry announcement = entry.getValue();

                if (announcement.enabled && announcement.toastEnabled) {
                    NamespacedKey advKey = new NamespacedKey("coreec", "announcement_" + key);
                    try {
                        if (Bukkit.getAdvancement(advKey) == null) {
                            Bukkit.getUnsafe().loadAdvancement(advKey, buildChildJson(announcement));
                        }
                        toastKeys.put(key, advKey);
                    } catch (Throwable t) {
                        plugin.getLogger().warning("No se pudo registrar toast de anuncio " + key + ": " + t.getMessage());
                    }
                }
            }
            toastsAvailable = true;
        } catch (Throwable t) {
            toastsAvailable = false;
            plugin.getLogger().warning("No se pudieron inicializar los toasts de anuncios: " + t.getMessage());
        }
    }

    private void startPerAnnouncementMode(AnnouncementsConfig config) {
        for (Map.Entry<String, AnnouncementsConfig.AnnouncementEntry> entryMap : config.announcements.entrySet()) {
            String key = entryMap.getKey();
            AnnouncementsConfig.AnnouncementEntry entry = entryMap.getValue();
            if (!entry.enabled) continue;
            long ticks = Math.max(1, entry.intervalSeconds * 20L);
            tasks.add(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> broadcast(key, entry), ticks, ticks));
        }
    }

    private void startGlobalRotationMode(AnnouncementsConfig config) {
        List<Map.Entry<String, AnnouncementsConfig.AnnouncementEntry>> enabled = config.announcements.entrySet().stream()
                .filter(entry -> entry.getValue().enabled)
                .collect(Collectors.toList());
        if (enabled.isEmpty()) return;

        long ticks = Math.max(1, config.settings.globalIntervalSeconds * 20L);
        tasks.add(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            Map.Entry<String, AnnouncementsConfig.AnnouncementEntry> entryMap = config.settings.random
                    ? enabled.get(ThreadLocalRandom.current().nextInt(enabled.size()))
                    : enabled.get(rotationIndex++ % enabled.size());
            broadcast(entryMap.getKey(), entryMap.getValue());
        }, ticks, ticks));
    }

    private void broadcast(String key, AnnouncementsConfig.AnnouncementEntry entry) {
        AnnouncementsConfig config = plugin.getConfigManager().getAnnouncementsConfig();

        // 1. Enviar líneas de chat a todo el servidor si está activado
        if (entry.chatEnabled && entry.lines != null) {
            for (String line : entry.lines) {
                String resolved = resolvePlaceholders(line, config.placeholders);
                Bukkit.broadcast(TextUtils.format(resolved));
            }
        }

        // 2. Preparar Title si está configurado y habilitado
        boolean hasTitle = entry.title != null && !entry.title.isBlank();
        boolean hasSubtitle = entry.subtitle != null && !entry.subtitle.isBlank();
        Title titleObject = null;

        if (entry.titleEnabled && (hasTitle || hasSubtitle)) {
            Component titleComp = hasTitle ? TextUtils.format(resolvePlaceholders(entry.title, config.placeholders)) : Component.empty();
            Component subComp = hasSubtitle ? TextUtils.format(resolvePlaceholders(entry.subtitle, config.placeholders)) : Component.empty();
            Title.Times times = Title.Times.times(
                    Duration.ofMillis(Math.max(0, entry.titleFadeIn) * 50L),
                    Duration.ofMillis(Math.max(1, entry.titleStay) * 50L),
                    Duration.ofMillis(Math.max(0, entry.titleFadeOut) * 50L)
            );
            titleObject = Title.title(titleComp, subComp, times);
        }

        // 3. Preparar ActionBar si está configurado y habilitado
        Component actionBarComp = (entry.actionBarEnabled && entry.actionBar != null && !entry.actionBar.isBlank())
                ? TextUtils.format(resolvePlaceholders(entry.actionBar, config.placeholders))
                : null;

        // 4. Despachar a todos los jugadores online de forma segura en Folia
        final Title finalTitle = titleObject;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(plugin, scheduledTask -> {
                if (!player.isOnline()) return;

                org.dqnylux.mincore.model.PlayerData pData = plugin.getPlayerManager() != null ? plugin.getPlayerManager().get(player.getUniqueId()) : null;
                boolean allowTitles = pData == null || pData.isTitlesEnabled();
                if (finalTitle != null && allowTitles) {
                    player.showTitle(finalTitle);
                }
                if (actionBarComp != null) {
                    player.sendActionBar(actionBarComp);
                }
                if (entry.toastEnabled) {
                    showToast(player, key.toLowerCase());
                }
                if (entry.soundEnabled && entry.sound != null && !entry.sound.isBlank()) {
                    XSound.matchXSound(entry.sound).ifPresent(s -> s.play(player));
                }
            }, null);
        }
    }

    private void showToast(Player player, String key) {
        if (!toastsAvailable || player == null) return;
        NamespacedKey advKey = toastKeys.get(key);
        if (advKey == null) return;

        try {
            Advancement advancement = Bukkit.getAdvancement(advKey);
            if (advancement == null) return;

            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            if (!progress.isDone()) progress.awardCriteria(CRITERIA);

            player.getScheduler().runDelayed(plugin, task -> {
                if (progress.isDone()) progress.revokeCriteria(CRITERIA);
            }, () -> {}, 5L);
        } catch (Throwable ignored) {}
    }

    private String resolvePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || text.isBlank() || placeholders == null) return text;
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%%" + entry.getKey() + "%%", entry.getValue());
            result = result.replace("%%" + entry.getKey().toLowerCase() + "%%", entry.getValue());
            result = result.replace("%%" + entry.getKey().toUpperCase() + "%%", entry.getValue());
        }
        return result;
    }

    private String buildRootJson() {
        JsonObject icon = new JsonObject();
        icon.addProperty("id", "minecraft:beacon");
        icon.addProperty("item", "minecraft:beacon");

        JsonObject display = new JsonObject();
        display.add("icon", icon);
        display.add("title", GsonComponentSerializer.gson().serializeToTree(Component.empty()));
        display.add("description", GsonComponentSerializer.gson().serializeToTree(Component.empty()));
        display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/stone.png");
        display.addProperty("show_toast", false);
        display.addProperty("announce_to_chat", false);
        display.addProperty("hidden", true);

        JsonObject root = new JsonObject();
        root.add("display", display);
        root.add("criteria", impossibleCriteria());
        return root.toString();
    }

    private String buildChildJson(AnnouncementsConfig.AnnouncementEntry entry) {
        JsonObject icon = new JsonObject();
        String rawIcon = (entry.toastIcon == null || entry.toastIcon.isBlank()) ? "beacon" : entry.toastIcon.trim().toLowerCase();
        String iconKey = rawIcon.contains(":") ? rawIcon : "minecraft:" + rawIcon;
        icon.addProperty("id", iconKey);
        icon.addProperty("item", iconKey);

        AnnouncementsConfig config = plugin.getConfigManager().getAnnouncementsConfig();
        Map<String, String> placeholders = config != null ? config.placeholders : null;
        String resolvedTitle = resolvePlaceholders(entry.toastTitle, placeholders);
        String resolvedDesc = resolvePlaceholders(entry.toastDescription, placeholders);

        JsonObject display = new JsonObject();
        display.add("icon", icon);
        display.add("title", GsonComponentSerializer.gson().serializeToTree(TextUtils.format(resolvedTitle)));
        display.add("description", GsonComponentSerializer.gson().serializeToTree(TextUtils.format(resolvedDesc)));
        display.addProperty("frame", (entry.toastFrame == null || entry.toastFrame.isBlank()) ? "goal" : entry.toastFrame.trim().toLowerCase());
        display.addProperty("show_toast", true);
        display.addProperty("announce_to_chat", false);
        display.addProperty("hidden", true);

        JsonObject child = new JsonObject();
        child.addProperty("parent", ROOT_KEY.toString());
        child.add("display", display);
        child.add("criteria", impossibleCriteria());
        return child.toString();
    }

    private JsonObject impossibleCriteria() {
        JsonObject trigger = new JsonObject();
        trigger.addProperty("trigger", "minecraft:impossible");
        JsonObject criteria = new JsonObject();
        criteria.add(CRITERIA, trigger);
        return criteria;
    }
}

