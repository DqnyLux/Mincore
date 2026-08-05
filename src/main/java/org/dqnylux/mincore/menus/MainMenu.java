package org.dqnylux.mincore.menus;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MainMenuConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.models.MenuItem;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.MenuStructure;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Menú de perfil (/perfil): acceso a cosméticos + toggles persistentes del
 * jugador (chat global y mensajes privados, ambos respaldados en BD vía
 * PlayerData). El layout completo - forma del menú, filas, y material/nombre/
 * lore de cada botón - viene de menus/main_menu.yml (MainMenuConfig),
 * parseado como cuadrícula de símbolos (MenuStructure). Sección 17, cero
 * hardcodeo: rediseñar este menú es editar YAML, nunca esta clase.
 */
public class MainMenu {

    public static void open(Player player, Mincore plugin) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        MainMenuConfig layout = plugin.getConfigManager().getMainMenuConfig();

        Gui gui = Gui.gui()
                .title(TextUtils.format(messages.menus.mainTitle))
                .rows(layout.rows)
                // Sin esto, triumph-gui deja sacar/mover los ítems del menú
                // (no cancela clics por defecto, a diferencia de InvUI).
                .disableAllInteractions()
                .create();

        Map<Character, List<MenuStructure.Cell>> cells = MenuStructure.parse(layout.structure);

        place(gui, cellsFor(cells, '#'), (r, c) -> background(plugin, layout.fillerMaterial));
        place(gui, cellsFor(cells, symbol(layout.profileHeadSymbol)),
                (r, c) -> CosmeticsGui.infoHead(plugin, player, layout.profileHead));
        place(gui, cellsFor(cells, symbol(layout.cosmeticsButtonSymbol)),
                (r, c) -> cosmeticsButton(plugin, player, layout.cosmeticsButton));
        place(gui, cellsFor(cells, symbol(layout.flyToggleSymbol)),
                (r, c) -> flyToggleItem(gui, r, c, plugin, player, layout.flyToggle));
        place(gui, cellsFor(cells, symbol(layout.globalChatToggleSymbol)),
                (r, c) -> toggleItem(gui, r, c, plugin, player, layout.globalChatToggle,
                        PlayerData::isGlobalChat, PlayerData::setGlobalChat));
        place(gui, cellsFor(cells, symbol(layout.messagesToggleSymbol)),
                (r, c) -> toggleItem(gui, r, c, plugin, player, layout.messagesToggle,
                        PlayerData::isMessagesEnabled, PlayerData::setMessagesEnabled));
        place(gui, cellsFor(cells, symbol(layout.mentionsToggleSymbol)),
                (r, c) -> toggleItem(gui, r, c, plugin, player, layout.mentionsToggle,
                        PlayerData::isMentionsEnabled, PlayerData::setMentionsEnabled));

        gui.open(player);
    }

    private static char symbol(String value) {
        return value == null || value.isEmpty() ? '\0' : value.charAt(0);
    }

    private static List<MenuStructure.Cell> cellsFor(Map<Character, List<MenuStructure.Cell>> cells, char symbol) {
        return cells.getOrDefault(symbol, List.of());
    }

    private static void place(Gui gui, List<MenuStructure.Cell> cells, BiFunction<Integer, Integer, GuiItem> factory) {
        for (MenuStructure.Cell cell : cells) {
            gui.setItem(cell.row(), cell.col(), factory.apply(cell.row(), cell.col()));
        }
    }

    private static GuiItem background(Mincore plugin, String material) {
        return PaperItemBuilder.from(CosmeticsGui.parseMaterial(material))
                .name(Component.empty())
                .flags(CosmeticsGui.itemFlags(plugin))
                .asGuiItem();
    }

    private static List<Component> formattedLore(Player viewer, List<String> lore, double coins) {
        List<Component> result = new ArrayList<>();
        for (String line : lore) {
            result.add(TextUtils.format(line
                    .replace("%player%", viewer.getName())
                    .replace("%coins%", String.valueOf(coins))));
        }
        return result;
    }

    private static GuiItem cosmeticsButton(Mincore plugin, Player viewer, MenuItem item) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
        double coins = data == null ? 0 : data.getCoins();

        return PaperItemBuilder.from(CosmeticsGui.parseMaterial(item.material))
                .name(TextUtils.format(item.displayName))
                .lore(formattedLore(viewer, item.lore, coins))
                .flags(CosmeticsGui.itemFlags(plugin))
                .asGuiItem(click -> {
                    if (!plugin.getConfigManager().getMainConfig().modules.cosmetics.enabled) {
                        viewer.sendMessage(TextUtils.format(messages.prefix + messages.commands.cosmeticsDisabled));
                        return;
                    }
                    CategoryMenu.open(viewer, plugin);
                });
    }

    private static GuiItem toggleItem(Gui gui, int row, int col, Mincore plugin, Player player, MenuItem item,
                                       Predicate<PlayerData> getter,
                                       BiConsumer<PlayerData, Boolean> setter) {
        return PaperItemBuilder.from(toggleStack(plugin, player, item, getter))
                .asGuiItem(click -> {
                    PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
                    if (data == null) return;

                    setter.accept(data, !getter.test(data));
                    plugin.getPlayerManager().savePlayerAsync(data);
                    gui.updateItem(row, col, toggleStack(plugin, player, item, getter));
                });
    }

    private static ItemStack toggleStack(Mincore plugin, Player viewer, MenuItem item, Predicate<PlayerData> getter) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
        boolean enabled = data != null && getter.test(data);
        double coins = data == null ? 0 : data.getCoins();

        List<Component> lore = formattedLore(viewer, item.lore, coins);
        lore.add(TextUtils.format(enabled ? messages.menus.toggleOn : messages.menus.toggleOff));

        return PaperItemBuilder.from(CosmeticsGui.parseMaterial(item.material))
                .name(TextUtils.format(item.displayName))
                .lore(lore)
                .flags(CosmeticsGui.itemFlags(plugin))
                .build();
    }

    private static GuiItem flyToggleItem(Gui gui, int row, int col, Mincore plugin, Player player, MenuItem item) {
        return PaperItemBuilder.from(flyToggleStack(plugin, player, item))
                .asGuiItem(click -> {
                    MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
                    if (!player.hasPermission(plugin.getConfigManager().getMainConfig().permissions.fly)) {
                        player.sendMessage(TextUtils.format(messages.prefix + messages.commands.noPermission));
                        return;
                    }
                    boolean newState = !player.getAllowFlight();
                    player.setAllowFlight(newState);
                    player.setFlying(newState && player.isFlying());
                    gui.updateItem(row, col, flyToggleStack(plugin, player, item));
                });
    }

    private static ItemStack flyToggleStack(Mincore plugin, Player viewer, MenuItem item) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        boolean hasPermission = viewer.hasPermission(plugin.getConfigManager().getMainConfig().permissions.fly);
        PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
        double coins = data == null ? 0 : data.getCoins();
        List<Component> lore = formattedLore(viewer, item.lore, coins);
        lore.add(TextUtils.format(!hasPermission ? messages.menus.toggleLocked
                : viewer.getAllowFlight() ? messages.menus.toggleOn : messages.menus.toggleOff));
        return PaperItemBuilder.from(CosmeticsGui.parseMaterial(item.material))
                .name(TextUtils.format(item.displayName))
                .lore(lore)
                .flags(CosmeticsGui.itemFlags(plugin))
                .build();
    }
}
