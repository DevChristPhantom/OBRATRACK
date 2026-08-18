package com.obratrack.service;

import com.obratrack.core.Rutas;
import com.obratrack.model.Obra;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Implementacion remota de {@link IReporteService}: pide al host que genere el reporte
 * (reutilizando {@code PartidaService}/{@code MovimientoService} ya convertidos alla) y
 * descarga el Excel resultante a una copia local en {@link Rutas#cache()}, que se
 * sobreescribe en cada exportacion (es una copia de trabajo, no un historial).
 */
public class ReporteServiceRemoto implements IReporteService {

    private static final String SERVICIO = "ReporteService";

    @Override
    public Path exportarComparativoExcel(Obra obra) throws Exception {
        return descargar("exportarComparativoExcel", nombreCache(obra, "comparativo"), obra);
    }

    @Override
    public Path exportarMovimientosExcel(Obra obra, LocalDate fecha) throws Exception {
        String tipo = fecha != null ? "diario_" + fecha : "acumulado";
        return descargar("exportarMovimientosExcel", nombreCache(obra, tipo), obra, fecha);
    }

    @Override
    public Path exportarComparativoPeriodicoExcel(Obra obra, Granularidad granularidad) throws Exception {
        String tipo = "periodico_" + granularidad.name().toLowerCase();
        return descargar("exportarComparativoPeriodicoExcel", nombreCache(obra, tipo), obra, granularidad);
    }

    @Override
    public Path exportarResumenEjecutivoExcel(Obra obra) throws Exception {
        return descargar("exportarResumenEjecutivoExcel", nombreCache(obra, "resumen_ejecutivo"), obra);
    }

    private Path descargar(String metodo, String nombreArchivo, Object... args) throws IOException {
        Path destino = Rutas.cache().resolve(nombreArchivo);
        return RpcCliente.descargarArchivo(SERVICIO, metodo, destino, args);
    }

    private String nombreCache(Obra obra, String tipo) {
        String base = obra.getId() != null ? "obra" + obra.getId() : "obra";
        return "reporte_" + tipo + "_" + base + ".xlsx";
    }
}
