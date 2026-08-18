package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.AsientoCuaderno;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link ICuadernoService}: cada metodo llama por RPC a la PC anfitriona. */
public class CuadernoServiceRemoto implements ICuadernoService {

    private static final String SERVICIO = "CuadernoService";

    @Override
    public AsientoCuaderno crear(AsientoCuaderno a) throws SQLException {
        return rpc("crear", AsientoCuaderno.class, a);
    }

    @Override
    public List<AsientoCuaderno> listarPorObra(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<AsientoCuaderno>>() { }.getType();
        return rpc("listarPorObra", tipo, obraId);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
