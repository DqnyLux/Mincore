package org.dqnylux.mincore;

import org.bukkit.plugin.java.JavaPlugin;
import org.dqnylux.mincore.managers.CoreConfigManager;
import org.dqnylux.mincore.utils.ConsoleLogger;

public final class Mincore extends JavaPlugin {

    private static Mincore instance;
    private CoreConfigManager configManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        this.configManager = new CoreConfigManager(this);
        this.configManager.loadConfigs();

        ConsoleLogger.init(this);
        ConsoleLogger.logEnable(this, System.currentTimeMillis() - startTime);
    }

    @Override
    public void onDisable() {
        ConsoleLogger.logDisable(this);
    }

    public static Mincore getInstance() {
        return instance;
    }

    public CoreConfigManager getConfigManager() {
        return configManager;
    }
}