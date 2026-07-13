package org.dqnylux.mincore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

public class TabCompleteListener implements Listener {

    @EventHandler
    public void onChatTabComplete(PlayerChatTabCompleteEvent event) {
        String lastToken = event.getLastToken();
        if (!lastToken.startsWith("@")) return;

        String partial = lastToken.substring(1).toLowerCase();
        var completions = event.getTabCompletions();
        completions.clear();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase().startsWith(partial)) {
                completions.add("@" + online.getName());
            }
        }
    }
}
