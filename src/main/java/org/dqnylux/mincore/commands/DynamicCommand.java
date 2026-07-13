package org.dqnylux.mincore.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.utils.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Comando definido enteramente desde bots.yml (sección 12 del prompt) - no
 * existe como clase Java propia porque su nombre/alias/respuesta son datos de
 * configuración, no algo conocido en tiempo de compilación. Se registra
 * directamente en el CommandMap de Bukkit (ver DynamicCommandManager) porque
 * Lamp solo descubre comandos vía anotaciones sobre clases ya compiladas.
 */
public class DynamicCommand extends Command {

    private final List<String> response;

    public DynamicCommand(String name, List<String> aliases, List<String> response) {
        super(name);
        this.response = response;
        setAliases(aliases);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        for (String line : response) {
            Component formatted = sender instanceof Player player ? TextUtils.format(player, line) : TextUtils.format(line);
            sender.sendMessage(formatted);
        }
        return true;
    }
}
