package org.dqnylux.mincore.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.managers.chat.ChatFilterManager;
import org.dqnylux.mincore.managers.chat.ChatFormatHandler;
import org.dqnylux.mincore.managers.chat.ChatPunishmentHandler;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private static final Pattern INJECTION_GUARD = Pattern.compile("<click|<hover|<insert", Pattern.CASE_INSENSITIVE);

    private final Mincore plugin;
    private final ChatFilterManager filterManager;
    private final ChatPunishmentHandler punishmentHandler;
    private final ChatFormatHandler formatHandler;

    public ChatListener(Mincore plugin, ChatFilterManager filterManager, ChatPunishmentHandler punishmentHandler, ChatFormatHandler formatHandler) {
        this.plugin = plugin;
        this.filterManager = filterManager;
        this.punishmentHandler = punishmentHandler;
        this.formatHandler = formatHandler;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (INJECTION_GUARD.matcher(message).find()) {
            event.setCancelled(true);
            String alert = messages.prefix + messages.chat.injectionBlocked
                    .replace("%player%", player.getName())
                    .replace("%message%", message);
            Bukkit.broadcast(TextUtils.formatSafeChat(alert));
            return;
        }

        if (!player.hasPermission(plugin.getConfigManager().getFiltersConfig().bypassPermission)) {
            ChatFilterManager.FilterResult result = filterManager.process(player, message);

            if (result.cancelled()) {
                event.setCancelled(true);
                notifyCancelled(player, result.reason());
                return;
            }

            message = result.message();
            if (result.infraction()) {
                punishmentHandler.handleInfraction(player);
            }
        }

        message = formatHandler.applyPermissions(player, message);
        Component finalMessage = formatHandler.buildFinalChat(player, message);

        event.renderer((source, sourceDisplayName, msg, viewer) -> finalMessage);
        event.viewers().removeIf(audience -> isOptedOutOfGlobalChat(audience, player));
    }

    private boolean isOptedOutOfGlobalChat(Audience audience, Player sender) {
        if (!(audience instanceof Player viewer) || viewer.equals(sender)) return false;
        PlayerData viewerData = plugin.getPlayerManager().get(viewer.getUniqueId());
        return viewerData != null && !viewerData.isGlobalChat();
    }

    private void notifyCancelled(Player player, ChatFilterManager.CancelReason reason) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        String body = switch (reason) {
            case SPAM -> messages.chat.filterSpam;
            case REPETITION -> messages.chat.filterRepetition;
            case BAD_WORD -> messages.chat.filterBadWord;
            case ADS -> messages.chat.filterAds;
        };
        player.sendMessage(TextUtils.format(messages.prefix + body));

        String staffAlertPermission = plugin.getConfigManager().getFiltersConfig().punishment.staffAlertPermission;
        Component staffAlert = TextUtils.format(messages.prefix + messages.chat.staffAlert.replace("%player%", player.getName()));
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(staffAlertPermission)) {
                staff.sendMessage(staffAlert);
            }
        }
    }
}
