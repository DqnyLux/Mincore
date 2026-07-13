package org.dqnylux.mincore.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.util.UUID;

public class MessageCommand {

    private final Mincore plugin;

    public MessageCommand(Mincore plugin) {
        this.plugin = plugin;
    }

    @Command({"msg", "tell", "w", "whisper"})
    public void message(BukkitCommandActor actor, String player, String messageText) {
        CommandSender sender = actor.sender();
        MessagesConfig.PrivateMessages pm = plugin.getConfigManager().getMessagesConfig().privateMessages;
        String prefix = plugin.getConfigManager().getMessagesConfig().prefix;

        Player target = Bukkit.getPlayerExact(player);
        if (target == null) {
            sender.sendMessage(TextUtils.format(prefix + pm.playerOffline));
            return;
        }

        if (actor.isPlayer() && actor.requirePlayer().equals(target)) {
            sender.sendMessage(TextUtils.format(prefix + pm.cannotMessageSelf));
            return;
        }

        boolean bypass = !actor.isPlayer() || sender.hasPermission(plugin.getConfigManager().getMainConfig().permissions.messageBypass);
        PlayerData targetData = plugin.getPlayerManager().get(target.getUniqueId());
        if (!bypass && targetData != null && !targetData.isMessagesEnabled()) {
            sender.sendMessage(TextUtils.format(prefix + pm.targetDisabled));
            return;
        }

        String senderName = actor.isPlayer() ? actor.requirePlayer().getName() : sender.getName();
        deliver(sender, target, senderName, messageText, pm);

        if (actor.isPlayer()) {
            plugin.getReplyManager().setReplyTarget(actor.requirePlayer().getUniqueId(), target.getUniqueId());
        }
    }

    @Command({"reply", "r", "responder"})
    public void reply(BukkitCommandActor actor, String messageText) {
        MessagesConfig.PrivateMessages pm = plugin.getConfigManager().getMessagesConfig().privateMessages;
        String prefix = plugin.getConfigManager().getMessagesConfig().prefix;

        if (!actor.isPlayer()) {
            var messages = plugin.getConfigManager().getMessagesConfig();
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }

        Player player = actor.requirePlayer();
        UUID targetUuid = plugin.getReplyManager().getReplyTarget(player.getUniqueId());
        Player target = targetUuid != null ? Bukkit.getPlayer(targetUuid) : null;

        if (target == null) {
            plugin.getReplyManager().clear(player.getUniqueId());
            player.sendMessage(TextUtils.format(prefix + pm.noReplyTarget));
            return;
        }

        deliver(player, target, player.getName(), messageText, pm);
        plugin.getReplyManager().setReplyTarget(player.getUniqueId(), target.getUniqueId());
    }

    private void deliver(CommandSender sender, Player target, String senderName, String messageText, MessagesConfig.PrivateMessages pm) {
        Component body = TextUtils.formatSafeChat(messageText);

        sender.sendMessage(TextUtils.format(pm.senderFormat.replace("%target%", target.getName())).append(body));
        target.sendMessage(TextUtils.format(pm.targetFormat.replace("%sender%", senderName)).append(body));
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
}
