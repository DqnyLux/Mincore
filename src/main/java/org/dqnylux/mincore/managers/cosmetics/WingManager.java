package org.dqnylux.mincore.managers.cosmetics;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.config.models.WingCosmetic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderiza el layout ASCII-art de wings.yml - dos formas distintas según el
 * jugador esté en movimiento o quieto (sección 13.2 de v1): estela reforzada
 * (partícula+color+velocidad por símbolo, igual que el ParticleData original)
 * mientras se mueve, forma completa con aleteo senoidal + "respiración" al
 * estar quieto.
 */
public class WingManager {

    /**
     * "moving" lo calcula y pasa ActiveCosmeticsTask (comparando posición
     * real contra el tick anterior) - Player#getVelocity() NO sirve aquí,
     * mismo bug que ya se corrigió en TrailManager: Bukkit solo la actualiza
     * con retroceso/explosiones/física real, no con el caminar normal del
     * cliente, así que las alas casi nunca "encogían" a la estela reducida
     * al caminar/correr, solo al saltar - se veían siempre en su forma
     * completa tapando la pantalla.
     */
    public void render(Player player, WingCosmetic wings, boolean moving) {
        List<String> layout = wings.wings.layout;
        if (layout.isEmpty()) return;

        Location base = player.getLocation();
        double yawRad = Math.toRadians(base.getYaw());
        // +0.6 ancla el centro del ala en los hombros del jugador (igual que v1).
        double height = wings.wings.startVertical + 0.6;
        if (player.isSneaking()) height -= 0.3;

        if (moving) {
            renderMoving(wings, base, yawRad, height);
        } else {
            renderResting(wings, base, yawRad, height);
        }
    }

    /** Estela reforzada: un punto por símbolo distinto del layout (con su propio color/velocidad), sin dibujar la forma completa. */
    private void renderMoving(WingCosmetic wings, Location base, double yawRad, double height) {
        double pushBack = wings.wings.startDistanceToPlayer;
        Location point = base.clone().add(0, height, 0)
                .subtract(Math.cos(yawRad) * pushBack, 0, Math.sin(yawRad) * pushBack);

        for (WingCosmetic.WingParticleData data : distinctParticles(wings.wings.particleMap)) {
            spawnWingParticle(point, data, 2);
        }
    }

    /** Forma completa del ala (layout ASCII-art) con aleteo senoidal y balanceo de "respiración". */
    private void renderResting(WingCosmetic wings, Location base, double yawRad, double height) {
        double flap = Math.sin(System.currentTimeMillis() / 200.0 * wings.wings.flapSpeed) * Math.toRadians(wings.wings.maxAngle);
        double breathing = Math.sin(System.currentTimeMillis() / 300.0) * 0.05;
        double spacing = wings.wings.spacing;

        Location center = base.clone().add(0, height + breathing, 0);
        List<String> layout = wings.wings.layout;
        int rows = layout.size();

        for (int row = 0; row < rows; row++) {
            // El layout admite comas/espacios como separador visual entre columnas
            // (igual que wings.yml de v1, para poder alinear el ASCII-art en el
            // editor) - se ignoran, cada carácter restante es una columna.
            String line = layout.get(row).replace(",", "").replace(" ", "");
            for (int col = 0; col < line.length(); col++) {
                WingCosmetic.WingParticleData data = wings.wings.particleMap.get(String.valueOf(line.charAt(col)));
                if (data == null) continue;

                double drawY = (rows / 2.0 - row) * spacing;
                spawnWingPoint(center, data, col * spacing, drawY, -1, yawRad, flap);
                spawnWingPoint(center, data, col * spacing, drawY, 1, yawRad, flap);
            }
        }
    }

    private void spawnWingPoint(Location center, WingCosmetic.WingParticleData data, double x, double y, int side, double yawRad, double flap) {
        double sideYaw = yawRad + (side < 0 ? -flap : Math.PI + flap);
        Location point = center.clone().add(Math.cos(sideYaw) * x, y, Math.sin(sideYaw) * x);
        spawnWingParticle(point, data, 1);
    }

    private void spawnWingParticle(Location point, WingCosmetic.WingParticleData data, int count) {
        Particle particle = EffectUtils.parseParticle(data.particle, Particle.CLOUD);
        ParticleDisplay display = ParticleDisplay.of(particle).withCount(count).offset(0, 0, 0).withExtra(data.speed);

        if (particle == Particle.DUST && data.color != null && !data.color.isBlank()) {
            Color color = EffectUtils.parseColor(data.color, Color.WHITE);
            display.withColor(new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue()), 1.2f);
        }

        display.spawn(point);
    }

    private java.util.Collection<WingCosmetic.WingParticleData> distinctParticles(Map<String, WingCosmetic.WingParticleData> particleMap) {
        Map<String, WingCosmetic.WingParticleData> distinct = new LinkedHashMap<>();
        for (WingCosmetic.WingParticleData data : particleMap.values()) {
            distinct.putIfAbsent(data.particle, data);
        }
        return distinct.values();
    }
}
