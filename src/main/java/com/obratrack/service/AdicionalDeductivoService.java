package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.AdicionalDeductivo;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de adicionales y deductivos: modificaciones aprobadas al presupuesto contractual.
 * Numeracion correlativa por obra, como el resto de registros formales (valorizaciones,
 * asientos de cuaderno de obra).
 */
public class AdicionalDeductivoService implements IAdicionalDeductivoService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AdicionalDeductivo crear(AdicionalDeductivo ad) throws SQLException {
        if (ad.getDescripcion() == null || ad.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripcion es obligatoria");
        }
        String sql = """
            INSERT INTO adicional_deductivo (obra_id, numero, tipo, descripcion, monto, fecha_aprobacion,
                                              resolucion_aprobacion, usuario_registro, creado_en)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        synchronized (Database.LOCK) {
            Connection conn = Database.get();
            int siguienteNumero = siguienteNumero(conn, ad.getObraId());
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, ad.getObraId());
                ps.setInt(2, siguienteNumero);
                ps.setString(3, ad.getTipo().name());
                ps.setString(4, ad.getDescripcion().trim());
                ps.setDouble(5, ad.getMonto());
                ps.setString(6, ad.getFechaAprobacion().toString());
                ps.setString(7, ad.getResolucionAprobacion());
                ps.setString(8, ad.getUsuarioRegistro() != null ? ad.getUsuarioRegistro() : SesionActual.nombre());
                ps.setString(9, LocalDateTime.now().format(TS));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) ad.setId(rs.getLong(1));
                }
            }
            ad.setNumero(siguienteNumero);
        }
        return ad;
    }

    public void eliminar(long id) throws SQLException {
        String sql = "DELETE FROM adicional_deductivo WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    public List<AdicionalDeductivo> listarPorObra(long obraId) throws SQLException {
        List<AdicionalDeductivo> resultado = new ArrayList<>();
        String sql = "SELECT * FROM adicional_deductivo WHERE obra_id = ? ORDER BY numero DESC";
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

    private int siguienteNumero(Connection conn, long obraId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(numero), 0) + 1 FROM adicional_deductivo WHERE obra_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, obraId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    private AdicionalDeductivo mapear(ResultSet rs) throws SQLException {
        AdicionalDeductivo ad = new AdicionalDeductivo();
        ad.setId(rs.getLong("id"));
        ad.setObraId(rs.getLong("obra_id"));
        ad.setNumero(rs.getInt("numero"));
        ad.setTipo(AdicionalDeductivo.Tipo.valueOf(rs.getString("tipo")));
        ad.setDescripcion(rs.getString("descripcion"));
        ad.setMonto(rs.getDouble("monto"));
        ad.setFechaAprobacion(parseDate(rs.getString("fecha_aprobacion")));
        ad.setResolucionAprobacion(rs.getString("resolucion_aprobacion"));
        ad.setUsuarioRegistro(rs.getString("usuario_registro"));
        ad.setCreadoEn(rs.getString("creado_en"));
        return ad;
    }

    private java.time.LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return java.time.LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
