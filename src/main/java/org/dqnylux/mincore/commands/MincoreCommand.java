package org.dqnylux.mincore.commands;

import org.bukkit.command.CommandSender;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.managers.EconomyAdminHandler;
import org.dqnylux.mincore.menus.MainMenu;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

@SuppressWarnings("unused")
public class MincoreCommand {

    private final Mincore plugin;

    public MincoreCommand(Mincore plugin) {
        this.plugin = plugin;
    }

    private boolean checkPermission(CommandSender sender) {
        return checkPermission(sender, plugin.getConfigManager().getMainConfig().permissions.admin);
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                    plugin.getConfigManager().getMessagesConfig().commands.noPermission;
            sender.sendMessage(TextUtils.format(msg));
            return false;
        }
        return true;
    }

    @Command("mincore")
    public void defaultCommand(CommandSender sender) {
        if (!checkPermission(sender)) return;
        help(sender);
    }

    @Command("mincore reload")
    public void reload(CommandSender sender) {
        if (!checkPermission(sender)) return;

        long start = System.currentTimeMillis();
        plugin.getConfigManager().loadConfigs();
        plugin.getChatFilterManager().reload();
        plugin.getAnnouncementManager().start();
        plugin.getDynamicCommandManager().reload();
        long time = System.currentTimeMillis() - start;

        String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                plugin.getConfigManager().getMessagesConfig().commands.reloadSuccess
                        .replace("%ms%", String.valueOf(time));

        sender.sendMessage(TextUtils.format(msg));
    }

    @Command("mincore help")
    public void help(CommandSender sender) {
        if (!checkPermission(sender)) return;

        for (String line : plugin.getConfigManager().getMessagesConfig().commands.help) {
            sender.sendMessage(TextUtils.format(line));
        }
    }

    @Command("mincore menu")
    public void openMenu(BukkitCommandActor actor) {
        if (!checkPermission(actor.sender())) return;

        if (!actor.isPlayer()) {
            var messages = plugin.getConfigManager().getMessagesConfig();
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }

        MainMenu.open(actor.requirePlayer(), plugin);
    }

    @Command("mincore eco give")
    public void ecoGive(CommandSender sender, String player, double amount) {
        if (!checkPermission(sender)) return;
        EconomyAdminHandler.Result result = plugin.getEconomyAdminHandler().give(player, amount);
        replyEco(sender, result, player, amount, plugin.getConfigManager().getMessagesConfig().commands.ecoGiveSuccess);
    }

    @Command("mincore eco take")
    public void ecoTake(CommandSender sender, String player, double amount) {
        if (!checkPermission(sender)) return;
        EconomyAdminHandler.Result result = plugin.getEconomyAdminHandler().take(player, amount);
        replyEco(sender, result, player, amount, plugin.getConfigManager().getMessagesConfig().commands.ecoTakeSuccess);
    }

    @Command("mincore eco set")
    public void ecoSet(CommandSender sender, String player, double amount) {
        if (!checkPermission(sender)) return;
        EconomyAdminHandler.Result result = plugin.getEconomyAdminHandler().set(player, amount);
        replyEco(sender, result, player, amount, plugin.getConfigManager().getMessagesConfig().commands.ecoSetSuccess);
    }

    private void replyEco(CommandSender sender, EconomyAdminHandler.Result result, String player, double amount, String successTemplate) {
        var messages = plugin.getConfigManager().getMessagesConfig();
        String body = switch (result) {
            case SUCCESS -> successTemplate.replace("%player%", player).replace("%amount%", String.valueOf(amount));
            case PLAYER_OFFLINE -> messages.commands.ecoPlayerOffline;
            case INVALID_AMOUNT -> messages.commands.ecoInvalidAmount;
        };
        sender.sendMessage(TextUtils.format(messages.prefix + body));
    }

    @Command("mincore sync push")
    public void syncPush(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getCosmeticSyncManager().pushToDatabase();
        plugin.getConfigSyncManager().pushAll();
        var messages = plugin.getConfigManager().getMessagesConfig();
        sender.sendMessage(TextUtils.format(messages.prefix + messages.commands.syncPush));
    }

    @Command("mincore sync pull")
    public void syncPull(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getCosmeticSyncManager().pullFromDatabase();
        plugin.getConfigSyncManager().pullAll();
        var messages = plugin.getConfigManager().getMessagesConfig();
        sender.sendMessage(TextUtils.format(messages.prefix + messages.commands.syncPull));
    }

    @Command("mincore clearchat")
    public void clearChat(CommandSender sender) {
        if (!checkPermission(sender, plugin.getConfigManager().getMainConfig().permissions.clearchat)) return;

        var messages = plugin.getConfigManager().getMessagesConfig();
        int lines = plugin.getConfigManager().getMainConfig().commands.clearchatLines;
        net.kyori.adventure.text.Component blank = net.kyori.adventure.text.Component.empty();
        for (int i = 0; i < lines; i++) {
            org.bukkit.Bukkit.broadcast(blank);
        }
        org.bukkit.Bukkit.broadcast(TextUtils.format(
                messages.prefix + messages.commands.clearchatDone.replace("%player%", sender.getName())));
    }
}