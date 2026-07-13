package org.dqnylux.mincore.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.dqnylux.mincore.Mincore;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Generaliza el patrón de CosmeticSyncManager (sección 18) a TODA la
 * configuración, no solo cosméticos: cada archivo se guarda como texto
 * completo en mincore_config_sync, sincronizado por poll periódico + push/pull
 * manual - igual que la sección 5/13.4 - más pub/sub por Redis (sección 19)
 * para recarga instantánea en vez de esperar el siguiente poll.
 * storage.yml (database.yml aquí) nunca se sincroniza: define cómo conectarse
 * a la propia BD, es inherentemente de instancia.
 */
public class ConfigSyncManager {

    private static final String[] SYNCED_FILES = {
            "config.yml", "messages.yml", "filters.yml", "chatformat.yml", "bots.yml", "announcements.yml"
    };

    private static final String CONFIG_CHANNEL = "mincore:config:reload";
    private static final String COSMETICS_CHANNEL = "mincore:cosmetics:updated";

    private final Mincore plugin;
    private ScheduledTask pullTask;

    public ConfigSyncManager(Mincore plugin) {
        this.plugin = plugin;
    }

    private boolean networkModeActive() {
        DatabaseManager.StorageType type = plugin.getDatabaseManager().getStorageType();
        return type == DatabaseManager.StorageType.MYSQL || type == DatabaseManager.StorageType.MARIADB;
    }

    public void startAutoSync() {
        if (!networkModeActive()) return;
        pullTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> pullAll(), 20L, 300L, TimeUnit.SECONDS);
    }

    public void stop() {
        if (pullTask != null) pullTask.cancel();
    }

    /** Bloqueante (Jedis.subscribe no retorna hasta desuscribirse) - se lanza en su propio hilo async, nunca en el principal. */
    public void subscribeToReloads() {
        if (!networkModeActive()) return;

        JedisPool pool = plugin.getDatabaseManager().getJedisPool();
        if (pool == null) return;

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                            if (COSMETICS_CHANNEL.equals(channel)) {
                                plugin.getCosmeticSyncManager().pullFromDatabase();
                            } else {
                                pullAll();
                            }
                        });
                    }
                }, CONFIG_CHANNEL, COSMETICS_CHANNEL);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Mincore] Suscripción Redis de config terminada: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> pushAll() {
        if (!networkModeActive()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            for (String file : SYNCED_FILES) pushFile(file);
        });
    }

    public CompletableFuture<Void> pullAll() {
        if (!networkModeActive()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            boolean changed = false;
            for (String file : SYNCED_FILES) {
                changed |= pullFile(file);
            }
            if (changed) {
                org.dqnylux.mincore.utils.ConsoleLogger.info("<yellow>Configuración de red actualizada desde la base de datos - recargando.");
                Bukkit.getGlobalRegionScheduler().run(plugin, task -> reloadEverything());
            }
        });
    }

    private void pushFile(String fileName) {
        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return;

        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) return;

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            try (Connection connection = dataSource.getConnection()) {
                upsertConfig(connection, fileName, content);
            }
            plugin.getDatabaseManager().publishRedis(CONFIG_CHANNEL, fileName);
        } catch (IOException | SQLException e) {
            org.dqnylux.mincore.utils.ConsoleLogger.error("Error en push de " + fileName + ": " + e.getMessage());
        }
    }

    private boolean pullFile(String fileName) {
        DataSource dataSource = plugin.getDatabaseManager().getHikariDataSource();
        if (dataSource == null) return false;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT content FROM mincore_config_sync WHERE file_name = ?")) {
            statement.setString(1, fileName);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return false;

                String remoteContent = rs.getString("content");
                File file = new File(plugin.getDataFolder(), fileName);
                String localContent = file.exists() ? Files.readString(file.toPath(), StandardCharsets.UTF_8) : null;
                if (remoteContent.equals(localContent)) return false;

                Files.writeString(file.toPath(), remoteContent, StandardCharsets.UTF_8);
                return true;
            }
        } catch (IOException | SQLException e) {
            org.dqnylux.mincore.utils.ConsoleLogger.error("Error en pull de " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    private void upsertConfig(Connection connection, String fileName, String content) throws SQLException {
        boolean sqlite = plugin.getDatabaseManager().getStorageType() == DatabaseManager.StorageType.SQLITE;
        String sql = sqlite
                ? "INSERT INTO mincore_config_sync (file_name, content, updated_at) VALUES (?, ?, ?) ON CONFLICT(file_name) DO UPDATE SET content=excluded.content, updated_at=excluded.updated_at"
                : "INSERT INTO mincore_config_sync (file_name, content, updated_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE content=VALUES(content), updated_at=VALUES(updated_at)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileName);
            statement.setString(2, content);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void reloadEverything() {
        plugin.getConfigManager().loadConfigs();
        plugin.getChatFilterManager().reload();
        plugin.getAnnouncementManager().start();
        plugin.getDynamicCommandManager().reload();
    }
}
