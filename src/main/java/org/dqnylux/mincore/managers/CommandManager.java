package org.dqnylux.mincore.managers;

import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.commands.MincoreCommand;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

public class CommandManager {

    private final Lamp<BukkitCommandActor> lamp;

    public CommandManager(Mincore plugin) {
        this.lamp = BukkitLamp.builder(plugin).build();
        this.lamp.register(new MincoreCommand(plugin));
    }

    public Lamp<BukkitCommandActor> getLamp() {
        return lamp;
    }
}