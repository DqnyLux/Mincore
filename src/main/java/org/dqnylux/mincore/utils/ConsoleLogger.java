package org.dqnylux.mincore.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;

public class ConsoleLogger {

    private static final ConsoleCommandSender SENDER = Bukkit.getConsoleSender();
    private static Component prefix = Component.empty();
    private static Component errorPrefix = Component.empty();

    public static void init(Mincore plugin) {
        MessagesConfig config = plugin.getConfigManager().getMessagesConfig();
        prefix = ColorUtils.format(config.prefix);
        errorPrefix = ColorUtils.format(config.prefix + config.console.errorPrefix);
    }

    public static void logEnable(Mincore plugin, long loadTime) {
        MessagesConfig config = plugin.getConfigManager().getMessagesConfig();
        String version = plugin.getDescription().getVersion();

        for (String line : config.console.startupLogo) {
            SENDER.sendMessage(ColorUtils.format(line));
        }

        SENDER.sendMessage(prefix.append(ColorUtils.format(config.console.startupSuccess)));

        String details = config.console.startupDetails
                .replace("%version%", version)
                .replace("%time%", String.valueOf(loadTime));

        SENDER.sendMessage(prefix.append(ColorUtils.format(details)));
    }

    public static void logDisable(Mincore plugin) {
        if (plugin.getConfigManager() == null) return;
        MessagesConfig config = plugin.getConfigManager().getMessagesConfig();
        SENDER.sendMessage(prefix.append(ColorUtils.format(config.console.shutdownMessage)));
    }

    public static void info(String message) {
        SENDER.sendMessage(prefix.append(ColorUtils.format(message)));
    }

    public static void error(String message) {
        SENDER.sendMessage(errorPrefix.append(ColorUtils.format(message)));
    }
}