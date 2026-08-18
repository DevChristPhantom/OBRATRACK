package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.MovimientoAuditoria;
import com.obratrack.model.ResumenPeriodo;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Implementacion remota de {@link IMovimientoService}: cada metodo llama por RPC a la PC anfitriona. */
public class MovimientoServiceRemoto implements IMovimientoService {

    private static final String SERVICIO = "MovimientoService";

    @Override
    public MovimientoAlmacen registrar(MovimientoAlmacen mov) throws SQLException {
        return rpc("registrar", MovimientoAlmacen.class, mov);
    }

    @Override
    public void actualizar(MovimientoAlmacen mov) throws SQLException {
        rpc("actualizar", void.class, mov);
    }

    @Override
    public void eliminar(long movimientoId) throws SQLException {
        rpc("eliminar", void.class, movimientoId);
    }

    @Override
    public MovimientoAlmacen obtenerPorId(long id) throws SQLException {
        return rpc("obtenerPorId", MovimientoAlmacen.class, id);
    }

    @Override
    public List<MovimientoAlmacen> listarPorObra(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<MovimientoAlmacen>>() { }.getType();
        return rpc("listarPorObra", tipo, obraId);
    }

    @Override
    public List<MovimientoAlmacen> listarPorObraYFecha(long obraId, LocalDate fecha) throws SQLException {
        Type tipo = new TypeToken<List<MovimientoAlmacen>>() { }.getType();
        return rpc("listarPorObraYFecha", tipo, obraId, fecha);
    }

    @Override
    public Map<Long, Double> totalEjecutadoPorPartida(long obraId) throws SQLException {
        Type tipo = new TypeToken<Map<Long, Double>>() { }.getType();
        return rpc("totalEjecutadoPorPartida", tipo, obraId);
    }

    @Override
    public double totalEjecutadoObra(long obraId) throws SQLException {
        return rpc("totalEjecutadoObra", double.class, obraId);
    }

    @Override
    public double totalEjecutadoEntreFechas(long obraId, LocalDate desde, LocalDate hasta) throws SQLException {
        return rpc("totalEjecutadoEntreFechas", double.class, obraId, desde, hasta);
    }

    @Override
    public List<ResumenPeriodo> resumenPorPeriodo(long obraId, Granularidad granularidad) throws SQLException {
        Type tipo = new TypeToken<List<ResumenPeriodo>>() { }.getType();
        return rpc("resumenPorPeriodo", tipo, obraId, granularidad);
    }

    @Override
    public List<MovimientoAuditoria> listarAuditoria(long obraId) throws SQLException {
        Type tipo = new TypeToken<List<MovimientoAuditoria>>() { }.getType();
        return rpc("listarAuditoria", tipo, obraId);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
