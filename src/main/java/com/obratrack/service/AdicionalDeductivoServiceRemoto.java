package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.AdicionalDeductivo;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

/** Implementacion remota de {@link IAdicionalDeductivoService}: cada metodo llama por RPC a la PC anfitriona. */
public class AdicionalDeductivoServiceRemoto implements IAdicionalDeductivoService {

    private static final String SERVICIO = "AdicionalDeductivoService";

    @Override
    public AdicionalDeductivo crear(AdicionalDeductivo ad) throws SQLException {
        return rpc("crear", AdicionalDeductivo.class, ad);
    }

    @Override
    public void eliminar(long id) throws SQLException {
        rpc("eliminar", void.class, id);
    }

    @Override
    public List<AdicionalDeductivo> listarPorObra(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<AdicionalDeductivo>>() { }.getType();
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
