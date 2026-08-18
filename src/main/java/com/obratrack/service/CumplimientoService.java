package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.model.ItemCumplimiento;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** CRUD de items de gestion y cumplimiento (riesgos, ambiental, calidad, SST). */
public class CumplimientoService implements ICumplimientoService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ItemCumplimiento crear(ItemCumplimiento i) throws SQLException {
        if (i.getDescripcion() == null || i.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripcion es obligatoria");
        }
        String sql = """
            INSERT INTO item_cumplimiento (obra_id, partida_id, categoria, descripcion, probabilidad,
                                            impacto, severidad, fecha, fecha_limite, estado, fecha_cierre,
                                            responsable, accion_seguimiento, usuario_registro, creado_en)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                aplicarParametros(ps, i);
                ps.setString(14, i.getUsuarioRegistro() != null ? i.getUsuarioRegistro() : SesionActual.nombre());
                ps.setString(15, LocalDateTime.now().format(TS));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) i.setId(rs.getLong(1));
                }
            }
        }
        return i;
    }

    public void actualizar(ItemCumplimiento i) throws SQLException {
        if (i.getId() == null) {
            throw new IllegalArgumentException("El item a editar no tiene id");
        }
        String sql = """
            UPDATE item_cumplimiento
            SET partida_id = ?, categoria = ?, descripcion = ?, probabilidad = ?, impacto = ?,
                severidad = ?, fecha = ?, fecha_limite = ?, estado = ?, fecha_cierre = ?,
                responsable = ?, accion_seguimiento = ?
            WHERE id = ?
        """;
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setObject(1, i.getPartidaId());
                ps.setString(2, i.getCategoria().name());
                ps.setString(3, i.getDescripcion().trim());
                ps.setString(4, i.getProbabilidad() != null ? i.getProbabilidad().name() : null);
                ps.setString(5, i.getImpacto() != null ? i.getImpacto().name() : null);
                ps.setString(6, i.getSeveridad().name());
                ps.setString(7, i.getFecha().toString());
                ps.setString(8, i.getFechaLimite() != null ? i.getFechaLimite().toString() : null);
                ps.setString(9, i.getEstado().name());
                ps.setString(10, i.getFechaCierre() != null ? i.getFechaCierre().toString() : null);
                ps.setString(11, i.getResponsable());
                ps.setString(12, i.getAccionSeguimiento());
                ps.setLong(13, i.getId());
                ps.executeUpdate();
            }
        }
    }

    public void eliminar(long id) throws SQLException {
        String sql = "DELETE FROM item_cumplimiento WHERE id = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    public List<ItemCumplimiento> listarPorObra(long obraId, ItemCumplimiento.Categoria categoria) throws SQLException {
        List<ItemCumplimiento> resultado = new ArrayList<>();
        String sql = "SELECT * FROM item_cumplimiento WHERE obra_id = ? AND categoria = ? ORDER BY fecha DESC, id DESC";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, obraId);
                ps.setString(2, categoria.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        resultado.add(mapear(rs));
                    }
                }
            }
        }
        return resultado;
    }

    private void aplicarParametros(PreparedStatement ps, ItemCumplimiento i) throws SQLException {
        ps.setLong(1, i.getObraId());
        ps.setObject(2, i.getPartidaId());
        ps.setString(3, i.getCategoria().name());
        ps.setString(4, i.getDescripcion().trim());
        ps.setString(5, i.getProbabilidad() != null ? i.getProbabilidad().name() : null);
        ps.setString(6, i.getImpacto() != null ? i.getImpacto().name() : null);
        ps.setString(7, i.getSeveridad().name());
        ps.setString(8, i.getFecha().toString());
        ps.setString(9, i.getFechaLimite() != null ? i.getFechaLimite().toString() : null);
        ps.setString(10, i.getEstado().name());
        ps.setString(11, i.getFechaCierre() != null ? i.getFechaCierre().toString() : null);
        ps.setString(12, i.getResponsable());
        ps.setString(13, i.getAccionSeguimiento());
    }

    private ItemCumplimiento mapear(ResultSet rs) throws SQLException {
        ItemCumplimiento i = new ItemCumplimiento();
        i.setId(rs.getLong("id"));
        i.setObraId(rs.getLong("obra_id"));
        long partidaId = rs.getLong("partida_id");
        i.setPartidaId(rs.wasNull() ? null : partidaId);
        i.setCategoria(ItemCumplimiento.Categoria.valueOf(rs.getString("categoria")));
        i.setDescripcion(rs.getString("descripcion"));
        String prob = rs.getString("probabilidad");
        i.setProbabilidad(prob != null ? ItemCumplimiento.Probabilidad.valueOf(prob) : null);
        String imp = rs.getString("impacto");
        i.setImpacto(imp != null ? ItemCumplimiento.Impacto.valueOf(imp) : null);
        i.setSeveridad(ItemCumplimiento.Severidad.valueOf(rs.getString("severidad")));
        i.setFecha(parseDate(rs.getString("fecha")));
        i.setFechaLimite(parseDate(rs.getString("fecha_limite")));
        i.setEstado(ItemCumplimiento.Estado.valueOf(rs.getString("estado")));
        i.setFechaCierre(parseDate(rs.getString("fecha_cierre")));
        i.setResponsable(rs.getString("responsable"));
        i.setAccionSeguimiento(rs.getString("accion_seguimiento"));
        i.setUsuarioRegistro(rs.getString("usuario_registro"));
        i.setCreadoEn(rs.getString("creado_en"));
        return i;
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
