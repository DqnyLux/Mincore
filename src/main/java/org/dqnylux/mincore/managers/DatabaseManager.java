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
            Bukkit.getLogger().severe("[CoreEC] Database error: " + ex.getMessage());
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

                // Sin esto, Hikari resuelve el driver vía DriverManager.getDriver(jdbcUrl),
                // que hace su propio ServiceLoader scan usando el classloader de
                // contexto del hilo actual - en un plugin de Paper con classloader
                // aislado (donde vive el driver incrustado), ese scan puede no
                // encontrarlo aunque la clase esté físicamente en el jar, y falla
                // con "Failed to get driver instance" (visto en vivo, aun con el
                // driver ya shadeado). setDriverClassName() evita ese camino por
                // completo: Hikari carga la clase directo con SU PROPIO
                // classloader (el mismo que ya tiene el driver, vía MincoreLoader
                // + shade), sin depender de qué hilo/classloader disparó el lookup.
                hikariConfig.setDriverClassName(storageType == StorageType.MARIADB
                        ? "org.mariadb.jdbc.Driver" : "com.mysql.cj.jdbc.Driver");

                hikariConfig.setUsername(config.mysql.username);
                hikariConfig.setPassword(config.mysql.password);
                hikariConfig.addDataSourceProperty("useSSL", String.valueOf(config.mysql.useSSL));
                hikariConfig.addDataSourceProperty("autoReconnect", "true");

                hikariConfig.setMaximumPoolSize(config.mysql.maximumPoolSize);
                hikariConfig.setMinimumIdle(config.mysql.minimumIdle);
                hikariConfig.setConnectionTimeout(config.mysql.connectionTimeout);
                hikariConfig.setIdleTimeout(config.mysql.idleTimeout);
                hikariConfig.setMaxLifetime(config.mysql.maxLifetime);
                hikariConfig.setPoolName("CoreEC-" + storageType + "-Pool");

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
                hikariConfig.setDriverClassName("org.sqlite.JDBC"); // mismo motivo que MariaDB/MySQL arriba
                hikariConfig.setMaximumPoolSize(1); // SQLite solo admite un escritor a la vez
                hikariConfig.setPoolName("CoreEC-SQLite-Pool");
            }

            // connect() corre esto dentro de CompletableFuture.runAsync(), que sin
            // Executor propio usa el ForkJoinPool común - esos hilos traen el
            // classloader de contexto del SISTEMA, no el del plugin. Hikari, al
            // cargar el driver por nombre (setDriverClassName de arriba), primero
            // intenta con ESE classloader de contexto y recién si falla cae a
            // HikariConfig.class.getClassLoader() - en Paper vainilla ese segundo
            // intento alcanza igual porque el PluginClasspathBuilder fusiona todo
            // en un solo classloader, pero en forks como UniverseSpigot esa fusión
            // puede no darse igual, y ambos intentos fallan con "Failed to load
            // driver class" aunque la clase esté físicamente en el jar (visto en
            // vivo). Fijar acá el classloader del propio plugin como contexto
            // antes de conectar hace que el PRIMER intento de Hikari ya alcance,
            // sin depender de esa fusión.
            Thread currentThread = Thread.currentThread();
            ClassLoader previousClassLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(getClass().getClassLoader());
            try {
                this.hikariDataSource = new HikariDataSource(hikariConfig);
            } finally {
                currentThread.setContextClassLoader(previousClassLoader);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[CoreEC] Error connecting to " + storageType + ": " + e.getMessage());
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
            addColumnIfMissing(statement, "mincore_players", "mentions_enabled", "BOOLEAN DEFAULT TRUE");

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

            // scope: "chat" (formatea el mensaje) o "name" (formatea el nombre
            // mostrado) - independientes entre sí (sección de PlayerData).
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_active_formats (
                      uuid VARCHAR(36) NOT NULL,
                      scope VARCHAR(10) NOT NULL DEFAULT 'chat',
                      format_id VARCHAR(50) NOT NULL,
                      PRIMARY KEY(uuid, scope, format_id)
                    )
                    """);
            addColumnIfMissing(statement, "mincore_active_formats", "scope", "VARCHAR(10) DEFAULT 'chat'");

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

            // Pozo Millonario (sistema de cajas/loot-boxes) - inventario virtual por
            // tipo de caja, sin ItemStacks reales, igual que mincore_unlocks.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_pozo_inventory (
                      uuid VARCHAR(36) NOT NULL,
                      box_type_id VARCHAR(64) NOT NULL,
                      amount INT NOT NULL DEFAULT 0,
                      PRIMARY KEY(uuid, box_type_id)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_pozo_points (
                      uuid VARCHAR(36) PRIMARY KEY,
                      points BIGINT NOT NULL DEFAULT 0
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_pozo_loot_history (
                      id VARCHAR(36) PRIMARY KEY,
                      uuid VARCHAR(36) NOT NULL,
                      box_type_id VARCHAR(64) NOT NULL,
                      reward_id VARCHAR(64) NOT NULL,
                      reward_name VARCHAR(100),
                      was_duplicate BOOLEAN DEFAULT FALSE,
                      opened_at BIGINT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_pozo_preferred_animation (
                      uuid VARCHAR(36) PRIMARY KEY,
                      animation_id VARCHAR(32)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_pozo_machines (
                      world VARCHAR(64) NOT NULL,
                      x INT NOT NULL,
                      y INT NOT NULL,
                      z INT NOT NULL,
                      box_type_id VARCHAR(64),
                      PRIMARY KEY(world, x, y, z)
                    )
                    """);

            // Disfraces activos (nombre+rango+skin) - persistidos en la BD
            // COMPARTIDA de la red para que el disfraz sobreviva al cambiar de
            // servidor por BungeeCord: cada backend lo re-aplica al join. Solo
            // /undisguise borra la fila.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_disguises (
                      uuid VARCHAR(36) PRIMARY KEY,
                      fake_name VARCHAR(16) NOT NULL,
                      fake_rank VARCHAR(64),
                      skin_value TEXT,
                      skin_signature TEXT
                    )
                    """);

            // Modelo de efecto ambiente por máquina (MachineEffect de ACubelets:
            // beacon/heart/helix/pulsar/rings/simple/sphere/spiral/vortex) -
            // tabla aparte y no columna nueva en mincore_pozo_machines porque
            // CREATE TABLE IF NOT EXISTS no altera tablas ya existentes.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_pozo_machine_effects (
                      world VARCHAR(64) NOT NULL,
                      x INT NOT NULL,
                      y INT NOT NULL,
                      z INT NOT NULL,
                      model VARCHAR(32) NOT NULL,
                      PRIMARY KEY(world, x, y, z)
                    )
                    """);

            // Puerto de VisualSanctions: solo el nivel de escalamiento por
            // categoría - el castigo real (razón/duración/estado activo) lo
            // trackea lo que sea que el comando de cada nivel ejecute
            // (Essentials/LiteBans/etc.), esta tabla no lo duplica.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_sanctions (
                      uuid VARCHAR(36) NOT NULL,
                      category VARCHAR(64) NOT NULL,
                      level INT NOT NULL DEFAULT 0,
                      timestamp BIGINT NOT NULL,
                      PRIMARY KEY(uuid, category)
                    )
                    """);

            // Puerto de TigerReports. mincore_report_reporters es lo que
            // permite "apilar" reportes: mismo target+razón agrega una fila
            // acá en vez de duplicar mincore_reports. id es un UUID de texto
            // (no AUTOINCREMENT/AUTO_INCREMENT) - mismo criterio que
            // mincore_pozo_loot_history, evita la rama SQLite-vs-MySQL para
            // obtener el id generado tras el INSERT.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_reports (
                      id VARCHAR(36) PRIMARY KEY,
                      reported_uuid VARCHAR(36) NOT NULL,
                      reported_name VARCHAR(16) NOT NULL,
                      reason VARCHAR(255) NOT NULL,
                      status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
                      created_at BIGINT NOT NULL,
                      server VARCHAR(64),
                      handled_by VARCHAR(36),
                      handled_by_name VARCHAR(16),
                      archived BOOLEAN NOT NULL DEFAULT FALSE,
                      abusive BOOLEAN NOT NULL DEFAULT FALSE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_report_reporters (
                      report_id VARCHAR(36) NOT NULL,
                      reporter_uuid VARCHAR(36) NOT NULL,
                      reporter_name VARCHAR(16) NOT NULL,
                      reported_at BIGINT NOT NULL,
                      PRIMARY KEY(report_id, reporter_uuid)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_report_cooldowns (
                      uuid VARCHAR(36) PRIMARY KEY,
                      last_report_at BIGINT NOT NULL,
                      abusive_cooldown_until BIGINT NOT NULL DEFAULT 0
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS mincore_report_comments (
                      id VARCHAR(36) PRIMARY KEY,
                      report_id VARCHAR(36) NOT NULL,
                      author_uuid VARCHAR(36) NOT NULL,
                      author_name VARCHAR(16) NOT NULL,
                      comment VARCHAR(255) NOT NULL,
                      created_at BIGINT NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CoreEC] Error creating schema: " + e.getMessage());
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
            Bukkit.getLogger().severe("[CoreEC] Error connecting to Redis: " + e.getMessage());
        }
    }

    /** No-op silencioso si Redis no está activo - los llamadores no necesitan comprobarlo antes. */
    public void publishRedis(String channel, String message) {
        if (jedisPool == null) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CoreEC] Error publicando en Redis (" + channel + "): " + e.getMessage());
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