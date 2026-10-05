package org.dqnylux.mincore.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.afk.managers.AfkManager;
import org.dqnylux.mincore.afk.model.AfkPlayer;
import org.dqnylux.mincore.afk.model.AfkZone;
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
            case "coins", "coins_raw" -> String.valueOf(data.getCoins());
            case "coins_formatted" -> TextUtils.formatAmount(data.getCoins());
            case "sucres", "sucre", "sucres_raw", "sucre_raw", "gems" -> String.valueOf(data.getSucres());
            case "sucres_formatted", "sucre_formatted", "gems_formatted" -> TextUtils.formatAmount(data.getSucres());
            case "setting_globalchat" -> String.valueOf(data.isGlobalChat());
            case "setting_messages" -> String.valueOf(data.isMessagesEnabled());
            case "tracking" -> String.valueOf(plugin.getDeathGPSTask().isTracking(player.getUniqueId()));

            // Identity, Disguise & Nickname unified master placeholders
            case "name", "colored_name", "displayname", "display_name", "player_name" -> resolveDisplayName(player, data);
            case "clean_name", "plain_name", "name_clean", "name_plain" -> TextUtils.stripColors(shownName(player));
            case "raw_name", "name_raw" -> shownName(player);
            case "real_name" -> player.getName();
            case "is_disguised" -> String.valueOf(plugin.getDisguiseManager() != null && plugin.getDisguiseManager().isDisguised(player));
            case "nickname" -> data.getNickname() != null ? data.getNickname() : "";
            case "nickname_clean", "nickname_plain" -> data.getNickname() != null ? TextUtils.stripColors(data.getNickname()) : "";
            case "has_nickname", "is_nicknamed" -> String.valueOf(data.getNickname() != null && !data.getNickname().isBlank());
            case "formatted_name" -> formatFullName(player, data);

            // AFK System placeholders
            case "afk", "is_afk" -> String.valueOf(plugin.getAfkManager() != null && plugin.getAfkManager().isAfk(player));
            case "afk_formatted" -> plugin.getAfkManager() != null && plugin.getAfkManager().isAfk(player)
                    ? TextUtils.toLegacyAmpersand(plugin.getConfigManager().getAfkConfig().messages.statusAfk)
                    : TextUtils.toLegacyAmpersand(plugin.getConfigManager().getAfkConfig().messages.statusActive);
            case "afk_time" -> plugin.getAfkManager() != null && plugin.getAfkManager().getAfkPlayer(player.getUniqueId()) != null
                    ? plugin.getAfkManager().getAfkPlayer(player.getUniqueId()).formatAfkDuration() : "0s";
            case "afk_reason" -> plugin.getAfkManager() != null && plugin.getAfkManager().getAfkPlayer(player.getUniqueId()) != null
                    ? plugin.getAfkManager().getAfkPlayer(player.getUniqueId()).getReason() : "";
            case "afk_count" -> String.valueOf(plugin.getAfkManager() != null ? plugin.getAfkManager().getAfkCount() : 0);
            case "afk_zone" -> plugin.getAfkManager() != null && plugin.getAfkManager().getAfkPlayer(player.getUniqueId()) != null
                    && plugin.getAfkManager().getAfkPlayer(player.getUniqueId()).getCurrentZone() != null
                    ? plugin.getAfkManager().getAfkPlayer(player.getUniqueId()).getCurrentZone() : "";

            // Cosmetics unlocked counters
            case "cosmetics_unlocked", "unlocked_cosmetics" -> String.valueOf(data.getUnlockedCosmetics().size());
            case "global_unlocked" -> String.valueOf(CosmeticsGui.globalUnlocked(plugin, player));
            case "global_total" -> String.valueOf(CosmeticsGui.globalTotal(plugin));

            // Formato/color crudo (ej. "&#f8f8ff") de cada cosmético textual -
            // útil para que otros plugins (holograma, TAB, scoreboard) lo
            // apliquen ellos mismos sin tener que leer el YAML de Mincore.
            case "namecolor" -> legacyColorPrefix(data, "namecolors");
            case "chatcolor" -> legacyColorPrefix(data, "chatcolors");
            case "prefix" -> TextUtils.toLegacyAmpersand(resolveRaw(data, "prefixes"));
            case "icon" -> TextUtils.toLegacyAmpersand(resolveRaw(data, "icons"));
            case "glow" -> resolveRaw(data, "glows");

            // FlyTime Module placeholders
            case "flytime", "flytime_formatted" -> resolveFlyTime(player, "formatted");
            case "flytime_raw", "flytime_seconds" -> resolveFlyTime(player, "raw");
            case "flytime_active", "flytime_enabled" -> resolveFlyTime(player, "enabled");
            case "flytime_flying" -> String.valueOf(player.isFlying());
            case "flytime_has_bypass" -> resolveFlyTime(player, "bypass");
            case "flytime_status" -> resolveFlyTime(player, "status");

            default -> resolveDynamic(player, data, key);
        };
    }

    private String resolveFlyTime(Player player, String type) {
        if (plugin.getFlyTimeManager() == null) return "";
        org.dqnylux.mincore.flytime.model.FlyTimePlayer ftPlayer = plugin.getFlyTimeManager().getPlayer(player.getUniqueId());
        boolean hasBypass = plugin.getFlyTimeManager().hasBypass(player);

        return switch (type) {
            case "raw" -> ftPlayer != null ? String.valueOf(ftPlayer.getSeconds()) : "0";
            case "bypass" -> String.valueOf(hasBypass);
            case "enabled" -> ftPlayer != null ? String.valueOf(ftPlayer.isEnabled()) : "false";
            case "formatted" -> {
                if (hasBypass) yield "∞";
                yield ftPlayer != null ? ftPlayer.formatDuration() : "0s";
            }
            case "status" -> {
                if (hasBypass) yield "Bypass (∞)";
                if (ftPlayer == null || !ftPlayer.hasTime()) yield "Sin tiempo";
                yield ftPlayer.isEnabled() ? "Activo" : "Desactivado";
            }
            default -> "";
        };
    }

    /**
     * Placeholders con parámetros: active_/active_name_/active_price_ operan
     * sobre el cosmético EQUIPADO de una categoría; unlocked_/total_ sobre la
     * categoría completa; name_/price_/status_ sobre un ítem específico
     * (categoría + id).
     */
    private String resolveDynamic(Player player, PlayerData data, String key) {
        if (key.startsWith("afk_zone_")) {
            String res = resolveAfkZone(player, key);
            if (res != null) return res;
        }

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

        // TheRewards Module Placeholders
        if (key.startsWith("rewards_")) {
            String res = resolveRewards(player, key);
            if (res != null) return res;
        }

        // BetterProfiles Module Placeholders
        if (key.startsWith("profile_")) {
            String res = resolveProfile(player, key);
            if (res != null) return res;
        }

        // PlayerTimeLimit Module Placeholders
        if (key.startsWith("timelimit_")) {
            String res = resolveTimeLimit(player, key);
            if (res != null) return res;
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

    /**
     * Resuelve el nombre maestro unificado del jugador para TAB, Scoreboards,
     * Hologramas y Chat. Si está disfrazado (/disguise), devuelve el nombre falso
     * sin filtrar cosméticos personales. Si tiene apodo (/nick), devuelve el apodo
     * con su formato y colores aplicados (o el cosmético namecolor si el apodo no
     * define un color propio). Si no tiene apodo ni disfraz, devuelve el nombre
     * real con el cosmético namecolor activo.
     */
    private String resolveDisplayName(Player player, PlayerData data) {
        if (plugin.getDisguiseManager() != null && plugin.getDisguiseManager().isDisguised(player)) {
            return plugin.getDisguiseManager().displayName(player);
        }
        String shown = shownName(player);
        String colorTag = resolveRaw(data, "namecolors");
        return coloredText(colorTag, shown);
    }

    private String formatFullName(Player player, PlayerData data) {
        if (plugin.getDisguiseManager() != null && plugin.getDisguiseManager().isDisguised(player)) {
            return plugin.getDisguiseManager().displayName(player);
        }
        return TextUtils.toLegacyAmpersand(
                "<reset>" + resolveRaw(data, "prefixes") + "<reset>"
                        + "<reset>" + resolveRaw(data, "namecolors") + shownName(player) + "<reset>"
                        + "<reset>" + resolveRaw(data, "icons") + "<reset>");
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

    private String resolveAfkZone(Player player, String key) {
        AfkManager afkManager = plugin.getAfkManager();
        if (afkManager == null) return "";

        AfkPlayer afkPlayer = afkManager.getAfkPlayer(player.getUniqueId());
        Location loc = player.getLocation();
        AfkZone currentZone = afkManager.getZoneAt(loc);

        // Placeholders de la zona actual del jugador
        switch (key) {
            case "afk_zone_display" -> {
                return currentZone != null ? TextUtils.toLegacyAmpersand(currentZone.getDisplayName()) : "";
            }
            case "afk_zone_coins" -> {
                return currentZone != null ? TextUtils.formatAmount(currentZone.getCoins()) : "0";
            }
            case "afk_zone_sucres" -> {
                return currentZone != null ? TextUtils.formatAmount(currentZone.getSucres()) : "0";
            }
            case "afk_zone_interval" -> {
                return currentZone != null ? String.valueOf(currentZone.getRewardIntervalSeconds()) : "0";
            }
            case "afk_zone_has_permission" -> {
                return String.valueOf(currentZone != null && (!currentZone.hasPermission() || player.hasPermission(currentZone.getPermission())));
            }
            case "afk_zone_time_remaining", "afk_zone_time_remaining_formatted", "afk_zone_progress_bar", "afk_zone_progress_percent" -> {
                if (currentZone == null || afkPlayer == null) {
                    return key.contains("percent") ? "0%" : (key.contains("formatted") ? "0s" : (key.contains("bar") ? "" : "0"));
                }
                long elapsed = System.currentTimeMillis() - afkPlayer.getLastZoneReward();
                int interval = currentZone.getRewardIntervalSeconds();
                int remainingSec = Math.max(0, interval - (int) (elapsed / 1000L));
                int currentSec = interval - remainingSec;

                if (key.equals("afk_zone_time_remaining")) {
                    return String.valueOf(remainingSec);
                }
                if (key.equals("afk_zone_time_remaining_formatted")) {
                    return remainingSec < 60 ? remainingSec + "s" : String.format("%02d:%02d", remainingSec / 60, remainingSec % 60);
                }
                if (key.equals("afk_zone_progress_percent")) {
                    return String.format("%.0f%%", ((double) currentSec / interval) * 100.0);
                }
                if (key.equals("afk_zone_progress_bar")) {
                    int totalBars = 20;
                    float percent = interval > 0 ? (float) currentSec / interval : 0f;
                    int progressBars = (int) (totalBars * Math.min(1.0f, Math.max(0.0f, percent)));
                    return TextUtils.toLegacyAmpersand("<#55FF55>" + "■".repeat(progressBars) + "<#555555>" + "■".repeat(Math.max(0, totalBars - progressBars)));
                }
            }
        }

        // Placeholders de zonas por ID específico (ej: %coreec_afk_zone_display_vip_afk%)
        if (key.startsWith("afk_zone_display_")) {
            String zoneId = key.substring("afk_zone_display_".length());
            AfkZone z = afkManager.getZone(zoneId);
            return z != null ? TextUtils.toLegacyAmpersand(z.getDisplayName()) : "";
        }
        if (key.startsWith("afk_zone_coins_")) {
            String zoneId = key.substring("afk_zone_coins_".length());
            AfkZone z = afkManager.getZone(zoneId);
            return z != null ? TextUtils.formatAmount(z.getCoins()) : "0";
        }
        if (key.startsWith("afk_zone_sucres_")) {
            String zoneId = key.substring("afk_zone_sucres_".length());
            AfkZone z = afkManager.getZone(zoneId);
            return z != null ? TextUtils.formatAmount(z.getSucres()) : "0";
        }
        if (key.startsWith("afk_zone_interval_")) {
            String zoneId = key.substring("afk_zone_interval_".length());
            AfkZone z = afkManager.getZone(zoneId);
            return z != null ? String.valueOf(z.getRewardIntervalSeconds()) : "0";
        }
        if (key.startsWith("afk_zone_permission_")) {
            String zoneId = key.substring("afk_zone_permission_".length());
            AfkZone z = afkManager.getZone(zoneId);
            return z != null ? z.getPermission() : "";
        }

        return null;
    }

    private String resolveRewards(Player player, String key) {
        org.dqnylux.mincore.rewards.manager.RewardsManager manager = plugin.getRewardsManager();
        if (manager == null) return null;

        org.dqnylux.mincore.rewards.model.PlayerRewardProfile profile = manager.getDataManager().getProfile(player.getUniqueId());
        if (profile == null) return null;

        switch (key) {
            case "rewards_streak" -> {
                return String.valueOf(profile.getCurrentStreak());
            }
            case "rewards_streak_claimed_today" -> {
                return String.valueOf(profile.hasClaimedStreakToday(manager.getZoneId()));
            }
            case "rewards_playtime" -> {
                long totalSecs = manager.getPlayerPlaySeconds(player);
                return org.dqnylux.mincore.rewards.engine.TimeFormatter.formatSeconds(totalSecs, manager.getRewardsConfig().timeFormat);
            }
            case "rewards_playtime_seconds" -> {
                return String.valueOf(manager.getPlayerPlaySeconds(player));
            }
            case "rewards_playtime_minutes" -> {
                return String.valueOf(manager.getPlayerPlayMinutes(player));
            }
            case "rewards_claimed_milestones" -> {
                return String.valueOf(profile.getClaimedPlayTime().size());
            }
        }

        if (key.startsWith("rewards_cooldown_raw_")) {
            String rewardId = key.substring("rewards_cooldown_raw_".length());
            org.dqnylux.mincore.rewards.model.RewardDefinition reward = manager.getReward(rewardId);
            if (reward == null || reward.oneTime) return "0";
            long last = profile.getCooldown(reward.id);
            long elapsed = (System.currentTimeMillis() - last) / 1000L;
            long remaining = Math.max(0, reward.countdown - elapsed);
            return String.valueOf(remaining);
        }

        if (key.startsWith("rewards_cooldown_")) {
            String rewardId = key.substring("rewards_cooldown_".length());
            org.dqnylux.mincore.rewards.model.RewardDefinition reward = manager.getReward(rewardId);
            if (reward == null || reward.oneTime) return "0s";
            long last = profile.getCooldown(reward.id);
            long elapsed = (System.currentTimeMillis() - last) / 1000L;
            long remaining = Math.max(0, reward.countdown - elapsed);
            if (remaining <= 0) return "0s";
            return org.dqnylux.mincore.rewards.engine.TimeFormatter.formatSeconds(remaining, manager.getRewardsConfig().timeFormat);
        }

        if (key.startsWith("rewards_available_")) {
            String rewardId = key.substring("rewards_available_".length());
            org.dqnylux.mincore.rewards.model.RewardDefinition reward = manager.getReward(rewardId);
            if (reward == null) return "false";
            if (reward.oneTime && profile.isOneTimeClaimed(reward.id)) return "false";
            if (!reward.oneTime && reward.countdown > 0) {
                long last = profile.getCooldown(reward.id);
                long elapsed = (System.currentTimeMillis() - last) / 1000L;
                if (elapsed < reward.countdown) return "false";
            }
            if (reward.requirePermission && reward.permission != null && !reward.permission.isBlank() && !player.hasPermission(reward.permission)) return "false";
            if (reward.requirements != null && !reward.requirements.isEmpty()) {
                for (org.dqnylux.mincore.rewards.model.RewardRequirement req : reward.requirements) {
                    if (!org.dqnylux.mincore.rewards.engine.RewardRequirementChecker.checkRequirement(player, req)) {
                        return "false";
                    }
                }
            }
            return "true";
        }

        return null;
    }

    private String resolveProfile(Player player, String key) {
        if (plugin.getProfileManager() == null) return "";
        org.dqnylux.mincore.profiles.model.UserProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return "";

        return switch (key) {
            case "profile_likes" -> String.valueOf(profile.getLikes());
            case "profile_bio", "profile_status" -> profile.getStatus() != null ? profile.getStatus() : "";
            case "profile_discord" -> orEmpty(profile.getSocial("discord"));
            case "profile_youtube" -> orEmpty(profile.getSocial("youtube"));
            case "profile_twitch" -> orEmpty(profile.getSocial("twitch"));
            case "profile_twitter" -> orEmpty(profile.getSocial("twitter"));
            case "profile_instagram" -> orEmpty(profile.getSocial("instagram"));
            case "profile_tiktok" -> orEmpty(profile.getSocial("tiktok"));
            case "profile_kills" -> String.valueOf(player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS));
            case "profile_deaths" -> String.valueOf(player.getStatistic(org.bukkit.Statistic.DEATHS));
            case "profile_kdr" -> {
                int kills = player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS);
                int deaths = player.getStatistic(org.bukkit.Statistic.DEATHS);
                yield deaths == 0 ? String.format("%.2f", (double) kills) : String.format("%.2f", (double) kills / deaths);
            }
            default -> {
                if (key.startsWith("profile_social_")) {
                    yield orEmpty(profile.getSocial(key.substring("profile_social_".length())));
                }
                yield null;
            }
        };
    }

    private String resolveTimeLimit(Player player, String key) {
        if (plugin.getTimeLimitManager() == null) return "";
        var manager = plugin.getTimeLimitManager();
        var user = manager.getUser(player.getUniqueId());
        boolean hasBypass = player.isOp() || player.hasPermission(plugin.getConfigManager().getTimeLimitConfig().settings.bypassPermission);

        return switch (key) {
            case "timelimit_bypass", "timelimit_unlimited" -> String.valueOf(hasBypass);
            case "timelimit_status" -> hasBypass ? "Ilimitado (∞)" : manager.formatTime(manager.getRemainingGlobalSeconds(player));
            case "timelimit_spent_global", "timelimit_spent" -> user != null ? String.valueOf(user.getSpentSeconds("global")) : "0";
            case "timelimit_spent_global_formatted", "timelimit_spent_formatted" -> user != null ? manager.formatTime(user.getSpentSeconds("global")) : "0s";
            case "timelimit_remaining_global", "timelimit_remaining" -> hasBypass ? "999999" : String.valueOf(manager.getRemainingGlobalSeconds(player));
            case "timelimit_remaining_global_formatted", "timelimit_remaining_formatted" -> hasBypass ? "∞" : manager.formatTime(manager.getRemainingGlobalSeconds(player));
            case "timelimit_max_global", "timelimit_max" -> hasBypass ? "999999" : String.valueOf(manager.getMaxGlobalSeconds(player));
            case "timelimit_max_global_formatted", "timelimit_max_formatted" -> hasBypass ? "∞" : manager.formatTime(manager.getMaxGlobalSeconds(player));
            case "timelimit_spent_world" -> user != null ? String.valueOf(user.getSpentSeconds(player.getWorld().getName())) : "0";
            case "timelimit_spent_world_formatted" -> user != null ? manager.formatTime(user.getSpentSeconds(player.getWorld().getName())) : "0s";
            case "timelimit_remaining_world" -> hasBypass ? "999999" : String.valueOf(manager.getRemainingWorldSeconds(player, player.getWorld().getName()));
            case "timelimit_remaining_world_formatted" -> {
                long maxW = manager.getMaxWorldSeconds(player, player.getWorld().getName());
                if (maxW <= 0) yield "Sin límite";
                yield hasBypass ? "∞" : manager.formatTime(manager.getRemainingWorldSeconds(player, player.getWorld().getName()));
            }
            default -> {
                if (key.startsWith("timelimit_spent_")) {
                    String world = key.substring("timelimit_spent_".length());
                    yield user != null ? String.valueOf(user.getSpentSeconds(world)) : "0";
                }
                if (key.startsWith("timelimit_remaining_formatted_")) {
                    String world = key.substring("timelimit_remaining_formatted_".length());
                    long maxW = manager.getMaxWorldSeconds(player, world);
                    if (maxW <= 0) yield "Sin límite";
                    yield hasBypass ? "∞" : manager.formatTime(manager.getRemainingWorldSeconds(player, world));
                }
                if (key.startsWith("timelimit_remaining_")) {
                    String world = key.substring("timelimit_remaining_".length());
                    yield hasBypass ? "999999" : String.valueOf(manager.getRemainingWorldSeconds(player, world));
                }
                yield null;
            }
        };
    }
}
