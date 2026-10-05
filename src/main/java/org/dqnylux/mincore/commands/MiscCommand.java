package org.dqnylux.mincore.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.annotation.Usage;
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
    @Usage("/fly [jugador]")
    @Description("Alterna el modo de vuelo para ti o para otro jugador")
    public void fly(BukkitCommandActor actor, @Optional @Named("jugador") @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (!plugin.getConfigManager().getMainConfig().commands.flyEnabled) return;
        if (!actor.sender().hasPermission(plugin.getConfigManager().getMainConfig().permissions.fly)) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.noPermission));
            return;
        }

        Player player = resolveTarget(actor, messages, target);
        if (player == null) return;

        // Si FlyTime está activo y el jugador objetivo actúa sobre sí mismo sin permiso de vuelo ilimitado
        if (plugin.getConfigManager().getModulesConfig().flytime && (target == null || target.isBlank())) {
            if (plugin.getFlyTimeManager() != null && !plugin.getFlyTimeManager().hasBypass(player)) {
                plugin.getFlyTimeManager().toggleFlight(player);
                return;
            }
        }

        boolean newState = !player.getAllowFlight();
        player.setAllowFlight(newState);
        player.setFlying(newState);

        if (plugin.getConfigManager().getModulesConfig().flytime && plugin.getFlyTimeManager() != null) {
            var ftPlayer = plugin.getFlyTimeManager().getOrCreatePlayer(player.getUniqueId());
            ftPlayer.setEnabled(newState);
            if (!newState) {
                plugin.getFlyTimeManager().addFallProtection(player.getUniqueId(),
                        plugin.getConfigManager().getFlyTimeConfig().fallDamageImmunitySeconds);
            }
        }

        String message = newState ? messages.commands.flyEnabled : messages.commands.flyDisabled;
        player.sendMessage(TextUtils.format(messages.prefix + message));

        if (newState) {
            if (plugin.getEssentialsToastManager() != null) {
                plugin.getEssentialsToastManager().showToast(player, org.dqnylux.mincore.managers.essentials.EssentialsToastManager.ToastType.FLY);
            }
            if (plugin.getFlyTimeManager() != null) {
                plugin.getFlyTimeManager().sendTitle(player, "<gradient:#70E1FF:#0088FF><bold>¡MODO VUELO!</bold></gradient>", "<#888888>Vuelo activado", 10, 40, 10);
                plugin.getFlyTimeManager().sendActionBar(player, "<#70E1FF>✦ <#EAEAEA>Modo de vuelo activado");
                plugin.getFlyTimeManager().playSound(player, "ITEM_ARMOR_EQUIP_ELYTRA", 1.0f, 1.0f);
            }
        }

        if (!actor.isPlayer() || !actor.requirePlayer().equals(player)) {
            String otherMessage = newState ? messages.commands.flyEnabledOther : messages.commands.flyDisabledOther;
            actor.sender().sendMessage(TextUtils.format(messages.prefix + otherMessage.replace("%player%", player.getName())));
        }
    }

    @Command("gmc")
    @CommandPermission("coreec.command.gamemode")
    @Usage("/gmc [jugador]")
    @Description("Cambia el modo de juego a Creativo")
    public void gamemodeCreative(BukkitCommandActor actor, @Optional @Named("jugador") @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        changeGamemode(actor, GameMode.CREATIVE, target);
    }

    @Command("gms")
    @CommandPermission("coreec.command.gamemode")
    @Usage("/gms [jugador]")
    @Description("Cambia el modo de juego a Supervivencia")
    public void gamemodeSurvival(BukkitCommandActor actor, @Optional @Named("jugador") @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        changeGamemode(actor, GameMode.SURVIVAL, target);
    }

    @Command("gma")
    @CommandPermission("coreec.command.gamemode")
    @Usage("/gma [jugador]")
    @Description("Cambia el modo de juego a Aventura")
    public void gamemodeAdventure(BukkitCommandActor actor, @Optional @Named("jugador") @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
        changeGamemode(actor, GameMode.ADVENTURE, target);
    }

    @Command("gmsp")
    @CommandPermission("coreec.command.gamemode")
    @Usage("/gmsp [jugador]")
    @Description("Cambia el modo de juego a Espectador")
    public void gamemodeSpectator(BukkitCommandActor actor, @Optional @Named("jugador") @SuggestWith(OnlinePlayerSuggestionProvider.class) String target) {
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

        if (!actor.isPlayer() || !actor.requirePlayer().equals(player)) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.gamemodeChangedOther
                    .replace("%player%", player.getName())
                    .replace("%gamemode%", mode.name().toLowerCase())));
        }
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
