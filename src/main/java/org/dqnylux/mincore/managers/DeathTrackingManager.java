package org.dqnylux.mincore.managers;

import org.bukkit.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * En memoria, sin persistencia en BD (igual que el prompt original - un
 * reinicio durante una ventana de rastreo activa pierde el registro).
 */
public class DeathTrackingManager {

    public record Death(int id, Location location, long expiresAt) {
    }

    private final Map<Integer, Death> active = new ConcurrentHashMap<>();

    public int register(Location location, int expirationMinutes) {
        // ThreadLocalRandom, no un Random compartido - register() puede
        // llamarse desde varios hilos de región de Folia a la vez (jugadores
        // muriendo en regiones distintas en el mismo tick), y Random no es
        // seguro bajo esa concurrencia.
        int id;
        do {
            id = 1000 + ThreadLocalRandom.current().nextInt(9000);
        } while (active.containsKey(id));

        active.put(id, new Death(id, location.clone(), System.currentTimeMillis() + expirationMinutes * 60_000L));
        return id;
    }

    public Death get(int id) {
        Death death = active.get(id);
        if (death == null) return null;

        if (System.currentTimeMillis() > death.expiresAt()) {
            active.remove(id);
            return null;
        }
        return death;
    }

    public void remove(int id) {
        active.remove(id);
    }
}
