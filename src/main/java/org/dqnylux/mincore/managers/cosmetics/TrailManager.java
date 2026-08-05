package org.dqnylux.mincore.managers.cosmetics;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.config.models.CosmeticItem;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Sin estado propio - drawTrail() se llama cada tick por el task del jugador.
 * Los 10 estilos originales de v1 (sección 13.2) más POLYGON. Los parámetros
 * visuales (partícula/color/velocidad/radio) vienen del CosmeticItem (sección
 * 17); el "tick" de animación se deriva del reloj del sistema en vez de un
 * contador de instancia (sin estado compartido entre jugadores).
 *
 * "moving"/"hasLanded" llegan desde ActiveCosmeticsTask (que sí mantiene
 * estado por jugador: última posición conocida y transición aire->suelo) -
 * Player#getVelocity() no sirve para detectar caminar/correr (Bukkit solo la
 * llena con física real - retroceso, saltos - no con el movimiento normal
 * dirigido por el cliente), así que esa detección se movió ahí.
 */
public class TrailManager {

    public void drawTrail(Player player, CosmeticItem item, boolean hasLanded, boolean moving) {
        String style = item.value == null ? "" : item.value.toUpperCase();
        Location location = player.getLocation();
        long tick = System.currentTimeMillis() / 50;

        switch (style) {
            case "ORBIT" -> drawOrbit(location, item, tick);
            case "SPARKS" -> drawSparks(location, item, tick, moving);
            case "PUDDLE" -> drawPuddle(location, item, tick, moving);
            case "SPIRAL" -> drawSpiral(location, item, tick);
            case "RINGS" -> drawRings(location, item, tick, hasLanded);
            case "HALO" -> drawHalo(location, item, tick);
            case "CHAOS" -> drawChaos(location, item);
            case "TORNADO" -> drawTornado(location, item, tick);
            case "WAVES" -> drawWaves(location, item, tick, moving);
            case "POLYGON" -> drawPolygon(location, item, tick);
            case "SPHERE" -> drawSphere(location, item, tick);
            default -> drawSteps(location, item, tick, moving);
        }
    }

    private void drawSteps(Location location, CosmeticItem item, long tick, boolean moving) {
        if (!moving) {
            drawIdlePulse(location, item, tick);
            return;
        }
        double yawRad = Math.toRadians(location.getYaw());
        boolean rightFoot = (tick % 4) < 2;
        double offsetX = Math.cos(yawRad) * (rightFoot ? 0.25 : -0.25);
        double offsetZ = Math.sin(yawRad) * (rightFoot ? 0.25 : -0.25);
        EffectUtils.spawnParticle(location.clone().add(offsetX, 0.1, offsetZ), item, Particle.CLOUD, 1, 0, 0, 0, 0);
    }

    /**
     * Versión tenue y de baja frecuencia (~1 vez por segundo) de un estilo
     * que normalmente solo se ve al caminar/saltar - así un cosmético
     * equipado nunca se ve "roto" (invisible) al estar parado, aunque la
     * diferencia con caminar/aterrizar siga siendo clara. Reutilizada por
     * STEPS/SPARKS/PUDDLE/WAVES/RINGS.
     */
    private void drawIdlePulse(Location location, CosmeticItem item, long tick) {
        if (tick % 20 >= 3) return;
        EffectUtils.spawnParticle(location.clone().add(0, 0.1, 0), item, Particle.CLOUD, 1, 0, 0, 0, 0);
    }

    private void drawOrbit(Location location, CosmeticItem item, long tick) {
        double radius = item.radius > 0 ? item.radius : 0.8;
        double angle = tick * 0.4;
        EffectUtils.spawnParticle(location.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius), item, Particle.CLOUD, 1, 0, 0, 0, 0);
        EffectUtils.spawnParticle(location.clone().add(-Math.cos(angle) * radius, 0.1, -Math.sin(angle) * radius), item, Particle.CLOUD, 1, 0, 0, 0, 0);
    }

    private void drawSparks(Location location, CosmeticItem item, long tick, boolean moving) {
        if (!moving) {
            drawIdlePulse(location, item, tick);
            return;
        }
        double x = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6;
        double z = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6;
        EffectUtils.spawnParticle(location.clone().add(x, 0.1, z), item, Particle.CLOUD, 1, 0, 0.1, 0, 0.1);
    }

    private void drawPuddle(Location location, CosmeticItem item, long tick, boolean moving) {
        if (!moving) {
            drawIdlePulse(location, item, tick);
            return;
        }
        for (int i = 0; i < 2; i++) {
            double x = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.8;
            double z = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.8;
            EffectUtils.spawnParticle(location.clone().add(x, 0, z), item, Particle.CLOUD, 1, 0, 0, 0, 0);
        }
    }

    private void drawSpiral(Location location, CosmeticItem item, long tick) {
        double spiralY = (tick % 20) * 0.08;
        double angle = tick * 0.5;
        EffectUtils.spawnParticle(location.clone().add(Math.cos(angle) * 0.5, spiralY, Math.sin(angle) * 0.5), item, Particle.CLOUD, 1, 0, 0, 0, 0);
    }

    private void drawRings(Location location, CosmeticItem item, long tick, boolean hasLanded) {
        if (!hasLanded) {
            drawIdlePulse(location, item, tick);
            return;
        }
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            EffectUtils.spawnParticle(location.clone().add(Math.cos(angle) * 0.6, 0.05, Math.sin(angle) * 0.6), item, Particle.CLOUD, 1, 0, 0, 0, 0);
        }
    }

    private void drawHalo(Location location, CosmeticItem item, long tick) {
        for (int i = 0; i < 8; i++) {
            double angle = i * (Math.PI / 4) + tick * 0.1;
            EffectUtils.spawnParticle(location.clone().add(Math.cos(angle) * 0.5, 0.2, Math.sin(angle) * 0.5), item, Particle.CLOUD, 1, 0, 0, 0, 0);
        }
    }

    private void drawChaos(Location location, CosmeticItem item) {
        double x = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
        double y = ThreadLocalRandom.current().nextDouble();
        double z = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
        EffectUtils.spawnParticle(location.clone().add(x, y, z), item, Particle.CLOUD, 1, 0, 0, 0, 0);
    }

    private void drawTornado(Location location, CosmeticItem item, long tick) {
        for (int i = 1; i <= 5; i++) {
            double angle = tick * 0.3 + i;
            EffectUtils.spawnParticle(location.clone().add(Math.cos(angle) * (0.1 * i), i * 0.2, Math.sin(angle) * (0.1 * i)), item, Particle.CLOUD, 1, 0, 0, 0, 0);
        }
    }

    private void drawWaves(Location location, CosmeticItem item, long tick, boolean moving) {
        if (!moving) {
            drawIdlePulse(location, item, tick);
            return;
        }
        double wave = Math.sin(tick * 0.5) * 0.8;
        double yawRad = Math.toRadians(location.getYaw());
        double x = Math.cos(yawRad + Math.PI / 2) * -0.5 + Math.cos(yawRad) * wave;
        double z = Math.sin(yawRad + Math.PI / 2) * -0.5 + Math.sin(yawRad) * wave;
        EffectUtils.spawnParticle(location.clone().add(x, 0, z), item, Particle.CLOUD, 1, 0, 0, 0, 0);
    }

    /** Polígono regular de N lados (item.pointCount) girando lentamente alrededor de los pies - forma geométrica genérica, no un círculo. */
    private void drawPolygon(Location location, CosmeticItem item, long tick) {
        int sides = Math.max(3, EffectUtils.resolvePointCount(item, 5));
        double radius = item.radius > 0 ? item.radius : 0.6;
        double rotation = tick * 0.05;
        int pointsPerEdge = 4;

        for (int side = 0; side < sides; side++) {
            double angleA = rotation + side * (2 * Math.PI / sides);
            double angleB = rotation + (side + 1) * (2 * Math.PI / sides);
            double ax = Math.cos(angleA) * radius;
            double az = Math.sin(angleA) * radius;
            double bx = Math.cos(angleB) * radius;
            double bz = Math.sin(angleB) * radius;

            for (int p = 0; p < pointsPerEdge; p++) {
                double t = p / (double) pointsPerEdge;
                double x = ax + (bx - ax) * t;
                double z = az + (bz - az) * t;
                EffectUtils.spawnParticle(location.clone().add(x, 0.1, z), item, Particle.CLOUD, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Nube de puntos repartidos en la superficie de una esfera (distribución de Fibonacci, no filas/columnas) flotando sobre los pies - forma 3D genérica, distinta de todo lo demás (plano en el suelo). */
    private void drawSphere(Location location, CosmeticItem item, long tick) {
        double radius = item.radius > 0 ? item.radius : 0.4;
        int points = EffectUtils.resolvePointCount(item, 10);
        double rotation = tick * 0.15;

        for (int i = 0; i < points; i++) {
            double phi = Math.acos(1 - 2.0 * (i + 0.5) / points);
            double theta = Math.PI * (1 + Math.sqrt(5)) * (i + 0.5) + rotation;

            double x = radius * Math.cos(theta) * Math.sin(phi);
            double y = radius * Math.cos(phi) + 0.5;
            double z = radius * Math.sin(theta) * Math.sin(phi);
            EffectUtils.spawnParticle(location.clone().add(x, y, z), item, Particle.CLOUD, 1, 0, 0, 0, 0);
        }
    }
}
