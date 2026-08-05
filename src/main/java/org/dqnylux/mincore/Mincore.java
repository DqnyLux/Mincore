package org.dqnylux.mincore;

import com.github.retrooper.packetevents.PacketEvents;
import dev.triumphteam.gui.TriumphGui;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.dqnylux.mincore.listeners.AdvancementBlockListener;
import org.dqnylux.mincore.listeners.AntiSignatureListener;
import org.dqnylux.mincore.listeners.AutoResponderListener;
import org.dqnylux.mincore.listeners.ChatListener;
import org.dqnylux.mincore.listeners.CombatCosmeticsListener;
import org.dqnylux.mincore.listeners.DeathListener;
import org.dqnylux.mincore.listeners.PlayerConnectionListener;
import org.dqnylux.mincore.listeners.StaffListener;
import org.dqnylux.mincore.listeners.TabCompleteListener;
import org.dqnylux.mincore.managers.AnnouncementManager;
import org.dqnylux.mincore.managers.CommandManager;
import org.dqnylux.mincore.managers.ConfigSyncManager;
import org.dqnylux.mincore.managers.CoreConfigManager;
import org.dqnylux.mincore.managers.DatabaseManager;
import org.dqnylux.mincore.managers.DeathTrackingManager;
import org.dqnylux.mincore.managers.DynamicCommandManager;
import org.dqnylux.mincore.managers.EconomyAdminHandler;
import org.dqnylux.mincore.managers.MotdManager;
import org.dqnylux.mincore.managers.PlayerManager;
import org.dqnylux.mincore.managers.ReplyManager;
import org.dqnylux.mincore.managers.chat.ChatFilterManager;
import org.dqnylux.mincore.managers.chat.ChatFormatHandler;
import org.dqnylux.mincore.managers.chat.ChatPunishmentHandler;
import org.dqnylux.mincore.managers.cosmetics.CosmeticConfigManager;
import org.dqnylux.mincore.managers.cosmetics.CosmeticSyncManager;
import org.dqnylux.mincore.managers.cosmetics.EffectRegistry;
import org.dqnylux.mincore.managers.cosmetics.GlowManager;
import org.dqnylux.mincore.managers.cosmetics.PreviewZoneManager;
import org.dqnylux.mincore.managers.cosmetics.TabListManager;
import org.dqnylux.mincore.managers.cosmetics.TrailManager;
import org.dqnylux.mincore.managers.cosmetics.WingManager;
import org.dqnylux.mincore.managers.staff.StaffModeManager;
import org.dqnylux.mincore.managers.staff.StaffNetworkManager;
import org.dqnylux.mincore.managers.staff.VanishManager;
import org.dqnylux.mincore.pozomillonario.listeners.PozoMachineInteractListener;
import org.dqnylux.mincore.pozomillonario.managers.PozoAnimationRegistry;
import org.dqnylux.mincore.pozomillonario.managers.PozoCatalogManager;
import org.dqnylux.mincore.pozomillonario.managers.PozoCraftingManager;
import org.dqnylux.mincore.pozomillonario.managers.PozoDataManager;
import org.dqnylux.mincore.pozomillonario.managers.PozoLootHistoryManager;
import org.dqnylux.mincore.pozomillonario.managers.PozoMachineManager;
import org.dqnylux.mincore.pozomillonario.managers.PozoOpenManager;
import org.dqnylux.mincore.pozomillonario.managers.PozoRewardDispatcher;
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
    private TabListManager tabListManager;
    private ActiveCosmeticsTask activeCosmeticsTask;
    private ElytraCosmeticsTask elytraCosmeticsTask;
    private CosmeticSyncManager cosmeticSyncManager;
    private DeathTrackingManager deathTrackingManager;
    private DeathGPSTask deathGPSTask;
    private ConfigSyncManager configSyncManager;
    private MotdManager motdManager;
    private PreviewZoneManager previewZoneManager;
    private VanishManager vanishManager;
    private StaffModeManager staffModeManager;
    private StaffNetworkManager staffNetworkManager;
    private org.dqnylux.mincore.managers.staff.FreezeManager freezeManager;
    private org.dqnylux.mincore.managers.staff.DisguiseManager disguiseManager;
    private org.dqnylux.mincore.managers.nametag.NametagDisplayManager nametagDisplayManager;
    private org.dqnylux.mincore.managers.CommandBlockerManager commandBlockerManager;
    private org.dqnylux.mincore.sanctions.managers.SanctionDataManager sanctionDataManager;
    private org.dqnylux.mincore.sanctions.managers.SanctionManager sanctionManager;
    private org.dqnylux.mincore.reports.managers.ReportDataManager reportDataManager;
    private org.dqnylux.mincore.reports.managers.ReportManager reportManager;
    private PozoCatalogManager pozoCatalogManager;
    private PozoDataManager pozoDataManager;
    private PozoLootHistoryManager pozoLootHistoryManager;
    private PozoRewardDispatcher pozoRewardDispatcher;
    private PozoOpenManager pozoOpenManager;
    private PozoCraftingManager pozoCraftingManager;
    private PozoMachineManager pozoMachineManager;
    private PozoAnimationRegistry pozoAnimationRegistry;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // TriumphGui.getPlugin() tiene un fallback con
        // JavaPlugin.getProvidingPlugin(BaseGui.class) que sí funciona aunque
        // la librería se cargue como dependencia externa (a diferencia del
        // ConfiguredPluginClassLoader-only de InvUI, que fue justo lo que
        // rompió con InvUI) - aun así se setea explícito aquí, barato y evita
        // depender de esa detección automática.
        TriumphGui.init(this);

        // PacketEvents lo inicializa y termina el plugin "packetevents"
        // instalado en el servidor (dependencia hard en paper-plugin.yml,
        // load: BEFORE) - Mincore solo se engancha a esa instancia ya
        // cargada. Ver el comentario en MincoreLoader.classloader() para el
        // porqué de no auto-hospedarlo.
        PacketEvents.getAPI().getEventManager().registerListener(new AntiSignatureListener());

        // EntityLib (Tofaa): entidades falsas TextDisplay para el nametag
        // propio (managers/nametag) - librería bundleada (MincoreLoader), no
        // un plugin externo opcional, así que se inicializa siempre acá, sin
        // chequeo de presencia (a diferencia de TabHook/LuckPermsHook).
        //
        // disableBStats() es OBLIGATORIO acá, no cosmético: el bStats propio
        // de EntityLib reutiliza clases internas de packetevents-spigot
        // (io.github.retrooper.packetevents.bstats.charts.CustomChart) - esa
        // librería NO se bundlea en MincoreLoader a propósito (se usa la del
        // plugin "packetevents" instalado aparte, ver el comentario arriba),
        // y el classloader de librerías de MincoreLoader no tiene visibilidad
        // hacia ese plugin externo, así que sin esto el server no arranca
        // (NoClassDefFoundError: CustomChart, apenas se llama a init()).
        EntityLib.init(new SpigotEntityLibPlatform(this),
                new APIConfig(PacketEvents.getAPI()).usePlatformLogger().disableBStats());

        this.configManager = new CoreConfigManager(this);
        this.configManager.loadConfigs();

        this.commandManager = new CommandManager(this);

        this.databaseManager = new DatabaseManager(this, this.configManager.getDatabaseConfig());
        this.databaseManager.connect();

        this.playerManager = new PlayerManager(this);
        this.economyAdminHandler = new EconomyAdminHandler(this);
        this.chatFilterManager = new ChatFilterManager(this);
        this.motdManager = new MotdManager(this);
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

        // Pozo Millonario: instanciado después de cosmeticConfigManager (el
        // dispatcher de recompensas tipo COSMETIC lo necesita) y después de
        // databaseManager.connect() (ya conectado más arriba).
        this.pozoCatalogManager = new PozoCatalogManager(this);
        this.pozoCatalogManager.loadConfigs();
        this.pozoDataManager = new PozoDataManager(this);
        this.pozoLootHistoryManager = new PozoLootHistoryManager(this);
        // Poda una vez al arrancar (no hace falta un job periódico - el
        // servidor típicamente reinicia al menos una vez al día).
        this.pozoLootHistoryManager.pruneOlderThan(this.pozoCatalogManager.getSettings().historyRetentionDays);
        this.pozoRewardDispatcher = new PozoRewardDispatcher(this);
        this.pozoOpenManager = new PozoOpenManager(this);
        this.pozoCraftingManager = new PozoCraftingManager(this);
        this.pozoMachineManager = new PozoMachineManager(this);
        this.pozoMachineManager.loadMachines();
        this.pozoAnimationRegistry = new PozoAnimationRegistry(this);
        Bukkit.getPluginManager().registerEvents(new PozoMachineInteractListener(this), this);

        this.effectRegistry = new EffectRegistry();
        this.trailManager = new TrailManager();
        this.wingManager = new WingManager();
        this.glowManager = new GlowManager();
        this.glowManager.startAutoResend(this);
        this.tabListManager = new TabListManager(this);
        this.tabListManager.startAutoResync();
        this.activeCosmeticsTask = new ActiveCosmeticsTask(this);
        this.elytraCosmeticsTask = new ElytraCosmeticsTask(this);
        this.cosmeticSyncManager = new CosmeticSyncManager(this);
        this.cosmeticSyncManager.startAutoSync();
        this.previewZoneManager = new PreviewZoneManager(this);
        Bukkit.getPluginManager().registerEvents(this.previewZoneManager, this);

        this.deathTrackingManager = new DeathTrackingManager();
        this.deathGPSTask = new DeathGPSTask(this);

        this.configSyncManager = new ConfigSyncManager(this);
        this.configSyncManager.startAutoSync();
        if (this.configManager.getDatabaseConfig().redis.enabled) {
            this.configSyncManager.subscribeToReloads();
        }

        this.vanishManager = new VanishManager(this);
        this.staffModeManager = new StaffModeManager(this);
        this.staffNetworkManager = new StaffNetworkManager(this);
        this.freezeManager = new org.dqnylux.mincore.managers.staff.FreezeManager(this);
        this.disguiseManager = new org.dqnylux.mincore.managers.staff.DisguiseManager(this);
        // Después de glowManager (necesita GlowManager.hideVanillaNametag/
        // showVanillaNametag para el camino sin TAB) y de disguiseManager
        // (necesita DisguiseManager.displayName/isDisguised).
        this.nametagDisplayManager = new org.dqnylux.mincore.managers.nametag.NametagDisplayManager(this);
        this.nametagDisplayManager.startAutoResend();
        this.commandBlockerManager = new org.dqnylux.mincore.managers.CommandBlockerManager(this);
        this.sanctionDataManager = new org.dqnylux.mincore.sanctions.managers.SanctionDataManager(this);
        this.sanctionManager = new org.dqnylux.mincore.sanctions.managers.SanctionManager(this);
        this.reportDataManager = new org.dqnylux.mincore.reports.managers.ReportDataManager(this);
        this.reportManager = new org.dqnylux.mincore.reports.managers.ReportManager(this);
        if (this.configManager.getDatabaseConfig().redis.enabled) {
            this.staffNetworkManager.subscribeToNetwork();
        }

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatCosmeticsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AdvancementBlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new StaffListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.dqnylux.mincore.listeners.FreezeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.dqnylux.mincore.listeners.CommandVisibilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.dqnylux.mincore.listeners.CommandBlockerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.dqnylux.mincore.sanctions.listeners.SanctionListener(this), this);

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

        if (this.glowManager != null) {
            this.glowManager.stop();
        }

        if (this.nametagDisplayManager != null) {
            this.nametagDisplayManager.stop();
        }

        if (this.tabListManager != null) {
            this.tabListManager.stop();
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

        if (this.pozoDataManager != null) {
            this.pozoDataManager.saveAllSync();
        }

        if (this.pozoMachineManager != null) {
            this.pozoMachineManager.despawnAll();
        }

        if (this.databaseManager != null) {
            this.databaseManager.close();
        }

        // No se llama a PacketEvents.getAPI().terminate() aquí: la instancia
        // pertenece al plugin "packetevents", no a Mincore, y otros plugins
        // del servidor pueden seguir dependiendo de ella tras deshabilitarse
        // Mincore.
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

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public PreviewZoneManager getPreviewZoneManager() {
        return previewZoneManager;
    }

    public MotdManager getMotdManager() {
        return motdManager;
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

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }

    public StaffNetworkManager getStaffNetworkManager() {
        return staffNetworkManager;
    }

    public org.dqnylux.mincore.managers.staff.FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public org.dqnylux.mincore.managers.staff.DisguiseManager getDisguiseManager() {
        return disguiseManager;
    }

    public org.dqnylux.mincore.managers.CommandBlockerManager getCommandBlockerManager() {
        return commandBlockerManager;
    }

    public org.dqnylux.mincore.sanctions.managers.SanctionDataManager getSanctionDataManager() {
        return sanctionDataManager;
    }

    public org.dqnylux.mincore.sanctions.managers.SanctionManager getSanctionManager() {
        return sanctionManager;
    }

    public org.dqnylux.mincore.reports.managers.ReportDataManager getReportDataManager() {
        return reportDataManager;
    }

    public org.dqnylux.mincore.reports.managers.ReportManager getReportManager() {
        return reportManager;
    }

    public org.dqnylux.mincore.managers.nametag.NametagDisplayManager getNametagDisplayManager() {
        return nametagDisplayManager;
    }

    public PozoCatalogManager getPozoCatalogManager() {
        return pozoCatalogManager;
    }

    public PozoDataManager getPozoDataManager() {
        return pozoDataManager;
    }

    public PozoLootHistoryManager getPozoLootHistoryManager() {
        return pozoLootHistoryManager;
    }

    public PozoRewardDispatcher getPozoRewardDispatcher() {
        return pozoRewardDispatcher;
    }

    public PozoOpenManager getPozoOpenManager() {
        return pozoOpenManager;
    }

    public PozoCraftingManager getPozoCraftingManager() {
        return pozoCraftingManager;
    }

    public PozoMachineManager getPozoMachineManager() {
        return pozoMachineManager;
    }

    public PozoAnimationRegistry getPozoAnimationRegistry() {
        return pozoAnimationRegistry;
    }
}