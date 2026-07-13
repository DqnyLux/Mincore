package org.dqnylux.mincore.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Expone %mincore_*%: coins, cosméticos equipados por categoría, toggles,
 * estado de rastreo, y variantes ya resueltas/coloreadas de namecolor/
 * chatcolor/prefix/icon para que otros plugins (holograma, TAB, scoreboard...)
 * puedan consumir el color/formato real sin tener que ir a leer el YAML.
 */
public class PAPIExpansion extends PlaceholderExpansion {

    private final Mincore plugin;

    public PAPIExpansion(Mincore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mincore";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
        if (data == null) return "";

        String key = params.toLowerCase();
        return switch (key) {
            case "coins" -> String.valueOf(data.getCoins());
            case "setting_globalchat" -> String.valueOf(data.isGlobalChat());
            case "setting_messages" -> String.valueOf(data.isMessagesEnabled());
            case "tracking" -> String.valueOf(plugin.getDeathGPSTask().isTracking(player.getUniqueId()));

            // Nombre garantizado (sin depender de %player_name% de PAPI) y la
            // misma variante ya coloreada con el namecolor equipado.
            case "name" -> player.getName();
            case "colored_name" -> resolveLegacy(data, "namecolors") + player.getName();

            // Formato/color crudo (ej. "&6") de cada cosmético textual - útil
            // para que otros plugins (holograma, TAB, scoreboard) lo apliquen
            // ellos mismos sin tener que leer el YAML de Mincore.
            case "namecolor" -> resolveLegacy(data, "namecolors");
            case "chatcolor" -> resolveLegacy(data, "chatcolors");
            case "prefix" -> resolveLegacy(data, "prefixes");
            case "icon" -> resolveLegacy(data, "icons");
            case "glow" -> resolveRaw(data, "glows");

            default -> key.startsWith("active_")
                    ? orEmpty(data.getActiveCosmetic(key.substring("active_".length())))
                    : null;
        };
    }

    /** Valor crudo del cosmético equipado (tal cual está escrito en el YAML), sin equipar = "". */
    private String resolveRaw(PlayerData data, String category) {
        String id = data.getActiveCosmetic(category);
        if (id == null) return "";

        CosmeticItem item = plugin.getCosmeticConfigManager().getItem(category, id);
        return item != null && item.value != null ? item.value : "";
    }

    /** Igual que resolveRaw pero convertido a código de color legado (§), compatible con cualquier plugin externo. */
    private String resolveLegacy(PlayerData data, String category) {
        String raw = resolveRaw(data, category);
        return raw.isEmpty() ? "" : TextUtils.formatLegacy(raw);
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
