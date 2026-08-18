package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.MetradoDetalle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD del metrado desagregado por sector de una partida. */
public class MetradoService implements IMetradoService {

    public MetradoDetalle crear(MetradoDetalle m) throws SQLException {
        if (m.getSector() == null || m.getSector().isBlank()) {
            throw new IllegalArgumentException("El sector/bloque es obligatorio");
        }
        String sql = "INSERT INTO metrado_detalle (partida_id, obra_id, sector, cantidad, observacion) VALUES (?, ?, ?, ?, ?)";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, m.getPartidaId());
                ps.setLong(2, m.getObraId());
                ps.setString(3, m.getSector().trim());
                ps.setDouble(4, m.getCantidad());
                ps.setString(5, m.getObservacion());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) m.setId(rs.getLong(1));
                }
            }
        }
        return m;
    }

    public void actualizar(MetradoDetalle m) throws SQLException {
        if (m.getId() == null) {
            throw new IllegalArgumentException("La linea de metrado a editar no tiene id");
        }
        String sql = "UPDATE metrado_detalle SET sector = ?, cantidad = ?, observacion = ? WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setString(1, m.getSector().trim());
                ps.setDouble(2, m.getCantidad());
                ps.setString(3, m.getObservacion());
                ps.setLong(4, m.getId());
                ps.executeUpdate();
            }
        }
    }

    public void eliminar(long id) throws SQLException {
        String sql = "DELETE FROM metrado_detalle WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    public List<MetradoDetalle> listarPorPartida(long partidaId) throws SQLException {
        List<MetradoDetalle> resultado = new ArrayList<>();
        String sql = "SELECT * FROM metrado_detalle WHERE partida_id = ? ORDER BY sector";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, partidaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        resultado.add(mapear(rs));
                    }
                }
            }
        }
        return resultado;
    }

    private MetradoDetalle mapear(ResultSet rs) throws SQLException {
        MetradoDetalle m = new MetradoDetalle();
        m.setId(rs.getLong("id"));
        m.setPartidaId(rs.getLong("partida_id"));
        m.setObraId(rs.getLong("obra_id"));
        m.setSector(rs.getString("sector"));
        m.setCantidad(rs.getDouble("cantidad"));
        m.setObservacion(rs.getString("observacion"));
        return m;
    }
}
