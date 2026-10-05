package org.dqnylux.mincore.managers;

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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generaliza el patrón de CosmeticSyncManager (sección 18): cada archivo se
 * guarda como texto completo en mincore_config_sync. Qué archivos se
 * sincronizan es 100% configurable (storage.yml -> sync.synced-files, por
 * defecto solo cosméticos/menús) - no un listado fijo en Java. storage.yml
 * (database.yml aquí) nunca se sincroniza: define cómo conectarse a la
 * propia BD, es inherentemente de instancia.
 *
 * SIN poll periódico a propósito: la sincronización antes corría sola cada
 * 300s (además de al conectar) sin que ningún admin hubiera pedido nada,
 * causando recargas completas (reloadEverything(), I/O de disco + reparse de
 * YAML) sin aviso ni control - el pedido explícito fue que esto SOLO se
 * mueva cuando se usa /coreec sync push|pull. subscribeToReloads() sigue
 * activo porque no es un disparador aparte: es la reacción, en tiempo real,
 * a ESE mismo comando corrido en OTRO server de la red (sección 19) - sin
 * esto, un push en el server A nunca llegaría al server B hasta que alguien
 * corriera /coreec sync pull a mano ahí también.
 */
public class ConfigSyncManager {

    private static final String CONFIG_CHANNEL = "coreec:config:reload";
    private static final String COSMETICS_CHANNEL = "coreec:cosmetics:updated";

    private final Mincore plugin;

    public ConfigSyncManager(Mincore plugin) {
        this.plugin = plugin;
    }

    private boolean networkModeActive() {
        DatabaseManager.StorageType type = plugin.getDatabaseManager().getStorageType();
        return type == DatabaseManager.StorageType.MYSQL || type == DatabaseManager.StorageType.MARIADB;
    }

    public void stop() {
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
                Bukkit.getLogger().warning("[CoreEC] Suscripción Redis de config terminada: " + e.getMessage());
            }
        });
    }

    private List<String> syncedFiles() {
        return plugin.getConfigManager().getDatabaseConfig().sync.syncedFiles;
    }

    public CompletableFuture<Void> pushAll() {
        if (!networkModeActive()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            for (String file : syncedFiles()) pushFile(file);
        });
    }

    public CompletableFuture<Void> pullAll() {
        if (!networkModeActive()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            boolean changed = false;
            for (String file : syncedFiles()) {
                changed |= pullFile(file);
            }
            if (changed) {
                org.dqnylux.mincore.utils.ConsoleLogger.info("<#FFEB3B>Configuración de red actualizada desde la base de datos - recargando.");
                // reloadEverything() es lectura de disco + parseo de YAML,
                // nada de API de Bukkit que dependa de una región específica
                // (mismo patrón que ya usa /coreec reload, que llama estos
                // mismos métodos directo sin saltar de hilo) - saltar a la
                // región global acá solo bloqueaba el hilo principal con
                // trabajo pesado de I/O cada vez que esto disparaba.
                reloadEverything();
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

                // Nunca bajar la versión de un archivo local: si la copia en
                // red quedó vieja (ej. nadie corrió /mincore sync push ni
                // /mincore reload después de una migración de esquema como
                // chatformat.yml v1->v2), pisar el archivo local recién
                // migrado con la vieja de la BD lo regresaría a la versión
                // anterior en cada pull (cada reinicio + cada 300s) - un loop
                // sin fin de "se desactualizó" que además borra cualquier
                // personalización hecha después de la migración.
                if (localContent != null && extractVersion(remoteContent) < extractVersion(localContent)) {
                    return false;
                }

                Files.writeString(file.toPath(), remoteContent, StandardCharsets.UTF_8);
                return true;
            }
        } catch (IOException | SQLException e) {
            org.dqnylux.mincore.utils.ConsoleLogger.error("Error en pull de " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    /** Lee el "version: N" de nivel raíz de un YAML de Mincore sin pasar por Okaeri - 0 si no se encuentra (archivo vacío/corrupto, nunca menor que una versión real). */
    private int extractVersion(String yamlContent) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?m)^version:\\s*(\\d+)").matcher(yamlContent);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
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
        plugin.getCosmeticConfigManager().loadConfigs();
        plugin.getChatFilterManager().reload();
        plugin.getAnnouncementManager().start();
        plugin.getDynamicCommandManager().reload();
    }
}
