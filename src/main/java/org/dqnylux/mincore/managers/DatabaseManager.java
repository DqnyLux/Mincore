package org.dqnylux.mincore.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.dqnylux.mincore.config.DatabaseConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private HikariDataSource hikariDataSource;
    private JedisPool jedisPool;
    private final DatabaseConfig config;

    public DatabaseManager(DatabaseConfig config) {
        this.config = config;
    }

    public boolean isConnected() {
        return hikariDataSource != null && !hikariDataSource.isClosed();
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
            connectMySQL();
            connectRedis();
        }).exceptionally(ex -> {
            Bukkit.getLogger().severe("[Mincore] Database error: " + ex.getMessage());
            return null;
        }).join();
    }

    private void connectMySQL() {
        if (!config.mysql.enabled) return;

        try {
            HikariConfig hikariConfig = new HikariConfig();

            String driverType = config.mysql.type.equalsIgnoreCase("MariaDB") ? "mariadb" : "mysql";
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
            hikariConfig.setPoolName("Mincore-" + config.mysql.type + "-Pool");

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

            this.hikariDataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Mincore] Error connecting to " + config.mysql.type + ": " + e.getMessage());
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