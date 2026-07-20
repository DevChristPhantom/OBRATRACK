package com.obratrack.service;

import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.model.ResumenPeriodo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Genera reportes de obra en Excel (.xlsx) y PDF.
 * Tres tipos:
 *   - Reporte diario: movimientos de una fecha concreta.
 *   - Reporte acumulado: todos los movimientos hasta hoy.
 *   - Comparativo presupuesto vs ejecutado: por partida.
 */
public class ReporteService {

    private static final DateTimeFormatter FMT_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();

    // ============================================================
    //  EXCEL
    // ============================================================

    /** Reporte comparativo presupuesto vs ejecutado por partida (Excel). */
    public Path exportarComparativoExcel(Obra obra) throws Exception {
        List<Partida> partidas = partidaService.listarPorObra(obra.getId());
        Map<Long, Double> ejecutadoPorPartida = movimientoService.totalEjecutadoPorPartida(obra.getId());

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Comparativo");
            EstilosExcel estilos = new EstilosExcel(wb);

            int fila = 0;
            fila = escribirCabeceraObra(hoja, estilos, obra, "REPORTE COMPARATIVO: PRESUPUESTO VS EJECUTADO", 7, fila);

            Row header = hoja.createRow(fila++);
            String[] cols = {"Codigo", "Descripcion", "Unidad", "Presupuestado S/.", "Ejecutado S/.", "Diferencia S/.", "% Avance"};
            for (int c = 0; c < cols.length; c++) {
                Cell celda = header.createCell(c);
                celda.setCellValue(cols[c]);
                celda.setCellStyle(estilos.header);
            }

            double totalPresup = 0, totalEjec = 0;
            for (Partida p : partidas) {
                Row r = hoja.createRow(fila++);
                double presup = p.isEsPadre() ? 0 : p.getCostoTotalPresupuestado();
                double ejec = p.isEsPadre() ? 0 : ejecutadoPorPartida.getOrDefault(p.getId(), 0.0);
                double dif = presup - ejec;
                double pct = presup > 0 ? (ejec / presup) * 100 : 0;

                crearCeldaTexto(r, 0, p.getCodigo() != null ? p.getCodigo() : "", estilos.textoBase(p.isEsPadre()));
                crearCeldaTexto(r, 1, p.getDescripcion(), estilos.textoBase(p.isEsPadre()));
                crearCeldaTexto(r, 2, p.getUnidad() != null ? p.getUnidad() : "", estilos.textoBase(p.isEsPadre()));

                if (!p.isEsPadre()) {
                    crearCeldaNumero(r, 3, presup, estilos.moneda);
                    crearCeldaNumero(r, 4, ejec, estilos.moneda);
                    crearCeldaNumero(r, 5, dif, dif < 0 ? estilos.monedaRoja : estilos.moneda);
                    crearCeldaNumero(r, 6, pct / 100.0, estilos.porcentajePorAvance(pct));
                    totalPresup += presup;
                    totalEjec += ejec;
                }
            }

            // Fila de totales
            Row totalRow = hoja.createRow(fila++);
            crearCeldaTexto(totalRow, 0, "TOTALES", estilos.totalTexto);
            for (int c = 1; c <= 2; c++) crearCeldaTexto(totalRow, c, "", estilos.totalTexto);
            crearCeldaNumero(totalRow, 3, totalPresup, estilos.totalMoneda);
            crearCeldaNumero(totalRow, 4, totalEjec, estilos.totalMoneda);
            crearCeldaNumero(totalRow, 5, totalPresup - totalEjec, estilos.totalMoneda);
            double pctTotal = totalPresup > 0 ? (totalEjec / totalPresup) : 0;
            crearCeldaNumero(totalRow, 6, pctTotal, estilos.totalPorcentaje);

            for (int c = 0; c < cols.length; c++) hoja.autoSizeColumn(c);

            return guardarWorkbook(wb, "comparativo", obra);
        }
    }

    /** Reporte de movimientos (diario si se pasa una fecha, acumulado si fecha == null). */
    public Path exportarMovimientosExcel(Obra obra, LocalDate fecha) throws Exception {
        List<MovimientoAlmacen> movimientos = (fecha != null)
                ? movimientoService.listarPorObraYFecha(obra.getId(), fecha)
                : movimientoService.listarPorObra(obra.getId());

        try (Workbook wb = new XSSFWorkbook()) {
            String titulo = (fecha != null)
                    ? "REPORTE DIARIO DE ALMACEN - " + fecha
                    : "REPORTE ACUMULADO DE ALMACEN";
            Sheet hoja = wb.createSheet(fecha != null ? "Diario" : "Acumulado");
            EstilosExcel estilos = new EstilosExcel(wb);

            int fila = 0;
            fila = escribirCabeceraObra(hoja, estilos, obra, titulo, 6, fila);

            Row header = hoja.createRow(fila++);
            String[] cols = {"Fecha", "Partida", "Tipo", "Cantidad", "Costo Unit. S/.", "Total S/."};
            for (int c = 0; c < cols.length; c++) {
                Cell celda = header.createCell(c);
                celda.setCellValue(cols[c]);
                celda.setCellStyle(estilos.header);
            }

            double total = 0;
            for (MovimientoAlmacen m : movimientos) {
                Row r = hoja.createRow(fila++);
                crearCeldaTexto(r, 0, m.getFecha().toString(), estilos.textoBase(false));
                crearCeldaTexto(r, 1, m.getPartidaCodigo() + " - " + m.getPartidaDescripcion(), estilos.textoBase(false));
                crearCeldaTexto(r, 2, m.getTipo().name(), estilos.textoBase(false));
                crearCeldaNumero(r, 3, m.getCantidad(), estilos.numero);
                crearCeldaNumero(r, 4, m.getCostoUnitarioReal(), estilos.moneda);
                crearCeldaNumero(r, 5, m.getCostoTotalReal(), estilos.moneda);
                total += (m.getTipo() == MovimientoAlmacen.Tipo.EGRESO ? m.getCostoTotalReal() : -m.getCostoTotalReal());
            }

            Row totalRow = hoja.createRow(fila++);
            crearCeldaTexto(totalRow, 0, "TOTAL EJECUTADO", estilos.totalTexto);
            for (int c = 1; c <= 4; c++) crearCeldaTexto(totalRow, c, "", estilos.totalTexto);
            crearCeldaNumero(totalRow, 5, total, estilos.totalMoneda);

            for (int c = 0; c < cols.length; c++) hoja.autoSizeColumn(c);

            return guardarWorkbook(wb, fecha != null ? "diario" : "acumulado", obra);
        }
    }

    /**
     * Comparativo temporal de ejecucion (diario / semanal / mensual) a lo largo
     * de la obra: cada fila es un periodo con egresos, ingresos, neto, acumulado
     * y % del presupuesto consumido (coloreado verde/amarillo/rojo).
     */
    public Path exportarComparativoPeriodicoExcel(Obra obra, Granularidad granularidad) throws Exception {
        List<ResumenPeriodo> periodos = movimientoService.resumenPorPeriodo(obra.getId(), granularidad);
        double presupuesto = obra.getPresupuestoTotal();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Comparativo " + granularidad.getEtiqueta());
            EstilosExcel estilos = new EstilosExcel(wb);

            int fila = 0;
            fila = escribirCabeceraObra(hoja, estilos, obra,
                    "COMPARATIVO " + granularidad.getEtiqueta().toUpperCase() + " DE EJECUCION", 9, fila);

            Row header = hoja.createRow(fila++);
            String[] cols = {"Periodo", "Desde", "Hasta", "N° Mov.", "Egresos S/.",
                    "Ingresos S/.", "Neto S/.", "Acumulado S/.", "% Presup."};
            for (int c = 0; c < cols.length; c++) {
                Cell celda = header.createCell(c);
                celda.setCellValue(cols[c]);
                celda.setCellStyle(estilos.header);
            }

            double totalEgresos = 0, totalIngresos = 0;
            for (ResumenPeriodo p : periodos) {
                Row r = hoja.createRow(fila++);
                double pctAcum = presupuesto > 0 ? (p.getAcumulado() / presupuesto) * 100 : 0;
                crearCeldaTexto(r, 0, p.getEtiqueta(), estilos.textoBase(false));
                crearCeldaTexto(r, 1, p.getInicio().toString(), estilos.textoBase(false));
                crearCeldaTexto(r, 2, p.getFin().toString(), estilos.textoBase(false));
                crearCeldaNumero(r, 3, p.getNumMovimientos(), estilos.numero);
                crearCeldaNumero(r, 4, p.getEgresos(), estilos.moneda);
                crearCeldaNumero(r, 5, p.getIngresos(), estilos.moneda);
                crearCeldaNumero(r, 6, p.getNeto(), estilos.moneda);
                crearCeldaNumero(r, 7, p.getAcumulado(), estilos.moneda);
                crearCeldaNumero(r, 8, pctAcum / 100.0, estilos.porcentajePorAvance(pctAcum));
                totalEgresos += p.getEgresos();
                totalIngresos += p.getIngresos();
            }

            Row totalRow = hoja.createRow(fila++);
            crearCeldaTexto(totalRow, 0, "TOTALES", estilos.totalTexto);
            for (int c = 1; c <= 3; c++) crearCeldaTexto(totalRow, c, "", estilos.totalTexto);
            crearCeldaNumero(totalRow, 4, totalEgresos, estilos.totalMoneda);
            crearCeldaNumero(totalRow, 5, totalIngresos, estilos.totalMoneda);
            double netoTotal = totalEgresos - totalIngresos;
            crearCeldaNumero(totalRow, 6, netoTotal, estilos.totalMoneda);
            crearCeldaNumero(totalRow, 7, netoTotal, estilos.totalMoneda);
            double pctTotal = presupuesto > 0 ? (netoTotal / presupuesto) : 0;
            crearCeldaNumero(totalRow, 8, pctTotal, estilos.totalPorcentaje);

            // Linea de referencia del presupuesto.
            fila++;
            Row refRow = hoja.createRow(fila++);
            crearCeldaTexto(refRow, 0, "Presupuesto total de la obra S/.", estilos.totalTexto);
            crearCeldaNumero(refRow, 1, presupuesto, estilos.totalMoneda);

            for (int c = 0; c < cols.length; c++) hoja.autoSizeColumn(c);

            return guardarWorkbook(wb, "periodico_" + granularidad.name().toLowerCase(), obra);
        }
    }

    private int escribirCabeceraObra(Sheet hoja, EstilosExcel estilos, Obra obra,
                                      String titulo, int anchoColumnas, int fila) {
        Row tituloRow = hoja.createRow(fila++);
        Cell tCell = tituloRow.createCell(0);
        tCell.setCellValue(titulo);
        tCell.setCellStyle(estilos.titulo);
        hoja.addMergedRegion(new CellRangeAddress(fila - 1, fila - 1, 0, anchoColumnas - 1));

        Row obraRow = hoja.createRow(fila++);
        Cell oCell = obraRow.createCell(0);
        oCell.setCellValue("Obra: " + obra.getNombre());
        oCell.setCellStyle(estilos.subtitulo);
        hoja.addMergedRegion(new CellRangeAddress(fila - 1, fila - 1, 0, anchoColumnas - 1));

        Row fechaRow = hoja.createRow(fila++);
        Cell fCell = fechaRow.createCell(0);
        fCell.setCellValue("Generado: " + LocalDate.now());
        fCell.setCellStyle(estilos.subtitulo);
        hoja.addMergedRegion(new CellRangeAddress(fila - 1, fila - 1, 0, anchoColumnas - 1));

        fila++; // fila en blanco
        return fila;
    }

    private Path guardarWorkbook(Workbook wb, String tipo, Obra obra) throws IOException {
        String nombre = String.format("ObraTrack_%s_%s_%s.xlsx",
                tipo, sanitizar(obra.getNombre()), LocalDate.now().atStartOfDay().format(FMT_ARCHIVO));
        Path destino = com.obratrack.core.Rutas.exports().resolve(nombre);
        try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
            wb.write(fos);
        }
        return destino;
    }

    private void crearCeldaTexto(Row fila, int col, String valor, CellStyle estilo) {
        Cell c = fila.createCell(col);
        c.setCellValue(celdaSegura(valor));
        if (estilo != null) c.setCellStyle(estilo);
    }

    /**
     * Neutraliza la inyeccion de formulas (CSV/Excel injection): si el texto empieza por
     * un caracter que Excel/LibreOffice interpretarian como formula, se le antepone un
     * apostrofo para que se trate como texto literal. Los datos provienen de entradas del
     * usuario o de Excel importados, asi que no son de confianza.
     */
    private String celdaSegura(String valor) {
        if (valor == null || valor.isEmpty()) return valor;
        char c0 = valor.charAt(0);
        if (c0 == '=' || c0 == '+' || c0 == '-' || c0 == '@' || c0 == '\t' || c0 == '\r') {
            return "'" + valor;
        }
        return valor;
    }

    private void crearCeldaNumero(Row fila, int col, double valor, CellStyle estilo) {
        Cell c = fila.createCell(col);
        c.setCellValue(valor);
        if (estilo != null) c.setCellStyle(estilo);
    }

    private String sanitizar(String texto) {
        if (texto == null) return "obra";
        return texto.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
    }
}
