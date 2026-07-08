package com.obratrack.ui.views;

import com.obratrack.model.Obra;
import com.obratrack.service.ReportePdf;
import com.obratrack.service.ReporteService;
import com.obratrack.ui.Icons;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Supplier;

/** Vista de reportes: exportar comparativo, diario y acumulado a Excel o PDF. */
public class ReportesView extends JPanel {

    private final Supplier<Obra> obraActivaProvider;
    private final ReporteService reporteExcel = new ReporteService();
    private final ReportePdf reportePdf = new ReportePdf();

    private final JLabel tituloObra = new JLabel();
    private final JTextField campoFechaDiario = new JTextField(LocalDate.now().toString());
    private final JLabel labelMensaje = new JLabel(" ");

    public ReportesView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Reportes");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        header.add(titulo, BorderLayout.WEST);
        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        header.add(tituloObra, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setOpaque(false);

        centro.add(tarjetaReporte(
                "Comparativo: Presupuesto vs Ejecutado",
                "Todas las partidas con su presupuesto, lo ejecutado, la diferencia y el % de avance.",
                () -> generar("comparativo", null, false),
                () -> generar("comparativo", null, true)));
        centro.add(Box.createVerticalStrut(14));

        centro.add(tarjetaReporteDiario());
        centro.add(Box.createVerticalStrut(14));

        centro.add(tarjetaReporte(
                "Acumulado de Almacen",
                "Todos los movimientos de ingreso/egreso registrados hasta hoy.",
                () -> generar("acumulado", null, false),
                () -> generar("acumulado", null, true)));
        centro.add(Box.createVerticalStrut(18));

        labelMensaje.setFont(Theme.FONT_BASE);
        labelMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(labelMensaje);

        centro.add(Box.createVerticalGlue());
        add(centro, BorderLayout.CENTER);
    }

    private JPanel tarjetaReporte(String titulo, String descripcion, Runnable accionExcel, Runnable accionPdf) {
        JPanel card = new JPanel(new BorderLayout(12, 8));
        card.setBackground(Theme.BG_SECONDARY);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Theme.FONT_BOLD);
        lblTitulo.setForeground(Theme.TEXT_PRIMARY);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblDesc = new JLabel("<html><body style='width:480px'>" + descripcion + "</body></html>");
        lblDesc.setFont(Theme.FONT_BASE);
        lblDesc.setForeground(Theme.TEXT_SECONDARY);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(lblTitulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(lblDesc);
        card.add(textos, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(botonExportar("Excel", Theme.SUCCESS, accionExcel));
        botones.add(botonExportar("PDF", Theme.ACCENT, accionPdf));
        card.add(botones, BorderLayout.EAST);

        return card;
    }

    private JPanel tarjetaReporteDiario() {
        JPanel card = new JPanel(new BorderLayout(12, 8));
        card.setBackground(Theme.BG_SECONDARY);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel lblTitulo = new JLabel("Reporte Diario de Almacen");
        lblTitulo.setFont(Theme.FONT_BOLD);
        lblTitulo.setForeground(Theme.TEXT_PRIMARY);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblDesc = new JLabel("Movimientos de una fecha especifica.");
        lblDesc.setFont(Theme.FONT_BASE);
        lblDesc.setForeground(Theme.TEXT_SECONDARY);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel filaFecha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filaFecha.setOpaque(false);
        filaFecha.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblFecha = new JLabel("Fecha (AAAA-MM-DD):");
        lblFecha.setFont(Theme.FONT_BASE);
        lblFecha.setForeground(Theme.TEXT_SECONDARY);
        campoFechaDiario.setPreferredSize(new Dimension(130, 28));
        campoFechaDiario.setFont(Theme.FONT_BASE);
        filaFecha.add(lblFecha);
        filaFecha.add(campoFechaDiario);

        textos.add(lblTitulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(lblDesc);
        textos.add(filaFecha);
        card.add(textos, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(botonExportar("Excel", Theme.SUCCESS, () -> generar("diario", parsearFecha(), false)));
        botones.add(botonExportar("PDF", Theme.ACCENT, () -> generar("diario", parsearFecha(), true)));
        card.add(botones, BorderLayout.EAST);

        return card;
    }

    private JButton botonExportar(String texto, Color color, Runnable accion) {
        JButton btn = new JButton(texto, Icons.get("download", 15, Color.WHITE));
        btn.setIconTextGap(6);
        btn.setFont(Theme.FONT_BOLD);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(96, 34));
        btn.addActionListener(e -> accion.run());
        return btn;
    }

    private LocalDate parsearFecha() {
        try {
            return LocalDate.parse(campoFechaDiario.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Genera el reporte indicado. tipo: "comparativo" | "diario" | "acumulado".
     * Para "diario" la fecha no puede ser null.
     */
    private void generar(String tipo, LocalDate fecha, boolean pdf) {
        Obra obra = obraActivaProvider.get();
        if (obra == null) {
            mostrarMensaje("Selecciona una obra activa primero.", Theme.DANGER);
            return;
        }
        if ("diario".equals(tipo) && fecha == null) {
            mostrarMensaje("Fecha invalida. Usa el formato AAAA-MM-DD.", Theme.DANGER);
            return;
        }

        try {
            Path ruta;
            if (pdf) {
                ruta = switch (tipo) {
                    case "comparativo" -> reportePdf.exportarComparativoPdf(obra);
                    case "diario" -> reportePdf.exportarMovimientosPdf(obra, fecha);
                    default -> reportePdf.exportarMovimientosPdf(obra, null);
                };
            } else {
                ruta = switch (tipo) {
                    case "comparativo" -> reporteExcel.exportarComparativoExcel(obra);
                    case "diario" -> reporteExcel.exportarMovimientosExcel(obra, fecha);
                    default -> reporteExcel.exportarMovimientosExcel(obra, null);
                };
            }
            mostrarMensaje("Reporte generado: " + ruta.toAbsolutePath(), Theme.SUCCESS);
            abrirArchivo(ruta.toFile());
        } catch (Exception e) {
            mostrarMensaje("No se pudo generar el reporte: " + e.getMessage(), Theme.DANGER);
        }
    }

    private void abrirArchivo(File archivo) {
        try {
            if (Desktop.isDesktopSupported() && archivo.exists()) {
                Desktop.getDesktop().open(archivo);
            }
        } catch (Exception ignored) {
            // si no se puede abrir automaticamente, el usuario ya tiene la ruta en el mensaje
        }
    }

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        tituloObra.setText(obra != null ? "Obra: " + obra.getNombre() : "Selecciona una obra activa");
    }

    private void mostrarMensaje(String texto, Color color) {
        labelMensaje.setText(texto);
        labelMensaje.setForeground(color);
    }
}
