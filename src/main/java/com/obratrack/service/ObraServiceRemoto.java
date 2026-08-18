package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.Obra;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link IObraService}: cada metodo llama por RPC a la PC anfitriona. */
public class ObraServiceRemoto implements IObraService {

    private static final String SERVICIO = "ObraService";

    @Override
    public Obra crear(Obra obra) throws SQLException {
        return rpc("crear", Obra.class, obra);
    }

    @Override
    public List<Obra> listarTodas() throws SQLException {
        Type tipo = new TypeToken<List<Obra>>() { }.getType();
        return rpc("listarTodas", tipo);
    }

    @Override
    public List<Obra> listarActivas() throws SQLException {
        Type tipo = new TypeToken<List<Obra>>() { }.getType();
        return rpc("listarActivas", tipo);
    }

    @Override
    public void actualizarPresupuestoTotal(long obraId, double total) throws SQLException {
        rpc("actualizarPresupuestoTotal", void.class, obraId, total);
    }

    @Override
    public void actualizarRutaExcel(long obraId, String ruta) throws SQLException {
        rpc("actualizarRutaExcel", void.class, obraId, ruta);
    }

    @Override
    public void cambiarEstado(long obraId, Obra.Estado estado) throws SQLException {
        rpc("cambiarEstado", void.class, obraId, estado);
    }

    @Override
    public void actualizar(Obra obra) throws SQLException {
        rpc("actualizar", void.class, obra);
    }

    @Override
    public void eliminar(long obraId) throws SQLException {
        rpc("eliminar", void.class, obraId);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
