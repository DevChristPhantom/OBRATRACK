package com.obratrack.service;

import com.obratrack.model.Obra;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Contrato de {@link ReporteService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link ReporteServiceRemoto} cuando esta PC es cliente en
 * la red de la obra. Los tres metodos devuelven {@code Path} a un archivo recien
 * generado; por eso no viajan por el RPC generico, sino por el endpoint de descarga
 * de archivos de {@code ServidorHttp}/{@code RpcCliente} (ver {@link IDocumentoService}).
 * No se marcan {@code @Escritura}: generan un archivo nuevo pero no modifican la base
 * de datos, y cualquier rol (incluido el de solo lectura) puede exportar lo que ya ve.
 */
public interface IReporteService {

    Path exportarComparativoExcel(Obra obra) throws Exception;

    Path exportarMovimientosExcel(Obra obra, LocalDate fecha) throws Exception;

    Path exportarComparativoPeriodicoExcel(Obra obra, Granularidad granularidad) throws Exception;

    /** Ficha ejecutiva: consolida salud, avance economico, fisico, valorizaciones y cumplimiento. */
    Path exportarResumenEjecutivoExcel(Obra obra) throws Exception;
}
