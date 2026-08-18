package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.Valorizacion;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Valorizaciones mensuales: se calculan a partir de los movimientos de almacen
 * de un periodo y quedan como un corte formal (append-only, solo alta y borrado
 * de una emitida por error; no se edita un monto ya calculado).
 */
public class ValorizacionService implements IValorizacionService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MovimientoService movimientoService = new MovimientoService();

    /**
     * Genera la siguiente valorizacion de la obra para el periodo indicado: calcula el
     * monto ejecutado del periodo (de los movimientos de almacen), el acumulado de las
     * valorizaciones previas, la retencion y el neto a pagar, y lo guarda como un corte fijo.
     */
    public Valorizacion generar(long obraId, LocalDate periodoDesde, LocalDate periodoHasta,
                                 double pctRetencion, double montoAmortizacionAdelanto,
                                 String observaciones) throws SQLException {
        if (periodoHasta.isBefore(periodoDesde)) {
            throw new IllegalArgumentException("El fin del periodo no puede ser anterior al inicio");
        }

        double montoAcumuladoAntes = sumaAcumuladaPorObra(obraId);
        double montoEjecutadoPeriodo = movimientoService.totalEjecutadoEntreFechas(obraId, periodoDesde, periodoHasta);
        double montoRetencion = ValorizacionCalculo.calcularRetencion(montoEjecutadoPeriodo, pctRetencion);
        double montoNeto = ValorizacionCalculo.calcularNetoPagar(montoEjecutadoPeriodo, montoRetencion, montoAmortizacionAdelanto);

        Valorizacion v = new Valorizacion();
        v.setObraId(obraId);
        v.setPeriodoDesde(periodoDesde);
        v.setPeriodoHasta(periodoHasta);
        v.setMontoAcumuladoAntes(montoAcumuladoAntes);
        v.setMontoEjecutadoPeriodo(montoEjecutadoPeriodo);
        v.setPctRetencion(pctRetencion);
        v.setMontoRetencion(montoRetencion);
        v.setMontoAmortizacionAdelanto(montoAmortizacionAdelanto);
        v.setMontoNetoPagar(montoNeto);
        v.setObservaciones(observaciones);

        String sql = """
            INSERT INTO valorizacion (obra_id, numero, periodo_desde, periodo_hasta, fecha_emision,
                                       monto_ejecutado_periodo, monto_acumulado_antes, pct_retencion,
                                       monto_retencion, monto_amortizacion_adelanto, monto_neto_pagar,
                                       observaciones, usuario_registro, creado_en)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        synchronized (Database.LOCK) {
            Connection conn = Database.get();
            int siguienteNumero = siguienteNumero(conn, obraId);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, obraId);
                ps.setInt(2, siguienteNumero);
                ps.setString(3, periodoDesde.toString());
                ps.setString(4, periodoHasta.toString());
                ps.setString(5, v.getFechaEmision().toString());
                ps.setDouble(6, montoEjecutadoPeriodo);
                ps.setDouble(7, montoAcumuladoAntes);
                ps.setDouble(8, pctRetencion);
                ps.setDouble(9, montoRetencion);
                ps.setDouble(10, montoAmortizacionAdelanto);
                ps.setDouble(11, montoNeto);
                ps.setString(12, observaciones);
                ps.setString(13, v.getUsuarioRegistro() != null ? v.getUsuarioRegistro() : SesionActual.nombre());
                ps.setString(14, LocalDateTime.now().format(TS));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) v.setId(rs.getLong(1));
                }
            }
            v.setNumero(siguienteNumero);
        }
        return v;
    }

    public List<Valorizacion> listarPorObra(long obraId) throws SQLException {
        List<Valorizacion> resultado = new ArrayList<>();
        String sql = "SELECT * FROM valorizacion WHERE obra_id = ? ORDER BY numero DESC";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, obraId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        resultado.add(mapear(rs));
                    }
                }
            }
        }
        return resultado;
    }

    /** Elimina una valorizacion (solo para corregir un error antes de presentarla). */
    public void eliminar(long id) throws SQLException {
        String sql = "DELETE FROM valorizacion WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    /** Suma de lo ejecutado en todas las valorizaciones ya emitidas de la obra. */
    private double sumaAcumuladaPorObra(long obraId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto_ejecutado_periodo), 0) FROM valorizacion WHERE obra_id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, obraId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getDouble(1) : 0;
                }
            }
        }
    }

    private int siguienteNumero(Connection conn, long obraId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(numero), 0) + 1 FROM valorizacion WHERE obra_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, obraId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    private Valorizacion mapear(ResultSet rs) throws SQLException {
        Valorizacion v = new Valorizacion();
        v.setId(rs.getLong("id"));
        v.setObraId(rs.getLong("obra_id"));
        v.setNumero(rs.getInt("numero"));
        v.setPeriodoDesde(parseDate(rs.getString("periodo_desde")));
        v.setPeriodoHasta(parseDate(rs.getString("periodo_hasta")));
        v.setFechaEmision(parseDate(rs.getString("fecha_emision")));
        v.setMontoEjecutadoPeriodo(rs.getDouble("monto_ejecutado_periodo"));
        v.setMontoAcumuladoAntes(rs.getDouble("monto_acumulado_antes"));
        v.setPctRetencion(rs.getDouble("pct_retencion"));
        v.setMontoRetencion(rs.getDouble("monto_retencion"));
        v.setMontoAmortizacionAdelanto(rs.getDouble("monto_amortizacion_adelanto"));
        v.setMontoNetoPagar(rs.getDouble("monto_neto_pagar"));
        v.setObservaciones(rs.getString("observaciones"));
        v.setUsuarioRegistro(rs.getString("usuario_registro"));
        v.setCreadoEn(rs.getString("creado_en"));
        return v;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
