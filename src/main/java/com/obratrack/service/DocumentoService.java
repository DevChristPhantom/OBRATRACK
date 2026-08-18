package com.obratrack.service;

import com.obratrack.core.Database;
import com.obratrack.core.Rutas;
import com.obratrack.model.Documento;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Documentos de la obra: el archivo real se copia a la carpeta de datos de la
 * app ({@link Rutas#documentos()}); esta clase guarda la metadata en SQLite y
 * resuelve la ruta absoluta para abrir o borrar el archivo. Solo alta, lectura
 * y borrado: un plano nuevo se sube como version nueva, no se sobreescribe la anterior.
 */
public class DocumentoService implements IDocumentoService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Copia el archivo de origen al almacen de la app y guarda su metadata. */
    public Documento subir(Documento meta, Path archivoOrigen) throws SQLException, IOException {
        if (meta.getNombre() == null || meta.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del documento es obligatorio");
        }
        if (archivoOrigen == null || !Files.isRegularFile(archivoOrigen)) {
            throw new IllegalArgumentException("Elige un archivo valido para subir");
        }
        Path nombreArchivoPath = archivoOrigen.getFileName();
        if (nombreArchivoPath == null) {
            throw new IllegalArgumentException("Elige un archivo valido para subir");
        }
        String nombreOriginal = nombreArchivoPath.toString();

        if (meta.getCategoria() == Documento.Categoria.PLANO) {
            List<Documento> historial = listarVersiones(meta.getObraId(), meta.getNombre());
            meta.setVersion(DocumentoCalculo.siguienteVersion(historial));
        } else {
            meta.setVersion(1);
        }

        String extension = extension(nombreOriginal);
        String nombreArchivo = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path carpeta = Rutas.documentos().resolve(String.valueOf(meta.getObraId())).resolve(meta.getCategoria().name());
        Files.createDirectories(carpeta);
        Path destino = carpeta.resolve(nombreArchivo);
        Files.copy(archivoOrigen, destino, StandardCopyOption.REPLACE_EXISTING);

        meta.setRutaArchivo(Rutas.documentos().relativize(destino).toString());
        meta.setNombreArchivoOriginal(nombreOriginal);
        meta.setTamanoBytes(Files.size(destino));

        String sql = """
            INSERT INTO documento (obra_id, partida_id, categoria, nombre, version, ruta_archivo,
                                    nombre_archivo_original, tamano_bytes, fecha, descripcion,
                                    usuario_registro, creado_en)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try {
            synchronized (Database.LOCK) {
                try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, meta.getObraId());
                    ps.setObject(2, meta.getPartidaId());
                    ps.setString(3, meta.getCategoria().name());
                    ps.setString(4, meta.getNombre().trim());
                    ps.setInt(5, meta.getVersion());
                    ps.setString(6, meta.getRutaArchivo());
                    ps.setString(7, meta.getNombreArchivoOriginal());
                    ps.setLong(8, meta.getTamanoBytes());
                    ps.setString(9, meta.getFecha().toString());
                    ps.setString(10, meta.getDescripcion());
                    ps.setString(11, meta.getUsuarioRegistro() != null ? meta.getUsuarioRegistro() : SesionActual.nombre());
                    ps.setString(12, LocalDateTime.now().format(TS));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) meta.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            Files.deleteIfExists(destino); // no dejar un archivo huerfano si la fila no se pudo guardar
            throw e;
        }
        return meta;
    }

    public List<Documento> listarPorObra(long obraId, Documento.Categoria categoria) throws SQLException {
        List<Documento> resultado = new ArrayList<>();
        String sql = "SELECT * FROM documento WHERE obra_id = ? AND categoria = ? ORDER BY nombre, version DESC";
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

    /** Historial de versiones de un plano por su nombre (para calcular la siguiente version). */
    public List<Documento> listarVersiones(long obraId, String nombre) throws SQLException {
        List<Documento> resultado = new ArrayList<>();
        String sql = "SELECT * FROM documento WHERE obra_id = ? AND categoria = 'PLANO' AND nombre = ?";
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                ps.setLong(1, obraId);
                ps.setString(2, nombre.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        resultado.add(mapear(rs));
                    }
                }
            }
        }
        return resultado;
    }

    /** Borra la fila y el archivo fisico asociado. */
    public void eliminar(Documento d) throws SQLException {
        synchronized (Database.LOCK) {
            try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM documento WHERE id = ?")) {
                ps.setLong(1, d.getId());
                ps.executeUpdate();
            }
        }
        try {
            Files.deleteIfExists(archivoAbsoluto(d));
        } catch (IOException ignored) {
            // la fila ya se borro; si el archivo no se pudo borrar queda huerfano en disco (no bloquea la app)
        }
    }

    public Path archivoAbsoluto(Documento d) {
        return Rutas.documentos().resolve(d.getRutaArchivo());
    }

    private String extension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        return punto >= 0 && punto < nombreArchivo.length() - 1 ? nombreArchivo.substring(punto + 1) : "";
    }

    private Documento mapear(ResultSet rs) throws SQLException {
        Documento d = new Documento();
        d.setId(rs.getLong("id"));
        d.setObraId(rs.getLong("obra_id"));
        long partidaId = rs.getLong("partida_id");
        d.setPartidaId(rs.wasNull() ? null : partidaId);
        d.setCategoria(Documento.Categoria.valueOf(rs.getString("categoria")));
        d.setNombre(rs.getString("nombre"));
        d.setVersion(rs.getInt("version"));
        d.setRutaArchivo(rs.getString("ruta_archivo"));
        d.setNombreArchivoOriginal(rs.getString("nombre_archivo_original"));
        d.setTamanoBytes(rs.getLong("tamano_bytes"));
        d.setFecha(parseDate(rs.getString("fecha")));
        d.setDescripcion(rs.getString("descripcion"));
        d.setUsuarioRegistro(rs.getString("usuario_registro"));
        d.setCreadoEn(rs.getString("creado_en"));
        return d;
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
