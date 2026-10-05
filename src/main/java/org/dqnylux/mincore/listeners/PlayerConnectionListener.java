package org.dqnylux.mincore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
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

        // Primera conexión de verdad (nunca jugó antes) - solo tiene efecto
        // si ya se corrió /setspawn alguna vez (essentials.yml -> spawnWorld
        // vacío = todavía no hay nada guardado, no tocar nada en ese caso).
        if (plugin.getConfigManager().getModulesConfig().essentials && !player.hasPlayedBefore()) {
            Location configuredSpawn = resolveConfiguredSpawn();
            if (configuredSpawn != null) player.teleportAsync(configuredSpawn);
        }

        if (plugin.getConfigManager().getMainConfig().modules.customJoinQuitMessages) {
            event.joinMessage(null);
        }

        plugin.getPlayerManager().loadPlayer(player.getUniqueId(), player.getName())
                .thenAccept(data -> player.getScheduler().run(plugin, task -> onDataLoaded(player, data), () -> {
                }));
        // Migración única: jugadores que ya tenían un tag/ícono equipado
        // ANTES del fix de "el tag cosmético pisaba el rango real en el
        // tablist" (ver CosmeticsGui#applySideEffects) se quedaron con un
        // nodo de LuckPerms viejo que, ahora que equipar/desequipar ya no
        // toca LuckPerms para nada, nadie más va a limpiar - se queda pegado
        // para siempre. No-op instantáneo una vez removido la primera vez.
        if (org.dqnylux.mincore.hooks.LuckPermsHook.isEnabled()) {
            org.dqnylux.mincore.hooks.LuckPermsHook.purgeLegacyCosmeticNodes(player.getUniqueId());
        }
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
        plugin.getDisguiseManager().applyStoredOnJoin(player);
        plugin.getSkinPreviewManager().applyStoredOnJoin(player);
    }

    /**
     * El cliente descarta su tracking de entidades en CUALQUIER cambio de
     * dimensión (el servidor le manda un paquete de "respawn" al cruzar de
     * mundo) - nuestro TextDisplay de nametag es paquete puro, nunca fue una
     * entidad real para Bukkit, así que nada lo reenvía automáticamente como
     * sí pasa con las entidades reales al entrar en rango de nuevo. Sin este
     * refresh() explícito, el nametag quedaba invisible para todos hasta el
     * próximo ciclo de reenvío (≤ nametag.yml -> taskInterval, por defecto 1s)
     * en el mejor caso, o directamente ausente si nada más lo disparaba -
     * causa raíz real de "los nametags desaparecen al cambiar de mundo".
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // clearViewersOutsideOwnerWorld PRIMERO: limpia con certeza a los
        // viewers que quedaron en el mundo viejo (ver su javadoc - no
        // depende de que el tracker del server ya se haya actualizado, a
        // diferencia de lo que hace staggeredForceResync internamente).
        plugin.getNametagDisplayManager().clearViewersOutsideOwnerWorld(event.getPlayer());
        plugin.getNametagDisplayManager().staggeredForceResync(event.getPlayer());
        plugin.getNametagDisplayManager().staggeredResyncTrackedByViewer(event.getPlayer());
    }

    /**
     * Mismo problema que onWorldChange pero DENTRO del mismo mundo (/tp,
     * /tpa, ender pearl, warps, etc.): un teleport puede hacer que el cliente
     * de un viewer cercano pierda la relación de montura de nuestro
     * TextDisplay con el jugador real sin que Bukkit dispare ningún evento
     * que lo delate del lado del viewer - la única señal disponible es este
     * evento del lado del propio jugador que se teletransportó. Se agenda al
     * scheduler de la entidad (no se toca nada de Bukkit acá mismo) porque en
     * Folia el teleport es efectivamente async: para cuando este Runnable
     * corre, el jugador ya está en la región correcta de destino.
     */
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        // staggeredForceResync() ya salta internamente a la región del
        // jugador antes de tocar nada suyo (ver NametagDisplayManager) - no
        // hace falta agendarlo a mano acá.
        plugin.getNametagDisplayManager().staggeredForceResync(event.getPlayer());
        plugin.getNametagDisplayManager().staggeredResyncTrackedByViewer(event.getPlayer());
    }

    /**
     * Igual motivo que onWorldChange: morir y reaparecer manda al cliente un
     * paquete de respawn que le hace descartar su tracking de entidades (pase
     * o no de dimensión - un respawn en el MISMO mundo también lo dispara),
     * y nuestro TextDisplay es paquete puro, nada lo re-trackea solo. Este
     * handler faltaba por completo - a diferencia de un cambio de mundo o un
     * teletransporte, la muerte/respawn NO cambia a quién ve el servidor
     * trackeando a quién (Bukkit#getTrackedBy sigue devolviendo los mismos
     * viewers antes y después), así que el ciclo periódico de resendAll()
     * (que solo reenvía a viewers NUEVOS, ver NametagDisplayManager) nunca
     * detecta ningún cambio que corregir - el nametag quedaba roto para
     * siempre hasta el próximo teleport/cambio de mundo real. forceResync()
     * (reenvío incondicional a TODOS los viewers actuales) es lo que hace
     * falta acá, no refresh().
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getNametagDisplayManager().staggeredForceResync(event.getPlayer());
        plugin.getNametagDisplayManager().staggeredResyncTrackedByViewer(event.getPlayer());

        // Solo pisa el respawn si el jugador NO tiene cama/ancla válida - si
        // tiene una, Minecraft ya la respeta por su cuenta y no hay que tocar
        // nada. isBedSpawn()/isAnchorSpawn() ya vienen en false si la cama/el
        // ancla se destruyó u obstruyó, así que esto también cubre ese caso.
        if (plugin.getConfigManager().getModulesConfig().essentials && !event.isBedSpawn() && !event.isAnchorSpawn()) {
            Location configuredSpawn = resolveConfiguredSpawn();
            if (configuredSpawn != null) event.setRespawnLocation(configuredSpawn);
        }
    }

    private Location resolveConfiguredSpawn() {
        var essentials = plugin.getConfigManager().getEssentialsConfig();
        if (essentials.spawnWorld.isBlank()) return null;

        World world = Bukkit.getWorld(essentials.spawnWorld);
        if (world == null) return null;

        return new Location(world, essentials.spawnX, essentials.spawnY, essentials.spawnZ, essentials.spawnYaw, essentials.spawnPitch);
    }

    /**
     * Referencia real (UnlimitedNametags, PlayerListener#onPlayerDeath):
     * incluso si OTRO plugin cancela la muerte (anti-death-loss, safezone,
     * combat-tag, lo que sea), la cancelación puede dejar el estado del
     * cliente perturbado (parpadeo de pantalla de respawn, reseteo de
     * salud/posición) sin que PlayerRespawnEvent llegue a dispararse nunca -
     * el jugador queda sin ningún disparador de recuperación. Una muerte NO
     * cancelada no necesita esto: ya la cubre onRespawn de arriba.
     */
    @EventHandler(ignoreCancelled = false)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (event.isCancelled()) {
            throttledStaggeredResync(event.getEntity());
        }
    }

    /**
     * Cooldown mínimo (ms) entre dos staggeredForceResync() disparados por
     * onDeath/onZeroDamage PARA EL MISMO jugador - ver el javadoc de
     * onZeroDamage para el bug real que esto evita (un disparador que puede
     * repetirse muchas veces por segundo, a diferencia de join/teleport/
     * respawn/worldchange que son eventos discretos y raros).
     */
    private static final long RECOVERY_COOLDOWN_MILLIS = 5000L;
    private final java.util.Map<java.util.UUID, Long> lastRecoveryTrigger = new java.util.concurrent.ConcurrentHashMap<>();

    private void throttledStaggeredResync(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastRecoveryTrigger.put(player.getUniqueId(), now);
        if (last != null && now - last < RECOVERY_COOLDOWN_MILLIS) return;

        plugin.getNametagDisplayManager().staggeredForceResync(player);
    }

    /**
     * Referencia real (UnlimitedNametags, PlayerListener#onZeroDamage):
     * un golpe que termina en 0 de daño final (armadura absorbió todo,
     * resistencia, etc.) suele venir acompañado de velocity/knockback que
     * un anticheat (acá: Vulcan) puede reescribir o corregir de forma
     * similar a un teletransporte, sin disparar PlayerTeleportEvent -
     * heurística barata para cubrir ese hueco sin depender de que el
     * anticheat dispare ningún evento propio que podamos enganchar.
     *
     * BUG REAL que yo mismo introduje al agregar esto: sin cooldown, un
     * combate sostenido contra cualquier mob que pegue seguido (muy real acá
     * - JustCombat/MythicMobs/LevelledMobs/CombatPets instalados) puede
     * generar golpes de 0 de daño varias veces por segundo, cada uno
     * disparando un staggeredForceResync() completo (reenvío incondicional a
     * TODOS los viewers, x5 con los reintentos escalonados) - exactamente el
     * mismo tipo de saturación de paquetes que ya se había arreglado en
     * resendAll() esta misma sesión, ahora reintroducido por una vía nueva.
     * La referencia real (UnlimitedNametags) ya se cubre de esto con su
     * propio "run-id map" que impide que una cadena de recuperación nueva se
     * apile sobre una que ya está en curso para el mismo jugador - acá se
     * resuelve más simple, con un cooldown mínimo entre disparos.
     */
    @EventHandler(ignoreCancelled = true)
    public void onZeroDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getFinalDamage() != 0) return;
        throttledStaggeredResync(player);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffectChange(org.bukkit.event.entity.EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        boolean oldWasInvis = event.getOldEffect() != null && event.getOldEffect().getType().equals(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        boolean newIsInvis = event.getNewEffect() != null && event.getNewEffect().getType().equals(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        if (oldWasInvis || newIsInvis) {
            player.getScheduler().run(plugin, task -> {
                if (!player.isOnline()) return;
                plugin.getNametagDisplayManager().staggeredForceResync(player);
                plugin.getNametagDisplayManager().staggeredResyncTrackedByViewer(player);
            }, () -> {});
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(org.bukkit.event.player.PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) return;
            plugin.getNametagDisplayManager().staggeredForceResync(player);
            plugin.getNametagDisplayManager().staggeredResyncTrackedByViewer(player);
        }, () -> {});
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
        plugin.getNametagPassengerGuardListener().onViewerQuit(player.getUniqueId());
        plugin.getGlowManager().onQuit(player);
        plugin.getMessageDeletionManager().handleQuit(player.getUniqueId());
        if (plugin.getAntiPacketExploitListener() != null) {
            plugin.getAntiPacketExploitListener().cleanupPlayer(player.getUniqueId());
        }
        // Evita una fuga de memoria lenta - mismo motivo que
        // CosmeticsGui.clearSortPreference un poco más abajo.
        lastRecoveryTrigger.remove(player.getUniqueId());
        // El método ya existía con este propósito exacto (ver su javadoc)
        // pero nunca se llamaba desde ningún lado - cada jugador que abrió
        // alguna vez un menú de cosméticos quedaba para siempre en el mapa
        // estático de CosmeticsGui, una fuga de memoria lenta e ilimitada.
        org.dqnylux.mincore.menus.CosmeticsGui.clearSortPreference(player.getUniqueId());
    }

    private void onDataLoaded(Player player, PlayerData data) {
        if (data == null || !player.isOnline()) return;

        if (data.hasNickname() && !plugin.getDisguiseManager().isDisguised(player)) {
            net.kyori.adventure.text.Component nickComp = TextUtils.format(data.getNickname());
            player.displayName(nickComp);
            player.customName(nickComp);
            player.setCustomNameVisible(true);
        }

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
        // refresh() PRIMERO: es el único de los dos que crea la entidad si
        // todavía no existe (forceResync/staggeredForceResync no spawnean,
        // solo reenvían a una entidad YA creada - ver sus javadocs). Recién
        // después, staggeredForceResync(): el join es otro momento donde un
        // solo paquete perdido deja al jugador sin nametag propio visible
        // para nadie, sin ningún otro disparador que lo cure hasta el
        // próximo ciclo de resync completo (~10s, ver NametagDisplayManager).
        plugin.getNametagDisplayManager().refresh(player);
        plugin.getNametagDisplayManager().staggeredForceResync(player);
        plugin.getNametagDisplayManager().staggeredResyncTrackedByViewer(player);
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
        String shownName = plugin.getDisguiseManager().displayName(player);

        if (item != null && !item.lines.isEmpty()) {
            broadcastLines(item.lines, shownName);
            return;
        }

        String template = item != null && item.value != null && !item.value.isBlank()
                ? item.value : messages.joinQuit.joinMessage;
        Bukkit.broadcast(TextUtils.format(template.replace("%player%", shownName)));
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
