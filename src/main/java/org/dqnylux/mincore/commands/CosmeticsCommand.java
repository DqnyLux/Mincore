package org.dqnylux.mincore.commands;

import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.menus.CategoryMenu;
import org.dqnylux.mincore.menus.MainMenu;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.util.function.BiConsumer;

public class CosmeticsCommand {

    private final Mincore plugin;

    public CosmeticsCommand(Mincore plugin) {
        this.plugin = plugin;
    }

    @Command({"cosmetics", "cosmeticos"})
    public void cosmetics(BukkitCommandActor actor) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        if (!plugin.getConfigManager().getMainConfig().modules.cosmetics.enabled) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.cosmeticsDisabled));
            return;
        }
        openIfPlayer(actor, CategoryMenu::open);
    }

    @Command({"perfil", "profile"})
    public void profile(BukkitCommandActor actor) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        if (!plugin.getConfigManager().getMainConfig().modules.profileMenu) {
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.profileDisabled));
            return;
        }
        openIfPlayer(actor, MainMenu::open);
    }

    private void openIfPlayer(BukkitCommandActor actor, BiConsumer<Player, Mincore> opener) {
        if (!actor.isPlayer()) {
            MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }
        opener.accept(actor.requirePlayer(), plugin);
    }
}
