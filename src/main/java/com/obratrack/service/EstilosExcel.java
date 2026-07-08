package com.obratrack.service;

import org.apache.poi.ss.usermodel.*;

/**
 * Centraliza los estilos de celda usados en los reportes Excel.
 * Se crea una instancia por Workbook (los CellStyle no se pueden compartir entre libros).
 */
class EstilosExcel {

    final CellStyle titulo;
    final CellStyle subtitulo;
    final CellStyle header;
    final CellStyle moneda;
    final CellStyle monedaRoja;
    final CellStyle numero;
    final CellStyle totalTexto;
    final CellStyle totalMoneda;
    final CellStyle totalPorcentaje;

    private final CellStyle textoNormal;
    private final CellStyle textoPadre;
    private final CellStyle pctVerde;
    private final CellStyle pctAmarillo;
    private final CellStyle pctRojo;

    EstilosExcel(Workbook wb) {
        DataFormat formato = wb.createDataFormat();

        Font fontTitulo = wb.createFont();
        fontTitulo.setBold(true);
        fontTitulo.setFontHeightInPoints((short) 14);
        titulo = wb.createCellStyle();
        titulo.setFont(fontTitulo);
        titulo.setAlignment(HorizontalAlignment.LEFT);

        Font fontSub = wb.createFont();
        fontSub.setFontHeightInPoints((short) 11);
        subtitulo = wb.createCellStyle();
        subtitulo.setFont(fontSub);

        Font fontHeader = wb.createFont();
        fontHeader.setBold(true);
        fontHeader.setColor(IndexedColors.WHITE.getIndex());
        header = wb.createCellStyle();
        header.setFont(fontHeader);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        aplicarBordes(header);

        moneda = wb.createCellStyle();
        moneda.setDataFormat(formato.getFormat("#,##0.00"));
        aplicarBordes(moneda);

        monedaRoja = wb.createCellStyle();
        monedaRoja.setDataFormat(formato.getFormat("#,##0.00"));
        Font fontRoja = wb.createFont();
        fontRoja.setColor(IndexedColors.RED.getIndex());
        monedaRoja.setFont(fontRoja);
        aplicarBordes(monedaRoja);

        numero = wb.createCellStyle();
        numero.setDataFormat(formato.getFormat("#,##0.00"));
        aplicarBordes(numero);

        textoNormal = wb.createCellStyle();
        aplicarBordes(textoNormal);

        textoPadre = wb.createCellStyle();
        Font fontPadre = wb.createFont();
        fontPadre.setBold(true);
        textoPadre.setFont(fontPadre);
        aplicarBordes(textoPadre);

        // Totales
        Font fontTotal = wb.createFont();
        fontTotal.setBold(true);
        totalTexto = wb.createCellStyle();
        totalTexto.setFont(fontTotal);
        totalTexto.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalTexto.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        aplicarBordes(totalTexto);

        totalMoneda = wb.createCellStyle();
        totalMoneda.setDataFormat(formato.getFormat("#,##0.00"));
        totalMoneda.setFont(fontTotal);
        totalMoneda.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalMoneda.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        aplicarBordes(totalMoneda);

        totalPorcentaje = wb.createCellStyle();
        totalPorcentaje.setDataFormat(formato.getFormat("0.0%"));
        totalPorcentaje.setFont(fontTotal);
        totalPorcentaje.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalPorcentaje.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        aplicarBordes(totalPorcentaje);

        // Porcentajes por color de avance
        pctVerde = crearEstiloPorcentaje(wb, formato, IndexedColors.GREEN.getIndex());
        pctAmarillo = crearEstiloPorcentaje(wb, formato, IndexedColors.DARK_YELLOW.getIndex());
        pctRojo = crearEstiloPorcentaje(wb, formato, IndexedColors.RED.getIndex());
    }

    CellStyle textoBase(boolean esPadre) {
        return esPadre ? textoPadre : textoNormal;
    }

    CellStyle porcentajePorAvance(double pct) {
        if (pct > 100) return pctRojo;
        if (pct >= 80) return pctAmarillo;
        return pctVerde;
    }

    private CellStyle crearEstiloPorcentaje(Workbook wb, DataFormat formato, short colorFuente) {
        CellStyle estilo = wb.createCellStyle();
        estilo.setDataFormat(formato.getFormat("0.0%"));
        Font font = wb.createFont();
        font.setColor(colorFuente);
        font.setBold(true);
        estilo.setFont(font);
        aplicarBordes(estilo);
        return estilo;
    }

    private void aplicarBordes(CellStyle estilo) {
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }
}
