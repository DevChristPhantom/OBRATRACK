package com.obratrack.service;

import com.obratrack.model.Documento;
import com.obratrack.red.Escritura;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link DocumentoService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link DocumentoServiceRemoto} cuando esta PC es cliente
 * en la red de la obra. A diferencia de los demas servicios, {@code subir} y
 * {@code archivoAbsoluto} no viajan por el RPC generico (un {@code Path} solo tiene
 * sentido en la maquina que lo genero) — usan los endpoints dedicados de
 * {@code ServidorHttp}/{@code RpcCliente} para mover los bytes del archivo.
 */
public interface IDocumentoService {

    /** Marcado {@code @Escritura} solo por documentacion: la subida no pasa por el
     *  despachador RPC generico, asi que el permiso se verifica a mano en el host
     *  (ver {@code ServidorHttp.manejarSubirArchivo}). */
    @Escritura
    Documento subir(Documento meta, Path archivoOrigen) throws SQLException, IOException;

    List<Documento> listarPorObra(long obraId, Documento.Categoria categoria) throws SQLException;

    List<Documento> listarVersiones(long obraId, String nombre) throws SQLException;

    @Escritura
    void eliminar(Documento d) throws SQLException;

    Path archivoAbsoluto(Documento d);
}
