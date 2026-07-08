package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.Obra;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** CRUD de obras: crear, listar, actualizar, cambiar estado. */
public class ObraService {

    public Obra crear(Obra obra) throws SQLException {
        String sql = """
            INSERT INTO obra (nombre, descripcion, fecha_inicio, fecha_fin_estimada,
                               presupuesto_total, estado, ruta_excel_origen, fecha_creacion)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, obra.getNombre());
            ps.setString(2, obra.getDescripcion());
            ps.setString(3, str(obra.getFechaInicio()));
            ps.setString(4, str(obra.getFechaFinEstimada()));
            ps.setDouble(5, obra.getPresupuestoTotal());
            ps.setString(6, obra.getEstado().name());
            ps.setString(7, obra.getRutaExcelOrigen());
            ps.setString(8, str(obra.getFechaCreacion() != null ? obra.getFechaCreacion() : LocalDate.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) obra.setId(rs.getLong(1));
            }
        }
        return obra;
    }

    public List<Obra> listarTodas() throws SQLException {
        List<Obra> resultado = new ArrayList<>();
        String sql = "SELECT * FROM obra ORDER BY fecha_creacion DESC, id DESC";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    public List<Obra> listarActivas() throws SQLException {
        List<Obra> resultado = new ArrayList<>();
        String sql = "SELECT * FROM obra WHERE estado = 'ACTIVA' ORDER BY nombre";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    public void actualizarPresupuestoTotal(long obraId, double total) throws SQLException {
        String sql = "UPDATE obra SET presupuesto_total = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setDouble(1, total);
            ps.setLong(2, obraId);
            ps.executeUpdate();
        }
    }

    public void actualizarRutaExcel(long obraId, String ruta) throws SQLException {
        String sql = "UPDATE obra SET ruta_excel_origen = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, ruta);
            ps.setLong(2, obraId);
            ps.executeUpdate();
        }
    }

    public void cambiarEstado(long obraId, Obra.Estado estado) throws SQLException {
        String sql = "UPDATE obra SET estado = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setLong(2, obraId);
            ps.executeUpdate();
        }
    }

    /** Actualiza los datos editables de la obra (nombre, descripcion, fechas, estado). */
    public void actualizar(Obra obra) throws SQLException {
        if (obra.getId() == null) {
            throw new IllegalArgumentException("La obra a editar no tiene id");
        }
        if (obra.getNombre() == null || obra.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la obra es obligatorio");
        }
        String sql = """
            UPDATE obra
            SET nombre = ?, descripcion = ?, fecha_inicio = ?, fecha_fin_estimada = ?, estado = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, obra.getNombre().trim());
            ps.setString(2, obra.getDescripcion());
            ps.setString(3, str(obra.getFechaInicio()));
            ps.setString(4, str(obra.getFechaFinEstimada()));
            ps.setString(5, obra.getEstado().name());
            ps.setLong(6, obra.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Elimina la obra y todo lo asociado: sus partidas y movimientos se borran en cascada
     * (FOREIGN KEY ON DELETE CASCADE); el historial de auditoria se limpia aparte porque
     * esa tabla no tiene clave foranea.
     */
    public void eliminar(long obraId) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "DELETE FROM movimiento_auditoria WHERE obra_id = ?")) {
            ps.setLong(1, obraId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM obra WHERE id = ?")) {
            ps.setLong(1, obraId);
            ps.executeUpdate();
        }
    }

    private Obra mapear(ResultSet rs) throws SQLException {
        Obra o = new Obra();
        o.setId(rs.getLong("id"));
        o.setNombre(rs.getString("nombre"));
        o.setDescripcion(rs.getString("descripcion"));
        o.setFechaInicio(parseDate(rs.getString("fecha_inicio")));
        o.setFechaFinEstimada(parseDate(rs.getString("fecha_fin_estimada")));
        o.setPresupuestoTotal(rs.getDouble("presupuesto_total"));
        o.setEstado(Obra.Estado.valueOf(rs.getString("estado")));
        o.setRutaExcelOrigen(rs.getString("ruta_excel_origen"));
        o.setFechaCreacion(parseDate(rs.getString("fecha_creacion")));
        return o;
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
