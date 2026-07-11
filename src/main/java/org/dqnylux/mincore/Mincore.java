package org.dqnylux.mincore;

import org.bukkit.plugin.java.JavaPlugin;
import org.dqnylux.mincore.managers.CommandManager;
import org.dqnylux.mincore.managers.CoreConfigManager;
import org.dqnylux.mincore.managers.DatabaseManager;
import org.dqnylux.mincore.utils.ConsoleLogger;

public final class Mincore extends JavaPlugin {

    private static Mincore instance;
    private CoreConfigManager configManager;
    private DatabaseManager databaseManager;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        this.configManager = new CoreConfigManager(this);
        this.configManager.loadConfigs();

        this.commandManager = new CommandManager(this);

        this.databaseManager = new DatabaseManager(this.configManager.getDatabaseConfig());
        this.databaseManager.connect();

        ConsoleLogger.init(this);
        ConsoleLogger.logEnable(this, System.currentTimeMillis() - startTime);
    }

    @Override
    public void onDisable() {
        ConsoleLogger.logDisable(this);

        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
    }

    @Override
    public org.bukkit.command.PluginCommand getCommand(@org.jetbrains.annotations.NotNull String name) {
        return null;
    }

    public static Mincore getInstance() {
        return instance;
    }

    public CoreConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}