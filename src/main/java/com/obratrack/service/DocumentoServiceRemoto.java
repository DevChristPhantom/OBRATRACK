package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.core.Rutas;
import com.obratrack.model.Documento;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion remota de {@link IDocumentoService}. {@code listarPorObra},
 * {@code listarVersiones} y {@code eliminar} son RPC normales (no involucran archivos
 * en el mensaje). {@code subir} sube el archivo local por streaming a
 * {@code POST /archivos/subir}; {@code archivoAbsoluto} descarga el archivo guardado en
 * el host a una copia local en {@link Rutas#cache()} y devuelve esa ruta local.
 */
public class DocumentoServiceRemoto implements IDocumentoService {

    private static final String SERVICIO = "DocumentoService";

    @Override
    public Documento subir(Documento meta, Path archivoOrigen) throws SQLException, IOException {
        Path nombreArchivoPath = archivoOrigen.getFileName();
        if (nombreArchivoPath == null) {
            throw new IllegalArgumentException("Elige un archivo valido para subir");
        }
        Map<String, String> metadatos = new LinkedHashMap<>();
        metadatos.put("obraId", String.valueOf(meta.getObraId()));
        metadatos.put("categoria", meta.getCategoria().name());
        metadatos.put("nombre", meta.getNombre() != null ? meta.getNombre() : "");
        metadatos.put("descripcion", meta.getDescripcion() != null ? meta.getDescripcion() : "");
        if (meta.getPartidaId() != null) metadatos.put("partidaId", String.valueOf(meta.getPartidaId()));
        metadatos.put("nombreOriginal", nombreArchivoPath.toString());
        return RpcCliente.subirArchivo(archivoOrigen, metadatos);
    }

    @Override
    public List<Documento> listarPorObra(long obraId, Documento.Categoria categoria) throws SQLException {
        Type tipo = new TypeToken<List<Documento>>() { }.getType();
        return rpc("listarPorObra", tipo, obraId, categoria);
    }

    @Override
    public List<Documento> listarVersiones(long obraId, String nombre) throws SQLException {
        Type tipo = new TypeToken<List<Documento>>() { }.getType();
        return rpc("listarVersiones", tipo, obraId, nombre);
    }

    @Override
    public void eliminar(Documento d) throws SQLException {
        rpc("eliminar", void.class, d);
    }

    /** No declara {@code throws} (misma firma que la version local): los errores de red se
     *  envuelven en {@link UncheckedIOException}, igual que hace la version local con
     *  cualquier fallo de disco al resolver la ruta. */
    @Override
    public Path archivoAbsoluto(Documento d) {
        try {
            Path destino = Rutas.cache().resolve(nombreLocal(d));
            return RpcCliente.descargarArchivo(SERVICIO, "archivoAbsoluto", destino, d);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String nombreLocal(Documento d) {
        String ruta = d.getRutaArchivo();
        if (ruta == null || ruta.isBlank()) return "documento_" + d.getId();
        int slash = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
        return slash >= 0 ? ruta.substring(slash + 1) : ruta;
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
