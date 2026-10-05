package org.dqnylux.mincore.tasks;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.WingsConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.config.models.WingCosmetic;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.WorldValidator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trails y wings, cada 3 ticks (sección 13.2). Un EntityScheduler por
 * jugador en vez de un único bucle global iterando a todos - necesario para
 * Folia (cada jugador puede estar en el hilo de una región distinta) y
 * corrige de raíz el bug de la nota 6 (nunca corre async llamando a
 * World.spawnParticle).
 */
public class ActiveCosmeticsTask {

    private final Mincore plugin;
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    /** Rastrea la transición aire->suelo por jugador - necesario para que el estilo RINGS de trails solo explote justo al aterrizar, igual que v1. */
    private final Map<UUID, Boolean> wasOnGround = new ConcurrentHashMap<>();

    /**
     * Última posición conocida por jugador, para saber si "se está moviendo"
     * de verdad. Player#getVelocity() NO sirve para esto - Bukkit solo la
     * actualiza con retroceso/explosiones/física real, no con el caminar
     * normal (eso lo maneja el cliente vía paquetes de posición) - por eso
     * ORBIT/SPARKS/PUDDLE/WAVES/STEPS (que dependían de esa velocidad)
     * parecían "solo funcionar al saltar": saltar es de los pocos momentos en
     * los que el servidor sí trae una velocidad real. Comparar la posición
     * contra el tick anterior sí detecta caminar/correr/sneak correctamente.
     */
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();

    public ActiveCosmeticsTask(Mincore plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        stop(player);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduled -> tick(player), () -> {
        }, 3L, 3L);
        tasks.put(player.getUniqueId(), task);
    }

    public void stop(Player player) {
        stop(player.getUniqueId());
    }

    public void stop(UUID uuid) {
        ScheduledTask task = tasks.remove(uuid);
        if (task != null) task.cancel();
        wasOnGround.remove(uuid);
        lastLocation.remove(uuid);
    }

    public void stopAll() {
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
        wasOnGround.clear();
        lastLocation.clear();
    }

    private void tick(Player player) {
        if (!player.isOnline()) return;
        if (!WorldValidator.isAllowed(plugin, player.getWorld())) return;
        if (org.dqnylux.mincore.managers.cosmetics.CosmeticVisibility.isHiddenFromOthers(plugin, player)) return;
        PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
        if (data == null) return;

        UUID uuid = player.getUniqueId();
        boolean onGround = player.isOnGround();
        boolean hasLanded = onGround && !wasOnGround.getOrDefault(uuid, true);
        wasOnGround.put(uuid, onGround);

        Location current = player.getLocation();
        Location previous = lastLocation.put(uuid, current.clone());
        boolean moving = previous != null
                && previous.getWorld().equals(current.getWorld())
                && previous.distanceSquared(current) > 0.0009;

        // Aislados en su propio try/catch a propósito: van en SECUENCIA en el
        // mismo tick, así que sin esto una excepción en el trail (ej. un
        // estilo con datos de catálogo raros) aborta el resto del método y
        // las alas JAMÁS llegan a dibujarse ese ciclo - un jugador con trail
        // Y alas equipadas a la vez veía "las alas no funcionan" cuando la
        // causa real era que el trail reventaba antes de llegar a esa línea.
        // Bug real reportado por el usuario, causa raíz confirmada acá.
        String trailId = data.getActiveCosmetic("trails");
        if (trailId != null) {
            CosmeticItem item = plugin.getCosmeticConfigManager().getItem("trails", trailId);
            if (item != null) {
                try {
                    plugin.getTrailManager().drawTrail(player, item, hasLanded, moving);
                } catch (Exception e) {
                    org.dqnylux.mincore.utils.ConsoleLogger.error("Error dibujando trail '" + trailId + "' de " + player.getName() + ": " + e);
                }
            }
        }

        String wingsId = data.getActiveCosmetic("wings");
        if (wingsId != null) {
            WingsConfig wingsConfig = plugin.getCosmeticConfigManager().getWings();
            WingCosmetic wingItem = wingsConfig.items.get(wingsId);
            if (wingItem != null) {
                try {
                    plugin.getWingManager().render(player, wingItem, moving);
                } catch (Exception e) {
                    org.dqnylux.mincore.utils.ConsoleLogger.error("Error dibujando alas '" + wingsId + "' de " + player.getName() + ": " + e);
                }
            }
        }
    }
}
