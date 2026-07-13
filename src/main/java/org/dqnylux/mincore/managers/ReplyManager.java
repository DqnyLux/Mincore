package org.dqnylux.mincore.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rastrea a quién responder con /reply. Relación bidireccional en memoria,
 * sin persistencia (se pierde al reconectar, igual que en el prompt original).
 */
public class ReplyManager {

    private final Map<UUID, UUID> lastTarget = new ConcurrentHashMap<>();

    public void setReplyTarget(UUID a, UUID b) {
        lastTarget.put(a, b);
        lastTarget.put(b, a);
    }

    public UUID getReplyTarget(UUID uuid) {
        return lastTarget.get(uuid);
    }

    public void clear(UUID uuid) {
        lastTarget.remove(uuid);
    }
}
