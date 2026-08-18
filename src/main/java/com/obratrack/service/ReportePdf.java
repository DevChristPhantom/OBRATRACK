package com.obratrack.service;

import com.obratrack.model.Actividad;
import com.obratrack.model.ItemCumplimiento;
import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.model.ResumenPeriodo;
import com.obratrack.model.Valorizacion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera reportes en PDF usando PDFBox (Apache 2.0).
 * Dibuja tablas con encabezado repetido y salto de pagina automatico.
 */
public class ReportePdf implements IReportePdf {

    private static final String CARPETA_EXPORTS = "exports";
    private static final DateTimeFormatter FMT_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final float MARGEN = 40f;
    private static final float ALTO_FILA = 18f;
    private static final float TAM_FUENTE = 9f;
    private static final PDFont FUENTE = PDType1Font.HELVETICA;
    private static final PDFont FUENTE_BOLD = PDType1Font.HELVETICA_BOLD;

    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();
    private final CronogramaService cronogramaService = new CronogramaService();
    private final ValorizacionService valorizacionService = new ValorizacionService();
    private final CumplimientoService cumplimientoService = new CumplimientoService();

    /** Comparativo presupuesto vs ejecutado (PDF, horizontal). */
    public Path exportarComparativoPdf(Obra obra) throws Exception {
        List<Partida> partidas = partidaService.listarPorObra(obra.getId());
        Map<Long, Double> ejecutado = movimientoService.totalEjecutadoPorPartida(obra.getId());

        String[] cols = {"Codigo", "Descripcion", "Und", "Presup. S/.", "Ejec. S/.", "Dif. S/.", "% Av."};
        float[] anchos = {70, 280, 35, 80, 80, 80, 50};

        try (PDDocument doc = new PDDocument()) {
            Lienzo lienzo = new Lienzo(doc, PDRectangle.A4, true); // horizontal
            lienzo.escribirCabecera(obra, "REPORTE COMPARATIVO: PRESUPUESTO VS EJECUTADO");
            lienzo.dibujarHeaderTabla(cols, anchos);

            double totalPresup = 0, totalEjec = 0;
            for (Partida p : partidas) {
                double presup = p.isEsPadre() ? 0 : p.getCostoTotalPresupuestado();
                double ejec = p.isEsPadre() ? 0 : ejecutado.getOrDefault(p.getId(), 0.0);
                double dif = presup - ejec;
                double pct = presup > 0 ? (ejec / presup) * 100 : 0;

                String[] valores = {
                        p.getCodigo() != null ? p.getCodigo() : "",
                        p.getDescripcion() != null ? p.getDescripcion() : "",
                        p.getUnidad() != null ? p.getUnidad() : "",
                        p.isEsPadre() ? "" : fmt(presup),
                        p.isEsPadre() ? "" : fmt(ejec),
                        p.isEsPadre() ? "" : fmt(dif),
                        p.isEsPadre() ? "" : String.format("%.1f%%", pct)
                };
                lienzo.dibujarFila(valores, anchos, p.isEsPadre(), cols);
                if (!p.isEsPadre()) { totalPresup += presup; totalEjec += ejec; }
            }

            double pctTotal = totalPresup > 0 ? (totalEjec / totalPresup) * 100 : 0;
            String[] totales = {"TOTALES", "", "", fmt(totalPresup), fmt(totalEjec),
                    fmt(totalPresup - totalEjec), String.format("%.1f%%", pctTotal)};
            lienzo.dibujarFilaTotal(totales, anchos, cols);

            lienzo.cerrar();
            return guardar(doc, "comparativo", obra);
        }
    }

    /** Movimientos de almacen (diario si fecha != null, acumulado si null). */
    public Path exportarMovimientosPdf(Obra obra, LocalDate fecha) throws Exception {
        List<MovimientoAlmacen> movimientos = (fecha != null)
                ? movimientoService.listarPorObraYFecha(obra.getId(), fecha)
                : movimientoService.listarPorObra(obra.getId());

        String[] cols = {"Fecha", "Partida", "Tipo", "Cant.", "C.Unit S/.", "Total S/."};
        float[] anchos = {70, 300, 60, 60, 80, 80};
        String titulo = (fecha != null) ? "REPORTE DIARIO DE ALMACEN - " + fecha : "REPORTE ACUMULADO DE ALMACEN";

        try (PDDocument doc = new PDDocument()) {
            Lienzo lienzo = new Lienzo(doc, PDRectangle.A4, true);
            lienzo.escribirCabecera(obra, titulo);
            lienzo.dibujarHeaderTabla(cols, anchos);

            double total = 0;
            for (MovimientoAlmacen m : movimientos) {
                String[] valores = {
                        m.getFecha().toString(),
                        m.getPartidaCodigo() + " - " + m.getPartidaDescripcion(),
                        m.getTipo().name(),
                        fmt(m.getCantidad()),
                        fmt(m.getCostoUnitarioReal()),
                        fmt(m.getCostoTotalReal())
                };
                lienzo.dibujarFila(valores, anchos, false, cols);
                total += (m.getTipo() == MovimientoAlmacen.Tipo.EGRESO ? m.getCostoTotalReal() : -m.getCostoTotalReal());
            }

            String[] totales = {"TOTAL EJECUTADO", "", "", "", "", fmt(total)};
            lienzo.dibujarFilaTotal(totales, anchos, cols);

            lienzo.cerrar();
            return guardar(doc, fecha != null ? "diario" : "acumulado", obra);
        }
    }

    /** Comparativo temporal de ejecucion (diario / semanal / mensual) en PDF horizontal. */
    public Path exportarComparativoPeriodicoPdf(Obra obra, Granularidad granularidad) throws Exception {
        List<ResumenPeriodo> periodos = movimientoService.resumenPorPeriodo(obra.getId(), granularidad);
        double presupuesto = obra.getPresupuestoTotal();

        String[] cols = {"Periodo", "Desde", "Hasta", "Mov", "Egresos S/.",
                "Ingresos S/.", "Neto S/.", "Acumulado S/.", "% Pres."};
        float[] anchos = {120, 60, 60, 35, 95, 95, 95, 100, 45};

        try (PDDocument doc = new PDDocument()) {
            Lienzo lienzo = new Lienzo(doc, PDRectangle.A4, true);
            lienzo.escribirCabecera(obra, "COMPARATIVO " + granularidad.getEtiqueta().toUpperCase() + " DE EJECUCION");
            lienzo.dibujarHeaderTabla(cols, anchos);

            double totalEgresos = 0, totalIngresos = 0;
            for (ResumenPeriodo p : periodos) {
                double pctAcum = presupuesto > 0 ? (p.getAcumulado() / presupuesto) * 100 : 0;
                String[] valores = {
                        p.getEtiqueta(),
                        p.getInicio().toString(),
                        p.getFin().toString(),
                        String.valueOf(p.getNumMovimientos()),
                        fmt(p.getEgresos()),
                        fmt(p.getIngresos()),
                        fmt(p.getNeto()),
                        fmt(p.getAcumulado()),
                        String.format("%.1f%%", pctAcum)
                };
                lienzo.dibujarFila(valores, anchos, false, cols);
                totalEgresos += p.getEgresos();
                totalIngresos += p.getIngresos();
            }

            double netoTotal = totalEgresos - totalIngresos;
            double pctTotal = presupuesto > 0 ? (netoTotal / presupuesto) * 100 : 0;
            String[] totales = {"TOTALES", "", "", "", fmt(totalEgresos), fmt(totalIngresos),
                    fmt(netoTotal), fmt(netoTotal), String.format("%.1f%%", pctTotal)};
            lienzo.dibujarFilaTotal(totales, anchos, cols);

            lienzo.cerrar();
            return guardar(doc, "periodico_" + granularidad.name().toLowerCase(), obra);
        }
    }

    /**
     * Ficha ejecutiva: consolida en una pagina vertical la salud de la obra, el avance
     * economico, el avance fisico del cronograma, las valorizaciones y el estado de
     * gestion y cumplimiento (riesgos, ambiental, calidad, SST).
     */
    public Path exportarResumenEjecutivoPdf(Obra obra) throws Exception {
        ResumenEjecutivoCalculo.Resumen r = construirResumenEjecutivo(obra);

        try (PDDocument doc = new PDDocument()) {
            Lienzo lienzo = new Lienzo(doc, PDRectangle.A4, false); // vertical: es una ficha, no una tabla ancha
            lienzo.escribirCabecera(obra, "FICHA EJECUTIVA DE OBRA");

            lienzo.dibujarSeccion("Datos generales (memoria descriptiva)");
            lienzo.dibujarPar("Ubicacion", obra.getUbicacion() != null ? obra.getUbicacion() : "No especificado");
            lienzo.dibujarPar("Entidad contratante",
                    obra.getEntidadContratante() != null ? obra.getEntidadContratante() : "No especificado");
            lienzo.dibujarPar("Modalidad de ejecucion",
                    obra.getModalidadEjecucion() != null ? obra.getModalidadEjecucion().name() : "No especificado");
            lienzo.dibujarPar("Sectores / bloques",
                    obra.getSectoresBloques() != null ? obra.getSectoresBloques() : "No especificado");

            lienzo.dibujarSeccion("Salud de la obra");
            lienzo.dibujarPar("Nivel", r.salud.titulo);
            lienzo.dibujarPar("Detalle", r.salud.detalle);

            lienzo.dibujarSeccion("Avance economico");
            lienzo.dibujarPar("Presupuesto S/.", fmt(r.presupuestoTotal));
            lienzo.dibujarPar("Ejecutado S/.", fmt(r.ejecutadoTotal));
            lienzo.dibujarPar("Diferencia S/.", fmt(r.presupuestoTotal - r.ejecutadoTotal));
            lienzo.dibujarPar("% Avance economico", String.format("%.1f%%", r.pctAvanceEconomico));

            lienzo.dibujarSeccion("Avance fisico (cronograma)");
            lienzo.dibujarPar("Actividades registradas", String.valueOf(r.actividadesTotal));
            lienzo.dibujarPar("Completadas", String.valueOf(r.actividadesCompletadas));
            lienzo.dibujarPar("Atrasadas", String.valueOf(r.actividadesAtrasadas));
            lienzo.dibujarPar("% Avance real", String.format("%.1f%%", r.pctAvanceFisicoReal));
            lienzo.dibujarPar("% Avance programado", String.format("%.1f%%", r.pctAvanceFisicoProgramado));

            lienzo.dibujarSeccion("Valorizaciones");
            lienzo.dibujarPar("Valorizaciones emitidas", String.valueOf(r.numeroValorizaciones));
            if (r.ultimaValorizacion != null) {
                lienzo.dibujarPar("Ultima valorizacion", "N°" + r.ultimaValorizacion.getNumero() + " ("
                        + r.ultimaValorizacion.getPeriodoDesde() + " a " + r.ultimaValorizacion.getPeriodoHasta() + ")");
                lienzo.dibujarPar("Monto neto ultima valorizacion S/.", fmt(r.ultimaValorizacion.getMontoNetoPagar()));
            }
            lienzo.dibujarPar("Total valorizado acumulado S/.", fmt(r.totalValorizadoAcumulado));

            lienzo.dibujarSeccion("Gestion y cumplimiento");
            String[] cols = {"Categoria", "Total", "Abiertos", "Vencidos"};
            float[] anchos = {150, 110, 110, 110};
            lienzo.dibujarHeaderTabla(cols, anchos);
            for (ResumenEjecutivoCalculo.ConteoCategoria cc : r.cumplimiento) {
                String[] valores = {cc.categoria.name(), String.valueOf(cc.total),
                        String.valueOf(cc.abiertos), String.valueOf(cc.vencidos)};
                lienzo.dibujarFila(valores, anchos, false, cols);
            }

            lienzo.cerrar();
            return guardar(doc, "resumen_ejecutivo", obra);
        }
    }

    /** Reune los datos de todas las areas de la obra para {@link ResumenEjecutivoCalculo#construir}. */
    ResumenEjecutivoCalculo.Resumen construirResumenEjecutivo(Obra obra) throws SQLException {
        List<Partida> partidas = partidaService.listarPorObra(obra.getId());
        Map<Long, Double> ejecutadoPorPartida = movimientoService.totalEjecutadoPorPartida(obra.getId());
        double ejecutadoTotal = movimientoService.totalEjecutadoObra(obra.getId());
        List<Actividad> actividades = cronogramaService.listarPorObra(obra.getId());
        List<Valorizacion> valorizaciones = valorizacionService.listarPorObra(obra.getId());

        Map<ItemCumplimiento.Categoria, List<ItemCumplimiento>> itemsPorCategoria = new LinkedHashMap<>();
        for (ItemCumplimiento.Categoria categoria : ItemCumplimiento.Categoria.values()) {
            itemsPorCategoria.put(categoria, cumplimientoService.listarPorObra(obra.getId(), categoria));
        }

        return ResumenEjecutivoCalculo.construir(obra, partidas, ejecutadoPorPartida, ejecutadoTotal,
                actividades, valorizaciones, itemsPorCategoria, LocalDate.now());
    }

    private Path guardar(PDDocument doc, String tipo, Obra obra) throws IOException {
        String nombre = String.format("ObraTrack_%s_%s_%s.pdf",
                tipo, sanitizar(obra.getNombre()), LocalDate.now().atStartOfDay().format(FMT_ARCHIVO));
        Path destino = com.obratrack.core.Rutas.exports().resolve(nombre);
        doc.save(destino.toFile());
        return destino;
    }

    private String fmt(double v) {
        return String.format("%,.2f", v);
    }

    private String sanitizar(String texto) {
        if (texto == null) return "obra";
        return texto.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
    }

    /**
     * Encapsula el dibujo sobre el PDF: maneja la posicion vertical actual,
     * el salto de pagina automatico y el stream de contenido.
     */
    private static final class Lienzo {
        private final PDDocument doc;
        private final PDRectangle tamPagina;
        private final boolean horizontal;
        private PDPage paginaActual;
        private PDPageContentStream stream;
        private float y;
        private float anchoUtil;

        Lienzo(PDDocument doc, PDRectangle tam, boolean horizontal) throws IOException {
            this.doc = doc;
            this.tamPagina = tam;
            this.horizontal = horizontal;
            nuevaPagina();
        }

        private void nuevaPagina() throws IOException {
            if (stream != null) stream.close();
            PDRectangle box = horizontal
                    ? new PDRectangle(tamPagina.getHeight(), tamPagina.getWidth())
                    : tamPagina;
            paginaActual = new PDPage(box);
            doc.addPage(paginaActual);
            stream = new PDPageContentStream(doc, paginaActual);
            anchoUtil = box.getWidth() - 2 * MARGEN;
            y = box.getHeight() - MARGEN;
        }

        void escribirCabecera(Obra obra, String titulo) throws IOException {
            texto(titulo, MARGEN, y, FUENTE_BOLD, 14);
            y -= 22;
            texto("Obra: " + obra.getNombre(), MARGEN, y, FUENTE, 11);
            y -= 16;
            texto("Generado: " + LocalDate.now(), MARGEN, y, FUENTE, 10);
            y -= 24;
        }

        void dibujarHeaderTabla(String[] cols, float[] anchos) throws IOException {
            float x = MARGEN;
            stream.setNonStrokingColor(40, 60, 110);
            stream.addRect(MARGEN, y - ALTO_FILA + 4, anchoUtil, ALTO_FILA);
            stream.fill();
            stream.setNonStrokingColor(255, 255, 255);
            for (int i = 0; i < cols.length; i++) {
                texto(ajustarAncho(cols[i], FUENTE_BOLD, TAM_FUENTE, anchos[i] - 6), x + 3, y - ALTO_FILA + 9, FUENTE_BOLD, TAM_FUENTE);
                x += anchos[i];
            }
            stream.setNonStrokingColor(0, 0, 0);
            y -= ALTO_FILA;
        }

        void dibujarFila(String[] valores, float[] anchos, boolean esPadre, String[] cols) throws IOException {
            if (y - ALTO_FILA < MARGEN) {
                nuevaPagina();
                dibujarHeaderTabla(cols, anchos);
            }
            float x = MARGEN;
            PDFont fuente = esPadre ? FUENTE_BOLD : FUENTE;
            for (int i = 0; i < valores.length; i++) {
                texto(ajustarAncho(valores[i], fuente, TAM_FUENTE, anchos[i] - 6), x + 3, y - 13, fuente, TAM_FUENTE);
                x += anchos[i];
            }
            // linea inferior
            stream.setStrokingColor(210, 210, 210);
            stream.moveTo(MARGEN, y - ALTO_FILA + 4);
            stream.lineTo(MARGEN + anchoUtil, y - ALTO_FILA + 4);
            stream.stroke();
            y -= ALTO_FILA;
        }

        void dibujarFilaTotal(String[] valores, float[] anchos, String[] cols) throws IOException {
            if (y - ALTO_FILA < MARGEN) {
                nuevaPagina();
                dibujarHeaderTabla(cols, anchos);
            }
            stream.setNonStrokingColor(220, 220, 220);
            stream.addRect(MARGEN, y - ALTO_FILA + 4, anchoUtil, ALTO_FILA);
            stream.fill();
            stream.setNonStrokingColor(0, 0, 0);
            float x = MARGEN;
            for (int i = 0; i < valores.length; i++) {
                texto(ajustarAncho(valores[i], FUENTE_BOLD, TAM_FUENTE, anchos[i] - 6), x + 3, y - 13, FUENTE_BOLD, TAM_FUENTE);
                x += anchos[i];
            }
            y -= ALTO_FILA;
        }

        private void texto(String txt, float x, float yPos, PDFont fuente, float tam) throws IOException {
            if (txt == null) txt = "";
            stream.beginText();
            stream.setFont(fuente, tam);
            stream.newLineAtOffset(x, yPos);
            stream.showText(limpiar(txt));
            stream.endText();
        }

        /**
         * PDFBox con fuentes estandar no soporta algunos caracteres; los limpiamos. Los
         * saltos de linea (p. ej. en un campo multilinea como sectores/bloques) se
         * cambian por un espacio primero, para que las palabras no queden pegadas al
         * perder el salto (en vez de "SueloAgua" queda "Suelo Agua").
         */
        private String limpiar(String txt) {
            return txt.replaceAll("[\\r\\n]+", " ").replaceAll("[^\\x20-\\x7E]", "");
        }

        /** Ancho real (en puntos) que ocupa un texto con la fuente y tamano dados. */
        private float anchoTexto(String s, PDFont fuente, float tam) throws IOException {
            return fuente.getStringWidth(limpiar(s)) / 1000f * tam;
        }

        /**
         * Recorta el texto con puntos suspensivos para que quepa exactamente en el ancho
         * disponible de su columna. Evita que una celda (p. ej. la descripcion) invada la
         * siguiente y se solapen las letras en el PDF.
         */
        private String ajustarAncho(String txt, PDFont fuente, float tam, float anchoDisponible) throws IOException {
            if (txt == null) return "";
            String limpio = limpiar(txt);
            if (anchoDisponible <= 0) return "";
            if (anchoTexto(limpio, fuente, tam) <= anchoDisponible) return limpio;
            String puntos = "...";
            int i = limpio.length();
            while (i > 0 && anchoTexto(limpio.substring(0, i) + puntos, fuente, tam) > anchoDisponible) {
                i--;
            }
            return i <= 0 ? "" : limpio.substring(0, i) + puntos;
        }

        /** Titulo de seccion con una linea divisoria debajo (para la ficha ejecutiva). */
        void dibujarSeccion(String titulo) throws IOException {
            if (y - 30 < MARGEN) nuevaPagina();
            y -= 6;
            texto(titulo, MARGEN, y, FUENTE_BOLD, 11);
            y -= 4;
            stream.setStrokingColor(180, 180, 180);
            stream.moveTo(MARGEN, y);
            stream.lineTo(MARGEN + anchoUtil, y);
            stream.stroke();
            y -= 16;
        }

        /** Fila "etiqueta: valor" de dos columnas (para la ficha ejecutiva). */
        void dibujarPar(String etiqueta, String valor) throws IOException {
            if (y - ALTO_FILA < MARGEN) nuevaPagina();
            float xValor = MARGEN + 200;
            texto(ajustarAncho(etiqueta, FUENTE_BOLD, TAM_FUENTE, 190), MARGEN, y - 13, FUENTE_BOLD, TAM_FUENTE);
            texto(ajustarAncho(valor, FUENTE, TAM_FUENTE, anchoUtil - 200), xValor, y - 13, FUENTE, TAM_FUENTE);
            y -= ALTO_FILA;
        }

        void cerrar() throws IOException {
            if (stream != null) stream.close();
        }
    }
}
