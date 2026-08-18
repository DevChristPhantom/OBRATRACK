package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.MetradoDetalle;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link IMetradoService}: cada metodo llama por RPC a la PC anfitriona. */
public class MetradoServiceRemoto implements IMetradoService {

    private static final String SERVICIO = "MetradoService";

    @Override
    public MetradoDetalle crear(MetradoDetalle m) throws SQLException {
        return rpc("crear", MetradoDetalle.class, m);
    }

    @Override
    public void actualizar(MetradoDetalle m) throws SQLException {
        rpc("actualizar", void.class, m);
    }

    @Override
    public void eliminar(long id) throws SQLException {
        rpc("eliminar", void.class, id);
    }

    @Override
    public List<MetradoDetalle> listarPorPartida(long partidaId) throws SQLException {
        Type tipo = new TypeToken<List<MetradoDetalle>>() { }.getType();
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
