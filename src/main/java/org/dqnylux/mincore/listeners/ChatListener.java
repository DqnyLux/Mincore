package org.dqnylux.mincore.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.managers.chat.ChatFilterManager;
import org.dqnylux.mincore.managers.chat.ChatFormatHandler;
import org.dqnylux.mincore.managers.chat.ChatPunishmentHandler;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private static final Pattern INJECTION_GUARD = Pattern.compile("<click|<hover|<insert", Pattern.CASE_INSENSITIVE);

    private final Mincore plugin;
    private final ChatFilterManager filterManager;
    private final ChatPunishmentHandler punishmentHandler;
    private final ChatFormatHandler formatHandler;

    public ChatListener(Mincore plugin, ChatFilterManager filterManager, ChatPunishmentHandler punishmentHandler, ChatFormatHandler formatHandler) {
        this.plugin = plugin;
        this.filterManager = filterManager;
        this.punishmentHandler = punishmentHandler;
        this.formatHandler = formatHandler;
    }

    // ignoreCancelled: el chat de congelados (FreezeListener) y el canal de
    // staff (StaffListener) cancelan el evento ANTES - este pipeline no debe
    // formatear/broadcastear un mensaje que ya fue desviado a otro canal.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        // Chequeo proactivo contra la base de datos real de LiteBans, en vez
        // de confiar en que su cancel del evento nos llegue antes que este
        // @EventHandler (HIGHEST + ignoreCancelled): LiteBans puede estar
        // escuchando el AsyncPlayerChatEvent legacy en vez del AsyncChatEvent
        // moderno de Paper - son dos eventos separados con un puente de
        // compatibilidad que no siempre sincroniza el cancelled entre sí (ver
        // el javadoc de LiteBansHook). Corre ANTES de tocar el mensaje: un
        // jugador muteado no debe consumir ningún ciclo del resto del
        // pipeline (filtros/formato/IA).
        if (org.dqnylux.mincore.hooks.LiteBansHook.isMuted(player)) {
            event.setCancelled(true);
            player.sendMessage(TextUtils.format(messages.prefix + messages.chat.filterMuted));
            return;
        }

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        // Se guarda ANTES de que process()/applyPermissions() lo pisen -
        // reviewAsync() necesita el texto tal cual lo escribió el jugador,
        // no la versión con **** que ChatFilterManager.process() ya aplicó
        // para lo que se MUESTRA en el chat. Mandarle a la IA el mensaje
        // censurado le arruina el contexto justo para las palabras fuertes
        // que más importa evaluar bien.
        String rawMessage = message;

        if (INJECTION_GUARD.matcher(message).find()) {
            event.setCancelled(true);
            String alert = messages.prefix + messages.chat.injectionBlocked
                    .replace("%player%", plugin.getDisguiseManager().displayName(player))
                    .replace("%message%", message);
            Bukkit.broadcast(TextUtils.formatSafeChat(alert));
            return;
        }

        // Texto YA pasado por el blocklist duro (con *** donde matcheó
        // badWords) - reviewAsync() lo usa solo para decidir SI conviene
        // consultar a la IA (¿la palabra de revisión sigue visible ahí, o ya
        // quedó tapada por el blocklist duro?), nunca como el texto que
        // realmente se manda a la IA (para eso sigue usando rawMessage,
        // sin censura, para no perder contexto).
        String censoredMessage = rawMessage;

        if (!player.hasPermission(plugin.getConfigManager().getFiltersConfig().bypassPermission)) {
            ChatFilterManager.FilterResult result = filterManager.process(player, message);

            if (result.cancelled()) {
                event.setCancelled(true);
                notifyCancelled(player, result.reason(), result.blockedWord(), result.message());
                return;
            }

            message = result.message();
            censoredMessage = message;
            if (result.infraction()) {
                punishmentHandler.handleInfraction(player);
            }
        }

        message = formatHandler.applyPermissions(player, message);
        Component finalMessage = formatHandler.buildFinalChat(player, message);

        String messageId = plugin.getMessageDeletionManager().generateMessageId();
        event.viewers().removeIf(audience -> isOptedOutOfGlobalChat(audience, player));

        java.util.Set<java.util.UUID> recipientUuids = new java.util.HashSet<>();
        for (Audience aud : event.viewers()) {
            if (aud instanceof Player p) {
                recipientUuids.add(p.getUniqueId());
            }
        }
        plugin.getMessageDeletionManager().registerMessage(messageId, player, rawMessage, recipientUuids, event.signedMessage());

        event.renderer((source, sourceDisplayName, msg, viewer) -> {
            if (viewer instanceof Player pViewer) {
                plugin.getMessageDeletionManager().storeRenderedMessage(pViewer.getUniqueId(), messageId, finalMessage, event.signedMessage());
                return plugin.getMessageDeletionManager().formatForViewer(pViewer, messageId, finalMessage);
            }
            return finalMessage;
        });

        // El mensaje ya se va a mostrar al instante (sin retenerlo) - la
        // revisión por IA corre aparte y, si confirma que es tóxico, lo borra
        // de la pantalla de todos unos cientos de ms después (ver comentario
        // de clase de ChatFilterManager). reviewAsync() decide solo, con
        // censoredMessage, si la palabra de revisión sigue visible pese al
        // blocklist duro o si ya quedó tapada por él.
        filterManager.reviewAsync(player, rawMessage, censoredMessage, event.signedMessage(), messageId);
    }

    private boolean isOptedOutOfGlobalChat(Audience audience, Player sender) {
        if (!(audience instanceof Player viewer) || viewer.equals(sender)) return false;
        // Un staff con un freeze enfocado o un jugador previsualizando skin no ve el chat global
        if (plugin.getFreezeManager().getFocusedTarget(viewer.getUniqueId()) != null) return true;
        if (plugin.getSkinPreviewManager() != null && plugin.getSkinPreviewManager().isInSession(viewer.getUniqueId())) return true;
        PlayerData viewerData = plugin.getPlayerManager().get(viewer.getUniqueId());
        return viewerData != null && !viewerData.isGlobalChat();
    }

    private void notifyCancelled(Player player, ChatFilterManager.CancelReason reason, String blockedWord, String originalMessage) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        String body = switch (reason) {
            case SPAM -> messages.chat.filterSpam;
            case FLOOD -> messages.chat.filterFlood;
            case TOO_LONG -> messages.chat.filterTooLong;
            case REPETITION -> messages.chat.filterRepetition;
            // blockedWord es null para el resto de razones (solo BAD_WORD lo
            // llena, ver el javadoc de FilterResult) - reemplazo defensivo
            // por si el mensaje default alguna vez trae %word% sin que
            // aplique, aunque hoy solo filterBadWord lo usa.
            case BAD_WORD -> messages.chat.filterBadWord.replace("%word%", blockedWord == null ? "" : blockedWord);
            case ADS -> messages.chat.filterAds;
        };
        player.sendMessage(TextUtils.format(messages.prefix + body));

        String staffAlertPermission = plugin.getConfigManager().getFiltersConfig().punishment.staffAlertPermission;
        // formatSafeChat (no format): %message% viene DIRECTO de lo que
        // escribió el jugador, sin pasar por el guard de <click/<hover/
        // <insert> (ese guard corta el mensaje ANTES de llegar acá, pero
        // otros tags MiniMessage arbitrarios seguirían sin filtrar) - mismo
        // patrón que ya usa injectionBlocked más arriba en este archivo para
        // el mismo motivo, nunca formatear texto crudo de un jugador con
        // permiso completo de tags.
        String staffAlertText = messages.prefix + messages.chat.staffAlert
                .replace("%player%", player.getName())
                .replace("%message%", originalMessage == null ? "" : originalMessage);
        Component staffAlert = TextUtils.formatSafeChat(staffAlertText);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(staffAlertPermission)) {
                staff.sendMessage(staffAlert);
            }
        }
    }
}
