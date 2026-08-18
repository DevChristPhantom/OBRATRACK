package com.obratrack.service;

import com.obratrack.model.AdicionalDeductivo;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link AdicionalDeductivoService}: las mismas firmas que la implementacion
 * local, para poder sustituirla por {@link AdicionalDeductivoServiceRemoto} (RPC) cuando
 * esta PC es cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IAdicionalDeductivoService {

    @Escritura
    AdicionalDeductivo crear(AdicionalDeductivo ad) throws SQLException;

    @Escritura
    void eliminar(long id) throws SQLException;

    List<AdicionalDeductivo> listarPorObra(long obraId) throws SQLException;
}
