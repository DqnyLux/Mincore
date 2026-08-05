package org.dqnylux.mincore.menus;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.CategoriesMenuConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.models.CategoryButton;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.config.models.MenuItem;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.MenuStructure;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lista las categorías con progreso desbloqueados/total y el cosmético
 * equipado. El layout completo (forma, filas, slot/material/nombre/lore por
 * categoría, botón de volver) viene de menus/categories_menu.yml
 * (CategoriesMenuConfig), parseado como cuadrícula de símbolos
 * (MenuStructure). Añadir, quitar o mover una categoría es un cambio de
 * YAML, no de código (sección 17).
 */
public class CategoryMenu {

    public static void open(Player player, Mincore plugin) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        CategoriesMenuConfig layout = plugin.getConfigManager().getCategoriesMenuConfig();

        Gui gui = Gui.gui()
                .title(TextUtils.format(messages.menus.categoriesTitle))
                .rows(layout.rows)
                .disableAllInteractions()
                .create();

        Map<Character, List<MenuStructure.Cell>> cells = MenuStructure.parse(layout.structure);
        char backSymbol = symbol(layout.backSymbol);
        char infoHeadSymbol = symbol(layout.infoHeadSymbol);

        for (Map.Entry<Character, List<MenuStructure.Cell>> entry : cells.entrySet()) {
            char sym = entry.getKey();
            GuiItem item;

            if (sym == '#') {
                item = background(plugin, layout.fillerMaterial);
            } else if (sym == '.') {
                continue;
            } else if (sym == backSymbol) {
                item = backButton(plugin, player, layout.backButton);
            } else if (sym == infoHeadSymbol) {
                item = CosmeticsGui.infoHead(plugin, player, layout.infoHead);
            } else {
                CategoryButton button = layout.categories.get(String.valueOf(sym));
                if (button == null) continue;
                item = categoryItem(plugin, player, button);
            }

            for (MenuStructure.Cell cell : entry.getValue()) {
                gui.setItem(cell.row(), cell.col(), item);
            }
        }

        gui.open(player);
    }

    private static char symbol(String value) {
        return value == null || value.isEmpty() ? '\0' : value.charAt(0);
    }

    private static GuiItem background(Mincore plugin, String material) {
        return PaperItemBuilder.from(CosmeticsGui.parseMaterial(material))
                .name(Component.empty())
                .flags(CosmeticsGui.itemFlags(plugin))
                .asGuiItem();
    }

    private static GuiItem backButton(Mincore plugin, Player viewer, MenuItem item) {
        List<Component> lore = new ArrayList<>();
        for (String line : item.lore) lore.add(TextUtils.format(line));

        return PaperItemBuilder.from(CosmeticsGui.parseMaterial(item.material))
                .name(TextUtils.format(item.displayName))
                .lore(lore)
                .flags(CosmeticsGui.itemFlags(plugin))
                .asGuiItem(click -> MainMenu.open(viewer, plugin));
    }

    private static GuiItem categoryItem(Mincore plugin, Player viewer, CategoryButton button) {
        if (!CosmeticsGui.isCategoryEnabled(plugin, button.category)) {
            return background(plugin, button.material);
        }

        return PaperItemBuilder.from(buildIcon(plugin, viewer, button))
                .flags(CosmeticsGui.itemFlags(plugin))
                .asGuiItem(click -> CosmeticsGui.open(viewer, plugin, button.category));
    }

    private static ItemStack buildIcon(Mincore plugin, Player viewer, CategoryButton button) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        String category = button.category;
        Map<String, CosmeticItem> items = CosmeticsGui.itemsForCategory(plugin, category);
        int total = items == null ? 0 : items.size();

        PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
        long unlocked = items == null || data == null ? 0 : items.keySet().stream()
                .filter(id -> CosmeticsGui.isAccessible(plugin, viewer, category, id, items.get(id)))
                .count();
        String equippedId = data == null ? null : data.getActiveCosmetic(category);
        CosmeticItem equippedItem = equippedId == null || items == null ? null : items.get(equippedId);
        String equipped = equippedItem != null ? equippedItem.displayName : equippedId;

        List<Component> lore = new ArrayList<>();
        for (String line : button.lore) lore.add(TextUtils.format(line));
        lore.add(TextUtils.format(messages.menus.categoryUnlockedLore
                .replace("%unlocked%", String.valueOf(unlocked))
                .replace("%total%", String.valueOf(total))));
        lore.add(TextUtils.format(equipped != null
                ? messages.menus.categoryEquippedLore.replace("%equipped%", equipped)
                : messages.menus.categoryNothingEquipped));

        return PaperItemBuilder.from(CosmeticsGui.parseMaterial(button.material))
                .name(TextUtils.format(button.displayName))
                .lore(lore)
                .build();
    }
}
