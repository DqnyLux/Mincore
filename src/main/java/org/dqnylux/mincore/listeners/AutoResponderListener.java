package org.dqnylux.mincore.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.BotsConfig;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.regex.Pattern;

public class AutoResponderListener implements Listener {

    private final Mincore plugin;

    public AutoResponderListener(Mincore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        BotsConfig bots = plugin.getConfigManager().getBotsConfig();

        for (BotsConfig.AutoResponder responder : bots.autoResponders.values()) {
            boolean matched = responder.triggers.stream().anyMatch(trigger -> matches(trigger, message, responder.regex));
            if (!matched) continue;

            if (responder.cancelOriginal) {
                event.setCancelled(true);
            }

            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
                for (String line : responder.response) {
                    Bukkit.broadcast(TextUtils.format(line));
                }
            }, 1L);
            return;
        }
    }

    private boolean matches(String trigger, String message, boolean regex) {
        if (regex) {
            return Pattern.compile(trigger, Pattern.CASE_INSENSITIVE).matcher(message).find();
        }
        return message.equalsIgnoreCase(trigger);
    }
}
