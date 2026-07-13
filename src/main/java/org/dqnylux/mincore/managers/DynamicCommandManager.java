package org.dqnylux.mincore.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.commands.DynamicCommand;
import org.dqnylux.mincore.config.BotsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registra los comandos de bots.yml (custom-commands) directamente en el
 * CommandMap de Bukkit vía reflexión (CraftServer#getCommandMap es público
 * en la implementación aunque no forma parte de la interfaz Server) - el
 * mismo mecanismo que usaba el plugin original para TODOS sus comandos
 * (sección 6), aquí acotado solo a los dinámicos porque Lamp no soporta
 * registrar comandos cuyo nombre se define en tiempo de ejecución.
 */
public class DynamicCommandManager {

    private final Mincore plugin;
    private final CommandMap commandMap;
    private final List<Command> registered = new ArrayList<>();

    public DynamicCommandManager(Mincore plugin) {
        this.plugin = plugin;
        this.commandMap = resolveCommandMap();
    }

    private CommandMap resolveCommandMap() {
        try {
            var method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().severe("[Mincore] No se pudo acceder al CommandMap para los comandos dinámicos: " + e.getMessage());
            return null;
        }
    }

    public void reload() {
        unregisterAll();
        if (commandMap == null) return;

        BotsConfig bots = plugin.getConfigManager().getBotsConfig();
        for (Map.Entry<String, BotsConfig.CustomCommand> entry : bots.customCommands.entrySet()) {
            BotsConfig.CustomCommand config = entry.getValue();
            if (!config.enabled || config.aliases.isEmpty()) continue;

            String primary = config.aliases.get(0);
            List<String> extraAliases = config.aliases.size() > 1
                    ? config.aliases.subList(1, config.aliases.size())
                    : List.of();

            DynamicCommand command = new DynamicCommand(primary, extraAliases, config.response);
            commandMap.register("mincore", command);
            registered.add(command);
        }
    }

    public void unregisterAll() {
        for (Command command : registered) {
            command.unregister(commandMap);
        }
        registered.clear();
    }
}
