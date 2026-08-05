package org.dqnylux.mincore.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.DeathSystemConfig;
import org.dqnylux.mincore.config.MessagePackConfig;
import org.dqnylux.mincore.config.models.MessagePackCosmetic;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.BedrockMenuHandler;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DeathListener implements Listener {

    private final Mincore plugin;
    private final Map<UUID, Integer> pendingBedrockPrompt = new ConcurrentHashMap<>();

    public DeathListener(Mincore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        applyDeathMessage(event, victim, killer);

        if (!plugin.getConfigManager().getMainConfig().deathTracking.enabled) return;

        int expirationMinutes = plugin.getConfigManager().getMainConfig().deathTracking.expirationMinutes;
        Location deathLocation = victim.getLocation();
        int deathId = plugin.getDeathTrackingManager().register(deathLocation, expirationMinutes);

        // El template ya trae sus propios <click:run_command:'...'>/<hover:show_text:'...'>
        // de MiniMessage (chat-message en death_system.yml) - no hace falta
        // envolverlo aparte en un ClickEvent/HoverEvent de Java.
        DeathSystemConfig deathSystem = plugin.getConfigManager().getDeathSystemConfig();
        String template = deathSystem.chatMessage
                .replace("%id%", String.valueOf(deathId))
                .replace("%world%", deathLocation.getWorld().getName())
                .replace("%x%", String.valueOf(deathLocation.getBlockX()))
                .replace("%y%", String.valueOf(deathLocation.getBlockY()))
                .replace("%z%", String.valueOf(deathLocation.getBlockZ()))
                .replace("%expiration%", String.valueOf(expirationMinutes));
        victim.sendMessage(TextUtils.format(template));

        if (plugin.getConfigManager().getMainConfig().deathTracking.bedrockAutoMenu) {
            pendingBedrockPrompt.put(victim.getUniqueId(), deathId);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Integer deathId = pendingBedrockPrompt.remove(player.getUniqueId());
        if (deathId == null) return;

        // Comprobación vía BedrockUtil (sin ningún import de Floodgate/Cumulus)
        // ANTES de tocar BedrockMenuHandler siquiera - ver BedrockUtil para el
        // porqué exacto (la JVM verifica el classfile completo, con todos sus
        // métodos, la primera vez que se invoca CUALQUIERA de ellos).
        if (!org.dqnylux.mincore.utils.BedrockUtil.isFloodgateInstalled()) return;

        player.getScheduler().runDelayed(plugin, task -> BedrockMenuHandler.promptTracking(plugin, player, deathId), () -> {
        }, 20L);
    }

    private void applyDeathMessage(PlayerDeathEvent event, Player victim, Player killer) {
        MessagePackCosmetic pack = resolvePack(victim, killer);
        if (pack == null || pack.messages == null) return;

        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        EntityDamageEvent.DamageCause cause = lastDamage != null ? lastDamage.getCause() : null;

        ItemStack weapon = killer != null ? killer.getInventory().getItemInMainHand() : null;
        boolean weaponNamed = weapon != null && weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName();

        String template = resolveMessage(pack, cause, killer != null, weaponNamed);
        if (template == null) return;

        String weaponName = weaponNamed ? PlainTextComponentSerializer.plainText().serialize(weapon.getItemMeta().displayName()) : "";
        // Nombre a mostrar vía DisguiseManager: un jugador con nick oculto no
        // debe delatarse en el mensaje de muerte/kill que ve TODO el server.
        String resolved = template.replace("%player%", plugin.getDisguiseManager().displayName(victim))
                .replace("%killer%", killer != null ? plugin.getDisguiseManager().displayName(killer) : "")
                .replace("%weapon%", weaponName);

        event.deathMessage(TextUtils.format(resolved));
        // PlayerDeathEvent trae su PROPIO flag showDeathMessages (independiente
        // del gamerule del mismo nombre) - si viene en false, deathMessage()
        // se ignora en silencio y no se difunde NADA, aunque el Component esté
        // bien puesto. Un cosmético de mensaje de muerte/kill equipado es una
        // intención explícita del jugador de querer verlo - se fuerza a true
        // para que nunca dependa de ese flag/gamerule.
        event.setShowDeathMessages(true);
    }

    /**
     * Prioridad: si el asesino tiene un kill-message PERSONALIZADO (no
     * "default") equipado, ese gana siempre. Si no, y la víctima tiene un
     * death-message personalizado equipado, ese gana. Si ninguno personalizó
     * nada, se usa el "default" del asesino (o de la víctima, si murió sola).
     */
    private MessagePackCosmetic resolvePack(Player victim, Player killer) {
        if (killer != null) {
            PlayerData killerData = plugin.getPlayerManager().get(killer.getUniqueId());
            String killerId = killerData != null ? killerData.getActiveCosmetic("kill-messages") : null;
            debugPackResolution("kill-messages", killer.getName(), killerData, killerId);
            if (killerId != null && !"default".equals(killerId)) {
                MessagePackCosmetic pack = plugin.getCosmeticConfigManager().getKillMessages().items.get(killerId);
                if (pack != null) return pack;
                Bukkit.getLogger().warning("[CoreEC] " + killer.getName() + " tiene kill-messages='" + killerId
                        + "' equipado pero ese id no existe en kill_messages.yml - cayendo al 'default'.");
            }
        }

        PlayerData victimData = plugin.getPlayerManager().get(victim.getUniqueId());
        String victimId = victimData != null ? victimData.getActiveCosmetic("death-messages") : null;
        debugPackResolution("death-messages", victim.getName(), victimData, victimId);
        if (victimId != null && !"default".equals(victimId)) {
            MessagePackCosmetic pack = plugin.getCosmeticConfigManager().getDeathMessages().items.get(victimId);
            if (pack != null) return pack;
            Bukkit.getLogger().warning("[CoreEC] " + victim.getName() + " tiene death-messages='" + victimId
                    + "' equipado pero ese id no existe en death_messages.yml - cayendo al 'default'.");
        }

        return killer != null
                ? plugin.getCosmeticConfigManager().getKillMessages().items.get("default")
                : plugin.getCosmeticConfigManager().getDeathMessages().items.get("default");
    }

    /**
     * Diagnóstico temporal - dos pasadas de revisión de código no encontraron
     * ningún bug en la lógica de resolvePack()/PlayerData, así que en vez de
     * seguir adivinando esto deja rastro exacto en consola de qué id vio el
     * servidor para cada muerte real: si el jugador equipó algo y esto
     * imprime "default" o "SIN PlayerData", el problema está en el equip/
     * persistencia, no en esta clase. Seguro de dejar en producción (una
     * línea por muerte, sin overhead real).
     */
    private void debugPackResolution(String category, String playerName, PlayerData data, String activeId) {
        Bukkit.getLogger().info("[CoreEC] [debug] " + playerName + " " + category + " = "
                + (data == null ? "SIN PlayerData (no está en caché)" : (activeId == null ? "null (nada equipado)" : activeId)));
    }

    /**
     * BUG RAÍZ encontrado: varios packs (cyberpunk/anime/ecuador/lol_player en
     * death_messages.yml, y varios más en kill_messages.yml) tienen el bucket
     * de una causa (ej. ENTITY_ATTACK) escrito SOLO con variante "killer" -
     * asumiendo que siempre hay un asesino real. Al morir por un mob
     * (killer == null, variant="default") ese "default" no existe NI en ese
     * bucket NI se probaba en ningún otro lado - resolveMessage() devolvía
     * null en silencio y no se difundía nada (el vanilla tampoco, ya que
     * deathMessage() nunca se llegaba a invocar). Ahora se prueba en cadena:
     * variante exacta -> resto de variantes del MISMO bucket -> variante
     * exacta del bucket "DEFAULT" del pack -> resto de variantes de ese
     * bucket - siempre se rescata algo si el pack tiene CUALQUIER línea
     * escrita, en vez de reventar por un hueco puntual del catálogo.
     */
    private String resolveMessage(MessagePackCosmetic pack, EntityDamageEvent.DamageCause cause, boolean hasKiller, boolean weaponNamed) {
        String causeKey = cause != null ? cause.name() : "DEFAULT";
        String variant = weaponNamed ? "weapon" : hasKiller ? "killer" : "default";

        List<String> lines = pickVariant(pack.messages.get(causeKey), variant);
        if (lines == null) lines = pickVariant(pack.messages.get("DEFAULT"), variant);
        if (lines == null || lines.isEmpty()) return null;

        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }

    /** variant exacta primero; "default" antes que "killer"/"weapon" en el resto (texto genérico, seguro para cualquier escenario - no menciona a %killer% si no lo hay). */
    private List<String> pickVariant(Map<String, List<String>> bucket, String variant) {
        if (bucket == null) return null;

        List<String> exact = bucket.get(variant);
        if (exact != null && !exact.isEmpty()) return exact;

        for (String fallback : List.of("default", "killer", "weapon")) {
            List<String> lines = bucket.get(fallback);
            if (lines != null && !lines.isEmpty()) return lines;
        }
        return null;
    }
}
