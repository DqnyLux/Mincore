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

    // ignoreCancelled: el chat de congelados (FreezeListener) y el canal de
    // staff (StaffListener) cancelan el evento ANTES - este pipeline no debe
    // formatear/broadcastear un mensaje que ya fue desviado a otro canal.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (INJECTION_GUARD.matcher(message).find()) {
            event.setCancelled(true);
            String alert = messages.prefix + messages.chat.injectionBlocked
                    .replace("%player%", plugin.getDisguiseManager().displayName(player))
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

        // El mensaje ya se va a mostrar al instante (sin retenerlo) - la
        // revisión por IA corre aparte y, si confirma que es tóxico, lo borra
        // de la pantalla de todos unos cientos de ms después (ver comentario
        // de clase de ChatFilterManager).
        filterManager.reviewAsync(player, message, event.signedMessage());
    }

    private boolean isOptedOutOfGlobalChat(Audience audience, Player sender) {
        if (!(audience instanceof Player viewer) || viewer.equals(sender)) return false;
        // Un staff con un freeze enfocado no ve el chat global mientras dura
        // - queda concentrado en esa conversación (ver FreezeManager).
        if (plugin.getFreezeManager().getFocusedTarget(viewer.getUniqueId()) != null) return true;
        PlayerData viewerData = plugin.getPlayerManager().get(viewer.getUniqueId());
        return viewerData != null && !viewerData.isGlobalChat();
    }

    private void notifyCancelled(Player player, ChatFilterManager.CancelReason reason) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        String body = switch (reason) {
            case SPAM -> messages.chat.filterSpam;
            case FLOOD -> messages.chat.filterFlood;
            case TOO_LONG -> messages.chat.filterTooLong;
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
