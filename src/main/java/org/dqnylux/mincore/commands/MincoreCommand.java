package org.dqnylux.mincore.commands;

import org.bukkit.command.CommandSender;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.managers.EconomyAdminHandler;
import org.dqnylux.mincore.menus.MainMenu;
import org.dqnylux.mincore.utils.TextUtils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Suggest;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.annotation.Usage;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@SuppressWarnings("unused")
public class MincoreCommand {

    private final Mincore plugin;

    public MincoreCommand(Mincore plugin) {
        this.plugin = plugin;
    }

    private boolean checkPermission(CommandSender sender) {
        return checkPermission(sender, plugin.getConfigManager().getMainConfig().permissions.admin);
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                    plugin.getConfigManager().getMessagesConfig().commands.noPermission;
            sender.sendMessage(TextUtils.format(msg));
            return false;
        }
        return true;
    }

    @Command("coreec")
    @CommandPermission("coreec.admin")
    @Usage("/coreec")
    @Description("Comando principal y menú de ayuda administrativa")
    public void defaultCommand(CommandSender sender) {
        if (!checkPermission(sender)) return;
        help(sender);
    }

    @Command("coreec reload")
    @CommandPermission("coreec.admin")
    @Usage("/coreec reload")
    @Description("Recarga las configuraciones locales del plugin")
    public void reload(CommandSender sender) {
        if (!checkPermission(sender)) return;

        long start = System.currentTimeMillis();
        plugin.getConfigManager().loadConfigs();
        plugin.getCosmeticConfigManager().loadConfigs();
        plugin.getChatFilterManager().reload();
        plugin.getAnnouncementManager().start();
        plugin.getDynamicCommandManager().reload();
        if (plugin.getEssentialsToastManager() != null) {
            plugin.getEssentialsToastManager().init();
        }
        // Re-registra los comandos con los alias raíz recién leídos de
        // config.yml -> commands (ver CommandManager#reload).
        plugin.getCommandManager().reload();

        // /coreec reload es SOLO local a propósito - ya no empuja a la red
        // acá (antes lo hacía siempre, aunque el admin solo quisiera
        // recargar un cambio de este server puntual). Para compartir con el
        // resto de la red hace falta el comando explícito /coreec sync push.
        long time = System.currentTimeMillis() - start;

        String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                plugin.getConfigManager().getMessagesConfig().commands.reloadSuccess
                        .replace("%ms%", String.valueOf(time));

        sender.sendMessage(TextUtils.format(msg));
    }

    @Command("coreec reload catalog")
    @CommandPermission("coreec.admin")
    @Usage("/coreec reload catalog")
    @Description("Recarga el catálogo de cosméticos desde los recursos")
    public void reloadCatalog(CommandSender sender) {
        if (!checkPermission(sender)) return;

        long start = System.currentTimeMillis();
        plugin.getCosmeticConfigManager().reloadFromResources();
        // Igual que /coreec reload: solo local, no empuja a la red. Usar
        // /coreec sync push si además se quiere compartir con el resto.
        long time = System.currentTimeMillis() - start;

        String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                plugin.getConfigManager().getMessagesConfig().commands.resetCatalogSuccess
                        .replace("%ms%", String.valueOf(time));
        sender.sendMessage(TextUtils.format(msg));
    }

    @Command("coreec help")
    @CommandPermission("coreec.admin")
    @Usage("/coreec help")
    @Description("Muestra la lista de comandos de administración")
    public void help(CommandSender sender) {
        if (!checkPermission(sender)) return;

        for (String line : plugin.getConfigManager().getMessagesConfig().commands.help) {
            sender.sendMessage(TextUtils.format(line));
        }
    }

    @Command("coreec menu")
    @CommandPermission("coreec.admin")
    @Usage("/coreec menu")
    @Description("Abre el menú principal de administración")
    public void openMenu(BukkitCommandActor actor) {
        if (!checkPermission(actor.sender())) return;

        if (!actor.isPlayer()) {
            var messages = plugin.getConfigManager().getMessagesConfig();
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }

        MainMenu.open(actor.requirePlayer(), plugin);
    }

    @Command("coreec eco give")
    @CommandPermission("coreec.admin")
    @Usage("/coreec eco give <jugador> <cantidad>")
    @Description("Entrega monedas a un jugador")
    public void ecoGive(CommandSender sender, @Named("jugador") @SuggestWith(OnlinePlayerSuggestionProvider.class) String player, @Named("cantidad") @Suggest({"100", "500", "1000", "5000", "10000"}) double amount) {
        if (!checkPermission(sender)) return;
        EconomyAdminHandler.Result result = plugin.getEconomyAdminHandler().give(player, amount);
        replyEco(sender, result, player, amount, plugin.getConfigManager().getMessagesConfig().commands.ecoGiveSuccess);
    }

    @Command("coreec eco take")
    @CommandPermission("coreec.admin")
    @Usage("/coreec eco take <jugador> <cantidad>")
    @Description("Retira monedas a un jugador")
    public void ecoTake(CommandSender sender, @Named("jugador") @SuggestWith(OnlinePlayerSuggestionProvider.class) String player, @Named("cantidad") @Suggest({"100", "500", "1000", "5000", "10000"}) double amount) {
        if (!checkPermission(sender)) return;
        EconomyAdminHandler.Result result = plugin.getEconomyAdminHandler().take(player, amount);
        replyEco(sender, result, player, amount, plugin.getConfigManager().getMessagesConfig().commands.ecoTakeSuccess);
    }

    @Command("coreec eco set")
    @CommandPermission("coreec.admin")
    @Usage("/coreec eco set <jugador> <cantidad>")
    @Description("Establece el balance de monedas de un jugador")
    public void ecoSet(CommandSender sender, @Named("jugador") @SuggestWith(OnlinePlayerSuggestionProvider.class) String player, @Named("cantidad") @Suggest({"0", "100", "500", "1000", "5000", "10000"}) double amount) {
        if (!checkPermission(sender)) return;
        EconomyAdminHandler.Result result = plugin.getEconomyAdminHandler().set(player, amount);
        replyEco(sender, result, player, amount, plugin.getConfigManager().getMessagesConfig().commands.ecoSetSuccess);
    }

    private void replyEco(CommandSender sender, EconomyAdminHandler.Result result, String player, double amount, String successTemplate) {
        var messages = plugin.getConfigManager().getMessagesConfig();
        String body = switch (result) {
            case SUCCESS -> successTemplate.replace("%player%", player).replace("%amount%", String.valueOf(amount));
            case PLAYER_OFFLINE -> messages.commands.ecoPlayerOffline;
            case INVALID_AMOUNT -> messages.commands.ecoInvalidAmount;
        };
        sender.sendMessage(TextUtils.format(messages.prefix + body));
    }

    @Command("coreec sync push")
    @CommandPermission("coreec.admin")
    @Usage("/coreec sync push")
    @Description("Sincroniza y sube configuraciones a la base de datos de red")
    public void syncPush(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getCosmeticSyncManager().pushToDatabase();
        plugin.getConfigSyncManager().pushAll();
        var messages = plugin.getConfigManager().getMessagesConfig();
        sender.sendMessage(TextUtils.format(messages.prefix + messages.commands.syncPush));
    }

    @Command("coreec sync pull")
    @CommandPermission("coreec.admin")
    @Usage("/coreec sync pull")
    @Description("Descarga y sincroniza configuraciones desde la base de datos de red")
    public void syncPull(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getCosmeticSyncManager().pullFromDatabase();
        plugin.getConfigSyncManager().pullAll();
        var messages = plugin.getConfigManager().getMessagesConfig();
        sender.sendMessage(TextUtils.format(messages.prefix + messages.commands.syncPull));
    }

    @Command("coreec commandblocker reload")
    @CommandPermission("coreec.admin")
    @Usage("/coreec commandblocker reload")
    @Description("Recarga la configuración del bloqueador de comandos")
    public void commandBlockerReload(CommandSender sender) {
        if (!checkPermission(sender)) return;

        long start = System.currentTimeMillis();
        plugin.getConfigManager().reloadCommandBlockerConfig();
        long time = System.currentTimeMillis() - start;

        String msg = plugin.getConfigManager().getMessagesConfig().prefix +
                plugin.getConfigManager().getMessagesConfig().commands.commandBlockerReloadSuccess
                        .replace("%ms%", String.valueOf(time));
        sender.sendMessage(TextUtils.format(msg));
    }

    @Command("coreec ai refreshwords")
    @CommandPermission("coreec.admin")
    @Usage("/coreec ai refreshwords")
    @Description("Genera palabras de revisión del filtro por IA")
    public void aiRefreshWords(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getChatFilterManager().generateReviewWordsAsync(sender);
    }

    @Command("coreec ai refreshbadwords")
    @CommandPermission("coreec.admin")
    @Usage("/coreec ai refreshbadwords")
    @Description("Genera malas palabras del filtro por IA")
    public void aiRefreshBadWords(CommandSender sender) {
        if (!checkPermission(sender)) return;
        plugin.getChatFilterManager().generateBadWordsAsync(sender);
    }

    @Command("coreec ai addbadword")
    @CommandPermission("coreec.admin")
    @Usage("/coreec ai addbadword <palabra>")
    @Description("Añade una mala palabra al filtro por IA")
    public void aiAddBadWord(CommandSender sender, @Named("palabra") String word) {
        if (!checkPermission(sender)) return;
        plugin.getChatFilterManager().addBadWordAsync(sender, word);
    }

    @Command("coreec ai pending")
    @CommandPermission("coreec.admin")
    @Usage("/coreec ai pending")
    @Description("Lista los patrones de chat pendientes de revisión por IA")
    public void aiPending(CommandSender sender) {
        if (!checkPermission(sender)) return;
        var messages = plugin.getConfigManager().getMessagesConfig();
        var pending = plugin.getChatFilterManager().listPendingPatterns();
        if (pending.isEmpty()) {
            sender.sendMessage(TextUtils.format(messages.prefix + messages.commands.aiPendingEmpty));
            return;
        }
        for (var p : pending) {
            String entry = messages.commands.aiPendingEntry
                    .replace("%id%", String.valueOf(p.id()))
                    .replace("%player%", p.playerName())
                    .replace("%message%", p.message())
                    .replace("%regex%", p.regex());
            sender.sendMessage(TextUtils.format(messages.prefix + entry));
        }
    }

    @Command("coreec ai block")
    @CommandPermission("coreec.admin")
    @Usage("/coreec ai block <id>")
    @Description("Bloquea y aprueba un patrón de chat pendiente")
    public void aiBlock(CommandSender sender, @Named("id") int id) {
        if (!checkPermission(sender)) return;
        var messages = plugin.getConfigManager().getMessagesConfig();
        String regex = plugin.getChatFilterManager().approvePendingPattern(id);
        String msg = regex != null
                ? messages.commands.aiBlockSuccess.replace("%id%", String.valueOf(id)).replace("%regex%", regex)
                : messages.commands.aiBlockNotFound.replace("%id%", String.valueOf(id));
        sender.sendMessage(TextUtils.format(messages.prefix + msg));
    }

    @Command("coreec ai allow")
    @CommandPermission("coreec.admin")
    @Usage("/coreec ai allow <id>")
    @Description("Permite y descarta un patrón de chat pendiente")
    public void aiAllow(CommandSender sender, @Named("id") int id) {
        if (!checkPermission(sender)) return;
        var messages = plugin.getConfigManager().getMessagesConfig();
        boolean removed = plugin.getChatFilterManager().rejectPendingPattern(id);
        String msg = removed
                ? messages.commands.aiAllowSuccess.replace("%id%", String.valueOf(id))
                : messages.commands.aiAllowNotFound.replace("%id%", String.valueOf(id));
        sender.sendMessage(TextUtils.format(messages.prefix + msg));
    }

    @Command("coreec setafkzone")
    @CommandPermission("coreec.admin")
    @Usage("/coreec setafkzone <nombre> <radio> [intervalo] [monedas] [requiereAfk]")
    @Description("Crea una zona AFK en tu posición actual")
    public void setAfkZone(BukkitCommandActor actor,
                           @Named("nombre") String name,
                           @Named("radio") @Suggest({"5", "10", "15", "20", "50"}) double radius,
                           @Optional @Named("intervalo") @Suggest({"30", "60", "120", "300"}) Integer interval,
                           @Optional @Named("monedas") @Suggest({"1", "5", "10", "50", "100"}) Double coins,
                           @Optional @Named("requiereAfk") @Suggest({"true", "false"}) Boolean requireAfk) {
        if (!checkPermission(actor.sender())) return;

        if (!actor.isPlayer()) {
            var messages = plugin.getConfigManager().getMessagesConfig();
            actor.sender().sendMessage(TextUtils.format(messages.prefix + messages.commands.playersOnly));
            return;
        }

        org.bukkit.entity.Player player = actor.requirePlayer();
        var afkConfig = plugin.getConfigManager().getAfkConfig();

        if (radius <= 0) {
            player.sendMessage(TextUtils.format(afkConfig.prefix + afkConfig.messages.invalidUsage.replace("%usage%", "/coreec setafkzone <nombre> <radio> [intervalo] [monedas] [requiereAfk]")));
            return;
        }

        org.bukkit.Location loc = player.getLocation();
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        double x1 = loc.getX() - radius;
        double y1 = Math.max(loc.getWorld() != null ? loc.getWorld().getMinHeight() : -64, loc.getY() - radius);
        double z1 = loc.getZ() - radius;
        double x2 = loc.getX() + radius;
        double y2 = Math.min(loc.getWorld() != null ? loc.getWorld().getMaxHeight() : 320, loc.getY() + radius);
        double z2 = loc.getZ() + radius;

        int rewardInterval = interval != null && interval > 0 ? interval : 60;
        double rewardCoins = coins != null && coins >= 0 ? coins : 5.0;
        boolean reqAfk = requireAfk != null && requireAfk;

        boolean created = plugin.getAfkManager().createZone(name, world, x1, y1, z1, x2, y2, z2, rewardInterval, rewardCoins, java.util.List.of(), reqAfk);
        if (created) {
            player.sendMessage(TextUtils.format(afkConfig.prefix + afkConfig.messages.zoneCreated.replace("%zone%", name)));
        } else {
            player.sendMessage(TextUtils.format(afkConfig.prefix + afkConfig.messages.zoneAlreadyExists.replace("%zone%", name)));
        }
    }

    @Command("coreec clearchat")
    @CommandPermission("coreec.admin.clearchat")
    @Usage("/coreec clearchat")
    @Description("Limpia el chat para todos los jugadores")
    public void clearChat(CommandSender sender) {
        if (!checkPermission(sender, plugin.getConfigManager().getMainConfig().permissions.clearchat)) return;

        var messages = plugin.getConfigManager().getMessagesConfig();
        int lines = plugin.getConfigManager().getMainConfig().commands.clearchatLines;
        net.kyori.adventure.text.Component blank = net.kyori.adventure.text.Component.empty();
        for (int i = 0; i < lines; i++) {
            org.bukkit.Bukkit.broadcast(blank);
        }
        org.bukkit.Bukkit.broadcast(TextUtils.format(
                messages.prefix + messages.commands.clearchatDone.replace("%player%", sender.getName())));
    }
}