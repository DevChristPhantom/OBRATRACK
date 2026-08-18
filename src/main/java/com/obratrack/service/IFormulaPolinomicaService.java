package com.obratrack.service;

import com.obratrack.model.MonomioPolinomico;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link FormulaPolinomicaService}: las mismas firmas que la implementacion
 * local, para poder sustituirla por {@link FormulaPolinomicaServiceRemoto} (RPC) cuando
 * esta PC es cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IFormulaPolinomicaService {

    @Escritura
    MonomioPolinomico crear(MonomioPolinomico m) throws SQLException;

    @Escritura
    void actualizar(MonomioPolinomico m) throws SQLException;

    @Escritura
    void eliminar(long id) throws SQLException;

    List<MonomioPolinomico> listarPorObra(long obraId) throws SQLException;
}
