package org.dqnylux.mincore.config.models;

import org.dqnylux.mincore.config.MincoreConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WingCosmetic extends CosmeticItem {

    public WingSettings wings = new WingSettings();

    public static class WingSettings extends MincoreConfig {
        public double flapSpeed = 1.0;
        public double maxAngle = 30.0;

        /** Layout ASCII-art: cada carácter no-espacio es un punto de partícula. */
        public List<String> layout = new ArrayList<>();

        /** Carácter -> nombre de partícula (org.bukkit.Particle). */
        public Map<String, String> particleMap = defaultParticleMap();

        private static Map<String, String> defaultParticleMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("#", "CLOUD");
            return map;
        }
    }
}
