package com.obratrack.service;

import com.obratrack.model.ItemCumplimiento;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de {@link CumplimientoService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link CumplimientoServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface ICumplimientoService {

    @Escritura
    ItemCumplimiento crear(ItemCumplimiento i) throws SQLException;

    @Escritura
    void actualizar(ItemCumplimiento i) throws SQLException;

    @Escritura
    void eliminar(long id) throws SQLException;

    List<ItemCumplimiento> listarPorObra(long obraId, ItemCumplimiento.Categoria categoria) throws SQLException;
}
