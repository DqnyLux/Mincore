package org.dqnylux.mincore.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
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
                        "# # # # H # # # #",
                        "# . C . G . M . #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
                .addIngredient('H', profileHead(plugin))
                .addIngredient('C', cosmeticsButton(plugin))
                .addIngredient('G', toggleItem(plugin, Material.WRITTEN_BOOK,
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

    /** Cabeza decorativa con la skin real del jugador (chat/lore no puede renderizar texturas). */
    private static AbstractItem profileHead(Mincore plugin) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Material.PLAYER_HEAD);
            }

            @Override
            public ItemProvider getItemProvider(Player viewer) {
                MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(viewer);
                    skull.setItemMeta(meta);
                }

                PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
                double coins = data == null ? 0 : data.getCoins();

                return new ItemBuilder(skull)
                        .setDisplayName(new AdventureComponentWrapper(TextUtils.format(
                                messages.menus.profileHeadName.replace("%player%", viewer.getName()))))
                        .addLoreLines(TextUtils.formatLegacy(
                                messages.menus.profileHeadCoinsLore.replace("%coins%", String.valueOf(coins))));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clicker, @NotNull InventoryClickEvent event) {
            }
        };
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
