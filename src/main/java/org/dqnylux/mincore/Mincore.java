package org.dqnylux.mincore;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.dqnylux.mincore.listeners.AntiSignatureListener;
import org.dqnylux.mincore.listeners.AutoResponderListener;
import org.dqnylux.mincore.listeners.ChatListener;
import org.dqnylux.mincore.listeners.CombatCosmeticsListener;
import org.dqnylux.mincore.listeners.DeathListener;
import org.dqnylux.mincore.listeners.PlayerConnectionListener;
import org.dqnylux.mincore.listeners.TabCompleteListener;
import org.dqnylux.mincore.managers.AnnouncementManager;
import org.dqnylux.mincore.managers.CommandManager;
import org.dqnylux.mincore.managers.ConfigSyncManager;
import org.dqnylux.mincore.managers.CoreConfigManager;
import org.dqnylux.mincore.managers.DatabaseManager;
import org.dqnylux.mincore.managers.DeathTrackingManager;
import org.dqnylux.mincore.managers.DynamicCommandManager;
import org.dqnylux.mincore.managers.EconomyAdminHandler;
import org.dqnylux.mincore.managers.PlayerManager;
import org.dqnylux.mincore.managers.ReplyManager;
import org.dqnylux.mincore.managers.chat.ChatFilterManager;
import org.dqnylux.mincore.managers.chat.ChatFormatHandler;
import org.dqnylux.mincore.managers.chat.ChatPunishmentHandler;
import org.dqnylux.mincore.managers.cosmetics.CosmeticConfigManager;
import org.dqnylux.mincore.managers.cosmetics.CosmeticSyncManager;
import org.dqnylux.mincore.managers.cosmetics.EffectRegistry;
import org.dqnylux.mincore.managers.cosmetics.GlowManager;
import org.dqnylux.mincore.managers.cosmetics.TrailManager;
import org.dqnylux.mincore.managers.cosmetics.WingManager;
import org.dqnylux.mincore.hooks.LuckPermsHook;
import org.dqnylux.mincore.hooks.PAPIExpansion;
import org.dqnylux.mincore.tasks.ActiveCosmeticsTask;
import org.dqnylux.mincore.tasks.DeathGPSTask;
import org.dqnylux.mincore.tasks.ElytraCosmeticsTask;
import org.dqnylux.mincore.utils.ConsoleLogger;

public final class Mincore extends JavaPlugin {

    private static Mincore instance;
    private CoreConfigManager configManager;
    private DatabaseManager databaseManager;
    private CommandManager commandManager;
    private PlayerManager playerManager;
    private EconomyAdminHandler economyAdminHandler;
    private ChatFilterManager chatFilterManager;
    private ReplyManager replyManager;
    private AnnouncementManager announcementManager;
    private DynamicCommandManager dynamicCommandManager;
    private CosmeticConfigManager cosmeticConfigManager;
    private EffectRegistry effectRegistry;
    private TrailManager trailManager;
    private WingManager wingManager;
    private GlowManager glowManager;
    private ActiveCosmeticsTask activeCosmeticsTask;
    private ElytraCosmeticsTask elytraCosmeticsTask;
    private CosmeticSyncManager cosmeticSyncManager;
    private DeathTrackingManager deathTrackingManager;
    private DeathGPSTask deathGPSTask;
    private ConfigSyncManager configSyncManager;

    @Override
    public void onLoad() {
        instance = this;

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false)
                .bStats(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(new AntiSignatureListener());

        this.configManager = new CoreConfigManager(this);
        this.configManager.loadConfigs();

        this.commandManager = new CommandManager(this);

        this.databaseManager = new DatabaseManager(this, this.configManager.getDatabaseConfig());
        this.databaseManager.connect();

        this.playerManager = new PlayerManager(this);
        this.economyAdminHandler = new EconomyAdminHandler(this);
        this.chatFilterManager = new ChatFilterManager(this);
        ChatPunishmentHandler chatPunishmentHandler = new ChatPunishmentHandler(this);
        ChatFormatHandler chatFormatHandler = new ChatFormatHandler(this);

        this.replyManager = new ReplyManager();

        Bukkit.getPluginManager().registerEvents(new ChatListener(this, chatFilterManager, chatPunishmentHandler, chatFormatHandler), this);
        Bukkit.getPluginManager().registerEvents(new AutoResponderListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TabCompleteListener(), this);

        this.dynamicCommandManager = new DynamicCommandManager(this);
        this.dynamicCommandManager.reload();

        this.announcementManager = new AnnouncementManager(this);
        this.announcementManager.start();

        this.cosmeticConfigManager = new CosmeticConfigManager(this);
        this.cosmeticConfigManager.loadConfigs();
        this.effectRegistry = new EffectRegistry();
        this.trailManager = new TrailManager();
        this.wingManager = new WingManager();
        this.glowManager = new GlowManager();
        this.activeCosmeticsTask = new ActiveCosmeticsTask(this);
        this.elytraCosmeticsTask = new ElytraCosmeticsTask(this);
        this.cosmeticSyncManager = new CosmeticSyncManager(this);
        this.cosmeticSyncManager.startAutoSync();

        this.deathTrackingManager = new DeathTrackingManager();
        this.deathGPSTask = new DeathGPSTask(this);

        this.configSyncManager = new ConfigSyncManager(this);
        this.configSyncManager.startAutoSync();
        if (this.configManager.getDatabaseConfig().redis.enabled) {
            this.configSyncManager.subscribeToReloads();
        }

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatCosmeticsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            LuckPermsHook.init();
        }

        ConsoleLogger.init(this);
        ConsoleLogger.logEnable(this, System.currentTimeMillis() - startTime);
    }

    @Override
    public void onDisable() {
        ConsoleLogger.logDisable(this);

        if (this.announcementManager != null) {
            this.announcementManager.stop();
        }

        if (this.cosmeticSyncManager != null) {
            this.cosmeticSyncManager.stop();
        }

        if (this.configSyncManager != null) {
            this.configSyncManager.stop();
        }

        if (this.activeCosmeticsTask != null) {
            this.activeCosmeticsTask.stopAll();
        }

        if (this.elytraCosmeticsTask != null) {
            this.elytraCosmeticsTask.stopAll();
        }

        if (this.deathGPSTask != null) {
            this.deathGPSTask.stopAll();
        }

        if (this.dynamicCommandManager != null) {
            this.dynamicCommandManager.unregisterAll();
        }

        if (this.playerManager != null) {
            this.playerManager.saveAllSync();
        }

        if (this.databaseManager != null) {
            this.databaseManager.close();
        }

        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
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

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public EconomyAdminHandler getEconomyAdminHandler() {
        return economyAdminHandler;
    }

    public ChatFilterManager getChatFilterManager() {
        return chatFilterManager;
    }

    public ReplyManager getReplyManager() {
        return replyManager;
    }

    public AnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    public DynamicCommandManager getDynamicCommandManager() {
        return dynamicCommandManager;
    }

    public CosmeticConfigManager getCosmeticConfigManager() {
        return cosmeticConfigManager;
    }

    public EffectRegistry getEffectRegistry() {
        return effectRegistry;
    }

    public TrailManager getTrailManager() {
        return trailManager;
    }

    public WingManager getWingManager() {
        return wingManager;
    }

    public GlowManager getGlowManager() {
        return glowManager;
    }

    public ActiveCosmeticsTask getActiveCosmeticsTask() {
        return activeCosmeticsTask;
    }

    public ElytraCosmeticsTask getElytraCosmeticsTask() {
        return elytraCosmeticsTask;
    }

    public CosmeticSyncManager getCosmeticSyncManager() {
        return cosmeticSyncManager;
    }

    public DeathTrackingManager getDeathTrackingManager() {
        return deathTrackingManager;
    }

    public DeathGPSTask getDeathGPSTask() {
        return deathGPSTask;
    }

    public ConfigSyncManager getConfigSyncManager() {
        return configSyncManager;
    }
}