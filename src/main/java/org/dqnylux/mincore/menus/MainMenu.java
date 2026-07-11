package org.dqnylux.mincore.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.utils.TextUtils;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.BaseItem;
import xyz.xenondevs.invui.window.Window;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class MainMenu {

    public static void open(Player player, Mincore plugin) {
        Gui gui = Gui.of(
                "# # # # # # # # #",
                "# . . . I . . . #",
                "# # # # # # # # #"
        );

        // Ítem decorativo
        gui.setItem('#', new BaseItem() {
            @Override
            public ItemStack getItemProvider() {
                return new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            }
        });

        // Ítem funcional (Diamante)
        gui.setItem('I', new BaseItem() {
            @Override
            public ItemStack getItemProvider() {
                return new org.bukkit.inventory.ItemStack(Material.DIAMOND);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                player.sendMessage(TextUtils.format("<green>¡Has hecho clic en el diamante del Mincore!"));
                player.closeInventory();
            }
        });

        Window window = Window.single()
                .setViewer(player)
                .setTitle(TextUtils.format("<bold><gradient:#ff5e62:#ff9966>Mincore Principal</gradient></bold>"))
                .setGui(gui)
                .build();

        window.open();
    }
}