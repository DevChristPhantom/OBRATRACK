package com.obratrack.service;

import com.obratrack.core.Modo;
import com.obratrack.core.RedEstado;

/**
 * Entrega la implementacion local o remota de cada servicio segun el modo de red
 * activo ({@link RedEstado#modo()}). La UI llama a estos metodos en vez de hacer
 * {@code new XxxService()} directamente, para que el mismo codigo funcione igual
 * en modo local/anfitriona (SQLite directo) y en modo cliente (RPC).
 */
public final class ServiceFactory {

    private ServiceFactory() {}

    private static boolean esCliente() {
        return RedEstado.modo() == Modo.CLIENTE;
    }

    public static IUsuarioService usuario() {
        return esCliente() ? new UsuarioServiceRemoto() : new UsuarioService();
    }

    public static IObraService obra() {
        return esCliente() ? new ObraServiceRemoto() : new ObraService();
    }

    public static IPartidaService partida() {
        return esCliente() ? new PartidaServiceRemoto() : new PartidaService();
    }

    public static IMovimientoService movimiento() {
        return esCliente() ? new MovimientoServiceRemoto() : new MovimientoService();
    }

    public static ICronogramaService cronograma() {
        return esCliente() ? new CronogramaServiceRemoto() : new CronogramaService();
    }

    public static ICuadernoService cuaderno() {
        return esCliente() ? new CuadernoServiceRemoto() : new CuadernoService();
    }

    public static ICumplimientoService cumplimiento() {
        return esCliente() ? new CumplimientoServiceRemoto() : new CumplimientoService();
    }

    public static IValorizacionService valorizacion() {
        return esCliente() ? new ValorizacionServiceRemoto() : new ValorizacionService();
    }

    public static IMetradoService metrado() {
        return esCliente() ? new MetradoServiceRemoto() : new MetradoService();
    }

    public static IApuService apu() {
        return esCliente() ? new ApuServiceRemoto() : new ApuService();
    }

    public static IFormulaPolinomicaService formulaPolinomica() {
        return esCliente() ? new FormulaPolinomicaServiceRemoto() : new FormulaPolinomicaService();
    }

    public static IDocumentoService documento() {
        return esCliente() ? new DocumentoServiceRemoto() : new DocumentoService();
    }

    public static IReporteService reporteExcel() {
        return esCliente() ? new ReporteServiceRemoto() : new ReporteService();
    }

    public static IReportePdf reportePdf() {
        return esCliente() ? new ReportePdfRemoto() : new ReportePdf();
    }

    public static IAdicionalDeductivoService adicionalDeductivo() {
        return esCliente() ? new AdicionalDeductivoServiceRemoto() : new AdicionalDeductivoService();
    }
}
