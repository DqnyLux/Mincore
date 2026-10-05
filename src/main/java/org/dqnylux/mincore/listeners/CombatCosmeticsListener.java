package org.dqnylux.mincore.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.managers.cosmetics.EffectUtils;
import org.dqnylux.mincore.managers.cosmetics.effects.GraveEffect;
import org.dqnylux.mincore.model.PlayerData;

/** Separado de DeathListener: este solo dispara las partículas/efectos visuales, no el mensaje ni el rastreo. */
public class CombatCosmeticsListener implements Listener {

    private final Mincore plugin;

    public CombatCosmeticsListener(Mincore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        playCategoryEffect(shooter, "projectile-effects", null, event.getEntity());
    }

    /**
     * Marca el proyectil como "ya golpeó algo" - los efectos de trail
     * (ProjectileTrailEffect y hermanos) chequean esto cada tick para cortar
     * la estela apenas llega a destino. Sin esto, para la mayoría de
     * proyectiles daba igual (isValid()/isDead() cambian rápido al aterrizar),
     * pero un FishHook (caña de pescar) sigue vivo/válido flotando en el agua
     * mucho después del "impacto" - la estela seguía animando indefinidamente.
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        EffectUtils.markProjectileHit(event.getEntity());
    }

    /**
     * BUG DE FARMING encontrado: la valla/cartel/cabeza/decoración de
     * GraveEffect son bloques reales, restaurados recién a los 5s - sin
     * esto, cualquiera podía picarlos ANTES de que se restauraran y
     * quedarse con cabezas de jugador (normalmente solo se consiguen
     * matando un Wither) y bloques gratis, repitiendo indefinidamente.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakGraveBlock(BlockBreakEvent event) {
        if (GraveEffect.isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        if (killer != null) {
            playCategoryEffect(killer, "kill-effects", victim.getLocation(), null);
        }
        playCategoryEffect(victim, "death-effects", victim.getLocation(), null);
    }

    private void playCategoryEffect(Player player, String category, Location location, org.bukkit.entity.Projectile projectile) {
        if (player == null || org.dqnylux.mincore.managers.cosmetics.CosmeticVisibility.isHiddenFromOthers(plugin, player)) return;
        PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
        if (data == null) return;

        String effectId = data.getActiveCosmetic(category);
        if (effectId == null) return;

        CosmeticItem item = plugin.getCosmeticConfigManager().getItem(category, effectId);
        if (item == null) return;

        if (projectile != null) {
            plugin.getEffectRegistry().playProjectileEffect(plugin, projectile, item);
        } else {
            plugin.getEffectRegistry().playEffect(plugin, player, location, item);
        }
    }
}
