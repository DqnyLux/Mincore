package org.dqnylux.mincore.managers.cosmetics;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagePackConfig;
import org.dqnylux.mincore.config.StandardCosmeticConfig;
import org.dqnylux.mincore.config.WingsConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.config.models.MessagePackCosmetic;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Carga los 12 archivos "estándar" (un StandardCosmeticConfig reutilizado por
 * cada uno), los 2 paquetes de mensajes y wings.yml. Separado de
 * CoreConfigManager porque cada categoría necesita su propio catálogo de
 * items por defecto - no tiene sentido meter 12 llamadas de este tipo en la
 * clase que ya administra config.yml/messages.yml/etc.
 */
public class CosmeticConfigManager {

    public static final String[] STANDARD_CATEGORIES = {
            "namecolors", "chatcolors", "prefixes", "icons", "glows",
            "join-messages", "join-effects", "projectile-effects",
            "kill-effects", "death-effects", "elytra-effects", "trails"
    };

    private final Mincore plugin;
    private final Map<String, StandardCosmeticConfig> categories = new LinkedHashMap<>();
    private MessagePackConfig killMessages;
    private MessagePackConfig deathMessages;
    private WingsConfig wings;

    public CosmeticConfigManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        for (String category : STANDARD_CATEGORIES) {
            categories.put(category, loadStandard(category));
        }
        killMessages = loadMessagePack("cosmetics/kill_messages.yml", this::defaultKillMessage);
        deathMessages = loadMessagePack("cosmetics/death_messages.yml", this::defaultDeathMessage);
        wings = loadFile(WingsConfig.class, "cosmetics/wings.yml");
    }

    private MessagePackConfig loadMessagePack(String fileName, Supplier<MessagePackCosmetic> defaultSupplier) {
        MessagePackConfig config = loadFile(MessagePackConfig.class, fileName);
        if (config.items.isEmpty()) {
            config.items.put("default", defaultSupplier.get());
            config.save();
        }
        return config;
    }

    private MessagePackCosmetic defaultKillMessage() {
        MessagePackCosmetic pack = new MessagePackCosmetic();
        pack.displayName = "Mensaje de asesinato por defecto";

        Map<String, List<String>> defaultBucket = new LinkedHashMap<>();
        defaultBucket.put("default", List.of("<red>%killer% <white>asesinó a <red>%player%<white>."));
        defaultBucket.put("weapon", List.of("<red>%killer% <white>asesinó a <red>%player% <white>con <yellow>%weapon%<white>."));

        Map<String, Map<String, List<String>>> messages = new LinkedHashMap<>();
        messages.put("DEFAULT", defaultBucket);
        pack.messages = messages;
        return pack;
    }

    private MessagePackCosmetic defaultDeathMessage() {
        MessagePackCosmetic pack = new MessagePackCosmetic();
        pack.displayName = "Mensaje de muerte por defecto";

        Map<String, List<String>> defaultBucket = new LinkedHashMap<>();
        defaultBucket.put("default", List.of("<gray>%player% <white>ha muerto."));

        Map<String, Map<String, List<String>>> messages = new LinkedHashMap<>();
        messages.put("DEFAULT", defaultBucket);
        pack.messages = messages;
        return pack;
    }

    private StandardCosmeticConfig loadStandard(String category) {
        StandardCosmeticConfig config = loadFile(StandardCosmeticConfig.class, "cosmetics/" + category.replace('-', '_') + ".yml");
        if (config.items.isEmpty()) {
            config.items.put("default", defaultItemFor(category));
            config.save();
        }
        return config;
    }

    private CosmeticItem defaultItemFor(String category) {
        CosmeticItem item = new CosmeticItem();
        item.displayName = "Cosmético por defecto";
        item.price = 0.0;

        switch (category) {
            case "namecolors", "chatcolors" -> item.value = "<white>";
            case "prefixes" -> item.value = "<#888888>[Jugador]";
            case "icons" -> item.value = "★";
            case "glows" -> item.value = "WHITE";
            case "join-messages" -> item.value = "<green>%player% se unió al servidor.";
            case "join-effects", "kill-effects" -> {
                item.effectType = "heart_burst";
                item.particle = "HEART";
                item.radius = 1.0;
            }
            case "death-effects" -> {
                item.effectType = "flame_ring";
                item.particle = "FLAME";
                item.radius = 1.0;
            }
            case "elytra-effects" -> {
                item.effectType = "trail_sparkle";
                item.particle = "END_ROD";
            }
            case "projectile-effects" -> {
                item.effectType = "projectile_trail";
                item.particle = "CRIT";
            }
            case "trails" -> {
                item.value = "STEPS";
                item.particle = "CLOUD";
            }
            default -> item.value = "";
        }
        return item;
    }

    private <T extends org.dqnylux.mincore.config.MincoreConfig> T loadFile(Class<T> clazz, String fileName) {
        return ConfigManager.create(clazz, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(new File(plugin.getDataFolder(), fileName));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });
    }

    public StandardCosmeticConfig getCategory(String category) {
        return categories.get(category);
    }

    public CosmeticItem getItem(String category, String itemId) {
        StandardCosmeticConfig config = categories.get(category);
        return config == null ? null : config.items.get(itemId);
    }

    public Map<String, StandardCosmeticConfig> getCategories() {
        return categories;
    }

    public MessagePackConfig getKillMessages() {
        return killMessages;
    }

    public MessagePackConfig getDeathMessages() {
        return deathMessages;
    }

    public WingsConfig getWings() {
        return wings;
    }
}
