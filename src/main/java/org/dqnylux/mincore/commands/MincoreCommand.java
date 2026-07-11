package org.dqnylux.mincore.commands;

import org.bukkit.command.CommandSender;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.menus.MainMenu;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;

@SuppressWarnings("unused")
public class MincoreCommand {

    private final Mincore plugin;

    public MincoreCommand(Mincore plugin) {
        this.plugin = plugin;
    }

    private boolean checkPermission(CommandSender sender) {
        if (!sender.hasPermission("mincore.admin")) {
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
    public void openMenu(PaperCommandActor actor) {
        if (!checkPermission(actor.sender())) return;

        if (!actor.isPlayer()) {
            String msg = plugin.getConfigManager().getMessagesConfig().prefix + "<red>Este comando solo puede ser ejecutado por un jugador.";
            actor.sender().sendMessage(TextUtils.format(msg));
            return;
        }

        MainMenu.open(actor.requirePlayer(), plugin);
    }
}