package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;

public class DatabaseConfig extends MincoreConfig {

    @Comment({
            "=======================================================",
            " BASE DE DATOS PRINCIPAL",
            " Motor de almacenamiento persistente del servidor.",
            "======================================================="
    })
    public MySQL mysql = new MySQL();

    @Comment({
            "",
            "=======================================================",
            " REDIS (MEMORIA EN CACHÉ Y SYNC)",
            " Útil si conectas Mincore a un proxy (BungeeCord/Velocity)",
            " para sincronizar datos entre múltiples servidores.",
            "======================================================="
    })
    public Redis redis = new Redis();

    public static class MySQL extends MincoreConfig {
        @Comment("¿Activar la conexión a la base de datos relacional?")
        public boolean enabled = false;

        @Comment({
                "",
                "Tipo de base de datos a usar.",
                "Opciones soportadas: ",
                "- MySQL   (Estándar, ideal para bases de datos externas)",
                "- MariaDB (Más rápido que MySQL, recomendado si usas panel Pterodactyl)",
                "- SQLite  (Base de datos local en un archivo, ideal para un solo servidor)"
        })
        public String type = "MariaDB";

        @Comment("Dirección IP o dominio del servidor de base de datos (Ej: 127.0.0.1, localhost)")
        public String host = "127.0.0.1";

        @Comment("Puerto de conexión (MySQL/MariaDB = 3306)")
        public int port = 3306;

        @Comment("Nombre de la base de datos a usar")
        public String database = "mincore_db";

        @Comment("Usuario de la base de datos")
        public String username = "root";

        @Comment("Contraseña del usuario de la base de datos")
        public String password = "password";

        @Comment("¿Usar conexión cifrada SSL? (Actívalo si tu base de datos está en otro país/servidor distinto)")
        public boolean useSSL = false;

        @Comment({
                "",
                "--- OPTIMIZACIÓN DEL POOL DE CONEXIONES (HIKARICP) ---",
                "Estos ajustes determinan cuánta memoria y conexiones a la red consume el plugin."
        })
        @Comment("Cantidad máxima de conexiones activas simultáneas. (10 suele ser suficiente para 100 jugadores)")
        public int maximumPoolSize = 10;

        @Comment("Cantidad de conexiones que se mantienen 'dormidas' listas para usarse rápidamente.")
        public int minimumIdle = 2;

        @Comment("Tiempo máximo de espera (en milisegundos) antes de rendirse al intentar conectar. (30000 = 30 seg)")
        public long connectionTimeout = 30000;

        @Comment("Tiempo (en milisegundos) que una conexión inactiva puede permanecer abierta antes de cerrarse. (600000 = 10 min)")
        public long idleTimeout = 600000;

        @Comment("Tiempo de vida máximo (en milisegundos) de una conexión en el pool para evitar fugas de memoria. (1800000 = 30 min)")
        public long maxLifetime = 1800000;

        @Comment({
                "",
                "--- OPTIMIZACIÓN DE RENDIMIENTO (PREPARED STATEMENTS) ---",
                "Recomendado dejar en 'true' para mejorar masivamente el rendimiento y prevenir inyecciones SQL."
        })
        public boolean cachePrepStmts = true;
        public int prepStmtCacheSize = 250;
        public int prepStmtCacheSqlLimit = 2048;
    }

    public static class Redis extends MincoreConfig {
        @Comment("¿Activar la conexión a Redis?")
        public boolean enabled = false;

        @Comment("Dirección IP o dominio del servidor Redis")
        public String host = "127.0.0.1";

        @Comment("Puerto de conexión (Por defecto es 6379)")
        public int port = 6379;

        @Comment("Contraseña de Redis (Dejar entre comillas vacías \"\" si no tiene seguridad)")
        public String password = "";

        @Comment("Tiempo máximo de espera (en milisegundos) al conectar")
        public int timeout = 2000;
    }
}