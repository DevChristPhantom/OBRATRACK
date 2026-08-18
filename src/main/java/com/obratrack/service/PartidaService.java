package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.Partida;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD de partidas de obra. Todo acceso se serializa bajo {@link Database#LOCK}. */
public class PartidaService implements IPartidaService {

    public void guardarTodas(long obraId, List<Partida> partidas) throws SQLException {
        String sql = """
            INSERT INTO partida (obra_id, codigo, descripcion, unidad, cantidad_presupuestada,
                                  costo_unitario, costo_total_presupuestado, es_padre, nivel)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        synchronized (Database.LOCK) {
            Connection conn = Database.get();
            boolean autoCommitOriginal = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Partida p : partidas) {
                    ps.setLong(1, obraId);
                    ps.setString(2, p.getCodigo());
                    ps.setString(3, p.getDescripcion());
                    ps.setString(4, p.getUnidad());
                    ps.setDouble(5, p.getCantidadPresupuestada());
                    ps.setDouble(6, p.getCostoUnitario());
                    ps.setDouble(7, p.getCostoTotalPresupuestado());
                    ps.setInt(8, p.isEsPadre() ? 1 : 0);
                    ps.setInt(9, p.getNivel());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    public List<Partida> listarPorObra(long obraId) throws SQLException {
        List<Partida> resultado = new ArrayList<>();
        String sql = "SELECT * FROM partida WHERE obra_id = ? ORDER BY codigo";
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

    /** Solo partidas ejecutables (hoja), para mostrar en el dropdown del formulario de registro. */
    public List<Partida> listarEjecutablesPorObra(long obraId) throws SQLException {
        List<Partida> resultado = new ArrayList<>();
        String sql = "SELECT * FROM partida WHERE obra_id = ? AND es_padre = 0 ORDER BY codigo";
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

    public void eliminarPorObra(long obraId) throws SQLException {
        String sql = "DELETE FROM partida WHERE obra_id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, obraId);
                ps.executeUpdate();
            }
        }
    }

    private Partida mapear(ResultSet rs) throws SQLException {
        Partida p = new Partida();
        p.setId(rs.getLong("id"));
        p.setObraId(rs.getLong("obra_id"));
        p.setCodigo(rs.getString("codigo"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setUnidad(rs.getString("unidad"));
        p.setCantidadPresupuestada(rs.getDouble("cantidad_presupuestada"));
        p.setCostoUnitario(rs.getDouble("costo_unitario"));
        p.setCostoTotalPresupuestado(rs.getDouble("costo_total_presupuestado"));
        p.setEsPadre(rs.getInt("es_padre") == 1);
        p.setNivel(rs.getInt("nivel"));
        return p;
    }
}
