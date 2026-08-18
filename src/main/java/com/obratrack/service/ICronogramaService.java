package com.obratrack.service;

import com.obratrack.model.Actividad;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link CronogramaService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link CronogramaServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface ICronogramaService {

    @Escritura
    Actividad crear(Actividad a) throws SQLException;

    @Escritura
    void actualizar(Actividad a) throws SQLException;

    @Escritura
    void eliminar(long id) throws SQLException;

    List<Actividad> listarPorObra(long obraId) throws SQLException;

    double sumaPesosPorObra(long obraId) throws SQLException;
}
