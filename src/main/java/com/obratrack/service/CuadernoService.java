package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.AsientoCuaderno;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuaderno de obra digital: solo alta y lectura de asientos (append-only, es un
 * registro legal de la obra). No existen metodos de edicion ni borrado a proposito,
 * igual que la auditoria de movimientos de almacen.
 */
public class CuadernoService implements ICuadernoService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Crea el asiento asignandole el siguiente numero correlativo de la obra (como un folio). */
    public AsientoCuaderno crear(AsientoCuaderno a) throws SQLException {
        if (a.getTexto() == null || a.getTexto().isBlank()) {
            throw new IllegalArgumentException("La anotacion no puede estar vacia");
        }
        String sql = """
            INSERT INTO asiento_cuaderno (obra_id, numero, fecha, tipo, clima, personal_obra,
                                           texto, usuario_registro, creado_en)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        synchronized (Database.LOCK) {
            Connection conn = Database.get();
            int siguienteNumero = siguienteNumero(conn, a.getObraId());
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, a.getObraId());
                ps.setInt(2, siguienteNumero);
                ps.setString(3, a.getFecha().toString());
                ps.setString(4, a.getTipo().name());
                ps.setString(5, a.getClima() != null ? a.getClima().name() : null);
                ps.setInt(6, a.getPersonalObra());
                ps.setString(7, a.getTexto().trim());
                ps.setString(8, a.getUsuarioRegistro() != null ? a.getUsuarioRegistro() : SesionActual.nombre());
                ps.setString(9, LocalDateTime.now().format(TS));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) a.setId(rs.getLong(1));
                }
            }
            a.setNumero(siguienteNumero);
        }
        return a;
    }

    public List<AsientoCuaderno> listarPorObra(long obraId) throws SQLException {
        List<AsientoCuaderno> resultado = new ArrayList<>();
        String sql = "SELECT * FROM asiento_cuaderno WHERE obra_id = ? ORDER BY numero DESC";
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
        String sql = "SELECT COALESCE(MAX(numero), 0) + 1 FROM asiento_cuaderno WHERE obra_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, obraId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    private AsientoCuaderno mapear(ResultSet rs) throws SQLException {
        AsientoCuaderno a = new AsientoCuaderno();
        a.setId(rs.getLong("id"));
        a.setObraId(rs.getLong("obra_id"));
        a.setNumero(rs.getInt("numero"));
        a.setFecha(parseDate(rs.getString("fecha")));
        a.setTipo(AsientoCuaderno.Tipo.valueOf(rs.getString("tipo")));
        String clima = rs.getString("clima");
        a.setClima(clima != null ? AsientoCuaderno.Clima.valueOf(clima) : null);
        a.setPersonalObra(rs.getInt("personal_obra"));
        a.setTexto(rs.getString("texto"));
        a.setUsuarioRegistro(rs.getString("usuario_registro"));
        a.setCreadoEn(rs.getString("creado_en"));
        return a;
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
