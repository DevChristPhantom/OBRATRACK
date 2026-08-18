package com.obratrack.ui.views;

import com.obratrack.model.AdicionalDeductivo;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.IAdicionalDeductivoService;
import com.obratrack.service.IObraService;
import com.obratrack.service.IPartidaService;
import com.obratrack.service.Permisos;
import com.obratrack.service.PresupuestoAnaliticoCalculo;
import com.obratrack.service.ServiceFactory;
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
 * Presupuesto analitico: costo directo (partidas) + gastos generales + utilidad + IGV =
 * presupuesto contractual; mas los adicionales/deductivos aprobados = presupuesto
 * actualizado vigente. Los porcentajes de GG/utilidad/IGV son datos de la obra
 * (editables aqui); los adicionales/deductivos tienen su propio CRUD append-only.
 */
public class PresupuestoAnaliticoView extends JPanel {

    private final IObraService obraService = ServiceFactory.obra();
    private final IPartidaService partidaService = ServiceFactory.partida();
    private final IAdicionalDeductivoService adicionalService = ServiceFactory.adicionalDeductivo();
    private final Supplier<Obra> obraActivaProvider;

    private final JLabel tituloObra = new JLabel();
    private final JLabel[] valoresResumen = new JLabel[9];
    private final DefaultTableModel tablaModelo;
    private final JTable tabla;
    private final List<AdicionalDeductivo> itemsActuales = new ArrayList<>();
    private Obra obraCargada;

    private static final String[] ETIQUETAS_RESUMEN = {
            "Costo directo (partidas)", "Gastos generales", "Utilidad", "Subtotal",
            "IGV", "Presupuesto contractual", "Adicionales", "Deductivos", "Presupuesto actualizado"
    };
    private static final String[] COLUMNAS = {"N°", "Tipo", "Descripcion", "Monto (S/.)", "Aprobacion", "Resolucion"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PresupuestoAnaliticoView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Presupuesto Analitico");
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

    /** Grilla de dos columnas (etiqueta + valor) con el desglose contractual. */
    private JPanel construirResumen() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JPanel grilla = new JPanel(new GridLayout(0, 2, 16, 4));
        grilla.setOpaque(false);
        for (int i = 0; i < ETIQUETAS_RESUMEN.length; i++) {
            JLabel etiqueta = new JLabel(ETIQUETAS_RESUMEN[i] + ":");
            etiqueta.setFont(Theme.FONT_BASE);
            etiqueta.setForeground(Theme.TEXT_SECONDARY);
            grilla.add(etiqueta);

            valoresResumen[i] = new JLabel("S/. 0.00");
            valoresResumen[i].setFont(Theme.FONT_BOLD);
            valoresResumen[i].setForeground(Theme.TEXT_PRIMARY);
            grilla.add(valoresResumen[i]);
        }
        panel.add(grilla, BorderLayout.CENTER);

        JButton btnEditarPct = new JButton("Editar % contractuales", Icons.get("settings", 15, Theme.TEXT_SECONDARY));
        btnEditarPct.setIconTextGap(6);
        btnEditarPct.setFont(Theme.FONT_BASE);
        btnEditarPct.setFocusPainted(false);
        btnEditarPct.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEditarPct.addActionListener(e -> editarPorcentajes());
        if (!Permisos.puedeGestionarObras()) btnEditarPct.setEnabled(false);
        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaBoton.setOpaque(false);
        filaBoton.add(btnEditarPct);
        panel.add(filaBoton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construirBarra() {
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        JButton btnNuevo = new JButton("Nuevo adicional/deductivo", Icons.get("add", 16, Color.WHITE));
        btnNuevo.setIconTextGap(6);
        btnNuevo.setFont(Theme.FONT_BOLD);
        btnNuevo.setBackground(Theme.ACCENT);
        btnNuevo.setForeground(Color.WHITE);
        btnNuevo.setFocusPainted(false);
        btnNuevo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNuevo.addActionListener(e -> nuevoAdicionalDeductivo());
        JButton btnEliminar = new JButton("Eliminar", Icons.get("delete", 15, Theme.DANGER));
        btnEliminar.setIconTextGap(6);
        btnEliminar.setForeground(Theme.DANGER);
        btnEliminar.setFont(Theme.FONT_BASE);
        btnEliminar.setFocusPainted(false);
        btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEliminar.addActionListener(e -> eliminarSeleccionado());
        if (!Permisos.puedeEscribir()) {
            btnNuevo.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
        botones.add(btnNuevo);
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
        itemsActuales.clear();
        obraCargada = obra;

        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa para ver su presupuesto analitico");
            limpiarResumen();
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre());

        try {
            List<Partida> partidas = partidaService.listarPorObra(obra.getId());
            double costoDirecto = partidas.stream()
                    .filter(p -> !p.isEsPadre())
                    .mapToDouble(Partida::getCostoTotalPresupuestado)
                    .sum();
            List<AdicionalDeductivo> items = adicionalService.listarPorObra(obra.getId());
            itemsActuales.addAll(items);

            for (AdicionalDeductivo ad : items) {
                tablaModelo.addRow(new Object[]{
                        ad.getNumero(), ad.getTipo(), ad.getDescripcion(),
                        String.format("%,.2f", ad.getMonto()),
                        ad.getFechaAprobacion() != null ? ad.getFechaAprobacion().format(FMT) : "",
                        ad.getResolucionAprobacion() != null ? ad.getResolucionAprobacion() : ""
                });
            }

            PresupuestoAnaliticoCalculo.Resultado r = PresupuestoAnaliticoCalculo.calcular(
                    costoDirecto, obra.getPctGastosGenerales(), obra.getPctUtilidad(), obra.getPctIgv(), items);
            actualizarResumen(r);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el presupuesto analitico: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarResumen(PresupuestoAnaliticoCalculo.Resultado r) {
        valoresResumen[0].setText(String.format("S/. %,.2f", r.costoDirecto));
        valoresResumen[1].setText(String.format("S/. %,.2f (%.1f%%)", r.montoGastosGenerales, r.pctGastosGenerales));
        valoresResumen[2].setText(String.format("S/. %,.2f (%.1f%%)", r.montoUtilidad, r.pctUtilidad));
        valoresResumen[3].setText(String.format("S/. %,.2f", r.subtotal));
        valoresResumen[4].setText(String.format("S/. %,.2f (%.1f%%)", r.montoIgv, r.pctIgv));
        valoresResumen[5].setText(String.format("S/. %,.2f", r.presupuestoContractual));
        valoresResumen[6].setText(String.format("S/. %,.2f", r.totalAdicionales));
        valoresResumen[7].setText(String.format("S/. %,.2f", r.totalDeductivos));
        valoresResumen[8].setText(String.format("S/. %,.2f", r.presupuestoActualizado));
    }

    private void limpiarResumen() {
        for (JLabel lbl : valoresResumen) lbl.setText("S/. 0.00");
    }

    // ============================================================
    //  Alta / eliminacion de adicionales-deductivos
    // ============================================================

    private AdicionalDeductivo seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return null;
        int modelRow = tabla.convertRowIndexToModel(fila);
        if (modelRow < 0 || modelRow >= itemsActuales.size()) return null;
        return itemsActuales.get(modelRow);
    }

    private void nuevoAdicionalDeductivo() {
        Obra obra = obraActivaProvider.get();
        if (obra == null) {
            mostrarError("Selecciona una obra activa primero.");
            return;
        }

        JComboBox<AdicionalDeductivo.Tipo> campoTipo = new JComboBox<>(AdicionalDeductivo.Tipo.values());
        JTextArea campoDescripcion = new JTextArea(3, 20);
        campoDescripcion.setLineWrap(true);
        campoDescripcion.setWrapStyleWord(true);
        JTextField campoMonto = new JTextField("0.00");
        JTextField campoFecha = new JTextField(LocalDate.now().toString());
        JTextField campoResolucion = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Tipo *"));
        form.add(campoTipo);
        form.add(new JLabel("Descripcion *"));
        form.add(new JScrollPane(campoDescripcion));
        form.add(new JLabel("Monto (S/.) *"));
        form.add(campoMonto);
        form.add(new JLabel("Fecha de aprobacion * (AAAA-MM-DD)"));
        form.add(campoFecha);
        form.add(new JLabel("Resolucion de aprobacion (opcional)"));
        form.add(campoResolucion);

        int op = JOptionPane.showConfirmDialog(this, form, "Nuevo adicional / deductivo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        String descripcion = campoDescripcion.getText().trim();
        if (descripcion.isEmpty()) {
            mostrarError("La descripcion es obligatoria.");
            return;
        }
        Double monto = parsearDouble(campoMonto.getText());
        if (monto == null || monto < 0) {
            mostrarError("El monto debe ser un numero mayor o igual a 0.");
            return;
        }
        LocalDate fecha = parsearFecha(campoFecha.getText());
        if (fecha == null) {
            mostrarError("La fecha de aprobacion es obligatoria, formato AAAA-MM-DD.");
            return;
        }

        AdicionalDeductivo ad = new AdicionalDeductivo();
        ad.setObraId(obra.getId());
        ad.setTipo((AdicionalDeductivo.Tipo) campoTipo.getSelectedItem());
        ad.setDescripcion(descripcion);
        ad.setMonto(monto);
        ad.setFechaAprobacion(fecha);
        String resolucion = campoResolucion.getText().trim();
        ad.setResolucionAprobacion(resolucion.isEmpty() ? null : resolucion);

        try {
            adicionalService.crear(ad);
            refrescar();
        } catch (SQLException e) {
            mostrarError("No se pudo guardar: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void eliminarSeleccionado() {
        AdicionalDeductivo ad = seleccionado();
        if (ad == null) {
            mostrarError("Selecciona un adicional/deductivo de la tabla para eliminar.");
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el " + ad.getTipo() + " N° " + ad.getNumero() + "?\n\n"
                        + "Usa esto solo para corregir un error de registro.",
                "Eliminar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            adicionalService.eliminar(ad.getId());
            refrescar();
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar: " + e.getMessage());
        }
    }

    // ============================================================
    //  Porcentajes contractuales (gastos generales / utilidad / IGV)
    // ============================================================

    private void editarPorcentajes() {
        if (obraCargada == null) {
            mostrarError("Selecciona una obra activa primero.");
            return;
        }
        JTextField campoGG = new JTextField(String.valueOf(obraCargada.getPctGastosGenerales()));
        JTextField campoUtilidad = new JTextField(String.valueOf(obraCargada.getPctUtilidad()));
        JTextField campoIgv = new JTextField(String.valueOf(obraCargada.getPctIgv()));

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("% Gastos generales"));
        form.add(campoGG);
        form.add(new JLabel("% Utilidad"));
        form.add(campoUtilidad);
        form.add(new JLabel("% IGV"));
        form.add(campoIgv);

        int op = JOptionPane.showConfirmDialog(this, form, "Editar porcentajes contractuales",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        Double gg = parsearDouble(campoGG.getText());
        Double utilidad = parsearDouble(campoUtilidad.getText());
        Double igv = parsearDouble(campoIgv.getText());
        if (gg == null || utilidad == null || igv == null || gg < 0 || utilidad < 0 || igv < 0) {
            mostrarError("Los porcentajes deben ser numeros mayores o iguales a 0.");
            return;
        }

        obraCargada.setPctGastosGenerales(gg);
        obraCargada.setPctUtilidad(utilidad);
        obraCargada.setPctIgv(igv);
        try {
            obraService.actualizar(obraCargada);
            refrescar();
        } catch (SQLException e) {
            mostrarError("No se pudo guardar: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
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
