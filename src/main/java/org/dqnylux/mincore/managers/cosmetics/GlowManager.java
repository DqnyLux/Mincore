package org.dqnylux.mincore.managers.cosmetics;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colorea el contorno del glow (y el prefijo "[STAFF]" por-viewer) vía un
 * equipo de scoreboard FALSO por paquete (WrapperPlayServerTeams, por
 * jugador que observa) en vez de un Team real registrado en el Scoreboard -
 * SIEMPRE, con o sin TAB Reborn instalado (antes, con TAB, el color se fijaba
 * vía su propia API - NameTagManager#setPrefix -, pero resultó poco fiable;
 * el color de glow es un caso lo bastante puntual - solo mientras alguien
 * tiene glow activo - como para aceptar el mismo trade-off ya documentado
 * abajo en hideVanillaNametag()).
 *
 * IMPORTANTE - lo que NO hace este manager: ocultar el nametag vanilla de un
 * jugador CON TAB instalado NO pasa por acá (ver hideVanillaNametag() más
 * abajo) - eso queda exclusivamente en manos de TabHook.hideNameTag(), la
 * propia API de TAB. Se probó unificar TODO (color + ocultar nametag) al
 * equipo falso de acá para TODOS los jugadores sin excepción, y rompió el
 * orden del tablist de TODO el servidor: TAB también usa el team REAL de
 * cada jugador para agrupar por rango y ordenar alfabéticamente adentro de
 * cada rango (el cliente solo recuerda UN equipo por jugador - el nuestro,
 * con un nombre aleatorio derivado del UUID, pisaba al de TAB). Mandar
 * nuestro propio equipo falso está bien SOLO para quien ya lo necesita por
 * otro motivo (glow activo, tag de staff) - a esos ya se les rompía el orden
 * antes de este cambio también (limitación conocida, ver el javadoc de
 * hideVanillaNametag), pero eso es un subconjunto chico de jugadores en vez
 * de todo el servidor todo el tiempo.
 */
public class GlowManager {

    private final Map<UUID, String> teamNames = new ConcurrentHashMap<>();
    private final Map<UUID, NamedTextColor> colors = new ConcurrentHashMap<>();
    private final Map<UUID, Component> staffPrefixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> staffPrefixPermissions = new ConcurrentHashMap<>();
    /** Jugadores con el nametag propio (NametagDisplayManager) activo Y sin TAB instalado - fuerza NameTagVisibility.NEVER en este mismo equipo falso en vez de crear uno nuevo aparte. */
    private final Set<UUID> nametagHidden = ConcurrentHashMap.newKeySet();
    private ScheduledTask resendTask;
    private Mincore plugin;

    /**
     * TAB (y cualquier otro plugin que gestione equipos para el nametag)
     * reenvía SUS PROPIOS paquetes de equipo periódicamente (al ordenar la
     * tablist, en cambios de permisos, etc.) - un jugador solo puede estar
     * en UN equipo a la vez del lado del cliente, así que cuando TAB
     * reasigna a alguien a su equipo real, se lo "roba" silenciosamente al
     * equipo falso de Mincore y el color/prefijo se pierde o cambia hasta
     * el próximo equipar/desequipar. Reenviar el equipo falso cada pocos
     * segundos mantiene a Mincore como "la última palabra".
     */
    public void startAutoResend(Mincore plugin) {
        this.plugin = plugin;
        resendTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> resendAll(), 5L, 5L, TimeUnit.SECONDS);
    }

    public void stop() {
        if (resendTask != null) resendTask.cancel();
    }

    private void resendAll() {
        Set<UUID> targets = ConcurrentHashMap.newKeySet();
        targets.addAll(colors.keySet());
        targets.addAll(staffPrefixes.keySet());
        targets.addAll(nametagHidden);
        if (targets.isEmpty()) return;

        for (UUID id : targets) {
            Player target = Bukkit.getPlayer(id);
            if (target == null) continue;
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                sendTeamFor(viewer, target);
            }
        }
    }

    /** Color de glow vía el equipo falso por paquete - ver el javadoc de la clase sobre por qué esto ya no necesita un camino aparte para TAB. */
    public void applyGlow(Player player, String colorName) {
        player.setGlowing(true);
        NamedTextColor color = parseColor(colorName);

        colors.put(player.getUniqueId(), color);
        teamNames.computeIfAbsent(player.getUniqueId(), k -> teamNameFor(player));
        resendTo(player);
    }

    public void removeGlow(Player player) {
        player.setGlowing(false);
        colors.remove(player.getUniqueId());
        resendTo(player);
    }

    /** Prefijo "[STAFF]" (o lo que diga staff.yml) visible SOLO para viewers con viewPermission - el resto ve el equipo sin prefijo (o directamente nada si tampoco hay glow). */
    public void applyStaffTag(Player player, String prefixText, String viewPermission) {
        staffPrefixes.put(player.getUniqueId(), TextUtils.format(prefixText));
        staffPrefixPermissions.put(player.getUniqueId(), viewPermission);
        teamNames.computeIfAbsent(player.getUniqueId(), k -> teamNameFor(player));
        resendTo(player);
    }

    public void removeStaffTag(Player player) {
        staffPrefixes.remove(player.getUniqueId());
        staffPrefixPermissions.remove(player.getUniqueId());
        resendTo(player);
    }

    /**
     * Limpia TODO el estado en memoria de este jugador al desconectarse. Sin
     * esto, un glow/tag activo justo en el momento de la desconexión (freeze
     * congelado, staffmode, cosmético equipado) queda pegado para siempre en
     * estos mapas - ninguna otra ruta lo saca de acá (FreezeManager.onQuit no
     * llama a removeGlow, y StaffModeManager.onQuit restaura el glow personal
     * pero nunca llama a removeStaffTag). El próximo join con el mismo UUID
     * hereda ese estado viejo (resendAll cada 5s lo reafirma solo), pisando
     * lo que realmente corresponde según su cosmético persistido - la causa
     * raíz del glow "equivocado" al reconectarse congelado o con un prefix.
     * No hace falta mandar paquetes de REMOVE a otros viewers acá: el
     * jugador ya desaparece de su mundo para todos al desconectarse.
     */
    public void onQuit(Player player) {
        UUID id = player.getUniqueId();
        colors.remove(id);
        teamNames.remove(id);
        staffPrefixes.remove(id);
        staffPrefixPermissions.remove(id);
        nametagHidden.remove(id);
    }

    /**
     * Camino SIN TAB para NametagDisplayManager (con TAB instalado, usa
     * TabHook.hideNameTag en su lugar - ver el javadoc de la clase sobre por
     * qué NO conviene mandar nuestro propio equipo acá cuando TAB ya tiene
     * uno real gestionando el orden del tablist). Fuerza
     * NameTagVisibility.NEVER en este mismo equipo falso (reutiliza toda la
     * infraestructura de arriba en vez de duplicarla) para que el nametag
     * vanilla no compita visualmente con el TextDisplay propio - el color/
     * prefijo de este mismo equipo (glow/staff tag) sigue funcionando igual,
     * solo se oculta el TEXTO del nombre.
     */
    public void hideVanillaNametag(Player player) {
        nametagHidden.add(player.getUniqueId());
        teamNames.computeIfAbsent(player.getUniqueId(), k -> teamNameFor(player));
        resendTo(player);
    }

    public void showVanillaNametag(Player player) {
        nametagHidden.remove(player.getUniqueId());
        resendTo(player);
    }

    private void resendTo(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendTeamFor(viewer, target);
        }
    }

    /** Reenvía el glow/tag de todos los jugadores activos a alguien que recién se conectó, para que también los vea. */
    public void resendActiveGlowsTo(Player viewer) {
        Set<UUID> targets = ConcurrentHashMap.newKeySet();
        targets.addAll(colors.keySet());
        targets.addAll(staffPrefixes.keySet());
        targets.addAll(nametagHidden);

        for (UUID id : targets) {
            Player target = Bukkit.getPlayer(id);
            if (target == null || target.equals(viewer)) continue;
            sendTeamFor(viewer, target);
        }
    }

    private void sendTeamFor(Player viewer, Player target) {
        UUID id = target.getUniqueId();
        boolean hasColor = colors.containsKey(id);
        boolean hasPrefix = staffPrefixes.containsKey(id);
        boolean hidden = nametagHidden.contains(id);
        String teamName = teamNames.get(id);

        if (!hasColor && !hasPrefix && !hidden) {
            if (teamName != null) {
                sendRemoveTeam(viewer, teamName);
                teamNames.remove(id);
            }
            return;
        }
        if (teamName == null) return;

        NamedTextColor color = colors.getOrDefault(id, NamedTextColor.WHITE);
        Component prefix = Component.empty();
        if (hasPrefix) {
            String permission = staffPrefixPermissions.get(id);
            if (permission == null || viewer.hasPermission(permission)) {
                prefix = staffPrefixes.get(id);
            }
        }
        WrapperPlayServerTeams.NameTagVisibility visibility = hidden
                ? WrapperPlayServerTeams.NameTagVisibility.NEVER
                : WrapperPlayServerTeams.NameTagVisibility.ALWAYS;

        sendCreateTeam(viewer, teamName, color, prefix, target.getName(), visibility);
    }

    private void sendCreateTeam(Player viewer, String teamName, NamedTextColor color, Component prefix, String entryName,
                                 WrapperPlayServerTeams.NameTagVisibility visibility) {
        // REMOVE antes de CREATE: idempotente si el viewer ya conocía este
        // equipo (ej. el jugador cambió de color de glow) - el cliente
        // ignora sin error un REMOVE de un equipo que no conoce.
        sendRemoveTeam(viewer, teamName);

        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(), prefix, Component.empty(),
                visibility,
                WrapperPlayServerTeams.CollisionRule.ALWAYS,
                color, WrapperPlayServerTeams.OptionData.NONE
        );
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                teamName, WrapperPlayServerTeams.TeamMode.CREATE, info, entryName
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    private void sendRemoveTeam(Player viewer, String teamName) {
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                teamName, WrapperPlayServerTeams.TeamMode.REMOVE, (WrapperPlayServerTeams.ScoreBoardTeamInfo) null
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    private String teamNameFor(Player player) {
        return "mc_glow_" + player.getUniqueId().toString().replace("-", "").substring(0, 12);
    }

    private static final Pattern HEX_IN_TAG = Pattern.compile("#([0-9a-fA-F]{6})");

    /**
     * Acepta tanto un color con nombre legado ("RED", el formato de
     * glows.yml) como un tag MiniMessage crudo con hex ("<#F8F8FF>" o
     * "<gradient:#a:#b>", por si algún día un glow.yml usa hex directo). Los
     * equipos de scoreboard vanilla NO soportan hex arbitrario, solo los 16
     * colores legado, así que un hex se aproxima al más parecido (mismo
     * criterio que nearestStainedGlass en DeathGPSTask) - para un gradiente,
     * se usa el PRIMER stop como aproximación de un solo color.
     */
    private NamedTextColor parseColor(String name) {
        if (name == null) return NamedTextColor.WHITE;
        String trimmed = name.trim();

        NamedTextColor direct = NamedTextColor.NAMES.value(trimmed.toLowerCase());
        if (direct != null) return direct;

        Matcher hexMatcher = HEX_IN_TAG.matcher(trimmed);
        if (hexMatcher.find()) {
            return nearestNamedColor(hexMatcher.group(1));
        }
        return NamedTextColor.WHITE;
    }

    private NamedTextColor nearestNamedColor(String hex) {
        int target = Integer.parseInt(hex, 16);
        int tr = (target >> 16) & 0xFF, tg = (target >> 8) & 0xFF, tb = target & 0xFF;

        NamedTextColor best = NamedTextColor.WHITE;
        long bestDistance = Long.MAX_VALUE;
        for (NamedTextColor candidate : NamedTextColor.NAMES.values()) {
            int cr = candidate.red(), cg = candidate.green(), cb = candidate.blue();
            long dr = cr - tr, dg = cg - tg, db = cb - tb;
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
