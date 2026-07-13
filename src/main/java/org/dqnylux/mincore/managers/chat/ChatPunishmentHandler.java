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
 * No se implementa el "toast" nativo vía Advancement efímero que describe el
 * prompt original: esa API está marcada @Deprecated/unsafe en Paper y choca
 * con el objetivo de multiversión de la sección 15. Se reemplaza por un
 * actionbar + sonido, con la misma función de aviso visible.
 */
public class ChatPunishmentHandler {

    private final Mincore plugin;

    public ChatPunishmentHandler(Mincore plugin) {
        this.plugin = plugin;
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

        if (data.getChatWarnings() >= config.maxWarnings) {
            data.setChatWarnings(0);
            plugin.getPlayerManager().savePlayerAsync(data);

            String command = config.punishCommand.replace("%player%", player.getName());
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }
}
