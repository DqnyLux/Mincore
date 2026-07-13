package org.dqnylux.mincore.managers.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.ChatFormatConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;

import java.util.regex.Pattern;

public class ChatFormatHandler {

    private static final Pattern LEGACY_COLOR = Pattern.compile("&#[0-9a-fA-F]{6}|&[0-9a-fA-F]");
    private static final Pattern LEGACY_FORMAT = Pattern.compile("&[lnmokr]");
    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("</?[a-zA-Z_:#][^<>]*>");

    private final Mincore plugin;

    public ChatFormatHandler(Mincore plugin) {
        this.plugin = plugin;
    }

    public String applyPermissions(Player player, String message) {
        if (!player.hasPermission("mincore.chat.minimessage")) {
            message = MINIMESSAGE_TAG.matcher(message).replaceAll("");
        }
        if (!player.hasPermission("mincore.chat.color")) {
            message = LEGACY_COLOR.matcher(message).replaceAll("");
        }
        if (!player.hasPermission("mincore.chat.format")) {
            message = LEGACY_FORMAT.matcher(message).replaceAll("");
        }
        return message;
    }

    public Component buildFinalChat(Player sender, String message) {
        ChatFormatConfig format = plugin.getConfigManager().getChatFormatConfig();
        String withMentions = format.mentions.enabled ? highlightMentions(sender, message, format.mentions) : message;

        PlayerData data = plugin.getPlayerManager().get(sender.getUniqueId());
        String namecolor = resolveCosmeticValue(data, "namecolors", "<white>");
        String chatcolor = resolveCosmeticValue(data, "chatcolors", "<white>");

        // %player_name%/%namecolor%/%chatcolor% los resolvemos nosotros mismos
        // (garantizado, sin depender de PlaceholderAPI) antes de pasar por
        // TextUtils.format, que además aplica cualquier otro placeholder de
        // PAPI (ej. %luckperms_prefix%) si el plugin está instalado.
        String prefixPart = format.parts.prefix.replace("%player_name%", sender.getName());
        String namePart = format.parts.name.replace("%player_name%", sender.getName()).replace("%namecolor%", namecolor);
        String iconPart = format.parts.icon.replace("%player_name%", sender.getName());
        String messageColorPart = format.parts.message.replace("%chatcolor%", chatcolor);

        return TextUtils.format(sender, prefixPart)
                .append(TextUtils.format(sender, namePart))
                .append(TextUtils.format(sender, iconPart))
                .append(TextUtils.format(sender, format.parts.arrow))
                .append(TextUtils.format(sender, messageColorPart))
                .append(TextUtils.formatSafeChat(withMentions));
    }

    private String resolveCosmeticValue(PlayerData data, String category, String fallback) {
        if (data == null) return fallback;
        String cosmeticId = data.getActiveCosmetic(category);
        if (cosmeticId == null) return fallback;

        CosmeticItem item = plugin.getCosmeticConfigManager().getItem(category, cosmeticId);
        return item != null && item.value != null && !item.value.isBlank() ? item.value : fallback;
    }

    private String highlightMentions(Player sender, String message, ChatFormatConfig.Mentions mentions) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) continue;

            Pattern mentionPattern = Pattern.compile("(?i)@?\\b" + Pattern.quote(online.getName()) + "\\b");
            if (!mentionPattern.matcher(message).find()) continue;

            message = mentionPattern.matcher(message).replaceAll(mentions.highlightColor + "@" + online.getName() + "<reset>");
            notifyMention(online, sender, mentions);
        }
        return message;
    }

    private void notifyMention(Player mentioned, Player sender, ChatFormatConfig.Mentions mentions) {
        if (mentions.actionbar) {
            mentioned.sendActionBar(TextUtils.format(mentions.actionbarMessage.replace("%player%", sender.getName())));
        }
        if (mentions.sound) {
            mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }
}
