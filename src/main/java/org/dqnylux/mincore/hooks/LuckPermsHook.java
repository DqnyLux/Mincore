package org.dqnylux.mincore.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;

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
        api.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == priority));
            user.data().add(PrefixNode.builder(prefix, priority).build());
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

    public static void setCustomSuffix(UUID uuid, String suffix, int priority) {
        if (api == null) return;
        api.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().clear(NodeType.SUFFIX.predicate(node -> node.getPriority() == priority));
            user.data().add(SuffixNode.builder(suffix, priority).build());
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
