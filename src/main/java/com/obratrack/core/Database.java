package com.obratrack.core;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestiona la conexion a la base de datos SQLite embebida.
 * Un solo archivo .db portable, sin servidor, vive junto al .jar/.exe en /data.
 *
 * <p><b>Concurrencia:</b> la app usa UNA sola conexion compartida. SQLite (via una
 * unica {@link Connection}) no es segura para ejecutar sentencias en paralelo desde
 * varios hilos. Por eso TODO acceso a la base debe realizarse sujetando {@link #LOCK}
 * (los servicios lo hacen). Esto serializa el trabajo del EDT y de los {@code SwingWorker}
 * de fondo (p. ej. la carga asincrona del Dashboard), evitando resultados cruzados.
 */
public final class Database {

    private static final String DB_FILE = "obratrack.db";
    private static Connection connection;

    /**
     * Candado global de acceso a la base de datos. Cualquier operacion que ejecute
     * sentencias debe hacerlo dentro de {@code synchronized (Database.LOCK)}.
     */
    public static final Object LOCK = new Object();

    private Database() {}

    public static Path dbPath() {
        return Rutas.data().resolve(DB_FILE);
    }

    public static synchronized Connection get() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbPath().toString();
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
                // Ajustes de rendimiento para una app de escritorio (un solo archivo local):
                st.execute("PRAGMA journal_mode = WAL;");     // escrituras mas rapidas y lecturas sin bloqueo
                st.execute("PRAGMA synchronous = NORMAL;");   // buen balance velocidad/seguridad en WAL
                st.execute("PRAGMA temp_store = MEMORY;");    // tablas temporales en RAM
                st.execute("PRAGMA cache_size = -8000;");     // ~8 MB de cache de paginas
            }
            inicializarEsquema(connection);
        }
        return connection;
    }

    /** Unidad de trabajo que se ejecuta dentro de una transaccion. */
    @FunctionalInterface
    public interface Trabajo {
        void ejecutar(Connection conn) throws SQLException;
    }

    /**
     * Ejecuta varias sentencias de forma atomica y bajo el candado global: si algo
     * falla, se revierte todo (rollback). Usar para operaciones de mas de un paso
     * (p. ej. registrar un movimiento y su entrada de auditoria).
     */
    public static void enTransaccion(Trabajo trabajo) throws SQLException {
        synchronized (LOCK) {
            Connection conn = get();
            boolean autoCommitPrevio = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                trabajo.ejecutar(conn);
                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitPrevio);
            }
        }
    }

    private static void inicializarEsquema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS obra (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    descripcion TEXT,
                    fecha_inicio TEXT,
                    fecha_fin_estimada TEXT,
                    presupuesto_total REAL DEFAULT 0,
                    estado TEXT DEFAULT 'ACTIVA',
                    ruta_excel_origen TEXT,
                    fecha_creacion TEXT
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS partida (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    codigo TEXT,
                    descripcion TEXT NOT NULL,
                    unidad TEXT,
                    cantidad_presupuestada REAL DEFAULT 0,
                    costo_unitario REAL DEFAULT 0,
                    costo_total_presupuestado REAL DEFAULT 0,
                    es_padre INTEGER DEFAULT 0,
                    nivel INTEGER DEFAULT 1,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS movimiento_almacen (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    partida_id INTEGER NOT NULL,
                    fecha TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    cantidad REAL NOT NULL,
                    costo_unitario_real REAL NOT NULL,
                    costo_total_real REAL NOT NULL,
                    observacion TEXT,
                    usuario_registro TEXT,
                    creado_en TEXT,
                    actualizado_en TEXT,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE,
                    FOREIGN KEY (partida_id) REFERENCES partida(id) ON DELETE CASCADE
                );
            """);

            // Historial de auditoria: registra cada creacion/edicion/eliminacion con su
            // fecha y hora, para control anti-fraude. Es una tabla append-only (solo INSERT).
            st.execute("""
                CREATE TABLE IF NOT EXISTS movimiento_auditoria (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER,
                    movimiento_id INTEGER,
                    accion TEXT NOT NULL,
                    detalle TEXT,
                    usuario TEXT,
                    fecha_hora TEXT NOT NULL
                );
            """);

            // Usuarios del sistema (login). La contrasena se guarda hasheada (PBKDF2), nunca en claro.
            // debe_cambiar_password = 1 obliga a definir una nueva clave en el proximo ingreso.
            st.execute("""
                CREATE TABLE IF NOT EXISTS usuario (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    nombre TEXT,
                    password_hash TEXT NOT NULL,
                    rol TEXT NOT NULL DEFAULT 'ADMIN',
                    activo INTEGER NOT NULL DEFAULT 1,
                    debe_cambiar_password INTEGER NOT NULL DEFAULT 0,
                    creado_en TEXT
                );
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_partida_obra ON partida(obra_id);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_mov_obra ON movimiento_almacen(obra_id);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_mov_partida ON movimiento_almacen(partida_id);");
            // Indice compuesto para el comparativo temporal y los reportes por fecha:
            // acelera GROUP BY fecha / filtros por rango cuando la obra acumula miles de movimientos.
            st.execute("CREATE INDEX IF NOT EXISTS idx_mov_obra_fecha ON movimiento_almacen(obra_id, fecha);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_aud_obra ON movimiento_auditoria(obra_id);");
        }
        // Migracion para bases de datos ya existentes (agrega columnas nuevas sin perder datos).
        migrarColumnas(conn);
    }

    /** Agrega columnas nuevas a bases de datos creadas antes de esta version. */
    private static void migrarColumnas(Connection conn) throws SQLException {
        agregarColumnaSiFalta(conn, "movimiento_almacen", "creado_en", "TEXT");
        agregarColumnaSiFalta(conn, "movimiento_almacen", "actualizado_en", "TEXT");
        agregarColumnaSiFalta(conn, "usuario", "debe_cambiar_password", "INTEGER NOT NULL DEFAULT 0");
    }

    private static void agregarColumnaSiFalta(Connection conn, String tabla, String columna, String tipo)
            throws SQLException {
        boolean existe = false;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equalsIgnoreCase(rs.getString("name"))) {
                    existe = true;
                    break;
                }
            }
        }
        if (!existe) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + tipo);
            }
        }
    }

    public static void cerrar() {
        synchronized (LOCK) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {}
        }
    }
}
