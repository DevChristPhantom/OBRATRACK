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
        if (RedEstado.modo() == Modo.CLIENTE) {
            throw new SQLException("Esta PC esta conectada como cliente a otra PC de la obra; "
                    + "esta funcion todavia no esta disponible en red (se agregara mas adelante).");
        }
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
                    fecha_creacion TEXT,
                    ubicacion TEXT,
                    entidad_contratante TEXT,
                    modalidad_ejecucion TEXT,
                    sectores_bloques TEXT,
                    pct_gastos_generales REAL DEFAULT 0,
                    pct_utilidad REAL DEFAULT 0,
                    pct_igv REAL DEFAULT 18.0
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

            // Cronograma: actividades con fechas programadas/reales y peso dentro del avance
            // total de la obra (curva S). partida_id es opcional, para relacionar con el costo.
            st.execute("""
                CREATE TABLE IF NOT EXISTS actividad (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    partida_id INTEGER,
                    codigo TEXT,
                    descripcion TEXT NOT NULL,
                    fecha_inicio_prog TEXT NOT NULL,
                    fecha_fin_prog TEXT NOT NULL,
                    fecha_inicio_real TEXT,
                    fecha_fin_real TEXT,
                    peso_porcentual REAL DEFAULT 0,
                    avance_real REAL DEFAULT 0,
                    orden INTEGER DEFAULT 0,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE,
                    FOREIGN KEY (partida_id) REFERENCES partida(id) ON DELETE SET NULL
                );
            """);

            // Cuaderno de obra digital: asientos append-only (registro legal de la obra),
            // numerados de forma correlativa por obra como un cuaderno foliado.
            st.execute("""
                CREATE TABLE IF NOT EXISTS asiento_cuaderno (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    numero INTEGER NOT NULL,
                    fecha TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    clima TEXT,
                    personal_obra INTEGER DEFAULT 0,
                    texto TEXT NOT NULL,
                    usuario_registro TEXT,
                    creado_en TEXT,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE
                );
            """);

            // Valorizaciones mensuales: corte formal del avance ejecutado en un periodo,
            // con retencion de garantia y neto a pagar. Append-only (numeracion correlativa).
            st.execute("""
                CREATE TABLE IF NOT EXISTS valorizacion (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    numero INTEGER NOT NULL,
                    periodo_desde TEXT NOT NULL,
                    periodo_hasta TEXT NOT NULL,
                    fecha_emision TEXT NOT NULL,
                    monto_ejecutado_periodo REAL DEFAULT 0,
                    monto_acumulado_antes REAL DEFAULT 0,
                    pct_retencion REAL DEFAULT 0,
                    monto_retencion REAL DEFAULT 0,
                    monto_amortizacion_adelanto REAL DEFAULT 0,
                    monto_neto_pagar REAL DEFAULT 0,
                    observaciones TEXT,
                    usuario_registro TEXT,
                    creado_en TEXT,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE
                );
            """);

            // Metrado desagregado por sector/bloque de cada partida (obra grande con varios frentes).
            st.execute("""
                CREATE TABLE IF NOT EXISTS metrado_detalle (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    partida_id INTEGER NOT NULL,
                    obra_id INTEGER NOT NULL,
                    sector TEXT NOT NULL,
                    cantidad REAL DEFAULT 0,
                    observacion TEXT,
                    FOREIGN KEY (partida_id) REFERENCES partida(id) ON DELETE CASCADE,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE
                );
            """);

            // Analisis de precios unitarios (APU): insumos (mano de obra/material/equipo/subcontrato)
            // que componen el costo unitario de cada partida.
            st.execute("""
                CREATE TABLE IF NOT EXISTS apu_insumo (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    partida_id INTEGER NOT NULL,
                    obra_id INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    descripcion TEXT NOT NULL,
                    unidad TEXT,
                    cantidad REAL DEFAULT 0,
                    precio_unitario REAL DEFAULT 0,
                    FOREIGN KEY (partida_id) REFERENCES partida(id) ON DELETE CASCADE,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE
                );
            """);

            // Documentos: planos (con version), especificaciones tecnicas, fotos de avance y
            // anexos. El archivo real vive en disco (Rutas.documentos()); aqui solo la metadata.
            st.execute("""
                CREATE TABLE IF NOT EXISTS documento (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    partida_id INTEGER,
                    categoria TEXT NOT NULL,
                    nombre TEXT NOT NULL,
                    version INTEGER DEFAULT 1,
                    ruta_archivo TEXT NOT NULL,
                    nombre_archivo_original TEXT,
                    tamano_bytes INTEGER DEFAULT 0,
                    fecha TEXT NOT NULL,
                    descripcion TEXT,
                    usuario_registro TEXT,
                    creado_en TEXT,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE,
                    FOREIGN KEY (partida_id) REFERENCES partida(id) ON DELETE SET NULL
                );
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_partida_obra ON partida(obra_id);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_actividad_obra ON actividad(obra_id);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_asiento_obra ON asiento_cuaderno(obra_id);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_valorizacion_obra ON valorizacion(obra_id);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_metrado_partida ON metrado_detalle(partida_id);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_apu_partida ON apu_insumo(partida_id);");
            // Gestion y cumplimiento: riesgos, compromisos ambientales, control de calidad y SST.
            // Comparten la misma forma (se identifica, se hace seguimiento, se cierra); solo RIESGO
            // usa probabilidad/impacto para calcular la severidad, en el resto se elige directamente.
            st.execute("""
                CREATE TABLE IF NOT EXISTS item_cumplimiento (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    partida_id INTEGER,
                    categoria TEXT NOT NULL,
                    descripcion TEXT NOT NULL,
                    probabilidad TEXT,
                    impacto TEXT,
                    severidad TEXT NOT NULL,
                    fecha TEXT NOT NULL,
                    fecha_limite TEXT,
                    estado TEXT NOT NULL,
                    fecha_cierre TEXT,
                    responsable TEXT,
                    accion_seguimiento TEXT,
                    usuario_registro TEXT,
                    creado_en TEXT,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE,
                    FOREIGN KEY (partida_id) REFERENCES partida(id) ON DELETE SET NULL
                );
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_documento_obra ON documento(obra_id, categoria);");
            // Adicionales y deductivos: modificaciones aprobadas al presupuesto contractual.
            st.execute("""
                CREATE TABLE IF NOT EXISTS adicional_deductivo (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    numero INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    descripcion TEXT NOT NULL,
                    monto REAL DEFAULT 0,
                    fecha_aprobacion TEXT NOT NULL,
                    resolucion_aprobacion TEXT,
                    usuario_registro TEXT,
                    creado_en TEXT,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE
                );
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_adicional_obra ON adicional_deductivo(obra_id);");
            // Formula polinomica: elementos (mano de obra, materiales, equipo...) con su
            // coeficiente de incidencia e indice de precios base, para el reajuste por INEI.
            st.execute("""
                CREATE TABLE IF NOT EXISTS monomio_polinomico (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    obra_id INTEGER NOT NULL,
                    descripcion TEXT NOT NULL,
                    coeficiente_incidencia REAL DEFAULT 0,
                    indice_base REAL DEFAULT 0,
                    FOREIGN KEY (obra_id) REFERENCES obra(id) ON DELETE CASCADE
                );
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_cumplimiento_obra ON item_cumplimiento(obra_id, categoria);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_monomio_obra ON monomio_polinomico(obra_id);");
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
        agregarColumnaSiFalta(conn, "obra", "ubicacion", "TEXT");
        agregarColumnaSiFalta(conn, "obra", "entidad_contratante", "TEXT");
        agregarColumnaSiFalta(conn, "obra", "modalidad_ejecucion", "TEXT");
        agregarColumnaSiFalta(conn, "obra", "sectores_bloques", "TEXT");
        agregarColumnaSiFalta(conn, "obra", "pct_gastos_generales", "REAL DEFAULT 0");
        agregarColumnaSiFalta(conn, "obra", "pct_utilidad", "REAL DEFAULT 0");
        agregarColumnaSiFalta(conn, "obra", "pct_igv", "REAL DEFAULT 18.0");
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
