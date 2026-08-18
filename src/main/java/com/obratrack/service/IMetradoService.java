package com.obratrack.service;

import com.obratrack.model.MetradoDetalle;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link MetradoService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link MetradoServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IMetradoService {

    @Escritura
    MetradoDetalle crear(MetradoDetalle m) throws SQLException;

    @Escritura
    void actualizar(MetradoDetalle m) throws SQLException;

    @Escritura
    void eliminar(long id) throws SQLException;

    List<MetradoDetalle> listarPorPartida(long partidaId) throws SQLException;
}
