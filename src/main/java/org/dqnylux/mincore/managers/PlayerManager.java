package org.dqnylux.mincore.managers;

import org.bukkit.Bukkit;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.model.PlayerData;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché en memoria de jugadores online: fuente de verdad mientras el jugador
 * está conectado, hidratada desde la base de datos al entrar y persistida al
 * salir/apagar. Sin lecturas a la BD durante el gameplay normal.
 */
public class PlayerManager {

    private final Mincore plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public CompletableFuture<PlayerData> loadPlayer(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
            if (dataSource == null) {
                PlayerData fallback = new PlayerData(uuid, name, 0.0, true, 0, true);
                cache.put(uuid, fallback);
                return fallback;
            }

            try (Connection connection = dataSource.getConnection()) {
                upsertPlayerRow(connection, uuid, name);

                PlayerData data;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT name, coins, global_chat, chat_warnings, messages_enabled FROM mincore_players WHERE uuid = ?")) {
                    select.setString(1, uuid.toString());
                    try (ResultSet rs = select.executeQuery()) {
                        data = rs.next()
                                ? new PlayerData(uuid, rs.getString("name"), rs.getDouble("coins"),
                                        rs.getBoolean("global_chat"), rs.getInt("chat_warnings"), rs.getBoolean("messages_enabled"))
                                : new PlayerData(uuid, name, 0.0, true, 0, true);
                    }
                }

                loadActiveCosmetics(connection, data);
                loadUnlockedCosmetics(connection, data);

                cache.put(uuid, data);
                return data;
            } catch (SQLException e) {
                Bukkit.getLogger().severe("[Mincore] Error cargando jugador " + name + ": " + e.getMessage());
                PlayerData fallback = new PlayerData(uuid, name, 0.0, true, 0, true);
                cache.put(uuid, fallback);
                return fallback;
            }
        });
    }

    private void upsertPlayerRow(Connection connection, UUID uuid, String name) throws SQLException {
        boolean sqlite = plugin.getDatabaseManager().getStorageType() == DatabaseManager.StorageType.SQLITE;
        String sql = sqlite
                ? "INSERT INTO mincore_players (uuid, name, coins) VALUES (?, ?, 0.0) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name"
                : "INSERT INTO mincore_players (uuid, name, coins) VALUES (?, ?, 0.0) ON DUPLICATE KEY UPDATE name = VALUES(name)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    private void loadActiveCosmetics(Connection connection, PlayerData data) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT category, cosmetic_id FROM mincore_active_cosmetics WHERE uuid = ?")) {
            select.setString(1, data.getUuid().toString());
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    data.setActiveCosmetic(rs.getString("category"), rs.getString("cosmetic_id"));
                }
            }
        }
    }

    private void loadUnlockedCosmetics(Connection connection, PlayerData data) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT category, cosmetic_id FROM mincore_unlocks WHERE uuid = ?")) {
            select.setString(1, data.getUuid().toString());
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    data.unlockCosmetic(rs.getString("category"), rs.getString("cosmetic_id"));
                }
            }
        }
    }

    public CompletableFuture<Void> savePlayerAsync(PlayerData data) {
        return CompletableFuture.runAsync(() -> savePlayerSync(data));
    }

    public void savePlayerSync(PlayerData data) {
        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return;

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE mincore_players SET name = ?, coins = ?, global_chat = ?, chat_warnings = ?, messages_enabled = ? WHERE uuid = ?")) {
                statement.setString(1, data.getName());
                statement.setDouble(2, data.getCoins());
                statement.setBoolean(3, data.isGlobalChat());
                statement.setInt(4, data.getChatWarnings());
                statement.setBoolean(5, data.isMessagesEnabled());
                statement.setString(6, data.getUuid().toString());
                statement.executeUpdate();
            }

            syncActiveCosmetics(connection, data);
            syncUnlockedCosmetics(connection, data);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[Mincore] Error guardando jugador " + data.getName() + ": " + e.getMessage());
        }
    }

    private void syncActiveCosmetics(Connection connection, PlayerData data) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM mincore_active_cosmetics WHERE uuid = ?")) {
            delete.setString(1, data.getUuid().toString());
            delete.executeUpdate();
        }
        if (data.getActiveCosmetics().isEmpty()) return;

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO mincore_active_cosmetics (uuid, category, cosmetic_id) VALUES (?, ?, ?)")) {
            for (Map.Entry<String, String> entry : data.getActiveCosmetics().entrySet()) {
                insert.setString(1, data.getUuid().toString());
                insert.setString(2, entry.getKey());
                insert.setString(3, entry.getValue());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void syncUnlockedCosmetics(Connection connection, PlayerData data) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM mincore_unlocks WHERE uuid = ?")) {
            delete.setString(1, data.getUuid().toString());
            delete.executeUpdate();
        }
        if (data.getUnlockedCosmetics().isEmpty()) return;

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO mincore_unlocks (uuid, cosmetic_id, category) VALUES (?, ?, ?)")) {
            for (String namespaced : data.getUnlockedCosmetics()) {
                int separator = namespaced.indexOf(':');
                if (separator < 0) continue;
                String category = namespaced.substring(0, separator);
                String itemId = namespaced.substring(separator + 1);

                insert.setString(1, data.getUuid().toString());
                insert.setString(2, itemId);
                insert.setString(3, category);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public void saveAndRemoveAsync(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            CompletableFuture.runAsync(() -> savePlayerSync(data));
        }
    }

    public void saveAllSync() {
        for (PlayerData data : cache.values()) {
            savePlayerSync(data);
        }
    }
}
