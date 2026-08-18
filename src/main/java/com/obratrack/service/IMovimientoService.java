package com.obratrack.service;

import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.MovimientoAuditoria;
import com.obratrack.model.ResumenPeriodo;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Contrato de {@link MovimientoService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link MovimientoServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IMovimientoService {

    @Escritura
    MovimientoAlmacen registrar(MovimientoAlmacen mov) throws SQLException;

    @Escritura
    void actualizar(MovimientoAlmacen mov) throws SQLException;

    @Escritura
    void eliminar(long movimientoId) throws SQLException;

    MovimientoAlmacen obtenerPorId(long id) throws SQLException;

    List<MovimientoAlmacen> listarPorObra(long obraId) throws SQLException;

    List<MovimientoAlmacen> listarPorObraYFecha(long obraId, LocalDate fecha) throws SQLException;

    Map<Long, Double> totalEjecutadoPorPartida(long obraId) throws SQLException;

    double totalEjecutadoObra(long obraId) throws SQLException;

    double totalEjecutadoEntreFechas(long obraId, LocalDate desde, LocalDate hasta) throws SQLException;

    List<ResumenPeriodo> resumenPorPeriodo(long obraId, Granularidad granularidad) throws SQLException;

    List<MovimientoAuditoria> listarAuditoria(long obraId) throws SQLException;
}
