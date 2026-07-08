package com.obratrack;

import com.obratrack.model.Partida;
import com.obratrack.service.ExcelImporter;
import com.obratrack.service.ImportResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del importador de Excel. Genera archivos .xlsx temporales en distintos
 * formatos (como pediria la skill: el Excel del usuario puede variar de estructura)
 * para verificar que la deteccion automatica funciona en cada caso.
 */
class ExcelImporterTest {

    private File archivoTemporal;

    @AfterEach
    void limpiar() {
        if (archivoTemporal != null && archivoTemporal.exists()) {
            archivoTemporal.delete();
        }
    }

    @Test
    void importaPresupuestoSimpleConHeaderEnPrimeraFila() throws IOException {
        archivoTemporal = crearExcel("presupuesto_simple.xlsx", workbook -> {
            Sheet hoja = workbook.createSheet("Presupuesto");
            escribirFila(hoja, 0, "Item", "Descripcion", "Unidad", "Metrado", "Precio", "Total");
            escribirFila(hoja, 1, "01", "Limpieza de terreno", "m2", "150", "5.00", "750.00");
            escribirFila(hoja, 2, "02", "Trazo y replanteo", "m2", "150", "11.67", "1750.50");
        });

        ImportResult resultado = new ExcelImporter().importar(archivoTemporal.getAbsolutePath());

        assertTrue(resultado.isExitoso());
        assertEquals(2, resultado.getPartidasImportadas().size());
        assertEquals(2500.50, resultado.getPresupuestoTotal(), 0.01);
    }

    @Test
    void detectaHeaderEnFilaDistintaDeLaPrimera() throws IOException {
        // Simula un Excel con metadata arriba (nombre de obra, cliente, etc.) antes del header real,
        // igual al caso real del presupuesto S10/Crystal Reports.
        archivoTemporal = crearExcel("con_metadata.xlsx", workbook -> {
            Sheet hoja = workbook.createSheet("Presupuesto");
            escribirFila(hoja, 0, "PRESUPUESTO CONSOLIDADO");
            escribirFila(hoja, 1, "Cliente:", "MUNICIPALIDAD X");
            escribirFila(hoja, 2, "Lugar:", "LIMA");
            escribirFila(hoja, 3, "Item", "Descripcion", "Und.", "Metrado", "Precio S/", "Parcial S/");
            escribirFila(hoja, 4, "01", "OBRAS PRELIMINARES"); // partida padre, sin unidad
            escribirFila(hoja, 5, "01.01", "Cerco provisional", "m", "200", "17.77", "3554.00");
        });

        ImportResult resultado = new ExcelImporter().importar(archivoTemporal.getAbsolutePath());

        assertTrue(resultado.isExitoso());
        assertEquals(2, resultado.getPartidasImportadas().size());

        Partida padre = resultado.getPartidasImportadas().get(0);
        assertTrue(padre.isEsPadre(), "La partida '01 OBRAS PRELIMINARES' deberia detectarse como padre (sin unidad)");

        Partida hija = resultado.getPartidasImportadas().get(1);
        assertFalse(hija.isEsPadre());
        assertEquals("m", hija.getUnidad());
        assertEquals(3554.00, hija.getCostoTotalPresupuestado(), 0.01);
    }

    @Test
    void omiteFilasDeSubtotalYVacias() throws IOException {
        archivoTemporal = crearExcel("con_subtotales.xlsx", workbook -> {
            Sheet hoja = workbook.createSheet("Presupuesto");
            escribirFila(hoja, 0, "Item", "Descripcion", "Unidad", "Metrado", "Precio", "Total");
            escribirFila(hoja, 1, "01.01", "Excavacion", "m3", "10", "25.00", "250.00");
            escribirFila(hoja, 2); // fila vacia
            escribirFila(hoja, 3, "", "TOTAL", "", "", "", "250.00"); // subtotal, sin unidad -> se omite
        });

        ImportResult resultado = new ExcelImporter().importar(archivoTemporal.getAbsolutePath());

        assertTrue(resultado.isExitoso());
        assertEquals(1, resultado.getPartidasImportadas().size());
        assertTrue(resultado.getFilasOmitidas() >= 1);
    }

    @Test
    void fallaConMensajeClaroSiNoHayColumnaDescripcion() throws IOException {
        archivoTemporal = crearExcel("sin_estructura.xlsx", workbook -> {
            Sheet hoja = workbook.createSheet("Hoja1");
            escribirFila(hoja, 0, "A", "B", "C");
            escribirFila(hoja, 1, "1", "2", "3");
        });

        ImportResult resultado = new ExcelImporter().importar(archivoTemporal.getAbsolutePath());

        assertFalse(resultado.isExitoso());
        assertFalse(resultado.getErrores().isEmpty());
    }

    @Test
    void manejaPreciosConSimboloSoles() throws IOException {
        archivoTemporal = crearExcel("con_simbolo_soles.xlsx", workbook -> {
            Sheet hoja = workbook.createSheet("Presupuesto");
            escribirFila(hoja, 0, "Item", "Descripcion", "Unidad", "Metrado", "Precio", "Total");
            escribirFila(hoja, 1, "01.01", "Concreto f'c=210", "m3", "20", "S/. 350.00", "S/. 7,000.00");
        });

        ImportResult resultado = new ExcelImporter().importar(archivoTemporal.getAbsolutePath());

        assertTrue(resultado.isExitoso());
        Partida p = resultado.getPartidasImportadas().get(0);
        assertEquals(350.00, p.getCostoUnitario(), 0.01);
        assertEquals(7000.00, p.getCostoTotalPresupuestado(), 0.01);
    }

    @Test
    void manejaExcelCompletamenteVacioSinCrash() throws IOException {
        archivoTemporal = crearExcel("vacio.xlsx", workbook -> workbook.createSheet("Hoja1"));

        ImportResult resultado = new ExcelImporter().importar(archivoTemporal.getAbsolutePath());

        assertFalse(resultado.isExitoso());
        assertFalse(resultado.getErrores().isEmpty());
    }

    @Test
    void listaVariasHojasYPermiteImportarLaElegida() throws IOException {
        // Reproduce el caso real del consolidado: varias hojas de presupuesto con
        // el mismo numero de partidas pero totales distintos (PPTO, PPTO 95%, ...).
        archivoTemporal = crearExcel("multi_hoja.xlsx", workbook -> {
            Sheet a = workbook.createSheet("Sheet1");
            escribirFila(a, 0, "Item", "Descripcion", "Unidad", "Metrado", "Precio S/", "Parcial S/");
            escribirFila(a, 1, "01.01", "Concreto", "m3", "10", "100.00", "1000.00");

            Sheet b = workbook.createSheet("PPTO 95%");
            escribirFila(b, 0, "Item", "Descripcion", "Unidad", "Metrado", "Precio S/", "Parcial S/");
            escribirFila(b, 1, "01.01", "Concreto", "m3", "10", "95.00", "950.00");
        });

        ExcelImporter importer = new ExcelImporter();
        List<ExcelImporter.HojaImportable> hojas =
                importer.listarHojasImportables(archivoTemporal.getAbsolutePath());
        assertEquals(2, hojas.size());

        // Por defecto (sin elegir hoja) importa la primera hoja valida: Sheet1 = 1000.
        ImportResult porDefecto = importer.importar(archivoTemporal.getAbsolutePath());
        assertEquals(1000.00, porDefecto.getPresupuestoTotal(), 0.01);

        // Elegir explicitamente la hoja 'PPTO 95%' importa su total = 950.
        ImportResult elegida = importer.importar(archivoTemporal.getAbsolutePath(), "PPTO 95%");
        assertTrue(elegida.isExitoso());
        assertEquals(950.00, elegida.getPresupuestoTotal(), 0.01);
    }

    @Test
    void hojaInexistenteDevuelveError() throws IOException {
        archivoTemporal = crearExcel("una_hoja.xlsx", workbook -> {
            Sheet a = workbook.createSheet("Presupuesto");
            escribirFila(a, 0, "Item", "Descripcion", "Unidad", "Metrado", "Precio", "Total");
            escribirFila(a, 1, "01.01", "Concreto", "m3", "10", "100.00", "1000.00");
        });

        ImportResult r = new ExcelImporter().importar(archivoTemporal.getAbsolutePath(), "NO_EXISTE");
        assertFalse(r.isExitoso());
        assertFalse(r.getErrores().isEmpty());
    }

    // ---------- helpers ----------

    private interface ConstructorHoja {
        void construir(Workbook workbook);
    }

    private File crearExcel(String nombre, ConstructorHoja constructor) throws IOException {
        File archivo = File.createTempFile("obratrack_test_", "_" + nombre);
        try (Workbook workbook = new XSSFWorkbook()) {
            constructor.construir(workbook);
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                workbook.write(fos);
            }
        }
        return archivo;
    }

    private void escribirFila(Sheet hoja, int filaIndex, String... valores) {
        Row fila = hoja.createRow(filaIndex);
        for (int i = 0; i < valores.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(valores[i]);
        }
    }
}
