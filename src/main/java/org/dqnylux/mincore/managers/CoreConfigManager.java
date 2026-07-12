package org.dqnylux.mincore.managers;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.bukkit.Bukkit;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.AnnouncementsConfig;
import org.dqnylux.mincore.config.BotsConfig;
import org.dqnylux.mincore.config.ChatFormatConfig;
import org.dqnylux.mincore.config.DatabaseConfig;
import org.dqnylux.mincore.config.FiltersConfig;
import org.dqnylux.mincore.config.MainConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.MincoreConfig;
import org.dqnylux.mincore.utils.TextUtils;

import java.io.File;

public class CoreConfigManager {

    private final Mincore plugin;
    private MessagesConfig messagesConfig;
    private DatabaseConfig databaseConfig;
    private MainConfig mainConfig;
    private FiltersConfig filtersConfig;
    private ChatFormatConfig chatFormatConfig;
    private BotsConfig botsConfig;
    private AnnouncementsConfig announcementsConfig;

    public CoreConfigManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        this.messagesConfig = loadConfig(MessagesConfig.class, "messages.yml", 1);
        this.databaseConfig = loadConfig(DatabaseConfig.class, "database.yml", 1);
        this.mainConfig = loadConfig(MainConfig.class, "config.yml", 1);
        this.filtersConfig = loadConfig(FiltersConfig.class, "filters.yml", 1);
        this.chatFormatConfig = loadConfig(ChatFormatConfig.class, "chatformat.yml", 1);
        this.botsConfig = loadConfig(BotsConfig.class, "bots.yml", 1);
        this.announcementsConfig = loadConfig(AnnouncementsConfig.class, "announcements.yml", 1);
    }

    private <T extends MincoreConfig> T loadConfig(Class<T> clazz, String fileName, int targetVersion) {
        T config = ConfigManager.create(clazz, (it) -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(new File(plugin.getDataFolder(), fileName));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        if (config.version < targetVersion) {
            printWarning(fileName, config.version, targetVersion);
            config.version = targetVersion;
            config.save();
        } else if (config.version > targetVersion) {
            printError(fileName, config.version, targetVersion);
        }

        return config;
    }

    private void printWarning(String fileName, int oldV, int newV) {
        String msg = "<#888888>[<#55FFFF>Mincore<#888888>] <yellow>El archivo <white>" + fileName + " <yellow>fue actualizado (v" + oldV + " -> v" + newV + ").";

        if (this.messagesConfig != null) {
            msg = this.messagesConfig.prefix + this.messagesConfig.console.configUpdated
                    .replace("%file%", fileName)
                    .replace("%old%", String.valueOf(oldV))
                    .replace("%new%", String.valueOf(newV));
        }

        Bukkit.getConsoleSender().sendMessage(TextUtils.format(msg));
    }

    private void printError(String fileName, int oldV, int newV) {
        String msg = "<#888888>[<#55FFFF>Mincore<#888888>] <red>Peligro: <white>" + fileName + " <red>tiene versión incompatible.";

        if (this.messagesConfig != null) {
            msg = this.messagesConfig.prefix + this.messagesConfig.console.configDowngraded
                    .replace("%file%", fileName)
                    .replace("%old%", String.valueOf(oldV))
                    .replace("%new%", String.valueOf(newV));
        }

        Bukkit.getConsoleSender().sendMessage(TextUtils.format(msg));
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public FiltersConfig getFiltersConfig() {
        return filtersConfig;
    }

    public ChatFormatConfig getChatFormatConfig() {
        return chatFormatConfig;
    }

    public BotsConfig getBotsConfig() {
        return botsConfig;
    }

    public AnnouncementsConfig getAnnouncementsConfig() {
        return announcementsConfig;
    }
}