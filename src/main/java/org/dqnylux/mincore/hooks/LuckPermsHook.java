package org.dqnylux.mincore.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;

import java.util.UUID;

/**
 * Wrapper estático - se inicializa solo si LuckPerms está presente (try/catch
 * sobre LuckPermsProvider.get()).
 *
 * setRawPrefix/removeCustomPrefix (usados por DisguiseManager) añaden/quitan
 * un nodo DIRECTO del jugador con prioridad alta (StaffConfig.prefixPriority)
 * en vez de borrar todos los nodos de prefijo antes de añadir el nuevo: así
 * nunca se toca el prefijo que el jugador ya tenía por su rango (normalmente
 * heredado de su grupo, con prioridad mucho más baja) - el disfraz solo se
 * superpone mientras está activo. Al quitarse el disfraz, solo se quita el
 * nodo con exactamente esa prioridad reservada, dejando intacto cualquier
 * otro prefijo (de rango o asignado a mano por un admin directamente al
 * jugador).
 *
 * Los cosméticos de "prefixes"/"icons" (tags/íconos comprables) YA NO pasan
 * por acá - ver el javadoc de CosmeticsGui#applySideEffects para el porqué
 * (pisaban el prefijo/sufijo REAL del rango en cualquier plugin externo que
 * leyera %luckperms_prefix%/%luckperms_suffix%, TAB Reborn incluido).
 *
 * purgeLegacyCosmeticNodes() limpia lo que ese sistema viejo dejó pegado:
 * cualquier jugador que tuviera un tag/ícono equipado ANTES de este cambio
 * se quedó con un PrefixNode/SuffixNode real en LuckPerms a prioridad 1000
 * (el valor fijo que usaba el sistema viejo, config.yml -> luckPerms.
 * prefixPriority/suffixPriority, ya eliminado) - como equipar/desequipar ya
 * no toca LuckPerms para nada, ese nodo viejo queda ahí PARA SIEMPRE, tapando
 * el prefijo/sufijo real del rango sin que elegir un tag distinto lo
 * reemplace (el nuevo cosmético ahora solo cambia %coreec_prefix%/
 * %coreec_icon%, nunca ese nodo fantasma). Se llama una vez por conexión
 * (PlayerConnectionListener) - no-op instantáneo una vez que el nodo ya fue
 * removido la primera vez.
 */
public final class LuckPermsHook {

    private static final int LEGACY_COSMETIC_PRIORITY = 1000;

    private static LuckPerms api;

    private LuckPermsHook() {
    }

    public static void init() {
        try {
            api = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            api = null;
        }
    }

    public static boolean isEnabled() {
        return api != null;
    }

    /** Usuario de LuckPerms ya cargado en memoria para este UUID, o null si LP no está o aún no está cargado. */
    public static net.luckperms.api.model.user.User getUser(UUID uuid) {
        if (api == null) return null;
        return api.getUserManager().getUser(uuid);
    }

    public static void purgeLegacyCosmeticNodes(UUID uuid) {
        if (api == null) return;
        api.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == LEGACY_COSMETIC_PRIORITY));
            user.data().clear(NodeType.SUFFIX.predicate(node -> node.getPriority() == LEGACY_COSMETIC_PRIORITY));
            api.getUserManager().saveUser(user);
        });
    }

    public static void removeCustomPrefix(UUID uuid, int priority) {
        if (api == null) return;
        api.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == priority));
            api.getUserManager().saveUser(user);
        });
    }

    /** Variante SIN conversión MiniMessage->legacy - para prefijos que ya vienen en formato legacy desde el propio LuckPerms (ej. el prefijo de un grupo, para el disguise). */
    public static void setRawPrefix(UUID uuid, String legacyPrefix, int priority) {
        if (api == null) return;
        api.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == priority));
            user.data().add(PrefixNode.builder(legacyPrefix, priority).build());
            api.getUserManager().saveUser(user);
        });
    }

    public static boolean groupExists(String name) {
        return api != null && api.getGroupManager().getGroup(name) != null;
    }

    public static java.util.Collection<String> getGroupNames() {
        if (api == null) return java.util.List.of();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (net.luckperms.api.model.group.Group group : api.getGroupManager().getLoadedGroups()) {
            names.add(group.getName());
        }
        return names;
    }

    /** Prefijo (legacy, como lo guarda LP) del grupo - null si el grupo no existe o no tiene prefijo. */
    public static String getGroupPrefix(String name) {
        if (api == null) return null;
        net.luckperms.api.model.group.Group group = api.getGroupManager().getGroup(name);
        if (group == null) return null;
        return group.getCachedData().getMetaData(net.luckperms.api.query.QueryOptions.nonContextual()).getPrefix();
    }

    public static String getPrimaryGroup(UUID uuid) {
        if (api == null) return "Usuario";
        net.luckperms.api.model.user.User user = getUser(uuid);
        if (user != null) {
            String primaryGroup = user.getPrimaryGroup();
            return primaryGroup != null && !primaryGroup.isBlank() ? primaryGroup : "Usuario";
        }
        return "Usuario";
    }

}
