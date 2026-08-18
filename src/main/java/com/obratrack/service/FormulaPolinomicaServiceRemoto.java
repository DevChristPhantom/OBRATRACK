package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.MonomioPolinomico;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link IFormulaPolinomicaService}: cada metodo llama por RPC a la PC anfitriona. */
public class FormulaPolinomicaServiceRemoto implements IFormulaPolinomicaService {

    private static final String SERVICIO = "FormulaPolinomicaService";

    @Override
    public MonomioPolinomico crear(MonomioPolinomico m) throws SQLException {
        return rpc("crear", MonomioPolinomico.class, m);
    }

    @Override
    public void actualizar(MonomioPolinomico m) throws SQLException {
        rpc("actualizar", void.class, m);
    }

    @Override
    public void eliminar(long id) throws SQLException {
        rpc("eliminar", void.class, id);
    }

    @Override
    public List<MonomioPolinomico> listarPorObra(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<MonomioPolinomico>>() { }.getType();
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
