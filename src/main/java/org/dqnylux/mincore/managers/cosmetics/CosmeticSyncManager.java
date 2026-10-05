package org.dqnylux.mincore.managers.cosmetics;

import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.StandardCosmeticConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.managers.DatabaseManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Sincroniza el CATÁLOGO (nombre/precio/material) de las categorías estándar
 * entre servidores que comparten la misma base de datos - no la propiedad de
 * cada jugador (eso vive en mincore_unlocks/mincore_active_cosmetics, ya
 * compartido automáticamente si apuntan a la misma BD). La tabla
 * mincore_global_cosmetics ya la crea DatabaseManager - no hay createTable()
 * aparte aquí, a diferencia del prompt original, para no mantener el mismo
 * esquema en dos sitios.
 *
 * SIN poll periódico a propósito (igual que ConfigSyncManager) - solo se
 * mueve con /coreec sync push|pull explícito, o con el pub/sub de Redis como
 * reacción directa a ESE mismo comando en otro server.
 *
 * Categorías filtradas por database.yml -> sync.syncedFiles (BUG REAL
 * encontrado y corregido: antes empujaba/traía TODAS las
 * STANDARD_CATEGORIES sin condición, ignorando esa lista por completo - la
 * lista solo gobernaba a ConfigSyncManager, que sincroniza el TEXTO
 * completo de esos mismos archivos por un camino aparte. Como las dos
 * sincronizaciones cubrían la misma data cosmética por mecanismos
 * distintos, achicar la lista en el YAML no sacaba nada de esta - ahora
 * ambas respetan la MISMA lista, una sola fuente de verdad).
 */
public class CosmeticSyncManager {

    private final Mincore plugin;

    public CosmeticSyncManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public void stop() {
    }

    /** "namecolors" -> ¿está "cosmetics/namecolors.yml" en sync.syncedFiles? Misma lista que usa ConfigSyncManager, solo traducida de nombre de categoría a nombre de archivo (STANDARD_CATEGORIES usa guiones, los archivos guión-bajo). */
    private boolean isSynced(String category) {
        List<String> syncedFiles = plugin.getConfigManager().getDatabaseConfig().sync.syncedFiles;
        return syncedFiles.contains("cosmetics/" + category.replace('-', '_') + ".yml");
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
                    if (!isSynced(category)) continue;
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
            plugin.getDatabaseManager().publishRedis("coreec:cosmetics:updated", "catalog");
        });
    }

    public CompletableFuture<Void> pullFromDatabase() {
        if (!networkModeActive()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
            if (dataSource == null) return;

            // SOLO se reescribe a disco la categoría que realmente tuvo un
            // valor distinto - antes se reescribían las 13 SIEMPRE, cada 5
            // minutos, aunque nada hubiera cambiado. Eso generaba bytes
            // apenas distintos a lo que ConfigSyncManager tiene guardado en
            // mincore_config_sync para esos mismos archivos (otro sync
            // aparte, también cada 5 min, sobre el TEXTO completo del
            // archivo) - su propio pullAll() los veía como "cambiados",
            // los volvía a pisar Y disparaba reloadEverything() (recarga
            // completa bloqueando el hilo principal) - un loop que se
            // retroalimentaba solo cada 5 minutos sin que nada real hubiera
            // cambiado nunca, causa raíz del log repetido y el lag.
            java.util.Set<String> changedCategories = new java.util.HashSet<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT category, item_id, display_name, value, material, price FROM mincore_global_cosmetics");
                 ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    String category = rs.getString("category");
                    if (!isSynced(category)) continue;
                    StandardCosmeticConfig config = plugin.getCosmeticConfigManager().getCategory(category);
                    if (config == null) continue;

                    CosmeticItem item = config.items.get(rs.getString("item_id"));
                    if (item == null) continue;

                    String displayName = rs.getString("display_name");
                    String value = rs.getString("value");
                    String material = rs.getString("material");
                    double price = rs.getDouble("price");

                    boolean itemChanged = !java.util.Objects.equals(item.displayName, displayName)
                            || !java.util.Objects.equals(item.value, value)
                            || !java.util.Objects.equals(item.material, material)
                            || item.price != price;
                    if (!itemChanged) continue;

                    item.displayName = displayName;
                    item.value = value;
                    item.material = material;
                    item.price = price;
                    changedCategories.add(category);
                }

                for (String category : changedCategories) {
                    plugin.getCosmeticConfigManager().saveStandardCategory(category);
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
