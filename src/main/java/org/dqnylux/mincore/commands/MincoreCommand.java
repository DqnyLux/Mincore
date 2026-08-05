package org.dqnylux.mincore.commands;

import org.bukkit.command.CommandSender;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.managers.EconomyAdminHandler;
import org.dqnylux.mincore.menus.MainMenu;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

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

    @Command("coreec")
    @CommandPermission("coreec.admin")
    public void defaultCommand(CommandSender sender) {
        if (!checkPermission(sender)) return;
        help(sender);
    }

    @Command("coreec reload")
    @CommandPermission("coreec.admin")
    public void reload(CommandSender sender) {
        if (!checkPermission(sender)) return;

        long start = System.currentTimeMillis();
        plugin.getConfigManager().loadConfigs();
        plugin.getCosmeticConfigManager().loadConfigs();
        plugin.getChatFilterManager().reload();
        plugin.getAnnouncementManager().start();
        plugin.getDynamicCommandManager().reload();

        // Si hay red compartida (MySQL/MariaDB), este servidor sube lo que
        // acaba de recargar localmente para que el resto de la red lo
        // reciba solo (Redis instantáneo si está activo, o el poll
        // periódico como respaldo) - sección 18: "un cambio en un servidor
        // se refleja en toda la red sin editar el YAML a mano en cada máquina".
        plugin.getConfigSyncManager().pushAll();
        plugin.getCosmeticSyncManager().pushToDatabase();

        long time = System.currentTimeMillis() - start;

        String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                plugin.getConfigManager().getMessagesConfig().commands.reloadSuccess
                        .replace("%ms%", String.valueOf(time));

        sender.sendMessage(TextUtils.format(msg));
    }

    @Command("coreec reload catalog")
    @CommandPermission("coreec.admin")
    public void reloadCatalog(CommandSender sender) {
        if (!checkPermission(sender)) return;

        long start = System.currentTimeMillis();
        plugin.getCosmeticConfigManager().reloadFromResources();
        plugin.getConfigSyncManager().pushAll();
        plugin.getCosmeticSyncManager().pushToDatabase();
        long time = System.currentTimeMillis() - start;

        String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                plugin.getConfigManager().getMessagesConfig().commands.resetCatalogSuccess
                        .replace("%ms%", String.valueOf(time));
        sender.sendMessage(TextUtils.format(msg));
    }

    @Command("coreec help")
    @CommandPermission("coreec.admin")
    public void help(CommandSender sender) {
        if (!checkPermission(sender)) return;

        for (String line : plugin.getConfigManager().getMessagesConfig().commands.help) {
            sender.sendMessage(TextUtils.format(line));
        }
    }

    @Command("coreec menu")
    @CommandPermission("coreec.admin")
    public void openMenu(BukkitCommandActor actor) {
        if (!checkPermission(actor.sender())) return;

        if (!actor.isPlayer()) {
            var messages = plugin.getConfigManager().getMessagesConfig();
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }

        MainMenu.open(actor.requirePlayer(), plugin);
    }

    @Command("coreec eco give")
    @CommandPermission("coreec.admin")
    public void ecoGive(CommandSender sender, @SuggestWith(OnlinePlayerSuggestionProvider.class) String player, double amount) {
        if (!checkPermission(sender)) return;
        EconomyAdminHandler.Result result = plugin.getEconomyAdminHandler().give(player, amount);
        replyEco(sender, result, player, amount, plugin.getConfigManager().getMessagesConfig().commands.ecoGiveSuccess);
    }

    @Command("coreec eco take")
    @CommandPermission("coreec.admin")
    public void ecoTake(CommandSender sender, @SuggestWith(OnlinePlayerSuggestionProvider.class) String player, double amount) {
        if (!checkPermission(sender)) return;
        EconomyAdminHandler.Result result = plugin.getEconomyAdminHandler().take(player, amount);
        replyEco(sender, result, player, amount, plugin.getConfigManager().getMessagesConfig().commands.ecoTakeSuccess);
    }

    @Command("coreec eco set")
    @CommandPermission("coreec.admin")
    public void ecoSet(CommandSender sender, @SuggestWith(OnlinePlayerSuggestionProvider.class) String player, double amount) {
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

    @Command("coreec sync push")
    @CommandPermission("coreec.admin")
    public void syncPush(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getCosmeticSyncManager().pushToDatabase();
        plugin.getConfigSyncManager().pushAll();
        var messages = plugin.getConfigManager().getMessagesConfig();
        sender.sendMessage(TextUtils.format(messages.prefix + messages.commands.syncPush));
    }

    @Command("coreec sync pull")
    @CommandPermission("coreec.admin")
    public void syncPull(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getCosmeticSyncManager().pullFromDatabase();
        plugin.getConfigSyncManager().pullAll();
        var messages = plugin.getConfigManager().getMessagesConfig();
        sender.sendMessage(TextUtils.format(messages.prefix + messages.commands.syncPull));
    }

    @Command("coreec commandblocker reload")
    @CommandPermission("coreec.admin")
    public void commandBlockerReload(CommandSender sender) {
        if (!checkPermission(sender)) return;

        long start = System.currentTimeMillis();
        plugin.getConfigManager().reloadCommandBlockerConfig();
        long time = System.currentTimeMillis() - start;

        String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                plugin.getConfigManager().getMessagesConfig().commands.commandBlockerReloadSuccess
                        .replace("%ms%", String.valueOf(time));
        sender.sendMessage(TextUtils.format(msg));
    }

    @Command("coreec ai refreshwords")
    @CommandPermission("coreec.admin")
    public void aiRefreshWords(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getChatFilterManager().generateReviewWordsAsync(sender);
    }

    @Command("coreec ai refreshbadwords")
    @CommandPermission("coreec.admin")
    public void aiRefreshBadWords(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getChatFilterManager().generateBadWordsAsync(sender);
    }

    @Command("coreec clearchat")
    @CommandPermission("coreec.admin.clearchat")
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