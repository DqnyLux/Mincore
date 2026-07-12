package org.dqnylux.mincore.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.DatabaseConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    public enum StorageType {
        SQLITE, MYSQL, MARIADB
    }

    private final Mincore plugin;
    private final DatabaseConfig config;
    private HikariDataSource hikariDataSource;
    private JedisPool jedisPool;
    private StorageType storageType;

    public DatabaseManager(Mincore plugin, DatabaseConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean isConnected() {
        return hikariDataSource != null && !hikariDataSource.isClosed();
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public boolean isRedisConnected() {
        if (jedisPool == null || jedisPool.isClosed()) return false;
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.ping().equalsIgnoreCase("PONG");
        } catch (Exception e) {
            return false;
        }
    }

    public void connect() {
        CompletableFuture.runAsync(() -> {
            connectDatabase();
            createSchema();
            connectRedis();
        }).exceptionally(ex -> {
            Bukkit.getLogger().severe("[Mincore] Database error: " + ex.getMessage());
            return null;
        }).join();
    }

    private void connectDatabase() {
        try {
            HikariConfig hikariConfig = new HikariConfig();

            boolean useRelationalServer = config.mysql.enabled && !config.mysql.type.equalsIgnoreCase("SQLite");

            if (useRelationalServer) {
                storageType = config.mysql.type.equalsIgnoreCase("MariaDB") ? StorageType.MARIADB : StorageType.MYSQL;
                String driverType = storageType == StorageType.MARIADB ? "mariadb" : "mysql";
                hikariConfig.setJdbcUrl("jdbc:" + driverType + "://" + config.mysql.host + ":" + config.mysql.port + "/" + config.mysql.database);

                hikariConfig.setUsername(config.mysql.username);
                hikariConfig.setPassword(config.mysql.password);
                hikariConfig.addDataSourceProperty("useSSL", String.valueOf(config.mysql.useSSL));
                hikariConfig.addDataSourceProperty("autoReconnect", "true");

                hikariConfig.setMaximumPoolSize(config.mysql.maximumPoolSize);
                hikariConfig.setMinimumIdle(config.mysql.minimumIdle);
                hikariConfig.setConnectionTimeout(config.mysql.connectionTimeout);
                hikariConfig.setIdleTimeout(config.mysql.idleTimeout);
                hikariConfig.setMaxLifetime(config.mysql.maxLifetime);
                hikariConfig.setPoolName("Mincore-" + storageType + "-Pool");

                if (config.mysql.cachePrepStmts) {
                    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
                    hikariConfig.addDataSourceProperty("prepStmtCacheSize", String.valueOf(config.mysql.prepStmtCacheSize));
                    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", String.valueOf(config.mysql.prepStmtCacheSqlLimit));
                    hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
                    hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
                    hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
                    hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
                    hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
                    hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
                    hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
                }
            } else {
                // Sin servidor MySQL/MariaDB configurado: cada servidor guarda su propia
                // base de datos local en un archivo, sin necesitar infraestructura externa.
                storageType = StorageType.SQLITE;
                plugin.getDataFolder().mkdirs();
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hikariConfig.setMaximumPoolSize(1); // SQLite solo admite un escritor a la vez
                hikariConfig.setPoolName("Mincore-SQLite-Pool");
            }

            this.hikariDataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Mincore] Error connecting to " + storageType + ": " + e.getMessage());
        }
    }

    private void createSchema() {
        if (hikariDataSource == null) return;

        try (Connection connection = hikariDataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_players (
                      uuid VARCHAR(36) PRIMARY KEY,
                      name VARCHAR(16),
                      coins DOUBLE DEFAULT 0.0
                    )
                    """);

            addColumnIfMissing(statement, "mincore_players", "global_chat", "BOOLEAN DEFAULT TRUE");
            addColumnIfMissing(statement, "mincore_players", "chat_warnings", "INT DEFAULT 0");
            addColumnIfMissing(statement, "mincore_players", "messages_enabled", "BOOLEAN DEFAULT TRUE");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_unlocks (
                      uuid VARCHAR(36) NOT NULL,
                      cosmetic_id VARCHAR(100) NOT NULL,
                      category VARCHAR(50) NOT NULL,
                      PRIMARY KEY(uuid, category, cosmetic_id)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_active_cosmetics (
                      uuid VARCHAR(36) NOT NULL,
                      category VARCHAR(50) NOT NULL,
                      cosmetic_id VARCHAR(100) NOT NULL,
                      PRIMARY KEY(uuid, category)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_global_cosmetics (
                      category VARCHAR(50) NOT NULL,
                      item_id VARCHAR(50) NOT NULL,
                      display_name VARCHAR(100),
                      value VARCHAR(100),
                      material VARCHAR(50),
                      price DOUBLE DEFAULT 0.0,
                      PRIMARY KEY(category, item_id)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_config_sync (
                      file_name VARCHAR(100) PRIMARY KEY,
                      content TEXT,
                      updated_at BIGINT
                    )
                    """);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[Mincore] Error creating schema: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(Statement statement, String table, String column, String definition) {
        try {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException ignored) {
            // La columna ya existe (migración incremental) - error esperado, se ignora.
        }
    }

    private void connectRedis() {
        if (!config.redis.enabled) return;

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            if (config.redis.password == null || config.redis.password.isEmpty()) {
                this.jedisPool = new JedisPool(poolConfig, config.redis.host, config.redis.port, config.redis.timeout);
            } else {
                this.jedisPool = new JedisPool(poolConfig, config.redis.host, config.redis.port, config.redis.timeout, config.redis.password);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Mincore] Error connecting to Redis: " + e.getMessage());
        }
    }

    /** No-op silencioso si Redis no está activo - los llamadores no necesitan comprobarlo antes. */
    public void publishRedis(String channel, String message) {
        if (jedisPool == null) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Mincore] Error publicando en Redis (" + channel + "): " + e.getMessage());
        }
    }

    public void close() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    public HikariDataSource getHikariDataSource() {
        return hikariDataSource;
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }
}