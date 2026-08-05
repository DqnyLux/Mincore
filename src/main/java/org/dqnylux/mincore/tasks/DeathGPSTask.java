package org.dqnylux.mincore.tasks;

import com.cryptomorin.xseries.XSound;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.DeathSystemConfig;
import org.dqnylux.mincore.managers.cosmetics.EffectUtils;
import org.dqnylux.mincore.utils.BedrockMenuHandler;
import org.dqnylux.mincore.utils.TextUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bossbar + holograma flotante (visible solo para quien rastrea) + flecha
 * direccional de 8 posiciones + baliza visual hacia la tumba, con dos modos
 * (death_system.yml beacon.mode - sección 17, cero hardcodeo):
 * GLASS = beacon vanilla real (bloques falsos enviados solo al cliente vía
 * PacketEvents, nunca tocan el mundo real) que el cliente renderiza con su
 * haz vertical de siempre - el color se aproxima al vidrio teñido real más
 * parecido (Minecraft solo tiene 16). En cuevas (sin acceso al cielo, un
 * beacon ahí no se vería) se usa en su lugar un EnderCrystal real
 * invulnerable/no persistente con haz de luz hacia arriba.
 * PARTICLES = columna de partículas DUST con el hex exacto de
 * waypoint.color, sin aproximar - funciona igual en superficie y en cuevas.
 */
public class DeathGPSTask {

    private final Mincore plugin;
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, HologramHandle> holograms = new ConcurrentHashMap<>();
    private final Map<UUID, EnderCrystal> caveCrystals = new ConcurrentHashMap<>();
    private final Map<UUID, List<Location>> fakeBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> ticksAlive = new ConcurrentHashMap<>();
    private final Map<UUID, Long> expiresAt = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> beaconVisible = new ConcurrentHashMap<>();

    public DeathGPSTask(Mincore plugin) {
        this.plugin = plugin;
    }

    /**
     * expiresAtMillis debe ser el mismo instante que
     * DeathTrackingManager.Death#expiresAt() para ese rastreo - el HUD
     * necesita saberlo para mostrar la cuenta regresiva y detenerse solo
     * cuando el registro ya expiró (v1 lo hacía con maxTicks; portarlo sin
     * esto dejaba el bossbar/holograma/baliza corriendo para siempre, sin
     * enterarse nunca de que el ID ya no era válido).
     */
    public void start(Player player, Location target, int deathId, long expiresAtMillis) {
        stop(player);
        UUID uuid = player.getUniqueId();
        expiresAt.put(uuid, expiresAtMillis);

        DeathSystemConfig deathSystem = plugin.getConfigManager().getDeathSystemConfig();

        BossBar bossBar = BossBar.bossBar(TextUtils.format(deathSystem.bossbar.loading),
                1f, parseColor(deathSystem.bossbar.color), parseOverlay(deathSystem.bossbar.style));
        player.showBossBar(bossBar);
        bossBars.put(uuid, bossBar);

        // Spawnear con el MUNDO Y la ubicación del jugador (no target.getWorld()
        // con player.getLocation() - mezclar el mundo de la tumba con las
        // coordenadas actuales del jugador es justo el bug que hacía que el
        // holograma "no apareciera": si moriste en un mundo distinto al que
        // estás ahora (Nether/End, o simplemente sin cama puesta ahí) el
        // holograma terminaba spawneado en un mundo donde el jugador ni
        // siquiera está parado. Se reposiciona al mundo/ubicación real de la
        // tumba en el primer tick() vía positionWaypointHologram().
        HologramHandle hologram = spawnHologram(player, deathSystem);
        holograms.put(uuid, hologram);

        boolean cave = !hasSkyAccess(target);
        boolean particles = isParticleMode(deathSystem);
        if (deathSystem.beacon.enabled) {
            if (particles) {
                spawnParticleBeam(player, target, deathSystem);
            } else if (cave) {
                showCaveCrystal(player, uuid, target, deathSystem);
            } else {
                fakeBlocks.put(uuid, new ArrayList<>());
                refreshSurfaceBeacon(player, target, deathSystem);
            }
            beaconVisible.put(uuid, true);
        }

        ticksAlive.put(uuid, 0);
        long interval = Math.max(1, deathSystem.updateIntervalTicks);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin,
                scheduled -> tick(player, target, deathId, cave), () -> cleanup(uuid), interval, interval);
        tasks.put(uuid, task);
    }

    /**
     * ARMOR_STAND (Bedrock vía AUTO, o forzado) es deliberadamente más
     * simple que TEXT_DISPLAY: una sola línea, sin escalado suave al
     * acercarse - un marker armor stand con nametag es más liviano de
     * renderizar/trackear que un display entity con interpolación, y se
     * traduce de forma mucho más confiable a través de Geyser. El objetivo
     * es bajo consumo, no paridad visual exacta con el modo completo.
     */
    private HologramHandle spawnHologram(Player player, DeathSystemConfig deathSystem) {
        boolean armorStand = useArmorStand(player, deathSystem);

        if (armorStand) {
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setSilent(true);
            stand.setCustomNameVisible(true);
            stand.setPersistent(false);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) other.hideEntity(plugin, stand);
            }
            return new HologramHandle(stand, deathSystem.hologramArmorStandSeparator);
        }

        TextDisplay hologram = (TextDisplay) player.getWorld().spawnEntity(player.getLocation(), EntityType.TEXT_DISPLAY);
        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setSeeThrough(true);
        hologram.setDefaultBackground(true);
        // Igual que v1: con estas dos duraciones en 3 ticks, el cliente
        // interpola posición Y transformación (escala incluida) en vez de
        // saltar de golpe cada vez que se reposiciona - se ve como un
        // waypoint deslizándose suave, no parpadeando.
        hologram.setTeleportDuration(3);
        hologram.setInterpolationDuration(3);
        hologram.setLineWidth(300);
        hologram.setPersistent(false);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) other.hideEntity(plugin, hologram);
        }
        return new HologramHandle(hologram);
    }

    private boolean useArmorStand(Player player, DeathSystemConfig deathSystem) {
        String mode = deathSystem.hologramMode == null ? "AUTO" : deathSystem.hologramMode.trim().toUpperCase();
        return switch (mode) {
            case "ARMOR_STAND" -> true;
            case "TEXT_DISPLAY" -> false;
            // BedrockUtil primero (sin imports de Floodgate/Cumulus) - recién si
            // el plugin está presente es seguro tocar BedrockMenuHandler, que sí
            // referencia esos tipos (ver BedrockUtil para el porqué exacto).
            default -> org.dqnylux.mincore.utils.BedrockUtil.isFloodgateInstalled() && BedrockMenuHandler.isBedrockPlayer(player);
        };
    }

    /**
     * Envuelve el TextDisplay (modo completo) o el ArmorStand (modo liviano
     * Bedrock/bajo consumo) detrás de la misma interfaz - el resto de la
     * clase (tick, positionWaypointHologram, cleanup) no necesita saber cuál
     * de los dos está usando cada jugador.
     */
    private static final class HologramHandle {
        private final TextDisplay textDisplay;
        private final ArmorStand armorStand;
        private final String armorStandSeparator;

        HologramHandle(TextDisplay textDisplay) {
            this.textDisplay = textDisplay;
            this.armorStand = null;
            this.armorStandSeparator = null;
        }

        HologramHandle(ArmorStand armorStand, String armorStandSeparator) {
            this.textDisplay = null;
            this.armorStand = armorStand;
            this.armorStandSeparator = armorStandSeparator;
        }

        boolean isValid() {
            return armorStand != null ? armorStand.isValid() : textDisplay.isValid();
        }

        Entity entity() {
            return armorStand != null ? armorStand : textDisplay;
        }

        /** raw = el texto SIN parsear todavía (puede traer \n) - cada modo decide cómo formatearlo. */
        void setText(String raw) {
            if (armorStand != null) {
                String separator = armorStandSeparator == null ? " " : armorStandSeparator;
                armorStand.customName(TextUtils.format(raw.replace("\n", separator)));
            } else {
                textDisplay.text(TextUtils.format(raw));
            }
        }

        /** El nametag de un ArmorStand small="true" flota ~1.65 bloques sobre sus pies (no está centrado en su ubicación como el TextDisplay con billboard CENTER) - se compensa acá para que ambos modos apunten al mismo punto visual. */
        void teleport(Location location) {
            if (armorStand != null) {
                entity().teleport(location.clone().subtract(0, 1.65, 0));
            } else {
                entity().teleport(location);
            }
        }

        /** Sin efecto en modo ArmorStand (no tiene Transformation) - el modo liviano no escala, a propósito. */
        void scale(float scale) {
            if (textDisplay == null) return;
            textDisplay.setTransformation(new Transformation(
                    new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
        }

        void remove() {
            entity().remove();
        }
    }

    private void tick(Player player, Location target, int deathId, boolean cave) {
        UUID uuid = player.getUniqueId();
        if (!player.isOnline()) {
            stop(uuid);
            return;
        }

        DeathSystemConfig deathSystem = plugin.getConfigManager().getDeathSystemConfig();

        // El registro en DeathTrackingManager expira solo (borrándose de su
        // mapa) sin avisarle a este HUD - sin este chequeo, el bossbar y el
        // holograma seguirían corriendo indefinidamente aunque el ID ya no
        // sea válido y arrivar ya no pueda hacer remove() sobre nada real.
        Long deadline = expiresAt.get(uuid);
        long remainingMillis = deadline != null ? deadline - System.currentTimeMillis() : 0;
        if (remainingMillis <= 0) {
            player.sendMessage(TextUtils.format(deathSystem.trackingExpired));
            stop(uuid);
            return;
        }
        String timeLeft = formatTime(remainingMillis);

        // Location#distance() lanza IllegalArgumentException entre mundos
        // distintos - si el jugador se fue a otro mundo (lobby/minijuego)
        // mientras rastreaba, no hay forma de calcular una distancia con
        // sentido. Se pausa la distancia (bossbar/holograma avisan que está
        // en otro mundo, la baliza no se toca) en vez de crashear el tick
        // entero cada 2 ticks - el holograma igual se reposiciona frente al
        // jugador para que no quede una tarjeta flotando en el aire donde
        // sea que estuviera parado antes de cambiar de mundo.
        if (!player.getWorld().equals(target.getWorld())) {
            BossBar pausedBar = bossBars.get(uuid);
            if (pausedBar != null) {
                pausedBar.name(TextUtils.format(deathSystem.trackingWrongWorld));
            }

            HologramHandle pausedHologram = holograms.get(uuid);
            if (pausedHologram != null && pausedHologram.isValid()) {
                pausedHologram.setText(deathSystem.hudOtherWorld.replace("%time%", timeLeft));
                Location holoLoc = player.getEyeLocation()
                        .add(player.getLocation().getDirection().multiply(deathSystem.hologramDistance))
                        .add(0, deathSystem.hologramHeight, 0);
                pausedHologram.teleport(holoLoc);
            }
            return;
        }

        double distance = player.getLocation().distance(target);
        String arrow = directionArrow(player.getLocation(), target);

        BossBar bossBar = bossBars.get(uuid);
        if (bossBar != null) {
            String title = deathSystem.bossbar.title
                    .replace("%distance%", String.valueOf(Math.round(distance)))
                    .replace("%arrow%", arrow)
                    .replace("%time%", timeLeft);
            bossBar.name(TextUtils.format(title));
            double maxDistance = Math.max(1.0, deathSystem.bossbar.maxDistance);
            bossBar.progress(Math.max(0f, Math.min(1f, (float) (1 - (distance / maxDistance)))));
        }

        HologramHandle hologram = holograms.get(uuid);
        if (hologram != null && hologram.isValid()) {
            String holoText = deathSystem.hudHologram
                    .replace("%color%", deathSystem.waypoint.color)
                    .replace("%icon%", deathSystem.waypoint.icon)
                    .replace("%distance%", String.valueOf(Math.round(distance)))
                    .replace("%arrow%", arrow)
                    .replace("%time%", timeLeft);
            hologram.setText(holoText);
            positionWaypointHologram(hologram, player, target, distance, deathSystem);
        }

        if (deathSystem.beacon.enabled) {
            boolean particles = isParticleMode(deathSystem);
            boolean shouldShow = distance > deathSystem.beacon.hideDistance;
            boolean currentlyShown = beaconVisible.getOrDefault(uuid, false);
            int ticks = ticksAlive.merge(uuid, Math.max(1, deathSystem.updateIntervalTicks), Integer::sum);
            int refreshTicks = Math.max(1, deathSystem.beacon.refreshTicks);

            if (particles) {
                // Efímeras por diseño (el cliente las hace desaparecer solo) -
                // no hay "ocultar", solo se deja de reenviar la columna.
                if (shouldShow && (!currentlyShown || ticks % refreshTicks == 0)) {
                    spawnParticleBeam(player, target, deathSystem);
                }
                beaconVisible.put(uuid, shouldShow);
            } else if (shouldShow != currentlyShown) {
                if (cave) {
                    if (shouldShow) showCaveCrystal(player, uuid, target, deathSystem);
                    else hideCaveCrystal(uuid);
                } else {
                    if (shouldShow) refreshSurfaceBeacon(player, target, deathSystem);
                    else hideSurfaceBeacon(player, uuid);
                }
                beaconVisible.put(uuid, shouldShow);
            } else if (shouldShow && !cave && ticks % refreshTicks == 0) {
                refreshSurfaceBeacon(player, target, deathSystem);
            }
        }

        if (distance <= deathSystem.arrivalDistance) {
            player.sendMessage(TextUtils.format(deathSystem.arrivedMessage));
            playArrivalSound(player, deathSystem.arrivalSound);
            plugin.getDeathTrackingManager().remove(deathId);
            stop(uuid);
        }
    }

    private void playArrivalSound(Player player, DeathSystemConfig.ArrivalSound arrivalSound) {
        if (arrivalSound.sound == null || arrivalSound.sound.isBlank()) return;
        XSound.matchXSound(arrivalSound.sound)
                .ifPresent(sound -> sound.play(player.getLocation(), arrivalSound.volume, arrivalSound.pitch));
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000L;
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    /**
     * Igual que v1: por debajo de hologramNearDistance el holograma se ancla
     * FLOTANDO SOBRE LA TUMBA (posición fija en el mundo, no sigue al
     * jugador) y se agranda a medida que uno se acerca; por encima de esa
     * distancia flota frente a los ojos del jugador apuntando en dirección a
     * la tumba, como un waypoint - no pegado a la cara del jugador sin más
     * (eso era el comportamiento antiguo de v2, distinto a v1).
     */
    private void positionWaypointHologram(HologramHandle hologram, Player player, Location target, double distance, DeathSystemConfig deathSystem) {
        double near = Math.max(1.0, deathSystem.hologramNearDistance);
        double yOffset = distance > near ? deathSystem.hologramMaxHeight : (distance / near) * deathSystem.hologramMaxHeight;

        Location targetHoloLoc;
        float scale;

        if (distance <= near) {
            targetHoloLoc = target.clone().add(0, 2.5 + yOffset, 0);
            scale = (float) (deathSystem.hologramScaleNear
                    + (distance / near) * (deathSystem.hologramScaleFar - deathSystem.hologramScaleNear));
        } else {
            Vector direction = target.clone().add(0, 2.5, 0).toVector()
                    .subtract(player.getEyeLocation().toVector()).normalize();
            targetHoloLoc = player.getEyeLocation().add(direction.multiply(near));
            targetHoloLoc.add(0, yOffset, 0);
            scale = (float) deathSystem.hologramScaleFar;
        }

        hologram.teleport(targetHoloLoc);
        hologram.scale(scale);
    }

    private boolean hasSkyAccess(Location location) {
        int highestY = location.getWorld().getHighestBlockYAt(location);
        return highestY <= location.getBlockY();
    }

    private boolean isParticleMode(DeathSystemConfig deathSystem) {
        return "PARTICLES".equalsIgnoreCase(deathSystem.beacon.mode == null ? "" : deathSystem.beacon.mode.trim());
    }

    private BossBar.Color parseColor(String name) {
        try {
            return BossBar.Color.valueOf(name.trim().toUpperCase());
        } catch (Exception e) {
            return BossBar.Color.YELLOW;
        }
    }

    private BossBar.Overlay parseOverlay(String legacyStyle) {
        // death_system.yml usa nombres vanilla clásicos (SOLID/SEGMENTED_N,
        // los mismos que BarStyle de la API legacy de Bukkit) para que sea
        // portable 1:1 desde v1 - Adventure los llama distinto (PROGRESS/NOTCHED_N).
        if (legacyStyle == null) return BossBar.Overlay.PROGRESS;
        String normalized = legacyStyle.trim().toUpperCase().replace("SEGMENTED", "NOTCHED");
        if (normalized.equals("SOLID")) return BossBar.Overlay.PROGRESS;
        try {
            return BossBar.Overlay.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    /** Columna de partículas DUST con el color EXACTO de waypoint.color (sin aproximar a un vidrio teñido) - solo visible para el jugador que rastrea. */
    private void spawnParticleBeam(Player player, Location target, DeathSystemConfig deathSystem) {
        Color color = EffectUtils.parseColor(deathSystem.waypoint.color, Color.RED);
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);

        Location base = groundBelow(target);
        double spacing = Math.max(0.1, deathSystem.beacon.particleSpacing);
        double height = Math.max(1.0, deathSystem.beacon.particleBeamHeight);

        for (double y = 0; y <= height; y += spacing) {
            player.spawnParticle(Particle.DUST, base.clone().add(0, y, 0), 1, 0, 0, 0, 0, dust);
        }
    }

    private void showCaveCrystal(Player player, UUID uuid, Location target, DeathSystemConfig deathSystem) {
        if (caveCrystals.containsKey(uuid)) return;
        EnderCrystal crystal = spawnCaveCrystal(target, deathSystem);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) other.hideEntity(plugin, crystal);
        }
        caveCrystals.put(uuid, crystal);
    }

    private void hideCaveCrystal(UUID uuid) {
        EnderCrystal crystal = caveCrystals.remove(uuid);
        if (crystal != null && !crystal.isDead()) crystal.remove();
    }

    private EnderCrystal spawnCaveCrystal(Location target, DeathSystemConfig deathSystem) {
        Location crystalLoc = target.clone().subtract(0, deathSystem.beacon.caveCrystalOffset, 0);
        if (crystalLoc.getY() < crystalLoc.getWorld().getMinHeight() + 1) {
            crystalLoc.setY(crystalLoc.getWorld().getMinHeight() + 1);
        }
        Location ceiling = target.clone();
        ceiling.setY(deathSystem.beacon.caveCeilingY);

        EnderCrystal crystal = (EnderCrystal) crystalLoc.getWorld().spawnEntity(crystalLoc, EntityType.END_CRYSTAL);
        crystal.setShowingBottom(false);
        crystal.setBeamTarget(ceiling);
        crystal.setInvulnerable(true);
        crystal.setPersistent(false);
        return crystal;
    }

    private void refreshSurfaceBeacon(Player player, Location target, DeathSystemConfig deathSystem) {
        Location ground = groundBelow(target);
        Location base = ground.clone().subtract(0, 1, 0);

        List<Location> tracked = fakeBlocks.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>());
        boolean firstTime = tracked.isEmpty();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location loc = base.clone().add(x, 0, z);
                if (firstTime) tracked.add(loc);
                sendFakeBlock(player, loc, Material.IRON_BLOCK);
            }
        }
        if (firstTime) {
            tracked.add(ground.clone());
            tracked.add(ground.clone().add(0, 1, 0));
        }
        sendFakeBlock(player, ground, Material.BEACON);
        sendFakeBlock(player, ground.clone().add(0, 1, 0), nearestStainedGlass(deathSystem.waypoint.color));
    }

    /** Restaura los bloques reales en las posiciones donde estaba el beacon falso, sin borrar el registro de "primera vez" (se vuelve a armar solo al reaparecer). */
    private void hideSurfaceBeacon(Player player, UUID uuid) {
        List<Location> blocks = fakeBlocks.get(uuid);
        if (blocks == null) return;
        for (Location location : blocks) restoreFakeBlock(player, location);
        blocks.clear();
    }

    /** Vidrio teñido más parecido (distancia euclidiana en RGB) al hex de waypoint.color - Minecraft no tiene vidrio de un color arbitrario, así que el modo GLASS solo puede aproximar. */
    private Material nearestStainedGlass(String hex) {
        Color target = EffectUtils.parseColor(hex, Color.RED);
        DyeColor best = DyeColor.RED;
        double bestDistance = Double.MAX_VALUE;

        for (DyeColor dye : DyeColor.values()) {
            Color dyeColor = dye.getColor();
            double dr = dyeColor.getRed() - target.getRed();
            double dg = dyeColor.getGreen() - target.getGreen();
            double db = dyeColor.getBlue() - target.getBlue();
            double distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = dye;
            }
        }

        return Material.valueOf(best.name() + "_STAINED_GLASS");
    }

    private Location groundBelow(Location location) {
        Location current = location.clone();
        while (current.getBlockY() > current.getWorld().getMinHeight() && !current.getBlock().getType().isSolid()) {
            current.subtract(0, 1, 0);
        }
        return current;
    }

    private void sendFakeBlock(Player player, Location location, Material material) {
        Vector3i pos = new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(material));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerBlockChange(pos, state));
    }

    private void restoreFakeBlock(Player player, Location location) {
        Vector3i pos = new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(location.getBlock().getBlockData());
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerBlockChange(pos, state));
    }

    private String directionArrow(Location from, Location to) {
        double yaw = from.getYaw();
        double targetYaw = Math.toDegrees(Math.atan2(from.getX() - to.getX(), to.getZ() - from.getZ()));
        double relative = (targetYaw - yaw) % 360;
        if (relative < 0) relative += 360;

        if (relative >= 337.5 || relative < 22.5) return "⬆";
        if (relative < 67.5) return "⬈";
        if (relative < 112.5) return "➡";
        if (relative < 157.5) return "⬊";
        if (relative < 202.5) return "⬇";
        if (relative < 247.5) return "⬋";
        if (relative < 292.5) return "⬅";
        return "⬉";
    }

    public boolean isTracking(UUID uuid) {
        return tasks.containsKey(uuid);
    }

    public void stop(Player player) {
        stop(player.getUniqueId());
    }

    public void stop(UUID uuid) {
        ScheduledTask task = tasks.remove(uuid);
        if (task != null) task.cancel();
        // El callback "retired" de runAtFixedRate (pasado como 3er argumento
        // en start()) NO es un "on cancel" genérico - en Folia solo dispara
        // si la tarea nunca llegó a ejecutarse ni una vez (entidad inválida
        // antes del primer run). Cancelar una tarea que YA viene corriendo
        // (el caso normal: llegar a la tumba, expirar, cambiar de rastreo)
        // nunca invocaba cleanup() por esa vía, dejando bossbar/holograma
        // visibles para siempre. cleanup() es idempotente (todo son
        // Map#remove que devuelven null en la segunda llamada), así que
        // llamarlo aquí explícito es seguro aunque el retired callback SÍ
        // llegue a disparar también en el caso límite que sí cubre.
        cleanup(uuid);
    }

    private void cleanup(UUID uuid) {
        BossBar bossBar = bossBars.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (bossBar != null && player != null) player.hideBossBar(bossBar);

        HologramHandle hologram = holograms.remove(uuid);
        if (hologram != null && hologram.isValid()) hologram.remove();

        EnderCrystal crystal = caveCrystals.remove(uuid);
        if (crystal != null && !crystal.isDead()) crystal.remove();

        List<Location> blocks = fakeBlocks.remove(uuid);
        if (blocks != null && player != null) {
            for (Location location : blocks) restoreFakeBlock(player, location);
        }
        ticksAlive.remove(uuid);
        expiresAt.remove(uuid);
        beaconVisible.remove(uuid);
    }

    public void stopAll() {
        for (UUID uuid : new ArrayList<>(tasks.keySet())) {
            stop(uuid);
        }
    }
}
