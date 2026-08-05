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

        /** Altura base (bloques sobre los pies) donde se ancla el centro del ala. */
        public double startVertical = 1.3;

        /** Distancia (bloques) hacia atrás del jugador donde empieza el ala. */
        public double startDistanceToPlayer = 0.3;

        /** Espaciado (bloques) entre cada punto del layout ASCII-art. */
        public double spacing = 0.22;

        /** Layout ASCII-art: cada carácter no-espacio es un punto de partícula. */
        public List<String> layout = new ArrayList<>();

        /** Carácter -> partícula/color/velocidad de ese punto del ala. */
        public Map<String, WingParticleData> particleMap = defaultParticleMap();

        private static Map<String, WingParticleData> defaultParticleMap() {
            Map<String, WingParticleData> map = new LinkedHashMap<>();
            map.put("#", new WingParticleData());
            return map;
        }
    }

    public static class WingParticleData extends MincoreConfig {
        public String particle = "CLOUD";

        /** #RRGGBB o nombre de org.bukkit.Color - vacío = sin tinte (partícula sin soporte de color). */
        public String color = "";

        public double speed = 0.0;

        public WingParticleData() {
        }

        public WingParticleData(String particle, String color, double speed) {
            this.particle = particle;
            this.color = color;
            this.speed = speed;
        }
    }
}
