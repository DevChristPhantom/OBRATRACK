package com.obratrack;

import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.MovimientoService;
import com.obratrack.service.ObraService;
import com.obratrack.service.PartidaService;
import com.obratrack.service.ReportePdf;
import com.obratrack.service.ReporteService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de punta a punta la ficha ejecutiva (Excel y PDF): genera un archivo real
 * en disco a partir de una obra con datos reales en la base y verifica que el
 * archivo generado tenga la firma binaria correcta de su formato (no solo que el
 * codigo no lance excepcion).
 */
class ReporteResumenEjecutivoTest {

    private final ObraService obraService = new ObraService();
    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();
    private final ReporteService reporteExcel = new ReporteService();
    private final ReportePdf reportePdf = new ReportePdf();
    private final long nano = System.nanoTime();
    private Long obraId;

    @Test
    void generaExcelConFirmaZipYPdfConFirmaPdf() throws Exception {
        Obra o = new Obra("OBRA_RESUMEN_" + nano, "", LocalDate.now().minusDays(30), LocalDate.now().plusDays(60));
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();

        partidaService.guardarTodas(obraId, List.of(new Partida("01.01", "Concreto", "m3", 10, 100)));
        Partida guardada = partidaService.listarPorObra(obraId).get(0);

        MovimientoAlmacen m = new MovimientoAlmacen();
        m.setObraId(obraId);
        m.setPartidaId(guardada.getId());
        m.setFecha(LocalDate.now());
        m.setTipo(MovimientoAlmacen.Tipo.EGRESO);
        m.setCantidad(3);
        m.setCostoUnitarioReal(100);
        movimientoService.registrar(m);

        Path excel = reporteExcel.exportarResumenEjecutivoExcel(o);
        assertTrue(Files.isRegularFile(excel), "el Excel de la ficha ejecutiva debe generarse en disco");
        byte[] firmaExcel = Files.readAllBytes(excel);
        assertTrue(firmaExcel.length > 2 && firmaExcel[0] == 'P' && firmaExcel[1] == 'K',
                "un .xlsx es un ZIP: debe empezar con la firma PK");

        Path pdf = reportePdf.exportarResumenEjecutivoPdf(o);
        assertTrue(Files.isRegularFile(pdf), "el PDF de la ficha ejecutiva debe generarse en disco");
        byte[] firmaPdf = Files.readAllBytes(pdf);
        assertTrue(firmaPdf.length > 4 && new String(firmaPdf, 0, 4).equals("%PDF"),
                "un PDF debe empezar con la firma %PDF");

        Files.deleteIfExists(excel);
        Files.deleteIfExists(pdf);
    }

    /**
     * Regresion detectada probando la app en vivo: un campo multilinea (sectores/bloques)
     * perdia el salto de linea al limpiarse para el PDF y las palabras quedaban pegadas
     * ("...administrativoSector B..."). Verifica que ahora quede un espacio.
     */
    @Test
    void elSaltoDeLineaEnSectoresBloquesNoPegaLasPalabrasEnElPdf() throws Exception {
        Obra o = new Obra("OBRA_SECTORES_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        o.setSectoresBloques("Sector A: Pabellon administrativo\nSector B: Aulas");
        obraService.crear(o);
        obraId = o.getId();

        Path pdf = reportePdf.exportarResumenEjecutivoPdf(o);
        String texto;
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            texto = new PDFTextStripper().getText(doc);
        }
        Files.deleteIfExists(pdf);

        assertFalse(texto.contains("administrativoSector"),
                "las palabras de dos lineas distintas no deben quedar pegadas");
        assertTrue(texto.contains("Sector A: Pabellon administrativo Sector B: Aulas"));
    }

    @AfterEach
    void limpiar() throws Exception {
        if (obraId != null) {
            obraService.eliminar(obraId);
        }
    }
}
