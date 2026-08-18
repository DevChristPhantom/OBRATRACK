package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.MonomioPolinomico;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD de los monomios de la formula polinomica de reajuste de precios de una obra. */
public class FormulaPolinomicaService implements IFormulaPolinomicaService {

    public MonomioPolinomico crear(MonomioPolinomico m) throws SQLException {
        if (m.getDescripcion() == null || m.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripcion del elemento es obligatoria");
        }
        String sql = "INSERT INTO monomio_polinomico (obra_id, descripcion, coeficiente_incidencia, indice_base) VALUES (?, ?, ?, ?)";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, m.getObraId());
                ps.setString(2, m.getDescripcion().trim());
                ps.setDouble(3, m.getCoeficienteIncidencia());
                ps.setDouble(4, m.getIndiceBase());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) m.setId(rs.getLong(1));
                }
            }
        }
        return m;
    }

    public void actualizar(MonomioPolinomico m) throws SQLException {
        if (m.getId() == null) {
            throw new IllegalArgumentException("El monomio a editar no tiene id");
        }
        String sql = "UPDATE monomio_polinomico SET descripcion = ?, coeficiente_incidencia = ?, indice_base = ? WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setString(1, m.getDescripcion().trim());
                ps.setDouble(2, m.getCoeficienteIncidencia());
                ps.setDouble(3, m.getIndiceBase());
                ps.setLong(4, m.getId());
                ps.executeUpdate();
            }
        }
    }

    public void eliminar(long id) throws SQLException {
        String sql = "DELETE FROM monomio_polinomico WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    public List<MonomioPolinomico> listarPorObra(long obraId) throws SQLException {
        List<MonomioPolinomico> resultado = new ArrayList<>();
        String sql = "SELECT * FROM monomio_polinomico WHERE obra_id = ? ORDER BY id";
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

    private MonomioPolinomico mapear(ResultSet rs) throws SQLException {
        MonomioPolinomico m = new MonomioPolinomico();
        m.setId(rs.getLong("id"));
        m.setObraId(rs.getLong("obra_id"));
        m.setDescripcion(rs.getString("descripcion"));
        m.setCoeficienteIncidencia(rs.getDouble("coeficiente_incidencia"));
        m.setIndiceBase(rs.getDouble("indice_base"));
        return m;
    }
}
