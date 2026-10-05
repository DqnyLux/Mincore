package org.dqnylux.mincore.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.commands.BukkitCommandWrapper;
import org.dqnylux.mincore.commands.DynamicCommand;
import org.dqnylux.mincore.config.BotsConfig;
import org.dqnylux.mincore.profiles.command.ProfileCommand;
import org.dqnylux.mincore.timelimit.command.TimeLimitCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registra los comandos de bots.yml (custom-commands) y comandos de módulos
 * independientes directamente en el CommandMap de Bukkit vía reflexión.
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
            Bukkit.getLogger().severe("[CoreEC] No se pudo acceder al CommandMap para los comandos dinámicos: " + e.getMessage());
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
            commandMap.register("coreec", command);
            registered.add(command);
        }

        if (plugin.getConfigManager().getModulesConfig().profiles) {
            ProfileCommand profileExec = new ProfileCommand(plugin);
            String primary = plugin.getConfigManager().getMainConfig().commands.profile;
            if (primary == null || primary.isBlank()) primary = "perfil";
            List<String> aliases = List.of("profile", "perfiles", "profiles");
            BukkitCommandWrapper cmd = new BukkitCommandWrapper(primary, "Sistema interactivo de perfiles de jugador", "/" + primary, aliases, profileExec, profileExec);
            commandMap.register("coreec", cmd);
            registered.add(cmd);
        }

        if (plugin.getConfigManager().getModulesConfig().timelimit) {
            TimeLimitCommand timeLimitExec = new TimeLimitCommand(plugin);
            List<String> aliases = List.of("ptl", "tiempolimite", "limitetiempo");
            BukkitCommandWrapper cmd = new BukkitCommandWrapper("timelimit", "Sistema de límite de tiempo de juego", "/timelimit", aliases, timeLimitExec, timeLimitExec);
            commandMap.register("coreec", cmd);
            registered.add(cmd);
        }
    }

    public void unregisterAll() {
        for (Command command : registered) {
            command.unregister(commandMap);
        }
        registered.clear();
    }
}
