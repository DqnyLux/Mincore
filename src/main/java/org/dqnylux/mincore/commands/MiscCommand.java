package org.dqnylux.mincore.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * /fly y los comandos de gamemode - no persisten entre reconexiones
 * (igual que el prompt original marca en su nota 10: es una decisión
 * deliberada del propio original, no algo que esta v2 deba corregir).
 * Ambos aceptan un jugador objetivo opcional - sin él, actúan sobre quien
 * ejecuta el comando (que entonces sí debe ser un jugador); con él, el
 * ejecutor puede ser consola.
 */
public class MiscCommand {

    private final Mincore plugin;

    public MiscCommand(Mincore plugin) {
        this.plugin = plugin;
    }

    @Command("fly")
    @CommandPermission("coreec.command.fly")
    public void fly(BukkitCommandActor actor, @Optional @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (!plugin.getConfigManager().getMainConfig().commands.flyEnabled) return;
        if (!actor.sender().hasPermission(plugin.getConfigManager().getMainConfig().permissions.fly)) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.noPermission));
            return;
        }

        Player player = resolveTarget(actor, messages, target);
        if (player == null) return;

        boolean newState = !player.getAllowFlight();
        player.setAllowFlight(newState);
        player.setFlying(newState && player.isFlying());

        String message = newState ? messages.commands.flyEnabled : messages.commands.flyDisabled;
        player.sendMessage(TextUtils.format(messages.prefix + message));
    }

    @Command("gmc")
    @CommandPermission("coreec.command.gamemode")
    public void gamemodeCreative(BukkitCommandActor actor, @Optional @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        changeGamemode(actor, GameMode.CREATIVE, target);
    }

    @Command("gms")
    @CommandPermission("coreec.command.gamemode")
    public void gamemodeSurvival(BukkitCommandActor actor, @Optional @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        changeGamemode(actor, GameMode.SURVIVAL, target);
    }

    @Command("gma")
    @CommandPermission("coreec.command.gamemode")
    public void gamemodeAdventure(BukkitCommandActor actor, @Optional @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        changeGamemode(actor, GameMode.ADVENTURE, target);
    }

    @Command("gmsp")
    @CommandPermission("coreec.command.gamemode")
    public void gamemodeSpectator(BukkitCommandActor actor, @Optional @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        changeGamemode(actor, GameMode.SPECTATOR, target);
    }

    private void changeGamemode(BukkitCommandActor actor, GameMode mode, String target) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (!plugin.getConfigManager().getMainConfig().commands.gamemodesEnabled) return;
        if (!actor.sender().hasPermission(plugin.getConfigManager().getMainConfig().permissions.gamemode)) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.noPermission));
            return;
        }

        Player player = resolveTarget(actor, messages, target);
        if (player == null) return;

        player.setGameMode(mode);
        player.sendMessage(TextUtils.format(messages.prefix +
                messages.commands.gamemodeChanged.replace("%gamemode%", mode.name().toLowerCase())));
    }

    /** Sin target: quien ejecuta el comando (debe ser un jugador). Con target: ese jugador, sin importar si quien ejecuta es consola. */
    private Player resolveTarget(BukkitCommandActor actor, MessagesConfig messages, String targetName) {
        if (targetName != null && !targetName.isBlank()) {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playerNotFound));
                return null;
            }
            return target;
        }

        if (!actor.isPlayer()) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return null;
        }
        return actor.requirePlayer();
    }
}
