package org.dqnylux.mincore.managers;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.bukkit.Bukkit;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.AfkConfig;
import org.dqnylux.mincore.config.AnnouncementsConfig;
import org.dqnylux.mincore.config.AntiPacketExploitConfig;
import org.dqnylux.mincore.config.BotsConfig;
import org.dqnylux.mincore.config.CategoriesMenuConfig;
import org.dqnylux.mincore.config.ChatFormatConfig;
import org.dqnylux.mincore.config.CommandBlockerConfig;
import org.dqnylux.mincore.config.CosmeticsMenuConfig;
import org.dqnylux.mincore.config.DatabaseConfig;
import org.dqnylux.mincore.config.DeathSystemConfig;
import org.dqnylux.mincore.config.EssentialsConfig;
import org.dqnylux.mincore.config.FiltersConfig;
import org.dqnylux.mincore.config.HomesConfig;
import org.dqnylux.mincore.config.MainConfig;
import org.dqnylux.mincore.config.MainMenuConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.MincoreConfig;
import org.dqnylux.mincore.config.ModulesConfig;
import org.dqnylux.mincore.config.NametagConfig;
import org.dqnylux.mincore.config.ReportsConfig;
import org.dqnylux.mincore.config.SanctionsConfig;
import org.dqnylux.mincore.config.StaffConfig;
import org.dqnylux.mincore.config.WarpsConfig;
import org.dqnylux.mincore.flytime.config.FlyTimeConfig;
import org.dqnylux.mincore.flytime.config.FlyTimeMessagesConfig;
import org.dqnylux.mincore.profiles.config.ProfilesConfig;
import org.dqnylux.mincore.profiles.config.ProfilesLayoutConfig;
import org.dqnylux.mincore.profiles.config.ProfilesMessagesConfig;
import org.dqnylux.mincore.rewards.config.RewardsConfig;
import org.dqnylux.mincore.rewards.config.RewardsLayoutsConfig;
import org.dqnylux.mincore.rewards.config.RewardsMessagesConfig;
import org.dqnylux.mincore.timelimit.config.TimeLimitConfig;
import org.dqnylux.mincore.timelimit.config.TimeLimitLayoutConfig;
import org.dqnylux.mincore.timelimit.config.TimeLimitMessagesConfig;
import org.dqnylux.mincore.vaults.config.VaultsConfig;
import org.dqnylux.mincore.vaults.config.VaultsLayoutsConfig;
import org.dqnylux.mincore.vaults.config.VaultsMessagesConfig;
import org.dqnylux.mincore.utils.TextUtils;

import java.io.File;

public class CoreConfigManager {

    private final Mincore plugin;
    private MessagesConfig messagesConfig;
    private DatabaseConfig databaseConfig;
    private MainConfig mainConfig;
    private FiltersConfig filtersConfig;
    private ChatFormatConfig chatFormatConfig;
    private DeathSystemConfig deathSystemConfig;
    private StaffConfig staffConfig;
    private BotsConfig botsConfig;
    private AnnouncementsConfig announcementsConfig;
    private MainMenuConfig mainMenuConfig;
    private CategoriesMenuConfig categoriesMenuConfig;
    private CosmeticsMenuConfig cosmeticsMenuConfig;
    private NametagConfig nametagConfig;
    private CommandBlockerConfig commandBlockerConfig;
    private SanctionsConfig sanctionsConfig;
    private ReportsConfig reportsConfig;
    private ModulesConfig modulesConfig;
    private EssentialsConfig essentialsConfig;
    private HomesConfig homesConfig;
    private WarpsConfig warpsConfig;
    private AfkConfig afkConfig;
    private RewardsConfig rewardsConfig;
    private RewardsMessagesConfig rewardsMessagesConfig;
    private RewardsLayoutsConfig rewardsLayoutsConfig;
    private VaultsConfig vaultsConfig;
    private VaultsMessagesConfig vaultsMessagesConfig;
    private VaultsLayoutsConfig vaultsLayoutsConfig;
    private FlyTimeConfig flyTimeConfig;
    private FlyTimeMessagesConfig flyTimeMessagesConfig;
    private ProfilesConfig profilesConfig;
    private ProfilesMessagesConfig profilesMessagesConfig;
    private ProfilesLayoutConfig profilesLayoutConfig;
    private TimeLimitConfig timeLimitConfig;
    private TimeLimitMessagesConfig timeLimitMessagesConfig;
    private TimeLimitLayoutConfig timeLimitLayoutConfig;
    private AntiPacketExploitConfig antiPacketExploitConfig;

    public CoreConfigManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        this.messagesConfig = loadConfig(MessagesConfig.class, "messages.yml", 1);
        this.databaseConfig = loadConfig(DatabaseConfig.class, "database.yml", 1);
        this.mainConfig = loadConfig(MainConfig.class, "config.yml", 1);
        this.filtersConfig = loadConfig(FiltersConfig.class, "filters.yml", 1);
        // v2: parts pasó de String plano a objeto {text: ...} (name suma
        // hover/suggest) y mentions se reestructuró en sub-secciones - un
        // chatformat.yml v1 existente no migra en caliente (choque de tipos
        // en el YAML), hay que borrarlo para que se regenere con el nuevo formato.
        this.chatFormatConfig = loadConfig(ChatFormatConfig.class, "chatformat.yml", 2);
        this.deathSystemConfig = loadConfig(DeathSystemConfig.class, "death_system.yml", 1);
        this.staffConfig = loadConfig(StaffConfig.class, "staff.yml", 1);
        this.botsConfig = loadConfig(BotsConfig.class, "bots.yml", 1);
        this.announcementsConfig = loadConfig(AnnouncementsConfig.class, "announcements.yml", 1);
        // menus/*.yml - una carpeta por config de menú (sección 17: cero
        // hardcodeo), igual que cosmetics/*.yml ya hace un archivo por categoría.
        this.mainMenuConfig = loadConfig(MainMenuConfig.class, "menus/main_menu.yml", 1);
        this.categoriesMenuConfig = loadConfig(CategoriesMenuConfig.class, "menus/categories_menu.yml", 1);
        this.cosmeticsMenuConfig = loadConfig(CosmeticsMenuConfig.class, "menus/cosmetics_menu.yml", 1);
        this.nametagConfig = loadConfig(NametagConfig.class, "nametag.yml", 1);
        this.commandBlockerConfig = loadConfig(CommandBlockerConfig.class, "commandblocker.yml", 1);
        this.sanctionsConfig = loadConfig(SanctionsConfig.class, "sanctions.yml", 1);
        this.reportsConfig = loadConfig(ReportsConfig.class, "reports.yml", 1);
        // modules.yml: interruptor de las features "core" nuevas (roadmap del
        // plan) - se carga primero que nada las necesite en Mincore#onEnable
        // para poder gatear si esos managers/listeners siquiera se instancian.
        this.modulesConfig = loadConfig(ModulesConfig.class, "modules.yml", 1);
        this.essentialsConfig = loadConfig(EssentialsConfig.class, "essentials.yml", 1);
        // v2: el GUI de casas y warps pasó a layout configurable (structure +
        // MenuItem) como los cosméticos - un homes.yml/warps.yml v1 existente
        // conserva guiRows/guiHomeLore* y se regenera con el nuevo formato.
        this.homesConfig = loadConfig(HomesConfig.class, "homes.yml", 2);
        this.warpsConfig = loadConfig(WarpsConfig.class, "warps.yml", 2);
        this.afkConfig = loadConfig(AfkConfig.class, "afk.yml", 7);
        this.rewardsConfig = loadConfig(RewardsConfig.class, "modules/rewards/rewards.yml", 1);
        this.rewardsMessagesConfig = loadConfig(RewardsMessagesConfig.class, "modules/rewards/rewards_messages.yml", 1);
        this.rewardsLayoutsConfig = loadConfig(RewardsLayoutsConfig.class, "modules/rewards/rewards_layouts.yml", 1);
        this.vaultsConfig = loadConfig(VaultsConfig.class, "modules/vaults/vaults.yml", 1);
        this.vaultsMessagesConfig = loadConfig(VaultsMessagesConfig.class, "modules/vaults/vaults_messages.yml", 1);
        this.vaultsLayoutsConfig = loadConfig(VaultsLayoutsConfig.class, "modules/vaults/vaults_layouts.yml", 1);
        this.flyTimeConfig = loadConfig(FlyTimeConfig.class, "modules/flytime/flytime.yml", 1);
        this.flyTimeMessagesConfig = loadConfig(FlyTimeMessagesConfig.class, "modules/flytime/flytime_messages.yml", 1);
        this.profilesConfig = loadConfig(ProfilesConfig.class, "modules/profiles/profiles.yml", 1);
        this.profilesMessagesConfig = loadConfig(ProfilesMessagesConfig.class, "modules/profiles/profiles_messages.yml", 1);
        this.profilesLayoutConfig = loadConfig(ProfilesLayoutConfig.class, "modules/profiles/profiles_layout.yml", 2);
        this.timeLimitConfig = loadConfig(TimeLimitConfig.class, "modules/timelimit/timelimit.yml", 1);
        this.timeLimitMessagesConfig = loadConfig(TimeLimitMessagesConfig.class, "modules/timelimit/timelimit_messages.yml", 1);
        this.timeLimitLayoutConfig = loadConfig(TimeLimitLayoutConfig.class, "modules/timelimit/timelimit_layout.yml", 1);
        this.antiPacketExploitConfig = loadConfig(AntiPacketExploitConfig.class, "antipacketexploit.yml", 1);
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
        String msg = "<#888888>[<#55FFFF>Mincore<#888888>] <#FFEB3B>El archivo <#FFFFFF>" + fileName + " <#FFEB3B>fue actualizado (v" + oldV + " -> v" + newV + ").";

        if (this.messagesConfig != null) {
            msg = this.messagesConfig.prefix + this.messagesConfig.console.configUpdated
                    .replace("%file%", fileName)
                    .replace("%old%", String.valueOf(oldV))
                    .replace("%new%", String.valueOf(newV));
        }

        Bukkit.getConsoleSender().sendMessage(TextUtils.format(msg));
    }

    private void printError(String fileName, int oldV, int newV) {
        String msg = "<#888888>[<#55FFFF>Mincore<#888888>] <#FF4C4C>Peligro: <#FFFFFF>" + fileName + " <#FF4C4C>tiene versión incompatible.";

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

    public DeathSystemConfig getDeathSystemConfig() {
        return deathSystemConfig;
    }

    public StaffConfig getStaffConfig() {
        return staffConfig;
    }

    public BotsConfig getBotsConfig() {
        return botsConfig;
    }

    public AnnouncementsConfig getAnnouncementsConfig() {
        return announcementsConfig;
    }

    public MainMenuConfig getMainMenuConfig() {
        return mainMenuConfig;
    }

    public CategoriesMenuConfig getCategoriesMenuConfig() {
        return categoriesMenuConfig;
    }

    public CosmeticsMenuConfig getCosmeticsMenuConfig() {
        return cosmeticsMenuConfig;
    }

    public NametagConfig getNametagConfig() {
        return nametagConfig;
    }

    public CommandBlockerConfig getCommandBlockerConfig() {
        return commandBlockerConfig;
    }

    public void reloadCommandBlockerConfig() {
        this.commandBlockerConfig = loadConfig(CommandBlockerConfig.class, "commandblocker.yml", 1);
    }

    public SanctionsConfig getSanctionsConfig() {
        return sanctionsConfig;
    }

    public ReportsConfig getReportsConfig() {
        return reportsConfig;
    }

    public ModulesConfig getModulesConfig() {
        return modulesConfig;
    }

    public EssentialsConfig getEssentialsConfig() {
        return essentialsConfig;
    }

    public HomesConfig getHomesConfig() {
        return homesConfig;
    }

    public WarpsConfig getWarpsConfig() {
        return warpsConfig;
    }

    public AfkConfig getAfkConfig() {
        return afkConfig;
    }

    public void reloadAfkConfig() {
        this.afkConfig = loadConfig(AfkConfig.class, "afk.yml", 2);
    }

    public RewardsConfig getRewardsConfig() {
        return rewardsConfig;
    }

    public RewardsMessagesConfig getRewardsMessagesConfig() {
        return rewardsMessagesConfig;
    }

    public RewardsLayoutsConfig getRewardsLayoutsConfig() {
        return rewardsLayoutsConfig;
    }

    public void reloadRewardsConfigs() {
        this.rewardsConfig = loadConfig(RewardsConfig.class, "modules/rewards/rewards.yml", 1);
        this.rewardsMessagesConfig = loadConfig(RewardsMessagesConfig.class, "modules/rewards/rewards_messages.yml", 1);
        this.rewardsLayoutsConfig = loadConfig(RewardsLayoutsConfig.class, "modules/rewards/rewards_layouts.yml", 1);
    }

    public VaultsConfig getVaultsConfig() {
        return vaultsConfig;
    }

    public VaultsMessagesConfig getVaultsMessagesConfig() {
        return vaultsMessagesConfig;
    }

    public VaultsLayoutsConfig getVaultsLayoutsConfig() {
        return vaultsLayoutsConfig;
    }

    public void reloadVaults() {
        this.vaultsConfig = loadConfig(VaultsConfig.class, "modules/vaults/vaults.yml", 1);
        this.vaultsMessagesConfig = loadConfig(VaultsMessagesConfig.class, "modules/vaults/vaults_messages.yml", 1);
        this.vaultsLayoutsConfig = loadConfig(VaultsLayoutsConfig.class, "modules/vaults/vaults_layouts.yml", 1);
    }

    public FlyTimeConfig getFlyTimeConfig() {
        return flyTimeConfig;
    }

    public FlyTimeMessagesConfig getFlyTimeMessagesConfig() {
        return flyTimeMessagesConfig;
    }

    public void reloadFlyTime() {
        this.flyTimeConfig = loadConfig(FlyTimeConfig.class, "modules/flytime/flytime.yml", 1);
        this.flyTimeMessagesConfig = loadConfig(FlyTimeMessagesConfig.class, "modules/flytime/flytime_messages.yml", 1);
    }

    public ProfilesConfig getProfilesConfig() {
        return profilesConfig;
    }

    public ProfilesMessagesConfig getProfilesMessagesConfig() {
        return profilesMessagesConfig;
    }

    public ProfilesLayoutConfig getProfilesLayoutConfig() {
        return profilesLayoutConfig;
    }

    public void loadProfilesConfigs() {
        this.profilesConfig = loadConfig(ProfilesConfig.class, "modules/profiles/profiles.yml", 1);
        this.profilesMessagesConfig = loadConfig(ProfilesMessagesConfig.class, "modules/profiles/profiles_messages.yml", 1);
        this.profilesLayoutConfig = loadConfig(ProfilesLayoutConfig.class, "modules/profiles/profiles_layout.yml", 2);
    }

    public TimeLimitConfig getTimeLimitConfig() {
        return timeLimitConfig;
    }

    public TimeLimitMessagesConfig getTimeLimitMessagesConfig() {
        return timeLimitMessagesConfig;
    }

    public TimeLimitLayoutConfig getTimeLimitLayoutConfig() {
        return timeLimitLayoutConfig;
    }

    public void loadTimeLimitConfigs() {
        this.timeLimitConfig = loadConfig(TimeLimitConfig.class, "modules/timelimit/timelimit.yml", 1);
        this.timeLimitMessagesConfig = loadConfig(TimeLimitMessagesConfig.class, "modules/timelimit/timelimit_messages.yml", 1);
        this.timeLimitLayoutConfig = loadConfig(TimeLimitLayoutConfig.class, "modules/timelimit/timelimit_layout.yml", 1);
    }

    public AntiPacketExploitConfig getAntiPacketExploitConfig() {
        return antiPacketExploitConfig;
    }

    public void reloadAntiPacketExploitConfig() {
        this.antiPacketExploitConfig = loadConfig(AntiPacketExploitConfig.class, "antipacketexploit.yml", 1);
    }
}