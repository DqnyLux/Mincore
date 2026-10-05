package org.dqnylux.mincore.managers;

import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.commands.CosmeticsCommand;
import org.dqnylux.mincore.commands.EconomyCommand;
import org.dqnylux.mincore.commands.EssentialsCommand;
import org.dqnylux.mincore.commands.MessageCommand;
import org.dqnylux.mincore.commands.MincoreCommand;
import org.dqnylux.mincore.commands.MiscCommand;
import org.dqnylux.mincore.commands.PreviewZoneCommand;
import org.dqnylux.mincore.commands.StaffCommand;
import org.dqnylux.mincore.commands.SucreCommand;
import org.dqnylux.mincore.commands.TrackCommand;
import org.dqnylux.mincore.config.AfkConfig;
import org.dqnylux.mincore.config.EssentialsConfig;
import org.dqnylux.mincore.config.HomesConfig;
import org.dqnylux.mincore.config.MainConfig;
import org.dqnylux.mincore.config.StaffConfig;
import org.dqnylux.mincore.config.WarpsConfig;
import org.dqnylux.mincore.homes.commands.HomesCommand;
import org.dqnylux.mincore.pozomillonario.commands.PozoCommand;
import org.dqnylux.mincore.warps.commands.WarpsCommand;
import org.dqnylux.mincore.afk.commands.AfkCommand;
import revxrsal.commands.Lamp;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.dynamic.Annotations;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Los nombres de comando "raíz" (mincore, fly, gmc...) son configurables desde
 * config.yml (sección 17 del prompt de reconstrucción) sin tocar las clases de
 * comando: cada @Command sigue anotado con su nombre canónico en Java, y este
 * AnnotationReplacer sustituye solo el primer token del path por el nombre
 * configurado antes de que Lamp registre el comando.
 */
public class CommandManager {

    private final Mincore plugin;
    private Lamp<BukkitCommandActor> lamp;

    public CommandManager(Mincore plugin) {
        this.plugin = plugin;
        this.lamp = buildLamp();
        registerCommands();
    }

    public void reload() {
        // En Paper con Brigadier/LifecycleEventManager no es posible recrear Lamp
        // en runtime sin provocar IllegalStateException en PaperLifecycleEventManager.
        // Las configuraciones de mensajes y módulos se leen dinámicamente en cada ejecución.
    }

    private Lamp<BukkitCommandActor> buildLamp() {
        Map<String, String> rootAliases = buildRootAliases(plugin.getConfigManager().getMainConfig().commands);
        Map<String, String> permissionAliases = buildPermissionAliases(
                plugin.getConfigManager().getMainConfig().permissions,
                plugin.getConfigManager().getStaffConfig().permissions,
                plugin.getConfigManager().getEssentialsConfig().permissions,
                plugin.getConfigManager().getHomesConfig().permissions,
                plugin.getConfigManager().getWarpsConfig().permissions,
                plugin.getConfigManager().getAfkConfig().permissions);

        return BukkitLamp.builder(plugin)
                .annotationReplacer(Command.class, (element, original) -> {
                    String[] value = original.value();
                    String[] replaced = new String[value.length];
                    for (int i = 0; i < value.length; i++) {
                        String[] tokens = value[i].split(" ", 2);
                        String root = rootAliases.getOrDefault(tokens[0], tokens[0]);
                        replaced[i] = tokens.length > 1 ? root + " " + tokens[1] : root;
                    }
                    return java.util.List.of(Annotations.create(Command.class, Map.of("value", (Object) replaced)));
                })
                .annotationReplacer(CommandPermission.class, (element, original) -> {
                    String resolved = permissionAliases.getOrDefault(original.value(), original.value());
                    return java.util.List.of(Annotations.create(CommandPermission.class, Map.of("value", (Object) resolved)));
                })
                .exceptionHandler(new MincoreExceptionHandler(plugin))
                .build();
    }

    private void registerCommands() {
        this.lamp.register(new MincoreCommand(plugin));
        this.lamp.register(new MessageCommand(plugin));
        this.lamp.register(new CosmeticsCommand(plugin));
        this.lamp.register(new TrackCommand(plugin));
        this.lamp.register(new MiscCommand(plugin));
        this.lamp.register(new EconomyCommand(plugin));
        this.lamp.register(new SucreCommand(plugin));
        this.lamp.register(new PreviewZoneCommand(plugin));
        this.lamp.register(new StaffCommand(plugin));
        this.lamp.register(new PozoCommand(plugin));
        this.lamp.register(new org.dqnylux.mincore.sanctions.commands.SanctionCommand(plugin));
        this.lamp.register(new org.dqnylux.mincore.reports.commands.ReportsCommand(plugin));
        this.lamp.register(new org.dqnylux.mincore.skins.commands.SkinCommand(plugin));
        this.lamp.register(new org.dqnylux.mincore.commands.SkinsPreviewZoneCommand(plugin));
        this.lamp.register(new org.dqnylux.mincore.commands.chat.DeleteMessageCommand(plugin));

        if (plugin.getConfigManager().getModulesConfig().essentials) {
            this.lamp.register(new EssentialsCommand(plugin));
        }
        if (plugin.getConfigManager().getModulesConfig().homes) {
            this.lamp.register(new HomesCommand(plugin));
        }
        if (plugin.getConfigManager().getModulesConfig().warps) {
            this.lamp.register(new WarpsCommand(plugin));
        }
        if (plugin.getConfigManager().getModulesConfig().afk) {
            this.lamp.register(new AfkCommand(plugin));
        }
        if (plugin.getConfigManager().getModulesConfig().rewards) {
            this.lamp.register(new org.dqnylux.mincore.rewards.commands.RewardsCommand(plugin));
        }
        if (plugin.getConfigManager().getModulesConfig().vaults) {
            this.lamp.register(new org.dqnylux.mincore.vaults.commands.VaultCommand(plugin));
            this.lamp.register(new org.dqnylux.mincore.vaults.commands.VaultAdminCommand(plugin));
        }
        if (plugin.getConfigManager().getModulesConfig().flytime) {
            this.lamp.register(new org.dqnylux.mincore.flytime.commands.FlyTimeCommand(plugin));
        }
    }

    private Map<String, String> buildRootAliases(MainConfig.Commands commands) {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("coreec", commands.mincore);
        aliases.put("fly", commands.fly);
        aliases.put("gmc", commands.gamemodeCreative);
        aliases.put("gms", commands.gamemodeSurvival);
        aliases.put("gma", commands.gamemodeAdventure);
        aliases.put("gmsp", commands.gamemodeSpectator);
        aliases.put("cosmetics", commands.cosmetics);
        aliases.put("perfil", commands.profile);
        aliases.put("ajustes", commands.settings);
        aliases.put("msg", commands.message);
        aliases.put("reply", commands.reply);
        aliases.put("trackcore", commands.track);
        aliases.put("staffmode", commands.staffMode);
        aliases.put("vanish", commands.vanish);
        aliases.put("helpop", commands.helpOp);
        aliases.put("report", commands.report);
        aliases.put("invsee", commands.invsee);
        aliases.put("enderchest", commands.enderchest);
        aliases.put("afk", commands.afk);
        aliases.put("balance", commands.balance);
        aliases.put("pay", commands.pay);
        aliases.put("eco", commands.eco);
        aliases.put("sucre", commands.sucre);
        aliases.put("skin", commands.skin);
        return aliases;
    }

    private Map<String, String> buildPermissionAliases(MainConfig.Permissions permissions, StaffConfig.Permissions staffPermissions, EssentialsConfig.Permissions essentialsPermissions, HomesConfig.Permissions homesPermissions, WarpsConfig.Permissions warpsPermissions, AfkConfig.Permissions afkPermissions) {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("coreec.admin", permissions.admin);
        aliases.put("coreec.admin.clearchat", permissions.clearchat);
        aliases.put("coreec.command.fly", permissions.fly);
        aliases.put("coreec.command.gamemode", permissions.gamemode);
        aliases.put("coreec.staff.mode", staffPermissions.staffMode);
        aliases.put("coreec.staff.vanish", staffPermissions.vanish);
        aliases.put("coreec.staff.freeze", staffPermissions.freeze);
        aliases.put("coreec.staff.chat", staffPermissions.staffChat);
        aliases.put("coreec.staff.disguise", staffPermissions.disguise);
        aliases.put("coreec.staff.realname", staffPermissions.realName);
        aliases.put("coreec.essentials.air", essentialsPermissions.air);
        aliases.put("coreec.essentials.broadcast", essentialsPermissions.broadcast);
        aliases.put("coreec.essentials.condense", essentialsPermissions.condense);
        aliases.put("coreec.essentials.disposal", essentialsPermissions.disposal);
        aliases.put("coreec.essentials.enchant", essentialsPermissions.enchant);
        aliases.put("coreec.essentials.exp", essentialsPermissions.exp);
        aliases.put("coreec.essentials.firetick", essentialsPermissions.firetick);
        aliases.put("coreec.essentials.flyspeed", essentialsPermissions.flyspeed);
        aliases.put("coreec.essentials.feed", essentialsPermissions.feed);
        aliases.put("coreec.essentials.forcerun", essentialsPermissions.forcerun);
        aliases.put("coreec.essentials.forcesay", essentialsPermissions.forcesay);
        aliases.put("coreec.essentials.hat", essentialsPermissions.hat);
        aliases.put("coreec.essentials.heal", essentialsPermissions.heal);
        aliases.put("coreec.essentials.god", essentialsPermissions.god);
        aliases.put("coreec.essentials.near", essentialsPermissions.near);
        aliases.put("coreec.essentials.nick", essentialsPermissions.nick);
        aliases.put("coreec.essentials.playerinfo", essentialsPermissions.playerinfo);
        aliases.put("coreec.essentials.ptime", essentialsPermissions.ptime);
        aliases.put("coreec.essentials.skull", essentialsPermissions.skull);
        aliases.put("coreec.essentials.smite", essentialsPermissions.smite);
        aliases.put("coreec.essentials.spawnmob", essentialsPermissions.spawnmob);
        aliases.put("coreec.essentials.speed", essentialsPermissions.speed);
        aliases.put("coreec.essentials.staff", essentialsPermissions.staff);
        aliases.put("coreec.essentials.suicide", essentialsPermissions.suicide);
        aliases.put("coreec.essentials.tp", essentialsPermissions.tp);
        aliases.put("coreec.essentials.tphere", essentialsPermissions.tphere);
        aliases.put("coreec.essentials.time", essentialsPermissions.time);
        aliases.put("coreec.essentials.weather", essentialsPermissions.weather);
        aliases.put("coreec.essentials.spawn", essentialsPermissions.spawn);
        aliases.put("coreec.essentials.setspawn", essentialsPermissions.setspawn);
        aliases.put("coreec.essentials.workbench", essentialsPermissions.workbench);
        aliases.put("coreec.essentials.anvil", essentialsPermissions.anvil);
        aliases.put("coreec.essentials.enderchest", essentialsPermissions.enderchest);
        aliases.put("coreec.essentials.enderchest.other", essentialsPermissions.enderchestOther);
        aliases.put("coreec.essentials.invsee", essentialsPermissions.invsee);
        aliases.put("coreec.essentials.clearinventory", essentialsPermissions.clearinventory);
        aliases.put("coreec.essentials.clearinventory.other", essentialsPermissions.clearinventoryOther);
        aliases.put("coreec.essentials.repair", essentialsPermissions.repair);
        aliases.put("coreec.essentials.repair.all", essentialsPermissions.repairAll);
        aliases.put("coreec.essentials.cartographytable", essentialsPermissions.cartographytable);
        aliases.put("coreec.essentials.grindstone", essentialsPermissions.grindstone);
        aliases.put("coreec.essentials.loom", essentialsPermissions.loom);
        aliases.put("coreec.essentials.smithingtable", essentialsPermissions.smithingtable);
        aliases.put("coreec.essentials.stonecutter", essentialsPermissions.stonecutter);
        aliases.put("coreec.essentials.top", essentialsPermissions.top);
        aliases.put("coreec.essentials.give", essentialsPermissions.give);
        aliases.put("coreec.essentials.item", essentialsPermissions.item);
        aliases.put("coreec.essentials.gamemode", essentialsPermissions.gamemode);
        aliases.put("coreec.essentials.gamemode.other", essentialsPermissions.gamemodeOther);
        aliases.put("coreec.essentials.gamemode.survival", essentialsPermissions.gms);
        aliases.put("coreec.essentials.gamemode.creative", essentialsPermissions.gmc);
        aliases.put("coreec.essentials.gamemode.adventure", essentialsPermissions.gma);
        aliases.put("coreec.essentials.gamemode.spectator", essentialsPermissions.gmsp);
        aliases.put("coreec.essentials.more", essentialsPermissions.more);
        aliases.put("coreec.essentials.ping", essentialsPermissions.ping);
        aliases.put("coreec.essentials.ping.other", essentialsPermissions.pingOther);
        aliases.put("coreec.essentials.walkspeed", essentialsPermissions.walkspeed);
        aliases.put("coreec.essentials.walkspeed.other", essentialsPermissions.walkspeedOther);
        aliases.put("coreec.essentials.rename", essentialsPermissions.rename);
        aliases.put("coreec.essentials.lore", essentialsPermissions.lore);
        aliases.put("coreec.essentials.burn", essentialsPermissions.burn);
        aliases.put("coreec.essentials.extinguish", essentialsPermissions.extinguish);
        aliases.put("coreec.essentials.extinguish.other", essentialsPermissions.extinguishOther);
        aliases.put("coreec.essentials.sudo", essentialsPermissions.sudo);
        aliases.put("coreec.essentials.kill", essentialsPermissions.kill);
        aliases.put("coreec.essentials.kill.other", essentialsPermissions.killOther);
        aliases.put("coreec.essentials.back", essentialsPermissions.back);
        aliases.put("coreec.homes.sethome", homesPermissions.sethome);
        aliases.put("coreec.homes.home", homesPermissions.home);
        aliases.put("coreec.homes.delhome", homesPermissions.delhome);
        aliases.put("coreec.homes.homes", homesPermissions.homes);
        aliases.put("coreec.homes.admin", homesPermissions.admin);
        aliases.put("coreec.warps.warp", warpsPermissions.warp);
        aliases.put("coreec.warps.setwarp", warpsPermissions.setwarp);
        aliases.put("coreec.warps.delwarp", warpsPermissions.delwarp);
        aliases.put("coreec.warps.warps", warpsPermissions.warps);
        aliases.put("coreec.warps.admin", warpsPermissions.admin);
        aliases.put("coreec.afk.use", afkPermissions.afk);
        aliases.put("coreec.afk.reason", afkPermissions.reason);
        aliases.put("coreec.afk.admin", afkPermissions.admin);
        aliases.put("coreec.afk.check", afkPermissions.check);
        aliases.put("coreec.afk.bypass.auto", afkPermissions.bypassAuto);
        aliases.put("coreec.afk.bypass.kick", afkPermissions.bypassKick);
        aliases.put("coreec.command.skin", permissions.skin);
        return aliases;
    }

    public Lamp<BukkitCommandActor> getLamp() {
        return lamp;
    }
}