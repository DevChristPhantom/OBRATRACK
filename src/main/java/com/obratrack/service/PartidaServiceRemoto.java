package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.Partida;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link IPartidaService}: cada metodo llama por RPC a la PC anfitriona. */
public class PartidaServiceRemoto implements IPartidaService {

    private static final String SERVICIO = "PartidaService";

    @Override
    public void guardarTodas(long obraId, List<Partida> partidas) throws SQLException {
        rpc("guardarTodas", void.class, obraId, partidas);
    }

    @Override
    public List<Partida> listarPorObra(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<Partida>>() { }.getType();
        return rpc("listarPorObra", tipo, obraId);
    }

    @Override
    public List<Partida> listarEjecutablesPorObra(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<Partida>>() { }.getType();
        return rpc("listarEjecutablesPorObra", tipo, obraId);
    }

    @Override
    public void eliminarPorObra(long obraId) throws SQLException {
        rpc("eliminarPorObra", void.class, obraId);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
