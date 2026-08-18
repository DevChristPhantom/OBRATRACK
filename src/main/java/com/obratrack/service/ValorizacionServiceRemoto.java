package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.Valorizacion;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Implementacion remota de {@link IValorizacionService}: cada metodo llama por RPC a la PC anfitriona. */
public class ValorizacionServiceRemoto implements IValorizacionService {

    private static final String SERVICIO = "ValorizacionService";

    @Override
    public Valorizacion generar(long obraId, LocalDate periodoDesde, LocalDate periodoHasta,
                                 double pctRetencion, double montoAmortizacionAdelanto,
                                 String observaciones) throws SQLException {
        return rpc("generar", Valorizacion.class, obraId, periodoDesde, periodoHasta,
                pctRetencion, montoAmortizacionAdelanto, observaciones);
    }

    @Override
    public List<Valorizacion> listarPorObra(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<Valorizacion>>() { }.getType();
        return rpc("listarPorObra", tipo, obraId);
    }

    @Override
    public void eliminar(long id) throws SQLException {
        rpc("eliminar", void.class, id);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
