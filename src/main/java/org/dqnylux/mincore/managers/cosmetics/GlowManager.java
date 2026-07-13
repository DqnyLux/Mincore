package org.dqnylux.mincore.managers.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combina Entity#setGlowing(true) con un equipo de scoreboard por color
 * (único mecanismo vanilla para colorear el contorno del glow).
 *
 * Si el plugin TAB está instalado, Mincore NO crea ni gestiona su propio
 * equipo: TAB ya asigna a cada jugador a su propio equipo dinámico para
 * pintar el nametag, y dos plugins compitiendo por el mismo jugador en
 * distintos equipos de scoreboard producía parpadeo/pérdida de color (el
 * último que escribe gana). En ese caso solo se activa/desactiva el glow
 * vanilla y se deja el color del contorno en manos de TAB. No se pudo fijar
 * una dependencia real de TAB-API (JitPack no sirve builds recientes de TAB
 * de forma confiable, sin mirror alternativo) - por eso la detección es solo
 * por presencia del plugin, sin llamar a su API.
 */
public class GlowManager {

    private final Map<String, Team> teamsByColor = new ConcurrentHashMap<>();

    private boolean tabPresent() {
        return Bukkit.getPluginManager().getPlugin("TAB") != null;
    }

    public void applyGlow(Player player, String colorName) {
        player.setGlowing(true);
        if (tabPresent()) return;

        Team team = getOrCreateTeam(parseColor(colorName));
        removeFromAllTeams(player);
        team.addEntry(player.getName());
    }

    public void removeGlow(Player player) {
        player.setGlowing(false);
        if (tabPresent()) return;

        removeFromAllTeams(player);
    }

    private void removeFromAllTeams(Player player) {
        for (Team team : teamsByColor.values()) {
            team.removeEntry(player.getName());
        }
    }

    private Team getOrCreateTeam(ChatColor color) {
        return teamsByColor.computeIfAbsent(color.name(), key -> {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team existing = scoreboard.getTeam("mincore_glow_" + key);
            if (existing != null) return existing;

            Team team = scoreboard.registerNewTeam("mincore_glow_" + key);
            team.setColor(color);
            return team;
        });
    }

    private ChatColor parseColor(String name) {
        try {
            return ChatColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatColor.WHITE;
        }
    }
}
