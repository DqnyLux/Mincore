package org.dqnylux.mincore.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Menú de perfil (/perfil): acceso a cosméticos + toggles persistentes del
 * jugador (chat global y mensajes privados, ambos respaldados en BD vía
 * PlayerData). Réplica de la sección 13.4 del prompt original con InvUI.
 */
public class MainMenu {

    public static void open(Player player, Mincore plugin) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# . C . G . M . #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
                .addIngredient('C', cosmeticsButton(plugin))
                .addIngredient('G', toggleItem(plugin, Material.PLAYER_HEAD,
                        m -> m.menus.toggleGlobalChatName, PlayerData::isGlobalChat,
                        (data, value) -> data.setGlobalChat(value)))
                .addIngredient('M', toggleItem(plugin, Material.WRITABLE_BOOK,
                        m -> m.menus.toggleMessagesName, PlayerData::isMessagesEnabled,
                        (data, value) -> data.setMessagesEnabled(value)))
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(new AdventureComponentWrapper(TextUtils.format(messages.menus.mainTitle)))
                .setGui(gui)
                .build();

        window.open();
    }

    private static AbstractItem cosmeticsButton(Mincore plugin) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
                return new ItemBuilder(Material.CHEST)
                        .setDisplayName(new AdventureComponentWrapper(TextUtils.format(messages.menus.cosmeticsButtonName)))
                        .addLoreLines(TextUtils.formatLegacy(messages.menus.cosmeticsButtonLore));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clicker, @NotNull InventoryClickEvent event) {
                MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
                if (!plugin.getConfigManager().getMainConfig().modules.cosmetics.enabled) {
                    clicker.sendMessage(TextUtils.format(messages.prefix + messages.commands.cosmeticsDisabled));
                    return;
                }
                CategoryMenu.open(clicker, plugin);
            }
        };
    }

    private static AbstractItem toggleItem(Mincore plugin, Material material,
                                           Function<MessagesConfig, String> nameSupplier,
                                           Predicate<PlayerData> getter,
                                           BiConsumer<PlayerData, Boolean> setter) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return buildToggle(null);
            }

            @Override
            public ItemProvider getItemProvider(Player viewer) {
                return buildToggle(viewer);
            }

            private ItemProvider buildToggle(Player viewer) {
                MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
                ItemBuilder builder = new ItemBuilder(material)
                        .setDisplayName(new AdventureComponentWrapper(TextUtils.format(nameSupplier.apply(messages))));

                if (viewer != null) {
                    PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
                    boolean enabled = data != null && getter.test(data);
                    builder.addLoreLines(TextUtils.formatLegacy(enabled ? messages.menus.toggleOn : messages.menus.toggleOff));
                }
                return builder;
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clicker, @NotNull InventoryClickEvent event) {
                PlayerData data = plugin.getPlayerManager().get(clicker.getUniqueId());
                if (data == null) return;

                setter.accept(data, !getter.test(data));
                plugin.getPlayerManager().savePlayerAsync(data);
                notifyWindows();
            }
        };
    }
}
