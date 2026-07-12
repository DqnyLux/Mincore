package org.dqnylux.mincore.managers;

import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.commands.CosmeticsCommand;
import org.dqnylux.mincore.commands.MessageCommand;
import org.dqnylux.mincore.commands.MincoreCommand;
import org.dqnylux.mincore.commands.MiscCommand;
import org.dqnylux.mincore.commands.TrackCommand;
import org.dqnylux.mincore.config.MainConfig;
import revxrsal.commands.Lamp;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.dynamic.Annotations;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

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
                .build();

        this.lamp.register(new MincoreCommand(plugin));
        this.lamp.register(new MessageCommand(plugin));
        this.lamp.register(new CosmeticsCommand(plugin));
        this.lamp.register(new TrackCommand(plugin));
        this.lamp.register(new MiscCommand(plugin));
    }

    private Map<String, String> buildRootAliases(MainConfig.Commands commands) {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("mincore", commands.mincore);
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
        return aliases;
    }

    public Lamp<BukkitCommandActor> getLamp() {
        return lamp;
    }
}