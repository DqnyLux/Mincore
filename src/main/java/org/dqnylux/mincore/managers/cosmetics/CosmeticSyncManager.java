package org.dqnylux.mincore.managers.cosmetics;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.StandardCosmeticConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.managers.DatabaseManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sincroniza el CATÁLOGO (nombre/precio/material) de las categorías estándar
 * entre servidores que comparten la misma base de datos - no la propiedad de
 * cada jugador (eso vive en mincore_unlocks/mincore_active_cosmetics, ya
 * compartido automáticamente si apuntan a la misma BD). La tabla
 * mincore_global_cosmetics ya la crea DatabaseManager - no hay createTable()
 * aparte aquí, a diferencia del prompt original, para no mantener el mismo
 * esquema en dos sitios.
 */
public class CosmeticSyncManager {

    private final Mincore plugin;
    private ScheduledTask pullTask;

    public CosmeticSyncManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public void startAutoSync() {
        if (plugin.getDatabaseManager().getStorageType() != DatabaseManager.StorageType.MYSQL
                && plugin.getDatabaseManager().getStorageType() != DatabaseManager.StorageType.MARIADB) {
            return; // solo tiene sentido en red compartida
        }
        pullTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> pullFromDatabase(), 15L, 300L, TimeUnit.SECONDS);
    }

    public void stop() {
        if (pullTask != null) pullTask.cancel();
    }

    private boolean networkModeActive() {
        DatabaseManager.StorageType type = plugin.getDatabaseManager().getStorageType();
        return type == DatabaseManager.StorageType.MYSQL || type == DatabaseManager.StorageType.MARIADB;
    }

    public CompletableFuture<Void> pushToDatabase() {
        if (!networkModeActive()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
            if (dataSource == null) return;

            try (Connection connection = dataSource.getConnection()) {
                for (String category : CosmeticConfigManager.STANDARD_CATEGORIES) {
                    StandardCosmeticConfig config = plugin.getCosmeticConfigManager().getCategory(category);
                    if (config == null) continue;

                    for (Map.Entry<String, CosmeticItem> entry : config.items.entrySet()) {
                        upsertGlobalCosmetic(connection, category, entry.getKey(), entry.getValue());
                    }
                }
            } catch (SQLException e) {
                org.dqnylux.mincore.utils.ConsoleLogger.error("Error en sync push: " + e.getMessage());
                return;
            }
            plugin.getDatabaseManager().publishRedis("mincore:cosmetics:updated", "catalog");
        });
    }

    public CompletableFuture<Void> pullFromDatabase() {
        if (!networkModeActive()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
            if (dataSource == null) return;

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT category, item_id, display_name, value, material, price FROM mincore_global_cosmetics");
                 ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    StandardCosmeticConfig config = plugin.getCosmeticConfigManager().getCategory(rs.getString("category"));
                    if (config == null) continue;

                    CosmeticItem item = config.items.get(rs.getString("item_id"));
                    if (item == null) continue;

                    item.displayName = rs.getString("display_name");
                    item.value = rs.getString("value");
                    item.material = rs.getString("material");
                    item.price = rs.getDouble("price");
                }

                for (StandardCosmeticConfig config : plugin.getCosmeticConfigManager().getCategories().values()) {
                    config.save();
                }
            } catch (SQLException e) {
                org.dqnylux.mincore.utils.ConsoleLogger.error("Error en sync pull: " + e.getMessage());
            }
        });
    }

    private void upsertGlobalCosmetic(Connection connection, String category, String itemId, CosmeticItem item) throws SQLException {
        boolean sqlite = plugin.getDatabaseManager().getStorageType() == DatabaseManager.StorageType.SQLITE;
        String sql = sqlite
                ? "INSERT INTO mincore_global_cosmetics (category, item_id, display_name, value, material, price) VALUES (?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT(category, item_id) DO UPDATE SET display_name=excluded.display_name, value=excluded.value, material=excluded.material, price=excluded.price"
                : "INSERT INTO mincore_global_cosmetics (category, item_id, display_name, value, material, price) VALUES (?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), value=VALUES(value), material=VALUES(material), price=VALUES(price)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            statement.setString(2, itemId);
            statement.setString(3, item.displayName);
            statement.setString(4, item.value);
            statement.setString(5, item.material);
            statement.setDouble(6, item.price);
            statement.executeUpdate();
        }
    }
}
