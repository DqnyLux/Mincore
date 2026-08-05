package org.dqnylux.mincore.managers;

import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.commands.CosmeticsCommand;
import org.dqnylux.mincore.commands.MessageCommand;
import org.dqnylux.mincore.commands.MincoreCommand;
import org.dqnylux.mincore.commands.MiscCommand;
import org.dqnylux.mincore.commands.PreviewZoneCommand;
import org.dqnylux.mincore.commands.StaffCommand;
import org.dqnylux.mincore.commands.TrackCommand;
import org.dqnylux.mincore.config.MainConfig;
import org.dqnylux.mincore.config.StaffConfig;
import org.dqnylux.mincore.pozomillonario.commands.PozoCommand;
import revxrsal.commands.Lamp;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.dynamic.Annotations;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

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

    private final Lamp<BukkitCommandActor> lamp;

    public CommandManager(Mincore plugin) {
        Map<String, String> rootAliases = buildRootAliases(plugin.getConfigManager().getMainConfig().commands);
        // Los @CommandPermission de los comandos ya llevan escrito el nodo
        // default como literal (ej. "coreec.staff.freeze") porque la
        // anotación exige una constante de compilación - este mapa lo
        // sustituye por el valor REAL configurado en config.yml/staff.yml al
        // registrar los comandos, igual que rootAliases hace con los nombres.
        Map<String, String> permissionAliases = buildPermissionAliases(
                plugin.getConfigManager().getMainConfig().permissions,
                plugin.getConfigManager().getStaffConfig().permissions);

        this.lamp = BukkitLamp.builder(plugin)
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

        this.lamp.register(new MincoreCommand(plugin));
        this.lamp.register(new MessageCommand(plugin));
        this.lamp.register(new CosmeticsCommand(plugin));
        this.lamp.register(new TrackCommand(plugin));
        this.lamp.register(new MiscCommand(plugin));
        this.lamp.register(new PreviewZoneCommand(plugin));
        this.lamp.register(new StaffCommand(plugin));
        this.lamp.register(new PozoCommand(plugin));
        this.lamp.register(new org.dqnylux.mincore.sanctions.commands.SanctionCommand(plugin));
        this.lamp.register(new org.dqnylux.mincore.reports.commands.ReportsCommand(plugin));
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
        aliases.put("msg", commands.message);
        aliases.put("reply", commands.reply);
        aliases.put("trackcore", commands.track);
        aliases.put("staffmode", commands.staffMode);
        aliases.put("vanish", commands.vanish);
        aliases.put("helpop", commands.helpOp);
        aliases.put("report", commands.report);
        aliases.put("invsee", commands.invsee);
        aliases.put("enderchest", commands.enderchest);
        return aliases;
    }

    private Map<String, String> buildPermissionAliases(MainConfig.Permissions permissions, StaffConfig.Permissions staffPermissions) {
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
        return aliases;
    }

    public Lamp<BukkitCommandActor> getLamp() {
        return lamp;
    }
}