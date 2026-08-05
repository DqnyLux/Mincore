package org.dqnylux.mincore.managers;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.bukkit.Bukkit;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.AnnouncementsConfig;
import org.dqnylux.mincore.config.BotsConfig;
import org.dqnylux.mincore.config.CategoriesMenuConfig;
import org.dqnylux.mincore.config.ChatFormatConfig;
import org.dqnylux.mincore.config.CommandBlockerConfig;
import org.dqnylux.mincore.config.CosmeticsMenuConfig;
import org.dqnylux.mincore.config.DatabaseConfig;
import org.dqnylux.mincore.config.DeathSystemConfig;
import org.dqnylux.mincore.config.FiltersConfig;
import org.dqnylux.mincore.config.MainConfig;
import org.dqnylux.mincore.config.MainMenuConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.MincoreConfig;
import org.dqnylux.mincore.config.NametagConfig;
import org.dqnylux.mincore.config.ReportsConfig;
import org.dqnylux.mincore.config.SanctionsConfig;
import org.dqnylux.mincore.config.StaffConfig;
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
}