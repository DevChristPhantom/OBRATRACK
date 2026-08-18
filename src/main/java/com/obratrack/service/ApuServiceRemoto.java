package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.ApuInsumo;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link IApuService}: cada metodo llama por RPC a la PC anfitriona. */
public class ApuServiceRemoto implements IApuService {

    private static final String SERVICIO = "ApuService";

    @Override
    public ApuInsumo crear(ApuInsumo i) throws SQLException {
        return rpc("crear", ApuInsumo.class, i);
    }

    @Override
    public void actualizar(ApuInsumo i) throws SQLException {
        rpc("actualizar", void.class, i);
    }

    @Override
    public void eliminar(long id) throws SQLException {
        rpc("eliminar", void.class, id);
    }

    @Override
    public List<ApuInsumo> listarPorPartida(long partidaId) throws SQLException {
        Type tipo = new TypeToken<List<ApuInsumo>>() { }.getType();
        return rpc("listarPorPartida", tipo, partidaId);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
