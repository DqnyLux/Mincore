package org.dqnylux.mincore.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.model.ResolvedPlayer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
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

    /**
     * Resuelve un nombre a (UUID, nombre real guardado) sin importar si el
     * jugador está conectado ahora mismo - a diferencia de
     * Bukkit.getOfflinePlayer(String) (deprecado, puede disparar una consulta
     * bloqueante a la API de Mojang para nombres que nunca vio localmente),
     * esto solo mira mincore_players, que YA tiene a cualquiera que alguna
     * vez se haya conectado a ESTE server (se upsertea en cada loadPlayer) -
     * cero red, mismo criterio "cero llamadas bloqueantes al hilo que llama"
     * que el resto de los *DataManager de este proyecto. null si nadie con
     * ese nombre jugó nunca acá.
     */
    public CompletableFuture<ResolvedPlayer> resolveOnlineOrOffline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return CompletableFuture.completedFuture(new ResolvedPlayer(online.getUniqueId(), online.getName()));

        return CompletableFuture.supplyAsync(() -> {
            DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
            if (dataSource == null) return null;

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement select = connection.prepareStatement(
                         "SELECT uuid, name FROM mincore_players WHERE LOWER(name) = LOWER(?)")) {
                select.setString(1, name);
                try (ResultSet rs = select.executeQuery()) {
                    if (!rs.next()) return null;
                    return new ResolvedPlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("[CoreEC] Error resolviendo jugador " + name + ": " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<PlayerData> loadPlayer(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
            if (dataSource == null) {
                PlayerData fallback = new PlayerData(uuid, name, 0.0, 0.0, true, 0, true, true, true);
                cache.put(uuid, fallback);
                return fallback;
            }

            try (Connection connection = dataSource.getConnection()) {
                upsertPlayerRow(connection, uuid, name);

                PlayerData data;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT name, nickname, coins, sucres, global_chat, chat_warnings, messages_enabled, mentions_enabled, titles_enabled FROM mincore_players WHERE uuid = ?")) {
                    select.setString(1, uuid.toString());
                    try (ResultSet rs = select.executeQuery()) {
                        if (rs.next()) {
                            data = new PlayerData(uuid, rs.getString("name"), rs.getDouble("coins"), rs.getDouble("sucres"),
                                    rs.getBoolean("global_chat"), rs.getInt("chat_warnings"),
                                    rs.getBoolean("messages_enabled"), rs.getBoolean("mentions_enabled"),
                                    rs.getBoolean("titles_enabled"));
                            data.setNickname(rs.getString("nickname"));
                        } else {
                            data = new PlayerData(uuid, name, 0.0, 0.0, true, 0, true, true, true);
                        }
                    }
                }

                loadActiveCosmetics(connection, data);
                loadUnlockedCosmetics(connection, data);
                loadActiveFormats(connection, data);

                cache.put(uuid, data);
                return data;
            } catch (SQLException e) {
                Bukkit.getLogger().severe("[CoreEC] Error cargando jugador " + name + ": " + e.getMessage());
                PlayerData fallback = new PlayerData(uuid, name, 0.0, 0.0, true, 0, true, true, true);
                cache.put(uuid, fallback);
                return fallback;
            }
        });
    }

    private void upsertPlayerRow(Connection connection, UUID uuid, String name) throws SQLException {
        boolean sqlite = plugin.getDatabaseManager().getStorageType() == DatabaseManager.StorageType.SQLITE;
        String sql = sqlite
                ? "INSERT INTO mincore_players (uuid, name, coins, sucres) VALUES (?, ?, 0.0, 0.0) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name"
                : "INSERT INTO mincore_players (uuid, name, coins, sucres) VALUES (?, ?, 0.0, 0.0) ON DUPLICATE KEY UPDATE name = VALUES(name)";

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

    private void loadActiveFormats(Connection connection, PlayerData data) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT scope, format_id FROM mincore_active_formats WHERE uuid = ?")) {
            select.setString(1, data.getUuid().toString());
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    data.toggleFormat(rs.getString("scope"), rs.getString("format_id"));
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
                    "UPDATE mincore_players SET name = ?, nickname = ?, coins = ?, sucres = ?, global_chat = ?, chat_warnings = ?, messages_enabled = ?, mentions_enabled = ?, titles_enabled = ? WHERE uuid = ?")) {
                statement.setString(1, data.getName());
                statement.setString(2, data.getNickname());
                statement.setDouble(3, data.getCoins());
                statement.setDouble(4, data.getSucres());
                statement.setBoolean(5, data.isGlobalChat());
                statement.setInt(6, data.getChatWarnings());
                statement.setBoolean(7, data.isMessagesEnabled());
                statement.setBoolean(8, data.isMentionsEnabled());
                statement.setBoolean(9, data.isTitlesEnabled());
                statement.setString(10, data.getUuid().toString());
                statement.executeUpdate();
            }

            syncActiveCosmetics(connection, data);
            syncActiveFormats(connection, data);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error guardando jugador " + data.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Persiste UN desbloqueo puntual, de forma idempotente (INSERT-si-no-existe,
     * nunca DELETE) - llamar justo después de data.unlockCosmetic(category, id)
     * en cada lugar que otorga uno (compra en el menú, recompensa de Pozo
     * Millonario...).
     *
     * Por qué no vive dentro de savePlayerSync/syncUnlockedCosmetics como
     * antes: ese método hacía DELETE de TODOS los desbloqueos del jugador y
     * volvía a INSERTar el Set completo en cada llamada a savePlayerAsync -
     * pero savePlayerAsync se llama constantemente desde lugares sin relación
     * (equipar OTRO cosmético, cambiar una config, etc.), cada uno en su
     * propio hilo async sin ningún orden garantizado entre sí. Si DOS guardados
     * para el mismo jugador se solapaban, el que terminaba de escribir
     * ÚLTIMO (no necesariamente el que arrancó último) pisaba la tabla entera
     * con SU snapshot en memoria - si ese snapshot se había tomado ANTES de
     * que se otorgara un cosmético nuevo (ej. justo antes de abrir una caja
     * de Pozo Millonario), el DELETE+INSERT del guardado más lento borraba el
     * desbloqueo recién dado. El jugador seguía viéndolo mientras seguía
     * conectado (vivía en el Set en memoria), pero desaparecía para siempre
     * en el próximo login - causa raíz real de "los cosméticos ganados con
     * las cajas desaparecen". Como un desbloqueo NUNCA se quita una vez dado
     * (es monótono - nada en el proyecto llama a lo contrario de
     * unlockCosmetic), no hace falta re-sincronizar el Set completo nunca:
     * insertar cada desbloqueo una sola vez, en el momento exacto en que se
     * otorga, elimina la carrera de raíz.
     */
    public void persistUnlock(UUID uuid, String category, String itemId) {
        CompletableFuture.runAsync(() -> {
            DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
            if (dataSource == null) return;

            boolean sqlite = plugin.getDatabaseManager().getStorageType() == DatabaseManager.StorageType.SQLITE;
            String sql = sqlite
                    ? "INSERT INTO mincore_unlocks (uuid, cosmetic_id, category) VALUES (?, ?, ?) ON CONFLICT(uuid, category, cosmetic_id) DO NOTHING"
                    : "INSERT IGNORE INTO mincore_unlocks (uuid, cosmetic_id, category) VALUES (?, ?, ?)";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, itemId);
                statement.setString(3, category);
                statement.executeUpdate();
            } catch (SQLException e) {
                Bukkit.getLogger().severe("[CoreEC] Error guardando desbloqueo " + category + ":" + itemId + " para " + uuid + ": " + e.getMessage());
            }
        });
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

    private void syncActiveFormats(Connection connection, PlayerData data) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM mincore_active_formats WHERE uuid = ?")) {
            delete.setString(1, data.getUuid().toString());
            delete.executeUpdate();
        }

        List<Map.Entry<String, String>> rows = new java.util.ArrayList<>();
        for (String formatId : data.getActiveFormats(PlayerData.FORMAT_SCOPE_CHAT)) {
            rows.add(Map.entry(PlayerData.FORMAT_SCOPE_CHAT, formatId));
        }
        for (String formatId : data.getActiveFormats(PlayerData.FORMAT_SCOPE_NAME)) {
            rows.add(Map.entry(PlayerData.FORMAT_SCOPE_NAME, formatId));
        }
        if (rows.isEmpty()) return;

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO mincore_active_formats (uuid, scope, format_id) VALUES (?, ?, ?)")) {
            for (Map.Entry<String, String> row : rows) {
                insert.setString(1, data.getUuid().toString());
                insert.setString(2, row.getKey());
                insert.setString(3, row.getValue());
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

    public boolean hasAccount(UUID uuid) {
        if (cache.containsKey(uuid)) return true;
        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM mincore_players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error comprobando cuenta de " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public double getCoins(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) return data.getCoins();

        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return 0.0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT coins FROM mincore_players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getDouble("coins") : 0.0;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error leyendo coins de " + uuid + ": " + e.getMessage());
            return 0.0;
        }
    }

    public boolean hasCoins(UUID uuid, double amount) {
        return amount >= 0 && Double.isFinite(amount) && getCoins(uuid) >= amount;
    }

    public boolean depositCoins(UUID uuid, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.addCoins(amount);
            savePlayerAsync(data);
            return true;
        }

        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE mincore_players SET coins = coins + ? WHERE uuid = ?")) {
            statement.setDouble(1, amount);
            statement.setString(2, uuid.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error depositando coins a " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public boolean withdrawCoins(UUID uuid, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        PlayerData data = cache.get(uuid);
        if (data != null) {
            if (data.getCoins() < amount) return false;
            data.removeCoins(amount);
            savePlayerAsync(data);
            return true;
        }

        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE mincore_players SET coins = coins - ? WHERE uuid = ? AND coins >= ?")) {
            statement.setDouble(1, amount);
            statement.setString(2, uuid.toString());
            statement.setDouble(3, amount);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error retirando coins de " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public boolean setCoins(UUID uuid, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.setCoins(amount);
            savePlayerAsync(data);
            return true;
        }

        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE mincore_players SET coins = ? WHERE uuid = ?")) {
            statement.setDouble(1, amount);
            statement.setString(2, uuid.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error estableciendo coins a " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public double getSucres(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) return data.getSucres();

        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return 0.0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT sucres FROM mincore_players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getDouble("sucres") : 0.0;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error leyendo sucres de " + uuid + ": " + e.getMessage());
            return 0.0;
        }
    }

    public boolean hasSucres(UUID uuid, double amount) {
        return amount >= 0 && Double.isFinite(amount) && getSucres(uuid) >= amount;
    }

    public boolean depositSucres(UUID uuid, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.addSucres(amount);
            savePlayerAsync(data);
            return true;
        }

        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE mincore_players SET sucres = sucres + ? WHERE uuid = ?")) {
            statement.setDouble(1, amount);
            statement.setString(2, uuid.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error depositando sucres a " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public boolean withdrawSucres(UUID uuid, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        PlayerData data = cache.get(uuid);
        if (data != null) {
            if (data.getSucres() < amount) return false;
            data.removeSucres(amount);
            savePlayerAsync(data);
            return true;
        }

        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE mincore_players SET sucres = sucres - ? WHERE uuid = ? AND sucres >= ?")) {
            statement.setDouble(1, amount);
            statement.setString(2, uuid.toString());
            statement.setDouble(3, amount);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error retirando sucres de " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public boolean setSucres(UUID uuid, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.setSucres(amount);
            savePlayerAsync(data);
            return true;
        }

        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE mincore_players SET sucres = ? WHERE uuid = ?")) {
            statement.setDouble(1, amount);
            statement.setString(2, uuid.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error estableciendo sucres a " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<List<org.dqnylux.mincore.model.BalanceEntry>> getTopBalances(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<org.dqnylux.mincore.model.BalanceEntry> list = new java.util.ArrayList<>();
            DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
            if (dataSource == null) return list;

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT name, uuid, coins FROM mincore_players ORDER BY coins DESC LIMIT ?")) {
                statement.setInt(1, Math.max(1, limit));
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("name");
                            String nickname = rs.getString("nickname");
                        String uuidStr = rs.getString("uuid");
                        double coins = rs.getDouble("coins");
                        try {
                            UUID u = UUID.fromString(uuidStr);
                            PlayerData cached = cache.get(u);
                            if (cached != null) {
                                coins = cached.getCoins();
                            }
                        } catch (Exception ignored) {}
                        list.add(new org.dqnylux.mincore.model.BalanceEntry(name, coins));
                    }
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("[CoreEC] Error obteniendo top balances: " + e.getMessage());
            }

            list.sort((a, b) -> Double.compare(b.coins(), a.coins()));
            return list;
        });
    }
}
