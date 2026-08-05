package org.dqnylux.mincore.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.StandardCosmeticConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.menus.CosmeticsGui;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.TextUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Expone %mincore_*%: coins, cosméticos equipados por categoría, toggles,
 * estado de rastreo, y variantes ya resueltas/coloreadas de namecolor/
 * chatcolor/prefix/icon para que otros plugins (holograma, TAB, scoreboard...)
 * puedan consumir el color/formato real sin tener que ir a leer el YAML.
 *
 * Placeholders por-ítem (name/price/status) usan la forma
 * %mincore_<accion>_<categoria>_<id>% - la categoría nunca lleva "_" (solo
 * "-", ej. join-messages), así que el id puede contener "_" sin ambigüedad:
 * se separa con límite 3 (acción, categoría, resto).
 */
public class PAPIExpansion extends PlaceholderExpansion {

    private final Mincore plugin;

    public PAPIExpansion(Mincore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "coreec";
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

            // Nombre a mostrar: el falso si está disfrazado, si no el real (sin
            // depender de %player_name% de PAPI) - YA CON el namecolor
            // equipado aplicado, como se ve en el chat. colored_name queda
            // como alias explícito con el mismo resultado; formatted_name
            // suma prefijo + namecolor + nombre + icono completos (lo mismo
            // que se ve en el chat, pero como placeholder para TAB/holograma/scoreboard).
            case "name", "colored_name" -> coloredText(resolveRaw(data, "namecolors"), shownName(player));
            case "formatted_name" -> TextUtils.toLegacyAmpersand(
                    "<reset>" + resolveRaw(data, "prefixes") + "<reset>"
                            + "<reset>" + resolveRaw(data, "namecolors") + shownName(player) + "<reset>"
                            + "<reset>" + resolveRaw(data, "icons") + "<reset>");

            // Formato/color crudo (ej. "&#f8f8ff") de cada cosmético textual -
            // útil para que otros plugins (holograma, TAB, scoreboard) lo
            // apliquen ellos mismos sin tener que leer el YAML de Mincore.
            case "namecolor" -> legacyColorPrefix(data, "namecolors");
            case "chatcolor" -> legacyColorPrefix(data, "chatcolors");
            case "prefix" -> TextUtils.toLegacyAmpersand(resolveRaw(data, "prefixes"));
            case "icon" -> TextUtils.toLegacyAmpersand(resolveRaw(data, "icons"));
            case "glow" -> resolveRaw(data, "glows");

            default -> resolveDynamic(player, data, key);
        };
    }

    /**
     * Placeholders con parámetros: active_/active_name_/active_price_ operan
     * sobre el cosmético EQUIPADO de una categoría; unlocked_/total_ sobre la
     * categoría completa; name_/price_/status_ sobre un ítem específico
     * (categoría + id).
     */
    private String resolveDynamic(Player player, PlayerData data, String key) {
        // Pozo Millonario (equivalente del PlaceholderHook de ACubelets):
        // %mincore_pozo_puntos%, %mincore_pozo_cajas% (total) y
        // %mincore_pozo_cajas_<tipo>% (por tipo de caja).
        if (key.startsWith("pozo_")) {
            org.dqnylux.mincore.pozomillonario.model.PozoPlayerData pozo = plugin.getPozoDataManager().get(player.getUniqueId());
            if (key.equals("pozo_puntos") || key.equals("pozo_points")) {
                return pozo == null ? "0" : String.valueOf(pozo.getPoints());
            }
            if (key.equals("pozo_cajas")) {
                int total = 0;
                if (pozo != null) {
                    for (int amount : pozo.getCubelets().values()) total += amount;
                }
                return String.valueOf(total);
            }
            if (key.startsWith("pozo_cajas_")) {
                return String.valueOf(pozo == null ? 0 : pozo.getCubeletCount(key.substring("pozo_cajas_".length())));
            }
        }

        if (key.startsWith("active_name_")) {
            CosmeticItem item = activeItem(data, key.substring("active_name_".length()));
            return item == null ? "" : TextUtils.formatLegacy(item.displayName);
        }
        if (key.startsWith("active_price_")) {
            CosmeticItem item = activeItem(data, key.substring("active_price_".length()));
            return item == null ? "0" : String.valueOf(item.price);
        }
        if (key.startsWith("active_")) {
            return orEmpty(data.getActiveCosmetic(key.substring("active_".length())));
        }
        if (key.startsWith("unlocked_")) {
            return String.valueOf(countUnlocked(player, data, key.substring("unlocked_".length())));
        }
        if (key.startsWith("total_")) {
            StandardCosmeticConfig config = plugin.getCosmeticConfigManager().getCategory(key.substring("total_".length()));
            return String.valueOf(config == null ? 0 : config.items.size());
        }

        String[] parts = key.split("_", 3);
        if (parts.length == 3) {
            CosmeticItem item = plugin.getCosmeticConfigManager().getItem(parts[1], parts[2]);
            if (item == null) return "";

            return switch (parts[0]) {
                case "name" -> TextUtils.formatLegacy(item.displayName);
                case "price" -> String.valueOf(item.price);
                case "status" -> TextUtils.formatLegacy(statusOf(player, data, parts[1], parts[2], item));
                default -> null;
            };
        }

        return null;
    }

    private CosmeticItem activeItem(PlayerData data, String category) {
        String id = data.getActiveCosmetic(category);
        return id == null ? null : plugin.getCosmeticConfigManager().getItem(category, id);
    }

    private long countUnlocked(Player player, PlayerData data, String category) {
        StandardCosmeticConfig config = plugin.getCosmeticConfigManager().getCategory(category);
        if (config == null) return 0;

        return config.items.entrySet().stream()
                .filter(entry -> CosmeticsGui.isAccessible(plugin, player, category, entry.getKey(), entry.getValue()))
                .count();
    }

    private String statusOf(Player player, PlayerData data, String category, String id, CosmeticItem cosmetic) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        boolean equipped = id.equals(data.getActiveCosmetic(category));
        boolean accessible = CosmeticsGui.isAccessible(plugin, player, category, id, cosmetic);
        return String.join(" ", CosmeticsGui.lockedStatus(messages, data, cosmetic, equipped, accessible, player)).trim();
    }

    /** Valor crudo del cosmético equipado (tal cual está escrito en el YAML), sin equipar = "". */
    /** Nombre a mostrar de este jugador: el falso si tiene un disguise activo, si no el real. */
    private String shownName(Player player) {
        return plugin.getDisguiseManager() != null ? plugin.getDisguiseManager().displayName(player) : player.getName();
    }

    private String resolveRaw(PlayerData data, String category) {
        String id = data.getActiveCosmetic(category);
        if (id == null) return "";

        CosmeticItem item = plugin.getCosmeticConfigManager().getItem(category, id);
        return item != null && item.value != null ? item.value : "";
    }

    /**
     * "raw" para namecolors/chatcolors es SOLO el tag de color (ej. "<#f8f8ff>"
     * o "<gradient:#a:#b>"), sin texto propio - pensado para pegarse delante
     * del nombre del jugador (sección "colored_name"/"formatted_name" arriba).
     * Serializar ese tag SOLO (sin texto) a legacy da string vacío, porque no
     * hay ningún carácter al que aplicarle el color - por eso se arma el tag
     * + el texto real ANTES de convertir a legacy, nunca por separado
     * (mismo motivo por el que un &#RRGGBB "suelto" en TAB/LuckPerms no
     * pintaba nada).
     */
    private String coloredText(String colorTag, String text) {
        return TextUtils.toLegacyAmpersand(colorTag + text);
    }

    /**
     * Devuelve SOLO el código de color legado (&#rrggbb) de un cosmético
     * textual, para plugins externos que arman su propio formato y solo
     * necesitan el color - envuelve el tag con un carácter marcador
     * invisible (para que la serialización no quede vacía) y lo descarta
     * después, quedándose únicamente con los códigos de color emitidos.
     */
    private static final String COLOR_PROBE_MARKER = "⁣";

    private String legacyColorPrefix(PlayerData data, String category) {
        String raw = resolveRaw(data, category);
        if (raw.isEmpty()) return "";
        return TextUtils.toLegacyAmpersand(raw + COLOR_PROBE_MARKER).replace(COLOR_PROBE_MARKER, "");
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
