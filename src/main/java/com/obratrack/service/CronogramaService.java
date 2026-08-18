package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.Actividad;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** CRUD de actividades del cronograma. Todo acceso se serializa bajo {@link Database#LOCK}. */
public class CronogramaService implements ICronogramaService {

    public Actividad crear(Actividad a) throws SQLException {
        String sql = """
            INSERT INTO actividad (obra_id, partida_id, codigo, descripcion, fecha_inicio_prog,
                                    fecha_fin_prog, fecha_inicio_real, fecha_fin_real,
                                    peso_porcentual, avance_real, orden)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                aplicarParametros(ps, a);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) a.setId(rs.getLong(1));
                }
            }
        }
        return a;
    }

    public void actualizar(Actividad a) throws SQLException {
        if (a.getId() == null) {
            throw new IllegalArgumentException("La actividad a editar no tiene id");
        }
        String sql = """
            UPDATE actividad
            SET partida_id = ?, codigo = ?, descripcion = ?, fecha_inicio_prog = ?, fecha_fin_prog = ?,
                fecha_inicio_real = ?, fecha_fin_real = ?, peso_porcentual = ?, avance_real = ?, orden = ?
            WHERE id = ?
        """;
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setObject(1, a.getPartidaId());
                ps.setString(2, a.getCodigo());
                ps.setString(3, a.getDescripcion());
                ps.setString(4, str(a.getFechaInicioProg()));
                ps.setString(5, str(a.getFechaFinProg()));
                ps.setString(6, str(a.getFechaInicioReal()));
                ps.setString(7, str(a.getFechaFinReal()));
                ps.setDouble(8, a.getPesoPorcentual());
                ps.setDouble(9, a.getAvanceReal());
                ps.setInt(10, a.getOrden());
                ps.setLong(11, a.getId());
                ps.executeUpdate();
            }
        }
    }

    public void eliminar(long id) throws SQLException {
        String sql = "DELETE FROM actividad WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    public List<Actividad> listarPorObra(long obraId) throws SQLException {
        List<Actividad> resultado = new ArrayList<>();
        String sql = "SELECT * FROM actividad WHERE obra_id = ? ORDER BY orden, fecha_inicio_prog";
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

    /** Suma de pesos ya registrados en la obra, para avisar si al agregar una actividad se pasa de 100%. */
    public double sumaPesosPorObra(long obraId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(peso_porcentual), 0) FROM actividad WHERE obra_id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, obraId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getDouble(1) : 0;
                }
            }
        }
    }

    private void aplicarParametros(PreparedStatement ps, Actividad a) throws SQLException {
        ps.setLong(1, a.getObraId());
        ps.setObject(2, a.getPartidaId());
        ps.setString(3, a.getCodigo());
        ps.setString(4, a.getDescripcion());
        ps.setString(5, str(a.getFechaInicioProg()));
        ps.setString(6, str(a.getFechaFinProg()));
        ps.setString(7, str(a.getFechaInicioReal()));
        ps.setString(8, str(a.getFechaFinReal()));
        ps.setDouble(9, a.getPesoPorcentual());
        ps.setDouble(10, a.getAvanceReal());
        ps.setInt(11, a.getOrden());
    }

    private Actividad mapear(ResultSet rs) throws SQLException {
        Actividad a = new Actividad();
        a.setId(rs.getLong("id"));
        a.setObraId(rs.getLong("obra_id"));
        long partidaId = rs.getLong("partida_id");
        a.setPartidaId(rs.wasNull() ? null : partidaId);
        a.setCodigo(rs.getString("codigo"));
        a.setDescripcion(rs.getString("descripcion"));
        a.setFechaInicioProg(parseDate(rs.getString("fecha_inicio_prog")));
        a.setFechaFinProg(parseDate(rs.getString("fecha_fin_prog")));
        a.setFechaInicioReal(parseDate(rs.getString("fecha_inicio_real")));
        a.setFechaFinReal(parseDate(rs.getString("fecha_fin_real")));
        a.setPesoPorcentual(rs.getDouble("peso_porcentual"));
        a.setAvanceReal(rs.getDouble("avance_real"));
        a.setOrden(rs.getInt("orden"));
        return a;
    }

    private String str(LocalDate d) {
        return d != null ? d.toString() : null;
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
