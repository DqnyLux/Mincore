package org.dqnylux.mincore.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.StandardCosmeticConfig;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;

import java.util.Map;

/**
 * Lista las categorías estándar con progreso desbloqueados/total y el
 * cosmético equipado. Las categorías desactivadas en config.yml
 * (modules.cosmetics.categories) se sustituyen por un panel de relleno.
 */
public class CategoryMenu {

    private static final Map<String, Material> ICONS = Map.ofEntries(
            Map.entry("namecolors", Material.NAME_TAG),
            Map.entry("chatcolors", Material.PAPER),
            Map.entry("prefixes", Material.BOOK),
            Map.entry("icons", Material.NETHER_STAR),
            Map.entry("glows", Material.GLOWSTONE_DUST),
            Map.entry("join-messages", Material.OAK_SIGN),
            Map.entry("join-effects", Material.FIREWORK_ROCKET),
            Map.entry("projectile-effects", Material.ARROW),
            Map.entry("kill-effects", Material.DIAMOND_SWORD),
            Map.entry("death-effects", Material.SKELETON_SKULL),
            Map.entry("elytra-effects", Material.ELYTRA),
            Map.entry("trails", Material.BLAZE_POWDER)
    );

    public static void open(Player player, Mincore plugin) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# a b c d e f g #",
                        "# h i j k l . . #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
                .addIngredient('a', categoryItem(plugin, "namecolors"))
                .addIngredient('b', categoryItem(plugin, "chatcolors"))
                .addIngredient('c', categoryItem(plugin, "prefixes"))
                .addIngredient('d', categoryItem(plugin, "icons"))
                .addIngredient('e', categoryItem(plugin, "glows"))
                .addIngredient('f', categoryItem(plugin, "trails"))
                .addIngredient('g', categoryItem(plugin, "elytra-effects"))
                .addIngredient('h', categoryItem(plugin, "join-messages"))
                .addIngredient('i', categoryItem(plugin, "join-effects"))
                .addIngredient('j', categoryItem(plugin, "kill-effects"))
                .addIngredient('k', categoryItem(plugin, "death-effects"))
                .addIngredient('l', categoryItem(plugin, "projectile-effects"))
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(new AdventureComponentWrapper(TextUtils.format(messages.menus.categoriesTitle)))
                .setGui(gui)
                .build();

        window.open();
    }

    private static Item categoryItem(Mincore plugin, String category) {
        if (!CosmeticsGui.isCategoryEnabled(plugin, category)) {
            return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE));
        }

        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return buildIcon(plugin, null, category);
            }

            @Override
            public ItemProvider getItemProvider(Player viewer) {
                return buildIcon(plugin, viewer, category);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clicker, @NotNull InventoryClickEvent event) {
                CosmeticsGui.open(clicker, plugin, category);
            }
        };
    }

    private static ItemProvider buildIcon(Mincore plugin, Player viewer, String category) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        Material material = ICONS.getOrDefault(category, Material.CHEST);
        StandardCosmeticConfig config = plugin.getCosmeticConfigManager().getCategory(category);
        int total = config == null ? 0 : config.items.size();

        ItemBuilder builder = new ItemBuilder(material)
                .setDisplayName(new AdventureComponentWrapper(TextUtils.format("<white>" + category)));

        if (viewer != null) {
            PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
            long unlocked = config == null || data == null ? 0 : config.items.keySet().stream()
                    .filter(id -> CosmeticsGui.isAccessible(plugin, viewer, category, id, config.items.get(id)))
                    .count();
            String equipped = data == null ? null : data.getActiveCosmetic(category);

            builder.addLoreLines(
                    TextUtils.formatLegacy(messages.menus.categoryUnlockedLore
                            .replace("%unlocked%", String.valueOf(unlocked))
                            .replace("%total%", String.valueOf(total))),
                    TextUtils.formatLegacy(equipped != null
                            ? messages.menus.categoryEquippedLore.replace("%equipped%", equipped)
                            : messages.menus.categoryNothingEquipped)
            );
        }

        return builder;
    }
}
