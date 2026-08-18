package com.obratrack.service;

import com.obratrack.model.Partida;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link PartidaService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link PartidaServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IPartidaService {

    @Escritura
    void guardarTodas(long obraId, List<Partida> partidas) throws SQLException;

    List<Partida> listarPorObra(long obraId) throws SQLException;

    List<Partida> listarEjecutablesPorObra(long obraId) throws SQLException;

    @Escritura
    void eliminarPorObra(long obraId) throws SQLException;
}
