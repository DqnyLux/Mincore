package org.dqnylux.mincore.managers.chat;

import com.cryptomorin.xseries.XSound;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.ChatFormatConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ChatFormatHandler {

    private static final Pattern LEGACY_COLOR = Pattern.compile("&#[0-9a-fA-F]{6}|&[0-9a-fA-F]");
    private static final Pattern LEGACY_FORMAT = Pattern.compile("&[lnmokr]");
    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("</?[a-zA-Z_:#][^<>]*>");

    private final Mincore plugin;
    private final Map<UUID, BossBar> mentionBossBars = new ConcurrentHashMap<>();
    private final MentionToastManager toastManager;

    public ChatFormatHandler(Mincore plugin) {
        this.plugin = plugin;
        this.toastManager = new MentionToastManager(plugin);
        this.toastManager.init();
    }

    public String applyPermissions(Player player, String message) {
        if (!player.hasPermission("coreec.chat.minimessage")) {
            message = MINIMESSAGE_TAG.matcher(message).replaceAll("");
        }
        if (!player.hasPermission("coreec.chat.color")) {
            message = LEGACY_COLOR.matcher(message).replaceAll("");
        }
        if (!player.hasPermission("coreec.chat.format")) {
            message = LEGACY_FORMAT.matcher(message).replaceAll("");
        }
        return message;
    }

    public Component buildFinalChat(Player sender, String message) {
        ChatFormatConfig format = plugin.getConfigManager().getChatFormatConfig();
        ChatFormatConfig.Parts parts = format.chat.parts;

        PlayerData data = plugin.getPlayerManager().get(sender.getUniqueId());
        String namecolor = resolveCosmeticValue(data, "namecolors", "<#FFFFFF>");
        String chatcolor = resolveCosmeticValue(data, "chatcolors", "<#FFFFFF>");

        // chatcolor se resuelve ANTES de resaltar menciones (no después) para
        // poder re-inyectarlo explícitamente después de CADA mención - ver
        // highlightMentions: no depender de que el cierre de </color> de la
        // mención "recuerde" solo el color exterior.
        String withMentions = format.mentions.enabled ? highlightMentions(sender, message, format.mentions, chatcolor) : message;

        // %player_name%/%namecolor%/%chatcolor% los resolvemos nosotros mismos
        // (garantizado, sin depender de PlaceholderAPI) antes de pasar por
        // TextUtils.format, que además aplica cualquier otro placeholder de
        // PAPI (ej. %luckperms_prefix%) si el plugin está instalado.
        //
        // Cada parte se envuelve en <reset>...<reset>: prefix/name/icon/arrow
        // se parsean como Components INDEPENDIENTES y luego se encadenan con
        // .append(), lo que en Adventure los deja como HERMANOS (hijos del
        // mismo componente raíz) - no heredan color entre sí. Sin el <reset>
        // inicial, una parte vacía (ej. sin prefijo equipado) podía arrastrar
        // el color visible de la parte anterior si el cliente/algún plugin
        // externo interpretaba el hueco; con <reset> al inicio y al final,
        // cada parte SIEMPRE es exactamente el color configurado, ni más ni menos.
        // El nombre a mostrar pasa por DisguiseManager: si el jugador está
        // disfrazado, TODO el chat (nombre, hover, suggest) usa el nick falso.
        String shownName = plugin.getDisguiseManager().displayName(sender);

        String separator = format.chat.partSeparator == null ? "" : format.chat.partSeparator;
        String prefixPart = wrapReset(parts.prefix.text.replace("%player_name%", shownName));
        String namePart = wrapReset(parts.name.text
                .replace("%player_name%", shownName)
                .replace("%namecolor%", namecolor));
        String iconPart = wrapReset(parts.icon.text.replace("%player_name%", shownName));
        String arrowPart = wrapReset(parts.arrow.text);

        Component nameComponent = TextUtils.format(sender, namePart);
        nameComponent = applyActiveFormats(data, PlayerData.FORMAT_SCOPE_NAME, nameComponent);
        if (parts.name.hover != null && !parts.name.hover.isEmpty()) {
            nameComponent = nameComponent.hoverEvent(HoverEvent.showText(buildHover(sender, parts.name.hover)));
        }
        if (parts.name.suggest != null && !parts.name.suggest.isBlank()) {
            nameComponent = nameComponent.clickEvent(ClickEvent.suggestCommand(
                    parts.name.suggest.replace("%player_name%", shownName)));
        }

        // El chatcolor se envuelve alrededor de TODO el mensaje (menciones ya
        // resaltadas incluidas) y se parsea de una sola vez, en vez de parsear
        // el mensaje aparte y aplicar colorIfAbsent(TextColor) después: un
        // gradiente/rainbow no es un TextColor único, así que colorIfAbsent
        // no podía representarlo (un tag de gradiente SIN texto encerrado ni
        // siquiera produce un color - queda sin aplicar del todo, perdiendo
        // el color de chat completo para cualquiera con un chatcolor tipo
        // gradiente). Envolviendo el string entero, MiniMessage reparte el
        // gradiente carácter por carácter como corresponde - y cada mención
        // (ver highlightMentions) ya viene con el chatcolor REABIERTO a mano
        // justo después de su </color>, así que el resto del mensaje sigue
        // coloreado incluso con un chatcolor tipo gradiente (confirmado: con
        // gradiente, cerrar un <color> anidado NO retoma la progresión sola,
        // hay que reabrirla explícita - por eso highlightMentions no confía
        // en el anidado implícito de MiniMessage para esto).
        String chatColorTag = parts.message.text.replace("%chatcolor%", chatcolor);
        Component messageComponent = TextUtils.formatSafeChat(chatColorTag + withMentions);
        messageComponent = applyActiveFormats(data, PlayerData.FORMAT_SCOPE_CHAT, messageComponent);

        Component sep = separator.isEmpty() ? Component.empty() : TextUtils.format(sender, separator);
        Component finalMessage = TextUtils.format(sender, prefixPart).append(sep)
                .append(nameComponent).append(sep)
                .append(TextUtils.format(sender, iconPart)).append(sep)
                .append(TextUtils.format(sender, arrowPart)).append(sep)
                .append(messageComponent);

        return applyItemToken(sender, format.interactiveItem, finalMessage);
    }

    /**
     * cosmetics/formats.yml (sección "formats" - toggles independientes, no
     * un "equipar uno" como las demás categorías): aplica todos los que el
     * jugador tenga activos a la vez, sobre el nombre (scope "name",
     * controlado desde el menú namecolors) o sobre el mensaje (scope "chat",
     * controlado desde chatcolors) - son estados independientes.
     */
    private Component applyActiveFormats(PlayerData data, String scope, Component target) {
        if (data == null) return target;
        for (String formatId : data.getActiveFormats(scope)) {
            TextDecoration decoration = mapFormatDecoration(formatId);
            if (decoration != null) target = target.decorate(decoration);
        }
        return target;
    }

    private TextDecoration mapFormatDecoration(String formatId) {
        return switch (formatId) {
            case "bold" -> TextDecoration.BOLD;
            case "italic" -> TextDecoration.ITALIC;
            case "underline" -> TextDecoration.UNDERLINED;
            case "strikethrough" -> TextDecoration.STRIKETHROUGH;
            case "magic" -> TextDecoration.OBFUSCATED;
            default -> null;
        };
    }

    /** Reemplaza el literal configurado (por defecto "[item]") por un componente hoverable con el ítem en mano. */
    private Component applyItemToken(Player sender, ChatFormatConfig.InteractiveItem interactiveItem, Component finalMessage) {
        if (interactiveItem.trigger == null || interactiveItem.trigger.isEmpty()) return finalMessage;

        return finalMessage.replaceText(builder -> builder.matchLiteral(interactiveItem.trigger).replacement((matchResult, componentBuilder) -> {
            ItemStack hand = sender.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                return TextUtils.format(sender, interactiveItem.emptyHand);
            }
            return Component.text("[")
                    .append(Component.translatable(hand.translationKey()))
                    .append(Component.text("]"))
                    .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                    .hoverEvent(hand.asHoverEvent());
        }));
    }

    private String wrapReset(String text) {
        return "<reset>" + (text == null ? "" : text) + "<reset>";
    }

    private Component buildHover(Player sender, List<String> lines) {
        Component hover = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) hover = hover.append(Component.newline());
            hover = hover.append(TextUtils.format(sender, lines.get(i)));
        }
        return hover;
    }

    private String resolveCosmeticValue(PlayerData data, String category, String fallback) {
        if (data == null) return fallback;
        String cosmeticId = data.getActiveCosmetic(category);
        if (cosmeticId == null) return fallback;

        CosmeticItem item = plugin.getCosmeticConfigManager().getItem(category, cosmeticId);
        return item != null && item.value != null && !item.value.isBlank() ? item.value : fallback;
    }

    private String highlightMentions(Player sender, String message, ChatFormatConfig.Mentions mentions, String chatColorTag) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) continue;

            PlayerData targetData = plugin.getPlayerManager().get(online.getUniqueId());
            if (targetData != null && !targetData.isMentionsEnabled()) continue;

            // El "@" ya no es obligatorio - basta con escribir el nombre del
            // jugador. "(?:^|(?<=\\W))" exige que lo que precede al nombre (o
            // al "@" opcional) sea el inicio del mensaje o un carácter que no
            // sea de palabra, para no disparar una mención sobre un nombre que
            // es solo substring de otra palabra (ej. "MiJuan" no menciona a "Juan").
            Pattern mentionPattern = Pattern.compile("(?i)(?:^|(?<=\\W))@?" + Pattern.quote(online.getName()) + "\\b");
            if (!mentionPattern.matcher(message).find()) continue;

            // <color:HEX>...</color> resalta el nombre, y justo después se
            // REABRE explícitamente el mismo chatColorTag - no alcanza con
            // confiar en que MiniMessage "recuerde" el color exterior al
            // cerrar la mención (con un chatcolor tipo GRADIENTE, cerrar un
            // <color> anidado corta la progresión del gradiente en vez de
            // continuarla, dejando el resto del mensaje sin colorear). Reabrir
            // el tag a mano es válido para cualquier tipo de chatcolor
            // (sólido, gradiente o rainbow) y no depende de ese detalle
            // interno de MiniMessage.
            message = mentionPattern.matcher(message)
                    .replaceAll(match -> "<color:" + mentions.highlightColor + ">" + match.group() + "</color>" + chatColorTag);
            notifyMention(online, sender, mentions);
        }
        return message;
    }

    private void notifyMention(Player mentioned, Player sender, ChatFormatConfig.Mentions mentions) {
        // El nombre mostrado en la línea de chat que el mencionado ya vio es
        // el falso (disfraz) - la notificación (actionbar/title/bossbar) debe
        // coincidir, si no revela el nombre real de quien lo mencionó.
        String senderName = plugin.getDisguiseManager().displayName(sender);
        // AsyncChatEvent corre fuera del hilo principal - sendActionBar/
        // showTitle/bossbar/playSound sobre el jugador mencionado deben
        // reprogramarse a su scheduler de entidad (igual que en
        // PlayerConnectionListener), si no la notificación puede fallar en silencio.
        mentioned.getScheduler().run(plugin, task -> {
            if (mentions.actionbar.enabled) {
                mentioned.sendActionBar(TextUtils.format(mentions.actionbar.text.replace("%player%", senderName)));
            }
            if (mentions.title.enabled) {
                Title title = Title.title(
                        TextUtils.format(mentions.title.main.replace("%player%", senderName)),
                        TextUtils.format(mentions.title.sub.replace("%player%", senderName)),
                        Title.Times.times(
                                Duration.ofMillis(mentions.title.fadeInTicks * 50L),
                                Duration.ofMillis(mentions.title.stayTicks * 50L),
                                Duration.ofMillis(mentions.title.fadeOutTicks * 50L)
                        )
                );
                mentioned.showTitle(title);
            }
            if (mentions.bossbar.enabled) {
                showMentionBossbar(mentioned, senderName, mentions.bossbar);
            }
            if (mentions.sound != null && !mentions.sound.isBlank()) {
                XSound.matchXSound(mentions.sound)
                        .ifPresent(sound -> sound.play(mentioned.getLocation(), 1f, 1f));
            }
            if (mentions.toast.enabled) {
                toastManager.showToast(mentioned);
            }
        }, () -> {});
    }

    private void showMentionBossbar(Player mentioned, String senderName, ChatFormatConfig.BossbarSection bossbarConfig) {
        BossBar previous = mentionBossBars.remove(mentioned.getUniqueId());
        if (previous != null) mentioned.hideBossBar(previous);

        BossBar.Color color = parseBossbarColor(bossbarConfig.color);
        BossBar.Overlay overlay = parseBossbarOverlay(bossbarConfig.overlay);
        BossBar bossBar = BossBar.bossBar(
                TextUtils.format(bossbarConfig.text.replace("%player%", senderName)),
                1f, color, overlay
        );
        mentioned.showBossBar(bossBar);
        mentionBossBars.put(mentioned.getUniqueId(), bossBar);

        mentioned.getScheduler().runDelayed(plugin, task -> {
            BossBar active = mentionBossBars.remove(mentioned.getUniqueId());
            if (active != null) mentioned.hideBossBar(active);
        }, () -> {}, Math.max(1, bossbarConfig.durationSeconds * 20));
    }

    private BossBar.Color parseBossbarColor(String name) {
        try {
            return BossBar.Color.valueOf(name.trim().toUpperCase());
        } catch (Exception e) {
            return BossBar.Color.YELLOW;
        }
    }

    private BossBar.Overlay parseBossbarOverlay(String name) {
        try {
            return BossBar.Overlay.valueOf(name.trim().toUpperCase());
        } catch (Exception e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
