package com.obratrack.service;

import com.obratrack.model.ApuInsumo;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link ApuService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link ApuServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IApuService {

    @Escritura
    ApuInsumo crear(ApuInsumo i) throws SQLException;

    @Escritura
    void actualizar(ApuInsumo i) throws SQLException;

    @Escritura
    void eliminar(long id) throws SQLException;

    List<ApuInsumo> listarPorPartida(long partidaId) throws SQLException;
}
