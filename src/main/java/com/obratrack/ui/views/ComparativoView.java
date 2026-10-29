package com.obratrack.ui.views;

import com.obratrack.model.Obra;
import com.obratrack.model.ResumenPeriodo;
import com.obratrack.service.Granularidad;
import com.obratrack.service.IMovimientoService;
import com.obratrack.service.IReportePdf;
import com.obratrack.service.IReporteService;
import com.obratrack.service.ServiceFactory;
import com.obratrack.ui.Icons;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Comparativo temporal de ejecucion de la obra: permite ver el avance
 * agrupado por dia, semana o mes a lo largo de toda la duracion de la obra,
 * con egresos, ingresos, neto, acumulado y % del presupuesto consumido.
 * Exporta a Excel y PDF.
 */
public class ComparativoView extends JPanel {

    private final Supplier<Obra> obraActivaProvider;
    private final IMovimientoService movimientoService;
    private final IReporteService reporteExcel = ServiceFactory.reporteExcel();
    private final IReportePdf reportePdf = ServiceFactory.reportePdf();

    private final JComboBox<Granularidad> comboGranularidad = new JComboBox<>(Granularidad.values());
    private final JLabel tituloObra = new JLabel();
    private final JLabel labelResumen = new JLabel(" ");
    private final JLabel labelMensaje = new JLabel(" ");
    private final DefaultTableModel tablaModelo;
    private final JTable tabla;
    private SwingWorker<DatosComparativo, Void> carga;
    private int versionCarga;
    private boolean exportando;
    private final java.util.List<JButton> botonesExportar = new java.util.ArrayList<>();

    private static final String[] COLUMNAS = {
            "Periodo", "Desde", "Hasta", "N° Mov.", "Egresos (S/.)",
            "Ingresos (S/.)", "Neto (S/.)", "Acumulado (S/.)", "% Presup."
    };

    public ComparativoView(Supplier<Obra> obraActivaProvider) {
        this(obraActivaProvider, ServiceFactory.movimiento());
    }

    ComparativoView(Supplier<Obra> obraActivaProvider, IMovimientoService movimientos) {
        this.movimientoService = movimientos;
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(construirHeader(), BorderLayout.NORTH);

        tablaModelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(tablaModelo);
        tabla.setFont(Theme.FONT_BASE);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setFont(Theme.FONT_BOLD);
        tabla.setDefaultRenderer(Object.class, new FilaColorRenderer());
        tabla.setAutoCreateRowSorter(true);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        add(construirPie(), BorderLayout.SOUTH);
    }

    private JPanel construirHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JPanel filaTitulo = new JPanel(new BorderLayout());
        filaTitulo.setOpaque(false);
        JLabel titulo = new JLabel("Comparativo Temporal");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        filaTitulo.add(titulo, BorderLayout.WEST);
        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        filaTitulo.add(tituloObra, BorderLayout.EAST);
        header.add(filaTitulo, BorderLayout.NORTH);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controles.setOpaque(false);
        JLabel lbl = new JLabel("Agrupar por:");
        lbl.setFont(Theme.FONT_BASE);
        lbl.setForeground(Theme.TEXT_SECONDARY);
        comboGranularidad.setFont(Theme.FONT_BASE);
        comboGranularidad.setPreferredSize(new Dimension(140, 30));
        comboGranularidad.addActionListener(e -> cargarDatos());
        controles.add(lbl);
        controles.add(comboGranularidad);
        controles.add(Box.createHorizontalStrut(16));
        controles.add(botonExportar("Exportar Excel", Theme.SUCCESS, this::exportarExcel));
        controles.add(botonExportar("Exportar PDF", Theme.ACCENT, this::exportarPdf));
        header.add(controles, BorderLayout.SOUTH);

        return header;
    }

    private JPanel construirPie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        labelResumen.setFont(Theme.FONT_BOLD);
        labelResumen.setForeground(Theme.TEXT_PRIMARY);
        labelMensaje.setFont(Theme.FONT_BASE);
        labelMensaje.setForeground(Theme.TEXT_SECONDARY);
        pie.add(labelResumen, BorderLayout.NORTH);
        pie.add(labelMensaje, BorderLayout.SOUTH);
        return pie;
    }

    private JButton botonExportar(String texto, Color color, Runnable accion) {
        JButton btn = new JButton(texto, Icons.get("download", 16, Color.WHITE));
        btn.setIconTextGap(8);
        btn.setFont(Theme.FONT_BOLD);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(150, 32));
        btn.addActionListener(e -> accion.run());
        botonesExportar.add(btn);
        return btn;
    }

    private Granularidad granularidadSeleccionada() {
        Object sel = comboGranularidad.getSelectedItem();
        return (sel instanceof Granularidad) ? (Granularidad) sel : Granularidad.MENSUAL;
    }

    /** Llamado por MainWindow al mostrar la vista o cambiar la obra activa. */
    public void refrescar() {
        cargarDatos();
    }

    private record DatosComparativo(Object[][] filas, String resumen, boolean vacio) {}

    private void cargarDatos() {
        int version = ++versionCarga;
        if (carga != null) carga.cancel(true);
        Obra obra = obraActivaProvider.get();
        Granularidad granularidad = granularidadSeleccionada();
        tablaModelo.setRowCount(0);
        mostrarMensaje(" ", Theme.TEXT_SECONDARY);
        labelResumen.setText(" ");
        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa");
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre() + " · Cargando...");
        carga = new SwingWorker<>() {
            @Override protected DatosComparativo doInBackground() throws Exception {
                List<ResumenPeriodo> periodos = movimientoService.resumenPorPeriodo(obra.getId(), granularidad);
                double presupuesto = obra.getPresupuestoTotal();
                Object[][] filas = new Object[periodos.size()][];
                int totalMov = 0;
                for (int i = 0; i < periodos.size(); i++) {
                    if (isCancelled()) return null;
                    ResumenPeriodo p = periodos.get(i);
                    double pctAcum = presupuesto > 0 ? p.getAcumulado() / presupuesto * 100 : 0;
                    filas[i] = new Object[]{p.getEtiqueta(), p.getInicio().toString(), p.getFin().toString(),
                            p.getNumMovimientos(), String.format("%,.2f", p.getEgresos()),
                            String.format("%,.2f", p.getIngresos()), String.format("%,.2f", p.getNeto()),
                            String.format("%,.2f", p.getAcumulado()), String.format("%.1f%%", pctAcum)};
                    totalMov += p.getNumMovimientos();
                }
                double totalNeto = periodos.isEmpty() ? 0 : periodos.get(periodos.size() - 1).getAcumulado();
                double pct = presupuesto > 0 ? totalNeto / presupuesto * 100 : 0;
                String resumen = String.format("%d periodos · %d movimientos · Ejecutado S/. %,.2f de S/. %,.2f (%.1f%%)",
                        periodos.size(), totalMov, totalNeto, presupuesto, pct);
                return new DatosComparativo(filas, resumen, periodos.isEmpty());
            }

            @Override protected void done() {
                if (version != versionCarga || isCancelled()) return;
                try {
                    DatosComparativo datos = get();
                    tablaModelo.setDataVector(datos.filas(), COLUMNAS);
                    labelResumen.setText(datos.resumen());
                    tituloObra.setText("Obra: " + obra.getNombre());
                    if (datos.vacio()) mostrarMensaje("Aun no hay movimientos registrados para esta obra.", Theme.WARNING);
                } catch (java.util.concurrent.CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    tituloObra.setText("Obra: " + obra.getNombre());
                    mostrarMensaje("No se pudo cargar el comparativo: " + e.getCause().getMessage(), Theme.DANGER);
                }
            }
        };
        carga.execute();
    }

    private void exportarExcel() { exportar(false); }
    private void exportarPdf() { exportar(true); }

    private void exportar(boolean pdf) {
        if (exportando) return;
        Obra obra = obraActivaProvider.get();
        if (obra == null) { mostrarMensaje("Selecciona una obra activa primero.", Theme.DANGER); return; }
        Granularidad granularidad = granularidadSeleccionada();
        exportando = true;
        botonesExportar.forEach(b -> b.setEnabled(false));
        mostrarMensaje("Generando reporte...", Theme.TEXT_SECONDARY);
        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception {
                Path ruta = pdf ? reportePdf.exportarComparativoPeriodicoPdf(obra, granularidad)
                        : reporteExcel.exportarComparativoPeriodicoExcel(obra, granularidad);
                abrirArchivo(ruta.toFile());
                return ruta;
            }
            @Override protected void done() {
                try {
                    mostrarMensaje((pdf ? "PDF" : "Excel") + " generado: " + get().toAbsolutePath(), Theme.SUCCESS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    mostrarMensaje("No se pudo generar el reporte: " + e.getCause().getMessage(), Theme.DANGER);
                } finally {
                    exportando = false;
                    botonesExportar.forEach(b -> b.setEnabled(true));
                }
            }
        }.execute();
    }

    private void abrirArchivo(File archivo) {
        try {
            if (Desktop.isDesktopSupported() && archivo.exists()) {
                Desktop.getDesktop().open(archivo);
            }
        } catch (Exception ignored) {
            // si no se puede abrir, el usuario ya tiene la ruta en el mensaje
        }
    }

    private void mostrarMensaje(String texto, Color color) {
        labelMensaje.setText(texto);
        labelMensaje.setForeground(color);
    }

    /** Colorea la columna "% Presup." segun el avance (verde/amarillo/rojo). */
    private static class FilaColorRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                        boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Color fondo = (row % 2 == 0) ? Theme.BG_PRIMARY : Theme.BG_SECONDARY;
            c.setBackground(isSelected ? Theme.BORDER : fondo);

            Color colorTexto = Theme.TEXT_PRIMARY;
            int colPct = COLUMNAS.length - 1; // "% Presup."
            String pctTexto = String.valueOf(table.getValueAt(row, colPct));
            if (pctTexto != null && pctTexto.endsWith("%")) {
                try {
                    double pct = Double.parseDouble(pctTexto.replace("%", "").trim());
                    if (column == colPct) colorTexto = Theme.colorPorAvance(pct);
                } catch (NumberFormatException ignored) {}
            }
            c.setForeground(colorTexto);
            return c;
        }
    }
}
