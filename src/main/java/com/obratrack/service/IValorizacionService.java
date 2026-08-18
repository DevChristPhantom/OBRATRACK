package com.obratrack.service;

import com.obratrack.model.Valorizacion;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de {@link ValorizacionService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link ValorizacionServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IValorizacionService {

    @Escritura
    Valorizacion generar(long obraId, LocalDate periodoDesde, LocalDate periodoHasta,
                          double pctRetencion, double montoAmortizacionAdelanto,
                          String observaciones) throws SQLException;

    List<Valorizacion> listarPorObra(long obraId) throws SQLException;

    @Escritura
    void eliminar(long id) throws SQLException;
}
