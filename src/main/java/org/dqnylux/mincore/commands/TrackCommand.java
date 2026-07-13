package org.dqnylux.mincore.commands;

import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.managers.DeathTrackingManager;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

public class TrackCommand {

    private final Mincore plugin;

    public TrackCommand(Mincore plugin) {
        this.plugin = plugin;
    }

    @Command("trackcore")
    public void track(BukkitCommandActor actor, int id) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (!actor.isPlayer()) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }

        Player player = actor.requirePlayer();
        DeathTrackingManager.Death death = plugin.getDeathTrackingManager().get(id);
        if (death == null) {
            player.sendMessage(TextUtils.format(messages.prefix + messages.death.trackingNotFound));
            return;
        }

        plugin.getDeathGPSTask().start(player, death.location(), id);
        player.sendMessage(TextUtils.format(messages.prefix + messages.death.trackingStarted));
    }
}
