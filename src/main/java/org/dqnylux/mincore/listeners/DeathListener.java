package org.dqnylux.mincore.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagePackConfig;
import org.dqnylux.mincore.config.models.MessagePackCosmetic;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.BedrockMenuHandler;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DeathListener implements Listener {

    private final Mincore plugin;
    private final Map<UUID, Integer> pendingBedrockPrompt = new ConcurrentHashMap<>();

    public DeathListener(Mincore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        applyDeathMessage(event, victim, killer);

        if (!plugin.getConfigManager().getMainConfig().deathTracking.enabled) return;

        int expirationMinutes = plugin.getConfigManager().getMainConfig().deathTracking.expirationMinutes;
        int deathId = plugin.getDeathTrackingManager().register(victim.getLocation(), expirationMinutes);

        String template = plugin.getConfigManager().getMessagesConfig().death.trackPrompt.replace("%id%", String.valueOf(deathId));
        Component message = TextUtils.format(template).clickEvent(ClickEvent.runCommand("/trackcore " + deathId));
        victim.sendMessage(message);

        if (plugin.getConfigManager().getMainConfig().deathTracking.bedrockAutoMenu) {
            pendingBedrockPrompt.put(victim.getUniqueId(), deathId);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Integer deathId = pendingBedrockPrompt.remove(player.getUniqueId());
        if (deathId == null) return;

        player.getScheduler().runDelayed(plugin, task -> BedrockMenuHandler.promptTracking(plugin, player, deathId), () -> {
        }, 20L);
    }

    private void applyDeathMessage(PlayerDeathEvent event, Player victim, Player killer) {
        MessagePackCosmetic pack = resolvePack(victim, killer);
        if (pack == null || pack.messages == null) return;

        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        EntityDamageEvent.DamageCause cause = lastDamage != null ? lastDamage.getCause() : null;

        ItemStack weapon = killer != null ? killer.getInventory().getItemInMainHand() : null;
        boolean weaponNamed = weapon != null && weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName();

        String template = resolveMessage(pack, cause, killer != null, weaponNamed);
        if (template == null) return;

        String weaponName = weaponNamed ? PlainTextComponentSerializer.plainText().serialize(weapon.getItemMeta().displayName()) : "";
        String resolved = template.replace("%player%", victim.getName())
                .replace("%killer%", killer != null ? killer.getName() : "")
                .replace("%weapon%", weaponName);

        event.deathMessage(TextUtils.format(resolved));
    }

    private MessagePackCosmetic resolvePack(Player victim, Player killer) {
        if (killer != null) {
            PlayerData killerData = plugin.getPlayerManager().get(killer.getUniqueId());
            String id = killerData != null ? killerData.getActiveCosmetic("kill-messages") : null;
            MessagePackConfig config = plugin.getCosmeticConfigManager().getKillMessages();
            return config.items.getOrDefault(id != null ? id : "default", config.items.get("default"));
        }

        PlayerData victimData = plugin.getPlayerManager().get(victim.getUniqueId());
        String id = victimData != null ? victimData.getActiveCosmetic("death-messages") : null;
        MessagePackConfig config = plugin.getCosmeticConfigManager().getDeathMessages();
        return config.items.getOrDefault(id != null ? id : "default", config.items.get("default"));
    }

    private String resolveMessage(MessagePackCosmetic pack, EntityDamageEvent.DamageCause cause, boolean hasKiller, boolean weaponNamed) {
        String causeKey = cause != null ? cause.name() : "DEFAULT";
        Map<String, List<String>> bucket = pack.messages.getOrDefault(causeKey, pack.messages.get("DEFAULT"));
        if (bucket == null) return null;

        String variant = weaponNamed ? "weapon" : hasKiller ? "killer" : "default";
        List<String> lines = bucket.getOrDefault(variant, bucket.get("default"));
        if (lines == null || lines.isEmpty()) return null;

        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }
}
