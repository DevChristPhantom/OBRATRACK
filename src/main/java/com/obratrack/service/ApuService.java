package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.ApuInsumo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD del Analisis de Precios Unitarios (APU) de una partida. */
public class ApuService implements IApuService {

    public ApuInsumo crear(ApuInsumo i) throws SQLException {
        if (i.getDescripcion() == null || i.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripcion del insumo es obligatoria");
        }
        String sql = """
            INSERT INTO apu_insumo (partida_id, obra_id, tipo, descripcion, unidad, cantidad, precio_unitario)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, i.getPartidaId());
                ps.setLong(2, i.getObraId());
                ps.setString(3, i.getTipo().name());
                ps.setString(4, i.getDescripcion().trim());
                ps.setString(5, i.getUnidad());
                ps.setDouble(6, i.getCantidad());
                ps.setDouble(7, i.getPrecioUnitario());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) i.setId(rs.getLong(1));
                }
            }
        }
        return i;
    }

    public void actualizar(ApuInsumo i) throws SQLException {
        if (i.getId() == null) {
            throw new IllegalArgumentException("El insumo a editar no tiene id");
        }
        String sql = """
            UPDATE apu_insumo SET tipo = ?, descripcion = ?, unidad = ?, cantidad = ?, precio_unitario = ?
            WHERE id = ?
        """;
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setString(1, i.getTipo().name());
                ps.setString(2, i.getDescripcion().trim());
                ps.setString(3, i.getUnidad());
                ps.setDouble(4, i.getCantidad());
                ps.setDouble(5, i.getPrecioUnitario());
                ps.setLong(6, i.getId());
                ps.executeUpdate();
            }
        }
    }

    public void eliminar(long id) throws SQLException {
        String sql = "DELETE FROM apu_insumo WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    public List<ApuInsumo> listarPorPartida(long partidaId) throws SQLException {
        List<ApuInsumo> resultado = new ArrayList<>();
        String sql = "SELECT * FROM apu_insumo WHERE partida_id = ? ORDER BY tipo, descripcion";
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

    private ApuInsumo mapear(ResultSet rs) throws SQLException {
        ApuInsumo i = new ApuInsumo();
        i.setId(rs.getLong("id"));
        i.setPartidaId(rs.getLong("partida_id"));
        i.setObraId(rs.getLong("obra_id"));
        i.setTipo(ApuInsumo.Tipo.valueOf(rs.getString("tipo")));
        i.setDescripcion(rs.getString("descripcion"));
        i.setUnidad(rs.getString("unidad"));
        i.setCantidad(rs.getDouble("cantidad"));
        i.setPrecioUnitario(rs.getDouble("precio_unitario"));
        return i;
    }
}
