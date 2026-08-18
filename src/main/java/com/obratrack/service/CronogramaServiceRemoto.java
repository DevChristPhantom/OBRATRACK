package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.Actividad;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link ICronogramaService}: cada metodo llama por RPC a la PC anfitriona. */
public class CronogramaServiceRemoto implements ICronogramaService {

    private static final String SERVICIO = "CronogramaService";

    @Override
    public Actividad crear(Actividad a) throws SQLException {
        return rpc("crear", Actividad.class, a);
    }

    @Override
    public void actualizar(Actividad a) throws SQLException {
        rpc("actualizar", void.class, a);
    }

    @Override
    public void eliminar(long id) throws SQLException {
        rpc("eliminar", void.class, id);
    }

    @Override
    public List<Actividad> listarPorObra(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<Actividad>>() { }.getType();
        return rpc("listarPorObra", tipo, obraId);
    }

    @Override
    public double sumaPesosPorObra(long obraId) throws SQLException {
        return rpc("sumaPesosPorObra", double.class, obraId);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
