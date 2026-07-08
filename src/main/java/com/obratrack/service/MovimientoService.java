package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.MovimientoAuditoria;
import com.obratrack.model.ResumenPeriodo;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MovimientoService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MovimientoAlmacen registrar(MovimientoAlmacen mov) throws SQLException {
        if (mov.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        String ahora = ahora();
        mov.setCreadoEn(ahora);
        mov.setActualizadoEn(ahora);
        String sql = """
            INSERT INTO movimiento_almacen (obra_id, partida_id, fecha, tipo, cantidad,
                                             costo_unitario_real, costo_total_real, observacion,
                                             usuario_registro, creado_en, actualizado_en)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, mov.getObraId());
            ps.setLong(2, mov.getPartidaId());
            ps.setString(3, mov.getFecha().toString());
            ps.setString(4, mov.getTipo().name());
            ps.setDouble(5, mov.getCantidad());
            ps.setDouble(6, mov.getCostoUnitarioReal());
            ps.setDouble(7, mov.getCostoTotalReal());
            ps.setString(8, mov.getObservacion());
            ps.setString(9, mov.getUsuarioRegistro() != null ? mov.getUsuarioRegistro() : SesionActual.nombre());
            ps.setString(10, ahora);
            ps.setString(11, ahora);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) mov.setId(rs.getLong(1));
            }
        }
        registrarAuditoria(mov.getObraId(), mov.getId(),
                MovimientoAuditoria.Accion.CREACION, describir(mov));
        return mov;
    }

    /** Actualiza un movimiento existente y deja constancia en el historial de auditoria. */
    public void actualizar(MovimientoAlmacen mov) throws SQLException {
        if (mov.getId() == null) {
            throw new IllegalArgumentException("El movimiento a editar no tiene id");
        }
        if (mov.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        MovimientoAlmacen anterior = obtenerPorId(mov.getId());
        String ahora = ahora();
        mov.setActualizadoEn(ahora);
        String sql = """
            UPDATE movimiento_almacen
            SET partida_id = ?, fecha = ?, tipo = ?, cantidad = ?, costo_unitario_real = ?,
                costo_total_real = ?, observacion = ?, actualizado_en = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, mov.getPartidaId());
            ps.setString(2, mov.getFecha().toString());
            ps.setString(3, mov.getTipo().name());
            ps.setDouble(4, mov.getCantidad());
            ps.setDouble(5, mov.getCostoUnitarioReal());
            ps.setDouble(6, mov.getCostoTotalReal());
            ps.setString(7, mov.getObservacion());
            ps.setString(8, ahora);
            ps.setLong(9, mov.getId());
            ps.executeUpdate();
        }
        registrarAuditoria(mov.getObraId(), mov.getId(),
                MovimientoAuditoria.Accion.EDICION, diferencias(anterior, mov));
    }

    public MovimientoAlmacen obtenerPorId(long id) throws SQLException {
        String sql = """
            SELECT m.*, p.codigo AS partida_codigo, p.descripcion AS partida_descripcion
            FROM movimiento_almacen m
            JOIN partida p ON p.id = m.partida_id
            WHERE m.id = ?
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public List<MovimientoAlmacen> listarPorObra(long obraId) throws SQLException {
        List<MovimientoAlmacen> resultado = new ArrayList<>();
        String sql = """
            SELECT m.*, p.codigo AS partida_codigo, p.descripcion AS partida_descripcion
            FROM movimiento_almacen m
            JOIN partida p ON p.id = m.partida_id
            WHERE m.obra_id = ?
            ORDER BY m.fecha DESC, m.id DESC
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, obraId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        }
        return resultado;
    }

    public List<MovimientoAlmacen> listarPorObraYFecha(long obraId, LocalDate fecha) throws SQLException {
        List<MovimientoAlmacen> resultado = new ArrayList<>();
        String sql = """
            SELECT m.*, p.codigo AS partida_codigo, p.descripcion AS partida_descripcion
            FROM movimiento_almacen m
            JOIN partida p ON p.id = m.partida_id
            WHERE m.obra_id = ? AND m.fecha = ?
            ORDER BY m.id DESC
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, obraId);
            ps.setString(2, fecha.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        }
        return resultado;
    }

    /** Total ejecutado (EGRESO - se resta INGRESO de devoluciones) por partida, para el dashboard. */
    public Map<Long, Double> totalEjecutadoPorPartida(long obraId) throws SQLException {
        Map<Long, Double> resultado = new HashMap<>();
        String sql = """
            SELECT partida_id,
                   SUM(CASE WHEN tipo = 'EGRESO' THEN costo_total_real ELSE -costo_total_real END) AS total
            FROM movimiento_almacen
            WHERE obra_id = ?
            GROUP BY partida_id
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, obraId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getLong("partida_id"), rs.getDouble("total"));
                }
            }
        }
        return resultado;
    }

    public double totalEjecutadoObra(long obraId) throws SQLException {
        String sql = """
            SELECT SUM(CASE WHEN tipo = 'EGRESO' THEN costo_total_real ELSE -costo_total_real END) AS total
            FROM movimiento_almacen WHERE obra_id = ?
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, obraId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0;
    }

    private static final DateTimeFormatter FMT_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    /**
     * Comparativo de ejecucion agrupado por periodo (diario, semanal o mensual)
     * a lo largo de toda la obra. Devuelve los periodos en orden cronologico
     * ascendente, cada uno con sus egresos, ingresos, neto y el acumulado corrido.
     *
     * Estrategia: se agrega primero por dia en SQL (rapido y exacto) y luego se
     * agrupan los dias en el bucket correspondiente en Java, lo que evita las
     * particularidades de strftime para semanas ISO y da etiquetas legibles.
     */
    public List<ResumenPeriodo> resumenPorPeriodo(long obraId, Granularidad granularidad) throws SQLException {
        String sql = """
            SELECT fecha,
                   SUM(CASE WHEN tipo = 'EGRESO'  THEN costo_total_real ELSE 0 END) AS egreso,
                   SUM(CASE WHEN tipo = 'INGRESO' THEN costo_total_real ELSE 0 END) AS ingreso,
                   COUNT(*) AS n
            FROM movimiento_almacen
            WHERE obra_id = ?
            GROUP BY fecha
            ORDER BY fecha
        """;

        // LinkedHashMap preserva el orden de insercion (cronologico, por el ORDER BY fecha).
        Map<String, ResumenPeriodo> buckets = new LinkedHashMap<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, obraId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate dia = LocalDate.parse(rs.getString("fecha"));
                    String clave = claveBucket(dia, granularidad);
                    ResumenPeriodo periodo = buckets.computeIfAbsent(clave, k -> crearPeriodo(dia, granularidad));
                    periodo.agregarEgresos(rs.getDouble("egreso"));
                    periodo.agregarIngresos(rs.getDouble("ingreso"));
                    periodo.agregarMovimientos(rs.getInt("n"));
                }
            }
        }

        // Calcular neto y acumulado corrido en orden cronologico.
        double acumulado = 0;
        List<ResumenPeriodo> resultado = new ArrayList<>(buckets.values());
        for (ResumenPeriodo p : resultado) {
            p.setNeto(p.getEgresos() - p.getIngresos());
            acumulado += p.getNeto();
            p.setAcumulado(acumulado);
        }
        return resultado;
    }

    /** Clave unica de agrupacion para un dia segun la granularidad. */
    private String claveBucket(LocalDate dia, Granularidad granularidad) {
        return switch (granularidad) {
            case DIARIO -> dia.toString();
            case SEMANAL -> {
                WeekFields wf = WeekFields.ISO;
                yield dia.get(wf.weekBasedYear()) + "-W" + dia.get(wf.weekOfWeekBasedYear());
            }
            case MENSUAL -> YearMonth.from(dia).toString();
        };
    }

    /** Crea un periodo vacio con su rango de fechas y etiqueta legible. */
    private ResumenPeriodo crearPeriodo(LocalDate dia, Granularidad granularidad) {
        switch (granularidad) {
            case DIARIO -> {
                return new ResumenPeriodo(dia.toString(), dia, dia);
            }
            case SEMANAL -> {
                WeekFields wf = WeekFields.ISO;
                LocalDate lunes = dia.with(wf.dayOfWeek(), 1);
                LocalDate domingo = lunes.plusDays(6);
                int semana = dia.get(wf.weekOfWeekBasedYear());
                String etiqueta = String.format("Sem %02d (%s a %s)",
                        semana, lunes.format(FMT_DIA_MES), domingo.format(FMT_DIA_MES));
                return new ResumenPeriodo(etiqueta, lunes, domingo);
            }
            default -> { // MENSUAL
                YearMonth ym = YearMonth.from(dia);
                return new ResumenPeriodo(ym.toString(), ym.atDay(1), ym.atEndOfMonth());
            }
        }
    }

    public void eliminar(long movimientoId) throws SQLException {
        MovimientoAlmacen previo = obtenerPorId(movimientoId); // snapshot para el historial
        String sql = "DELETE FROM movimiento_almacen WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, movimientoId);
            ps.executeUpdate();
        }
        if (previo != null) {
            registrarAuditoria(previo.getObraId(), movimientoId,
                    MovimientoAuditoria.Accion.ELIMINACION, describir(previo));
        }
    }

    // ============================================================
    //  Auditoria / historial
    // ============================================================

    private void registrarAuditoria(Long obraId, Long movimientoId,
                                    MovimientoAuditoria.Accion accion, String detalle) throws SQLException {
        String sql = """
            INSERT INTO movimiento_auditoria (obra_id, movimiento_id, accion, detalle, usuario, fecha_hora)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            if (obraId != null) ps.setLong(1, obraId); else ps.setNull(1, Types.INTEGER);
            if (movimientoId != null) ps.setLong(2, movimientoId); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, accion.name());
            ps.setString(4, detalle);
            ps.setString(5, SesionActual.nombre());
            ps.setString(6, ahora());
            ps.executeUpdate();
        }
    }

    /** Historial de cambios de la obra, del mas reciente al mas antiguo. */
    public List<MovimientoAuditoria> listarAuditoria(long obraId) throws SQLException {
        List<MovimientoAuditoria> resultado = new ArrayList<>();
        String sql = "SELECT * FROM movimiento_auditoria WHERE obra_id = ? ORDER BY id DESC";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setLong(1, obraId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimientoAuditoria a = new MovimientoAuditoria();
                    a.setId(rs.getLong("id"));
                    a.setObraId(rs.getLong("obra_id"));
                    a.setMovimientoId(rs.getLong("movimiento_id"));
                    a.setAccion(MovimientoAuditoria.Accion.valueOf(rs.getString("accion")));
                    a.setDetalle(rs.getString("detalle"));
                    a.setUsuario(rs.getString("usuario"));
                    a.setFechaHora(rs.getString("fecha_hora"));
                    resultado.add(a);
                }
            }
        }
        return resultado;
    }

    private String ahora() {
        return LocalDateTime.now().format(TS);
    }

    /** Descripcion legible de un movimiento para el historial. */
    private String describir(MovimientoAlmacen m) {
        String partida = m.getPartidaCodigo() != null
                ? m.getPartidaCodigo() : ("partida #" + m.getPartidaId());
        return String.format("%s %s | %s | cant %s x S/. %,.2f = S/. %,.2f%s",
                m.getTipo(), m.getFecha(), partida, fmtNum(m.getCantidad()),
                m.getCostoUnitarioReal(), m.getCostoTotalReal(),
                (m.getObservacion() != null && !m.getObservacion().isBlank())
                        ? " | " + m.getObservacion() : "");
    }

    /** Lista de campos que cambiaron entre el estado anterior y el nuevo. */
    private String diferencias(MovimientoAlmacen a, MovimientoAlmacen b) {
        if (a == null) return "Edicion de movimiento";
        List<String> cambios = new ArrayList<>();
        if (!Objects.equals(a.getFecha(), b.getFecha())) {
            cambios.add("fecha " + a.getFecha() + " -> " + b.getFecha());
        }
        if (a.getTipo() != b.getTipo()) {
            cambios.add("tipo " + a.getTipo() + " -> " + b.getTipo());
        }
        if (a.getCantidad() != b.getCantidad()) {
            cambios.add("cantidad " + fmtNum(a.getCantidad()) + " -> " + fmtNum(b.getCantidad()));
        }
        if (a.getCostoUnitarioReal() != b.getCostoUnitarioReal()) {
            cambios.add(String.format("costo unit. %,.2f -> %,.2f",
                    a.getCostoUnitarioReal(), b.getCostoUnitarioReal()));
        }
        if (!Objects.equals(a.getObservacion(), b.getObservacion())) {
            cambios.add("nota modificada");
        }
        return cambios.isEmpty() ? "Sin cambios de valores" : String.join("; ", cambios);
    }

    private String fmtNum(double v) {
        return (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private MovimientoAlmacen mapear(ResultSet rs) throws SQLException {
        MovimientoAlmacen m = new MovimientoAlmacen();
        m.setId(rs.getLong("id"));
        m.setObraId(rs.getLong("obra_id"));
        m.setPartidaId(rs.getLong("partida_id"));
        m.setPartidaCodigo(rs.getString("partida_codigo"));
        m.setPartidaDescripcion(rs.getString("partida_descripcion"));
        m.setFecha(LocalDate.parse(rs.getString("fecha")));
        m.setTipo(MovimientoAlmacen.Tipo.valueOf(rs.getString("tipo")));
        m.setCantidad(rs.getDouble("cantidad"));
        m.setCostoUnitarioReal(rs.getDouble("costo_unitario_real"));
        m.setCostoTotalReal(rs.getDouble("costo_total_real"));
        m.setObservacion(rs.getString("observacion"));
        m.setUsuarioRegistro(rs.getString("usuario_registro"));
        m.setCreadoEn(leerColumnaOpcional(rs, "creado_en"));
        m.setActualizadoEn(leerColumnaOpcional(rs, "actualizado_en"));
        return m;
    }

    /** Lee una columna que puede no existir en consultas antiguas, sin fallar. */
    private String leerColumnaOpcional(ResultSet rs, String columna) {
        try {
            return rs.getString(columna);
        } catch (SQLException e) {
            return null;
        }
    }
}
