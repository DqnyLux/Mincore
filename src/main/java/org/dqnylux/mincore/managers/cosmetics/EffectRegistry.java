package org.dqnylux.mincore.managers.cosmetics;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.managers.cosmetics.api.CosmeticEffect;
import org.dqnylux.mincore.managers.cosmetics.api.ElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.api.ProjectileEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.AbyssFangsEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.AllaysEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.AuraFarmingEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.BlackHoleProjectileEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.BurstEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.ChickensEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.CloudBurstElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.CloudTrailElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.CometTailElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.CryoCoreEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.AshesEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.CrystalShatterEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.DimensionalRiftEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.EnchantColumnEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.ExplosionEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.FallingBeamEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.FireworksEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.FixedEmitterElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.FlameRingEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.GenericTrailProjectileEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.GiantTotemEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.GlowMissileEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.GraveEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.GuardianEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.HeartBurstEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.HelixProjectileEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.IcewalkerEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.LightningEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.LightningWingsElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.LineElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.MatrixCodeElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.MeteorsEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.OrbitEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.PandasEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.PigTornadoEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.PolygonElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.PrismaticNovaEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.ProjectileTrailEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.PulsingBurstEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.RainbowRibbonElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.RainbowTrailProjectileEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.RingElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.RisingCloudEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.ScaleTransformEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.ShockwaveEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.SkeletonArmorEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.SmokeTrailProjectileEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.SniperEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.SnowVortexElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.StarShowerEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.StellarCollapseEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.SymphonyEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.VoidLotusEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.VolcanoEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.WardenEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.WingElytraEffect;
import org.dqnylux.mincore.managers.cosmetics.effects.WorldOrbitElytraEffect;
import org.dqnylux.mincore.utils.WorldValidator;

import java.util.HashMap;
import java.util.Map;

/**
 * Despacha por ID (case-insensitive), sin manejo de excepciones alrededor
 * de play() - un efecto roto se propaga hasta el handler del evento que lo
 * disparó, igual que en el prompt original (sección 13.1).
 *
 * El catálogo de v1 (~90 clases, una por cosmético) se colapsó a un puñado de
 * motores genéricos config-driven (sección 17, cero hardcodeo) - decenas de
 * cosméticos de v1 comparten literalmente el mismo algoritmo visual y solo
 * cambiaban partícula/color/sonido/radio en Java; acá esos valores viven en
 * el CosmeticItem de cada uno en YAML, no en una clase Java por cosmético.
 */
public class EffectRegistry {

    private final Map<String, CosmeticEffect> cosmeticEffects = new HashMap<>();
    private final Map<String, ElytraEffect> elytraEffects = new HashMap<>();
    private final Map<String, ProjectileEffect> projectileEffects = new HashMap<>();

    public EffectRegistry() {
        register(new AbyssFangsEffect());
        register(new HeartBurstEffect());
        register(new FlameRingEffect());
        register(new BurstEffect());
        register(new PulsingBurstEffect());
        register(new OrbitEffect());
        register(new FallingBeamEffect());
        register(new VolcanoEffect());
        register(new CryoCoreEffect());
        register(new GraveEffect());
        register(new LightningEffect());
        register(new ScaleTransformEffect());
        register(new SkeletonArmorEffect());
        register(new ExplosionEffect());
        register(new DimensionalRiftEffect());
        register(new EnchantColumnEffect());
        register(new AuraFarmingEffect());
        register(new FireworksEffect());
        register(new IcewalkerEffect());
        register(new MeteorsEffect());
        register(new StarShowerEffect());
        register(new SymphonyEffect());
        register(new AllaysEffect());
        register(new GuardianEffect());
        register(new PigTornadoEffect());
        register(new WardenEffect());
        register(new PandasEffect());
        register(new ChickensEffect());
        register(new VoidLotusEffect());
        register(new ShockwaveEffect());
        register(new PrismaticNovaEffect());
        register(new SniperEffect());
        register(new StellarCollapseEffect());
        register(new GlowMissileEffect());
        register(new RisingCloudEffect());
        register(new CrystalShatterEffect());
        register(new GiantTotemEffect());
        register(new AshesEffect());

        register(new MatrixCodeElytraEffect());
        register(new CloudTrailElytraEffect());
        register(new RingElytraEffect());
        register(new LineElytraEffect());
        register(new WingElytraEffect());
        register(new CloudBurstElytraEffect());
        register(new FixedEmitterElytraEffect());
        register(new WorldOrbitElytraEffect());
        register(new RainbowRibbonElytraEffect());
        register(new PolygonElytraEffect());
        register(new LightningWingsElytraEffect());
        register(new CometTailElytraEffect());
        register(new SnowVortexElytraEffect());

        register(new ProjectileTrailEffect());
        register(new SmokeTrailProjectileEffect());
        register(new GenericTrailProjectileEffect());
        register(new HelixProjectileEffect());
        register(new BlackHoleProjectileEffect());
        register(new RainbowTrailProjectileEffect());
    }

    public void register(CosmeticEffect effect) {
        cosmeticEffects.put(effect.getId().toLowerCase(), effect);
    }

    public void register(ElytraEffect effect) {
        elytraEffects.put(effect.getId().toLowerCase(), effect);
    }

    public void register(ProjectileEffect effect) {
        projectileEffects.put(effect.getId().toLowerCase(), effect);
    }

    public void playEffect(Mincore plugin, Player player, Location location, CosmeticItem item) {
        if (item == null || item.effectType == null) return;
        if (!WorldValidator.isAllowed(plugin, player.getWorld())) return;
        if (CosmeticVisibility.isHiddenFromOthers(plugin, player)) return;
        CosmeticEffect effect = cosmeticEffects.get(item.effectType.toLowerCase());
        if (effect == null) return;
        effect.play(plugin, player, location, item);
    }

    public void playElytraEffect(Mincore plugin, Player player, CosmeticItem item) {
        if (item == null || item.effectType == null) return;
        if (!WorldValidator.isAllowed(plugin, player.getWorld())) return;
        if (CosmeticVisibility.isHiddenFromOthers(plugin, player)) return;
        ElytraEffect effect = elytraEffects.get(item.effectType.toLowerCase());
        if (effect != null) effect.play(plugin, player, item);
    }

    public void playProjectileEffect(Mincore plugin, Projectile projectile, CosmeticItem item) {
        if (item == null || item.effectType == null) return;
        if (!WorldValidator.isAllowed(plugin, projectile.getWorld())) return;
        if (projectile.getShooter() instanceof Player shooter && CosmeticVisibility.isHiddenFromOthers(plugin, shooter)) return;
        ProjectileEffect effect = projectileEffects.get(item.effectType.toLowerCase());
        if (effect != null) effect.play(plugin, projectile, item);
    }
}
