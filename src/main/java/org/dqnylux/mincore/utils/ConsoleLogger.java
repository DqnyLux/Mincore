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
        prefix = TextUtils.format(config.prefix);
        errorPrefix = TextUtils.format(config.prefix + config.console.errorPrefix);
    }

    public static void logEnable(Mincore plugin, long loadTime) {
        MessagesConfig config = plugin.getConfigManager().getMessagesConfig();

        String author = plugin.getDescription().getAuthors().isEmpty() ? "Desconocido" : plugin.getDescription().getAuthors().get(0);
        String desc = plugin.getDescription().getDescription() != null ? plugin.getDescription().getDescription() : "Sin descripción";
        String version = plugin.getDescription().getVersion();

        String fork = Bukkit.getName();
        String serverVersion = Bukkit.getBukkitVersion();
        String javaVersion = System.getProperty("java.version");

        String[] versions = {"1.20", "1.21", "1.26"};
        StringBuilder supported = new StringBuilder();
        for (String v : versions) {
            if (serverVersion.contains(v)) {
                supported.append("<green>").append(v).append(" <#888888>| ");
            } else {
                supported.append("<#888888>").append(v).append(" | ");
            }
        }
        String supportedStr = supported.length() > 3 ? supported.substring(0, supported.length() - 3) : "";

        org.dqnylux.mincore.managers.DatabaseManager.StorageType storageType =
                plugin.getDatabaseManager() != null ? plugin.getDatabaseManager().getStorageType() : null;
        String dbStatus;

        if (storageType == null) {
            dbStatus = "<#888888>Desconocido - Sin inicializar";
        } else if (plugin.getDatabaseManager().isConnected()) {
            dbStatus = "<white>" + storageType + " <#888888>- <green>Conectada";
        } else {
            dbStatus = "<white>" + storageType + " <#888888>- <red>Error de conexión";
        }

        // --- ESTADO DINÁMICO DE REDIS ---
        String redisStatus;
        boolean redisEnabled = plugin.getConfigManager().getDatabaseConfig().redis.enabled;

        if (!redisEnabled) {
            redisStatus = "<#888888>Jedis (Redis) - Inactivo";
        } else if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isRedisConnected()) {
            redisStatus = "<white>Jedis (Redis) <#888888>- <green>Conectado";
        } else {
            redisStatus = "<white>Jedis (Redis) <#888888>- <red>Error de conexión";
        }

        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        String papiStatus = hasPapi ? "<green>Conectado" : "<red>No encontrado";

        for (String line : config.console.startupLogo) {
            SENDER.sendMessage(TextUtils.format(line));
        }

        for (String line : config.console.startupInfo) {
            String formatted = line.replace("%author%", author)
                    .replace("%description%", desc)
                    .replace("%version%", version)
                    .replace("%fork%", fork)
                    .replace("%server_version%", serverVersion)
                    .replace("%java%", javaVersion)
                    .replace("%supported_versions%", supportedStr)
                    .replace("%database%", dbStatus)
                    .replace("%redis%", redisStatus)
                    .replace("%hook_papi%", papiStatus)
                    .replace("%time%", String.valueOf(loadTime));
            SENDER.sendMessage(TextUtils.format(formatted));
        }
    }

    public static void logDisable(Mincore plugin) {
        if (plugin.getConfigManager() == null) return;
        MessagesConfig config = plugin.getConfigManager().getMessagesConfig();
        SENDER.sendMessage(prefix.append(TextUtils.format(config.console.shutdownMessage)));
    }

    public static void info(String message) {
        SENDER.sendMessage(prefix.append(TextUtils.format(message)));
    }

    public static void error(String message) {
        SENDER.sendMessage(errorPrefix.append(TextUtils.format(message)));
    }
}