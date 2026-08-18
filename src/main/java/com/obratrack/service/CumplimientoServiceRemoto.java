package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.ItemCumplimiento;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link ICumplimientoService}: cada metodo llama por RPC a la PC anfitriona. */
public class CumplimientoServiceRemoto implements ICumplimientoService {

    private static final String SERVICIO = "CumplimientoService";

    @Override
    public ItemCumplimiento crear(ItemCumplimiento i) throws SQLException {
        return rpc("crear", ItemCumplimiento.class, i);
    }

    @Override
    public void actualizar(ItemCumplimiento i) throws SQLException {
        rpc("actualizar", void.class, i);
    }

    @Override
    public void eliminar(long id) throws SQLException {
        rpc("eliminar", void.class, id);
    }

    @Override
    public List<ItemCumplimiento> listarPorObra(long obraId, ItemCumplimiento.Categoria categoria) throws SQLException {
        Type tipo = new TypeToken<List<ItemCumplimiento>>() { }.getType();
        return rpc("listarPorObra", tipo, obraId, categoria);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
