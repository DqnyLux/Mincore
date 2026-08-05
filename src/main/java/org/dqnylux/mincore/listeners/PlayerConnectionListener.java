package org.dqnylux.mincore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MainConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;

public class PlayerConnectionListener implements Listener {

    private final Mincore plugin;

    public PlayerConnectionListener(Mincore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().getMainConfig().modules.customJoinQuitMessages) {
            event.joinMessage(null);
        }

        plugin.getPlayerManager().loadPlayer(player.getUniqueId(), player.getName())
                .thenAccept(data -> player.getScheduler().run(plugin, task -> onDataLoaded(player, data), () -> {
                }));
        plugin.getPozoDataManager().loadPlayer(player.getUniqueId());
        // Los hologramas por-jugador de las máquinas del Pozo se ocultaron
        // solo a quienes estaban online al spawnearse - el recién llegado
        // vería los conteos de otros sin esto.
        plugin.getPozoMachineManager().hideAllFrom(player);

        plugin.getActiveCosmeticsTask().start(player);
        plugin.getElytraCosmeticsTask().start(player);
        // Los equipos de glow son paquetes falsos por-viewer (GlowManager) -
        // un jugador que recién se conecta no los recibió cuando se enviaron,
        // así que hay que reenviárselos para que también vea el color correcto.
        plugin.getGlowManager().resendActiveGlowsTo(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().getMainConfig().modules.customJoinQuitMessages) {
            event.quitMessage(null);
            broadcastQuitMessage(player, plugin.getPlayerManager().get(player.getUniqueId()));
        }

        plugin.getPlayerManager().saveAndRemoveAsync(player.getUniqueId());
        plugin.getPozoDataManager().saveAndRemoveAsync(player.getUniqueId());
        plugin.getPozoMachineManager().removeViewer(player.getUniqueId());
        plugin.getActiveCosmeticsTask().stop(player);
        plugin.getElytraCosmeticsTask().stop(player);
        plugin.getNametagDisplayManager().remove(player);
        plugin.getGlowManager().onQuit(player);
        // El método ya existía con este propósito exacto (ver su javadoc)
        // pero nunca se llamaba desde ningún lado - cada jugador que abrió
        // alguna vez un menú de cosméticos quedaba para siempre en el mapa
        // estático de CosmeticsGui, una fuga de memoria lenta e ilimitada.
        org.dqnylux.mincore.menus.CosmeticsGui.clearSortPreference(player.getUniqueId());
    }

    private void onDataLoaded(Player player, PlayerData data) {
        if (data == null || !player.isOnline()) return;

        applyConnectionCosmetics(player, data);

        MainConfig config = plugin.getConfigManager().getMainConfig();
        if (config.modules.customJoinQuitMessages) {
            broadcastJoinMessage(player, data);
        }
        if (config.modules.motd) {
            sendMotd(player);
        }
    }

    private void applyConnectionCosmetics(Player player, PlayerData data) {
        plugin.getTabListManager().apply(player);
        // Nametag propio: por si venía disfrazado/con namecolor de otro
        // server de la red, y por si el jugador que se acaba de conectar
        // debe VER los nametags de otros que ya estaban activos.
        plugin.getNametagDisplayManager().refresh(player);
        plugin.getNametagDisplayManager().syncNewViewer(player);

        String glowId = data.getActiveCosmetic("glows");
        if (glowId != null) {
            CosmeticItem item = plugin.getCosmeticConfigManager().getItem("glows", glowId);
            if (item != null) plugin.getGlowManager().applyGlow(player, item.value);
        }

        String joinEffectId = data.getActiveCosmetic("join-effects");
        if (joinEffectId != null) {
            CosmeticItem item = plugin.getCosmeticConfigManager().getItem("join-effects", joinEffectId);
            if (item != null) plugin.getEffectRegistry().playEffect(plugin, player, player.getLocation(), item);
        }
    }

    /**
     * El cosmético de join-messages equipado reemplaza el mensaje por defecto
     * de messages.yml. "lines" (banner multi-línea, ej. con &lt;center&gt;)
     * tiene prioridad sobre "value" (una sola línea) cuando el cosmético
     * define ambos.
     */
    private void broadcastJoinMessage(Player player, PlayerData data) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        CosmeticItem item = activeJoinMessageItem(data);

        if (item != null && !item.lines.isEmpty()) {
            broadcastLines(item.lines, player.getName());
            return;
        }

        String template = item != null && item.value != null && !item.value.isBlank()
                ? item.value : messages.joinQuit.joinMessage;
        Bukkit.broadcast(TextUtils.format(template.replace("%player%", player.getName())));
    }

    private void broadcastQuitMessage(Player player, PlayerData data) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        CosmeticItem item = activeJoinMessageItem(data);
        // getName() real filtraría el nombre real al público si el jugador se
        // desconecta disfrazado - displayName() cae a getName() solo si no
        // hay disfraz activo, sin cambiar el comportamiento normal.
        String shownName = plugin.getDisguiseManager().displayName(player);

        if (item != null && !item.quitLines.isEmpty()) {
            broadcastLines(item.quitLines, shownName);
            return;
        }

        String template = item != null && item.quitValue != null && !item.quitValue.isBlank()
                ? item.quitValue : messages.joinQuit.quitMessage;
        Bukkit.broadcast(TextUtils.format(template.replace("%player%", shownName)));
    }

    private CosmeticItem activeJoinMessageItem(PlayerData data) {
        if (data == null) return null;
        String cosmeticId = data.getActiveCosmetic("join-messages");
        return cosmeticId == null ? null : plugin.getCosmeticConfigManager().getItem("join-messages", cosmeticId);
    }

    private void broadcastLines(java.util.List<String> lines, String playerName) {
        for (String line : lines) {
            Bukkit.broadcast(TextUtils.format(line.replace("%player%", playerName)));
        }
    }

    private void sendMotd(Player player) {
        plugin.getMotdManager().sendMotd(player);
    }
}
