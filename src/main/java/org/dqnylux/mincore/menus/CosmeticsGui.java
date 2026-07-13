package org.dqnylux.mincore.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.StandardCosmeticConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.hooks.LuckPermsHook;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grid paginado genérico para UNA categoría - la misma clase sirve a las 12
 * categorías estándar. Corrige la nota 5 del prompt original (la lógica de
 * acceso/equipar estaba duplicada ~150 líneas entre CategoryMenu y
 * CosmeticsGui): aquí solo existe una vez, en isAccessible()/handleClick().
 * Clic sobre el cosmético ya equipado lo desequipa (y revierte su efecto
 * persistente: glow, prefix o suffix según la categoría).
 */
public class CosmeticsGui {

    public static boolean isCategoryEnabled(Mincore plugin, String category) {
        return plugin.getConfigManager().getMainConfig().modules.cosmetics.categories.getOrDefault(category, true);
    }

    public static void open(Player player, Mincore plugin, String category) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        if (!isCategoryEnabled(plugin, category)) {
            player.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.categoryDisabled));
            return;
        }

        StandardCosmeticConfig config = plugin.getCosmeticConfigManager().getCategory(category);
        if (config == null) return;

        List<Item> items = new ArrayList<>();
        for (Map.Entry<String, CosmeticItem> entry : config.items.entrySet()) {
            items.add(buildItem(plugin, category, entry.getKey(), entry.getValue()));
        }

        Gui gui = PagedGui.items(builder -> builder
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# < . . . . . > #"
                )
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new ItemBuilder(Material.ARROW).setDisplayName(new AdventureComponentWrapper(TextUtils.format(messages.menus.pagePrevious)));
                    }
                })
                .addIngredient('>', new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new ItemBuilder(Material.ARROW).setDisplayName(new AdventureComponentWrapper(TextUtils.format(messages.menus.pageNext)));
                    }
                })
                .setContent(items));

        Window window = Window.single()
                .setViewer(player)
                .setTitle(new AdventureComponentWrapper(TextUtils.format(messages.menus.categoryTitle.replace("%category%", category))))
                .setGui(gui)
                .build();

        window.open();
    }

    private static Item buildItem(Mincore plugin, String category, String id, CosmeticItem cosmetic) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return buildDisplay(plugin, null, category, id, cosmetic);
            }

            @Override
            public ItemProvider getItemProvider(Player viewer) {
                return buildDisplay(plugin, viewer, category, id, cosmetic);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clicker, @NotNull InventoryClickEvent event) {
                MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
                PlayerData data = plugin.getPlayerManager().get(clicker.getUniqueId());
                if (data == null) return;

                if (id.equals(data.getActiveCosmetic(category))) {
                    data.clearActiveCosmetic(category);
                    plugin.getPlayerManager().savePlayerAsync(data);
                    removeSideEffects(plugin, clicker, category);
                    clicker.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.unequipped));
                    notifyWindows();
                    return;
                }

                if (!isAccessible(plugin, clicker, category, id, cosmetic)) {
                    clicker.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.noAccess));
                    return;
                }

                data.setActiveCosmetic(category, id);
                plugin.getPlayerManager().savePlayerAsync(data);
                applySideEffects(plugin, clicker, category, cosmetic);
                clicker.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.equipped));
                notifyWindows();
            }
        };
    }

    private static ItemProvider buildDisplay(Mincore plugin, Player viewer, String category, String id, CosmeticItem cosmetic) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        ItemBuilder builder = new ItemBuilder(parseMaterial(cosmetic.material))
                .setDisplayName(new AdventureComponentWrapper(TextUtils.format(cosmetic.displayName)));

        List<xyz.xenondevs.inventoryaccess.component.ComponentWrapper> lore = new ArrayList<>();
        for (String line : cosmetic.lore) {
            lore.add(new AdventureComponentWrapper(TextUtils.format(line)));
        }

        if (viewer != null) {
            PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
            boolean equipped = data != null && id.equals(data.getActiveCosmetic(category));
            boolean accessible = isAccessible(plugin, viewer, category, id, cosmetic);
            String status = equipped ? messages.cosmetics.statusEquipped
                    : accessible ? messages.cosmetics.statusClickToEquip
                    : messages.cosmetics.statusLocked;
            lore.add(new AdventureComponentWrapper(TextUtils.format(status)));
        }

        builder.setLore(lore);
        return builder;
    }

    /**
     * Efectos que persisten fuera del propio render del chat/partículas y
     * deben aplicarse/revertirse al equipar/desequipar: glow (scoreboard),
     * prefijo e icono (nodos LuckPerms).
     */
    private static void applySideEffects(Mincore plugin, Player player, String category, CosmeticItem cosmetic) {
        var luckPermsConfig = plugin.getConfigManager().getMainConfig().luckPerms;
        switch (category) {
            case "glows" -> plugin.getGlowManager().applyGlow(player, cosmetic.value);
            case "prefixes" -> {
                if (LuckPermsHook.isEnabled()) {
                    LuckPermsHook.setCustomPrefix(player.getUniqueId(), cosmetic.value, luckPermsConfig.prefixPriority);
                }
            }
            case "icons" -> {
                if (LuckPermsHook.isEnabled()) {
                    LuckPermsHook.setCustomSuffix(player.getUniqueId(), cosmetic.value, luckPermsConfig.suffixPriority);
                }
            }
            default -> {
            }
        }
    }

    private static void removeSideEffects(Mincore plugin, Player player, String category) {
        var luckPermsConfig = plugin.getConfigManager().getMainConfig().luckPerms;
        switch (category) {
            case "glows" -> plugin.getGlowManager().removeGlow(player);
            case "prefixes" -> {
                if (LuckPermsHook.isEnabled()) LuckPermsHook.removeCustomPrefix(player.getUniqueId(), luckPermsConfig.prefixPriority);
            }
            case "icons" -> {
                if (LuckPermsHook.isEnabled()) LuckPermsHook.removeCustomSuffix(player.getUniqueId(), luckPermsConfig.suffixPriority);
            }
            default -> {
            }
        }
    }

    static boolean isAccessible(Mincore plugin, Player player, String category, String id, CosmeticItem cosmetic) {
        PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
        return cosmetic.price <= 0
                || player.hasPermission("mincore.cosmetic." + category + "." + id)
                || (data != null && data.hasCosmeticUnlocked(category, id));
    }

    private static Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (Exception e) {
            return Material.PAPER;
        }
    }
}
