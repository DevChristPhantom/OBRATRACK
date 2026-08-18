package com.obratrack.service;

import com.obratrack.model.Obra;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link ObraService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link ObraServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IObraService {

    @Escritura
    Obra crear(Obra obra) throws SQLException;

    List<Obra> listarTodas() throws SQLException;

    List<Obra> listarActivas() throws SQLException;

    @Escritura
    void actualizarPresupuestoTotal(long obraId, double total) throws SQLException;

    @Escritura
    void actualizarRutaExcel(long obraId, String ruta) throws SQLException;

    @Escritura
    void cambiarEstado(long obraId, Obra.Estado estado) throws SQLException;

    @Escritura
    void actualizar(Obra obra) throws SQLException;

    @Escritura
    void eliminar(long obraId) throws SQLException;
}
