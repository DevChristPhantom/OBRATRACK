package com.obratrack.service;

import com.obratrack.model.AsientoCuaderno;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link CuadernoService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link CuadernoServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface ICuadernoService {

    @Escritura
    AsientoCuaderno crear(AsientoCuaderno a) throws SQLException;

    List<AsientoCuaderno> listarPorObra(long obraId) throws SQLException;
}
