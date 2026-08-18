package com.obratrack.service;

import com.obratrack.core.Rutas;
import com.obratrack.model.Obra;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Implementacion remota de {@link IReportePdf}: pide al host que genere el reporte y
 * descarga el PDF resultante a una copia local en {@link Rutas#cache()}, que se
 * sobreescribe en cada exportacion (es una copia de trabajo, no un historial).
 */
public class ReportePdfRemoto implements IReportePdf {

    private static final String SERVICIO = "ReportePdf";

    @Override
    public Path exportarComparativoPdf(Obra obra) throws Exception {
        return descargar("exportarComparativoPdf", nombreCache(obra, "comparativo"), obra);
    }

    @Override
    public Path exportarMovimientosPdf(Obra obra, LocalDate fecha) throws Exception {
        String tipo = fecha != null ? "diario_" + fecha : "acumulado";
        return descargar("exportarMovimientosPdf", nombreCache(obra, tipo), obra, fecha);
    }

    @Override
    public Path exportarComparativoPeriodicoPdf(Obra obra, Granularidad granularidad) throws Exception {
        String tipo = "periodico_" + granularidad.name().toLowerCase();
        return descargar("exportarComparativoPeriodicoPdf", nombreCache(obra, tipo), obra, granularidad);
    }

    @Override
    public Path exportarResumenEjecutivoPdf(Obra obra) throws Exception {
        return descargar("exportarResumenEjecutivoPdf", nombreCache(obra, "resumen_ejecutivo"), obra);
    }

    private Path descargar(String metodo, String nombreArchivo, Object... args) throws IOException {
        Path destino = Rutas.cache().resolve(nombreArchivo);
        return RpcCliente.descargarArchivo(SERVICIO, metodo, destino, args);
    }

    private String nombreCache(Obra obra, String tipo) {
        String base = obra.getId() != null ? "obra" + obra.getId() : "obra";
        return "reporte_" + tipo + "_" + base + ".pdf";
    }
}
