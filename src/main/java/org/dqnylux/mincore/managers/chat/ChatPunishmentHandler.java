package org.dqnylux.mincore.managers.chat;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.FiltersConfig;
import org.dqnylux.mincore.model.PlayerData;

/**
 * El aviso se persiste en PlayerData (columna chat_warnings) en vez de un mapa
 * en memoria aparte, para reusar el mismo patrón de caché/persistencia de
 * PlayerManager y quedar listo para compartirse en red en la Fase 7.
 *
 * El toast nativo (Advancement efímero, igual que MentionToastManager) sí
 * está implementado - ya se probó que la API funciona en esta build de Paper
 * al conectar el toast de menciones, así que aplica el mismo mecanismo acá.
 */
public class ChatPunishmentHandler {

    private final Mincore plugin;
    private final InfractionToastManager toastManager;

    public ChatPunishmentHandler(Mincore plugin) {
        this.plugin = plugin;
        this.toastManager = new InfractionToastManager(plugin);
        this.toastManager.init();
    }

    public void handleInfraction(Player player) {
        FiltersConfig.Punishment config = plugin.getConfigManager().getFiltersConfig().punishment;
        PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
        if (data == null) return;

        data.addChatWarning();
        plugin.getPlayerManager().savePlayerAsync(data);

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        String actionbar = plugin.getConfigManager().getMessagesConfig().chat.warningActionbar
                .replace("%current%", String.valueOf(data.getChatWarnings()))
                .replace("%max%", String.valueOf(config.maxWarnings));
        player.sendActionBar(org.dqnylux.mincore.utils.TextUtils.format(actionbar));
        if (config.toast) {
            toastManager.showToast(player);
        }

        if (data.getChatWarnings() >= config.maxWarnings) {
            data.setChatWarnings(0);
            plugin.getPlayerManager().savePlayerAsync(data);

            String command = config.punishCommand.replace("%player%", player.getName());
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }
}
