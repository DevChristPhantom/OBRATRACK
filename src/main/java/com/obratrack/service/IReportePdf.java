package com.obratrack.service;

import com.obratrack.model.Obra;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Contrato de {@link ReportePdf}: las mismas firmas que la implementacion local, para
 * poder sustituirla por {@link ReportePdfRemoto} cuando esta PC es cliente en la red
 * de la obra. Ver {@link IReporteService} para la explicacion de por que estos metodos
 * no viajan por el RPC generico.
 */
public interface IReportePdf {

    Path exportarComparativoPdf(Obra obra) throws Exception;

    Path exportarMovimientosPdf(Obra obra, LocalDate fecha) throws Exception;

    Path exportarComparativoPeriodicoPdf(Obra obra, Granularidad granularidad) throws Exception;

    /** Ficha ejecutiva: consolida salud, avance economico, fisico, valorizaciones y cumplimiento. */
    Path exportarResumenEjecutivoPdf(Obra obra) throws Exception;
}
