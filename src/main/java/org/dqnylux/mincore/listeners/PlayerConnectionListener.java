package org.dqnylux.mincore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MainConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;

public class PlayerConnectionListener implements Listener {

    private final Mincore plugin;

    public PlayerConnectionListener(Mincore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().getMainConfig().modules.customJoinQuitMessages) {
            event.joinMessage(null);
        }

        plugin.getPlayerManager().loadPlayer(player.getUniqueId(), player.getName())
                .thenAccept(data -> player.getScheduler().run(plugin, task -> onDataLoaded(player, data), () -> {
                }));

        plugin.getActiveCosmeticsTask().start(player);
        plugin.getElytraCosmeticsTask().start(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().getMainConfig().modules.customJoinQuitMessages) {
            event.quitMessage(null);
            broadcastQuitMessage(player, plugin.getPlayerManager().get(player.getUniqueId()));
        }

        plugin.getPlayerManager().saveAndRemoveAsync(player.getUniqueId());
        plugin.getActiveCosmeticsTask().stop(player);
        plugin.getElytraCosmeticsTask().stop(player);
    }

    private void onDataLoaded(Player player, PlayerData data) {
        if (data == null || !player.isOnline()) return;

        applyConnectionCosmetics(player, data);

        MainConfig config = plugin.getConfigManager().getMainConfig();
        if (config.modules.customJoinQuitMessages) {
            broadcastJoinMessage(player, data);
        }
        if (config.modules.motd) {
            sendMotd(player);
        }
    }

    private void applyConnectionCosmetics(Player player, PlayerData data) {
        String glowId = data.getActiveCosmetic("glows");
        if (glowId != null) {
            CosmeticItem item = plugin.getCosmeticConfigManager().getItem("glows", glowId);
            if (item != null) plugin.getGlowManager().applyGlow(player, item.value);
        }

        String joinEffectId = data.getActiveCosmetic("join-effects");
        if (joinEffectId != null) {
            CosmeticItem item = plugin.getCosmeticConfigManager().getItem("join-effects", joinEffectId);
            if (item != null) plugin.getEffectRegistry().playEffect(plugin, player, player.getLocation(), item);
        }
    }

    /** El cosmético de join-messages equipado reemplaza el mensaje por defecto de messages.yml. */
    private void broadcastJoinMessage(Player player, PlayerData data) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        String template = messages.joinQuit.joinMessage;

        String cosmeticId = data.getActiveCosmetic("join-messages");
        if (cosmeticId != null) {
            CosmeticItem item = plugin.getCosmeticConfigManager().getItem("join-messages", cosmeticId);
            if (item != null && item.value != null && !item.value.isBlank()) {
                template = item.value;
            }
        }

        Bukkit.broadcast(TextUtils.format(template.replace("%player%", player.getName())));
    }

    private void broadcastQuitMessage(Player player, PlayerData data) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        String template = messages.joinQuit.quitMessage;

        if (data != null) {
            String cosmeticId = data.getActiveCosmetic("join-messages");
            if (cosmeticId != null) {
                CosmeticItem item = plugin.getCosmeticConfigManager().getItem("join-messages", cosmeticId);
                if (item != null && item.quitValue != null && !item.quitValue.isBlank()) {
                    template = item.quitValue;
                }
            }
        }

        Bukkit.broadcast(TextUtils.format(template.replace("%player%", player.getName())));
    }

    private void sendMotd(Player player) {
        for (String line : plugin.getConfigManager().getMessagesConfig().joinQuit.joinMotd) {
            player.sendMessage(TextUtils.format(player, line));
        }
    }
}
