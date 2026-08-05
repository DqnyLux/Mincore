package org.dqnylux.mincore.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.UUID;

/**
 * Wrapper estático - se inicializa solo si LuckPerms está presente (try/catch
 * sobre LuckPermsProvider.get()).
 *
 * El prefijo/sufijo de cosmético se añade como un nodo DIRECTO del jugador con
 * prioridad muy alta (config.yml -> luckPerms.prefixPriority/suffixPriority,
 * por defecto 1000) en vez de borrar todos los nodos de prefijo/sufijo antes
 * de añadir el nuevo: así nunca se toca el prefijo/sufijo que el jugador ya
 * tenía por su rango (normalmente un nodo heredado de su grupo, con prioridad
 * mucho más baja) - el cosmético solo se superpone mientras está equipado. Al
 * desequipar, solo se quita el nodo con exactamente esa prioridad reservada,
 * dejando intacto cualquier otro prefijo/sufijo (de rango o asignado a mano
 * por un admin directamente al jugador).
 */
public final class LuckPermsHook {

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

    public static void setCustomPrefix(UUID uuid, String prefix, int priority) {
        if (api == null) return;
        String legacyPrefix = TextUtils.toLegacyAmpersand(prefix);
        api.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == priority));
            user.data().add(PrefixNode.builder(legacyPrefix, priority).build());
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

    public static void setCustomSuffix(UUID uuid, String suffix, int priority) {
        if (api == null) return;
        String legacySuffix = TextUtils.toLegacyAmpersand(suffix);
        api.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().clear(NodeType.SUFFIX.predicate(node -> node.getPriority() == priority));
            user.data().add(SuffixNode.builder(legacySuffix, priority).build());
            api.getUserManager().saveUser(user);
        });
    }

    public static void removeCustomSuffix(UUID uuid, int priority) {
        if (api == null) return;
        api.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().clear(NodeType.SUFFIX.predicate(node -> node.getPriority() == priority));
            api.getUserManager().saveUser(user);
        });
    }
}
