package org.dqnylux.mincore.commands;

import org.bukkit.GameMode;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

/**
 * /fly y los comandos de gamemode - no persisten entre reconexiones
 * (igual que el prompt original marca en su nota 10: es una decisión
 * deliberada del propio original, no algo que esta v2 deba corregir).
 */
public class MiscCommand {

    private final Mincore plugin;

    public MiscCommand(Mincore plugin) {
        this.plugin = plugin;
    }

    @Command("fly")
    public void fly(BukkitCommandActor actor) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (!plugin.getConfigManager().getMainConfig().commands.flyEnabled) return;
        if (!actor.isPlayer()) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }
        if (!actor.sender().hasPermission(plugin.getConfigManager().getMainConfig().permissions.fly)) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.noPermission));
            return;
        }

        var player = actor.requirePlayer();
        boolean newState = !player.getAllowFlight();
        player.setAllowFlight(newState);
        player.setFlying(newState && player.isFlying());

        String message = newState ? messages.commands.flyEnabled : messages.commands.flyDisabled;
        player.sendMessage(TextUtils.format(messages.prefix + message));
    }

    @Command("gmc")
    public void gamemodeCreative(BukkitCommandActor actor) {
        changeGamemode(actor, GameMode.CREATIVE);
    }

    @Command("gms")
    public void gamemodeSurvival(BukkitCommandActor actor) {
        changeGamemode(actor, GameMode.SURVIVAL);
    }

    @Command("gma")
    public void gamemodeAdventure(BukkitCommandActor actor) {
        changeGamemode(actor, GameMode.ADVENTURE);
    }

    @Command("gmsp")
    public void gamemodeSpectator(BukkitCommandActor actor) {
        changeGamemode(actor, GameMode.SPECTATOR);
    }

    private void changeGamemode(BukkitCommandActor actor, GameMode mode) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (!plugin.getConfigManager().getMainConfig().commands.gamemodesEnabled) return;
        if (!actor.isPlayer()) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }
        if (!actor.sender().hasPermission(plugin.getConfigManager().getMainConfig().permissions.gamemode)) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.noPermission));
            return;
        }

        var player = actor.requirePlayer();
        player.setGameMode(mode);
        player.sendMessage(TextUtils.format(messages.prefix +
                messages.commands.gamemodeChanged.replace("%gamemode%", mode.name().toLowerCase())));
    }
}
