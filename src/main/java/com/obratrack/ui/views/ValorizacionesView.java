package com.obratrack.ui.views;

import com.obratrack.model.Obra;
import com.obratrack.model.Valorizacion;
import com.obratrack.service.IValorizacionService;
import com.obratrack.service.Permisos;
import com.obratrack.service.ServiceFactory;
import com.obratrack.service.ValorizacionCalculo;
import com.obratrack.ui.Icons;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Valorizaciones mensuales: corte formal del avance ejecutado por periodo, con
 * retencion de garantia y neto a pagar. Append-only (se genera a partir de los
 * movimientos de almacen del periodo); solo se permite eliminar una emitida por
 * error, no editar sus montos.
 */
public class ValorizacionesView extends JPanel {

    private final IValorizacionService valorizacionService = ServiceFactory.valorizacion();
    private final Supplier<Obra> obraActivaProvider;

    private final JLabel tituloObra = new JLabel();
    private final JLabel resumenTexto = new JLabel();
    private final DefaultTableModel tablaModelo;
    private final JTable tabla;
    private final List<Valorizacion> valorizacionesActuales = new ArrayList<>();

    private static final String[] COLUMNAS = {
            "N°", "Periodo", "Emision", "Ejecutado (S/.)", "Acumulado (S/.)",
            "Retencion %", "Retencion (S/.)", "Amortizacion (S/.)", "Neto a pagar (S/.)"
    };
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ValorizacionesView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Valorizaciones");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        header.add(titulo, BorderLayout.WEST);
        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        header.add(tituloObra, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tablaModelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(tablaModelo);
        tabla.setFont(Theme.FONT_BASE);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setFont(Theme.FONT_BOLD);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(construirResumen(), BorderLayout.NORTH);
        centro.add(construirBarra(), BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
    }

    /** Franja de resumen: presupuesto, acumulado, % avance financiero, saldo por valorizar. */
    private JPanel construirResumen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        resumenTexto.setFont(Theme.FONT_BOLD);
        resumenTexto.setForeground(Theme.TEXT_SECONDARY);
        panel.add(resumenTexto, BorderLayout.WEST);
        return panel;
    }

    private JPanel construirBarra() {
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        JButton btnNueva = new JButton("Nueva valorizacion", Icons.get("add", 16, Color.WHITE));
        btnNueva.setIconTextGap(6);
        btnNueva.setFont(Theme.FONT_BOLD);
        btnNueva.setBackground(Theme.ACCENT);
        btnNueva.setForeground(Color.WHITE);
        btnNueva.setFocusPainted(false);
        btnNueva.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNueva.addActionListener(e -> nuevaValorizacion());
        JButton btnEliminar = new JButton("Eliminar", Icons.get("delete", 15, Theme.DANGER));
        btnEliminar.setIconTextGap(6);
        btnEliminar.setForeground(Theme.DANGER);
        btnEliminar.setFont(Theme.FONT_BASE);
        btnEliminar.setFocusPainted(false);
        btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEliminar.addActionListener(e -> eliminarSeleccionada());
        if (!Permisos.puedeEscribir()) {
            btnNueva.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
        botones.add(btnNueva);
        botones.add(btnEliminar);
        wrap.add(botones, BorderLayout.NORTH);
        wrap.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return wrap;
    }

    // ============================================================
    //  Carga y refresco
    // ============================================================

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        tablaModelo.setRowCount(0);
        valorizacionesActuales.clear();

        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa para ver sus valorizaciones");
            resumenTexto.setText("");
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre());

        try {
            List<Valorizacion> valorizaciones = valorizacionService.listarPorObra(obra.getId());
            valorizacionesActuales.addAll(valorizaciones);

            for (Valorizacion v : valorizaciones) {
                tablaModelo.addRow(new Object[]{
                        v.getNumero(),
                        v.getPeriodoDesde().format(FMT) + " - " + v.getPeriodoHasta().format(FMT),
                        v.getFechaEmision() != null ? v.getFechaEmision().format(FMT) : "",
                        String.format("%,.2f", v.getMontoEjecutadoPeriodo()),
                        String.format("%,.2f", v.getMontoAcumuladoTotal()),
                        String.format("%.1f%%", v.getPctRetencion()),
                        String.format("%,.2f", v.getMontoRetencion()),
                        String.format("%,.2f", v.getMontoAmortizacionAdelanto()),
                        String.format("%,.2f", v.getMontoNetoPagar())
                });
            }

            actualizarResumen(obra, valorizaciones);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las valorizaciones: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarResumen(Obra obra, List<Valorizacion> valorizaciones) {
        double acumuladoTotal = valorizaciones.stream().mapToDouble(Valorizacion::getMontoEjecutadoPeriodo).sum();
        double presupuesto = obra.getPresupuestoTotal();
        double pctAvance = ValorizacionCalculo.calcularPctAvanceFinanciero(acumuladoTotal, presupuesto);
        double saldo = ValorizacionCalculo.calcularSaldoPorValorizar(acumuladoTotal, presupuesto);
        resumenTexto.setText(String.format(
                "Presupuesto: S/. %,.2f   ·   Valorizado acumulado: S/. %,.2f (%.1f%%)   ·   Saldo por valorizar: S/. %,.2f",
                presupuesto, acumuladoTotal, pctAvance, saldo));
    }

    // ============================================================
    //  Alta / eliminacion
    // ============================================================

    private Valorizacion seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return null;
        int modelRow = tabla.convertRowIndexToModel(fila);
        if (modelRow < 0 || modelRow >= valorizacionesActuales.size()) return null;
        return valorizacionesActuales.get(modelRow);
    }

    private void nuevaValorizacion() {
        Obra obra = obraActivaProvider.get();
        if (obra == null) {
            mostrarError("Selecciona una obra activa primero.");
            return;
        }

        LocalDate sugeridoDesde = valorizacionesActuales.stream()
                .map(Valorizacion::getPeriodoHasta)
                .max(LocalDate::compareTo)
                .map(f -> f.plusDays(1))
                .orElse(obra.getFechaInicio() != null ? obra.getFechaInicio() : LocalDate.now().withDayOfMonth(1));

        JTextField campoDesde = new JTextField(sugeridoDesde.toString());
        JTextField campoHasta = new JTextField(LocalDate.now().toString());
        JTextField campoRetencion = new JTextField("10.0");
        JTextField campoAmortizacion = new JTextField("0.0");
        JTextArea campoObservaciones = new JTextArea(4, 20);
        campoObservaciones.setLineWrap(true);
        campoObservaciones.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Periodo desde * (AAAA-MM-DD)"));
        form.add(campoDesde);
        form.add(new JLabel("Periodo hasta * (AAAA-MM-DD)"));
        form.add(campoHasta);
        form.add(new JLabel("% Retencion de garantia"));
        form.add(campoRetencion);
        form.add(new JLabel("Amortizacion de adelantos (S/., opcional)"));
        form.add(campoAmortizacion);
        form.add(new JLabel("Observaciones (opcional)"));
        form.add(new JScrollPane(campoObservaciones));

        int op = JOptionPane.showConfirmDialog(this, form, "Nueva valorizacion",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        LocalDate desde = parsearFecha(campoDesde.getText());
        LocalDate hasta = parsearFecha(campoHasta.getText());
        if (desde == null || hasta == null) {
            mostrarError("Las fechas del periodo son obligatorias, formato AAAA-MM-DD.");
            return;
        }
        if (hasta.isBefore(desde)) {
            mostrarError("El fin del periodo no puede ser anterior al inicio.");
            return;
        }
        Double retencion = parsearDouble(campoRetencion.getText());
        if (retencion == null || retencion < 0 || retencion > 100) {
            mostrarError("El % de retencion debe estar entre 0 y 100.");
            return;
        }
        Double amortizacion = parsearDouble(campoAmortizacion.getText());
        if (amortizacion == null || amortizacion < 0) {
            mostrarError("La amortizacion debe ser un numero mayor o igual a 0.");
            return;
        }

        try {
            Valorizacion generada = valorizacionService.generar(obra.getId(), desde, hasta,
                    retencion, amortizacion, campoObservaciones.getText().trim());
            refrescar();
            mostrarResumenGenerada(generada);
        } catch (SQLException e) {
            mostrarError("No se pudo generar la valorizacion: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarResumenGenerada(Valorizacion v) {
        String msg = String.format(
                "Valorizacion N° %d generada.%n%n"
                        + "Ejecutado en el periodo: S/. %,.2f%n"
                        + "Retencion (%.1f%%): S/. %,.2f%n"
                        + "Amortizacion de adelantos: S/. %,.2f%n"
                        + "Neto a pagar: S/. %,.2f%n%n"
                        + "Acumulado total: S/. %,.2f",
                v.getNumero(), v.getMontoEjecutadoPeriodo(), v.getPctRetencion(), v.getMontoRetencion(),
                v.getMontoAmortizacionAdelanto(), v.getMontoNetoPagar(), v.getMontoAcumuladoTotal());
        JOptionPane.showMessageDialog(this, msg, "Valorizacion generada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void eliminarSeleccionada() {
        Valorizacion v = seleccionada();
        if (v == null) {
            mostrarError("Selecciona una valorizacion de la tabla para eliminar.");
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la valorizacion N° " + v.getNumero() + "?\n\n"
                        + "Usa esto solo para corregir un error antes de presentarla: una vez entregada\n"
                        + "al cliente, la valorizacion no deberia borrarse.",
                "Eliminar valorizacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            valorizacionService.eliminar(v.getId());
            refrescar();
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar la valorizacion: " + e.getMessage());
        }
    }

    private LocalDate parsearFecha(String texto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) return null;
        try {
            return LocalDate.parse(t);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parsearDouble(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
