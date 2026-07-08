package com.obratrack.ui.views;

import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.MovimientoAuditoria;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.MovimientoService;
import com.obratrack.service.PartidaService;
import com.obratrack.ui.Icons;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Pantalla mas usada en campo: registrar, editar y eliminar ingreso/egreso de material contra una partida. */
public class AlmacenView extends JPanel {

    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();
    private final Supplier<Obra> obraActivaProvider;
    private final Runnable alRegistrar;

    private final JLabel tituloObra = new JLabel();
    private final JComboBox<Partida> comboPartida = new JComboBox<>();
    private final JComboBox<MovimientoAlmacen.Tipo> comboTipo = new JComboBox<>(MovimientoAlmacen.Tipo.values());
    private final JTextField campoFecha = new JTextField(LocalDate.now().toString());
    private final JLabel labelUnidad = new JLabel("-");
    private final JTextField campoCantidad = new JTextField();
    private final JTextField campoCostoUnitario = new JTextField();
    private final JLabel labelTotal = new JLabel("S/. 0.00");
    private final JTextArea campoObservacion = new JTextArea(2, 20);
    private final JLabel labelMensaje = new JLabel(" ");
    private final JLabel labelFormTitulo = new JLabel("Registrar ingreso/egreso");
    private JButton btnGuardar;

    private JTable tabla;
    private final DefaultTableModel tablaModelo;
    private final List<MovimientoAlmacen> movimientosActuales = new ArrayList<>();
    private Long editandoId = null;
    private Long comboObraId = null; // obra cuyas partidas ya estan cargadas en el combo

    private static final String[] COLUMNAS = {"Fecha", "Partida", "Tipo", "Cantidad", "Costo Unit.", "Total (S/.)", "Observacion"};

    public AlmacenView(Supplier<Obra> obraActivaProvider, Runnable alRegistrar) {
        this.obraActivaProvider = obraActivaProvider;
        this.alRegistrar = alRegistrar;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Almacen — Registro diario");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        header.add(titulo, BorderLayout.WEST);
        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        header.add(tituloObra, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(16, 0));
        centro.setOpaque(false);
        centro.add(construirFormulario(), BorderLayout.WEST);

        tablaModelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(tablaModelo);
        tabla.setFont(Theme.FONT_BASE);
        tabla.setRowHeight(26);
        tabla.getTableHeader().setFont(Theme.FONT_BOLD);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel tablaPanel = new JPanel(new BorderLayout(0, 8));
        tablaPanel.setOpaque(false);
        tablaPanel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        tablaPanel.add(construirBarraTabla(), BorderLayout.SOUTH);
        centro.add(tablaPanel, BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
    }

    private JPanel construirBarraTabla() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);

        JButton btnEditar = new JButton("Editar", Icons.get("settings", 15, Theme.TEXT_SECONDARY));
        btnEditar.setIconTextGap(6);
        estilizarBotonSecundario(btnEditar);
        btnEditar.addActionListener(e -> editarSeleccionado());

        JButton btnEliminar = new JButton("Eliminar", Icons.get("delete", 15, Theme.DANGER));
        btnEliminar.setIconTextGap(6);
        estilizarBotonSecundario(btnEliminar);
        btnEliminar.setForeground(Theme.DANGER);
        btnEliminar.addActionListener(e -> eliminarSeleccionado());

        JButton btnHistorial = new JButton("Historial de cambios", Icons.get("reportes", 15, Theme.PRIMARY));
        btnHistorial.setIconTextGap(6);
        estilizarBotonSecundario(btnHistorial);
        btnHistorial.addActionListener(e -> mostrarHistorial());

        barra.add(btnEditar);
        barra.add(btnEliminar);
        barra.add(btnHistorial);
        return barra;
    }

    private JPanel construirFormulario() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(340, 0));

        labelFormTitulo.setFont(Theme.FONT_BOLD);
        labelFormTitulo.setForeground(Theme.TEXT_PRIMARY);
        labelFormTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelFormTitulo);
        panel.add(Box.createVerticalStrut(14));

        panel.add(etiqueta("Tipo"));
        estilizarCombo(comboTipo);
        panel.add(comboTipo);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Fecha (AAAA-MM-DD)"));
        estilizar(campoFecha);
        panel.add(campoFecha);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Partida / material"));
        estilizarCombo(comboPartida);
        comboPartida.addActionListener(e -> onPartidaSeleccionada());
        panel.add(comboPartida);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Unidad"));
        labelUnidad.setFont(Theme.FONT_BASE);
        labelUnidad.setForeground(Theme.TEXT_PRIMARY);
        labelUnidad.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelUnidad);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Cantidad"));
        estilizar(campoCantidad);
        campoCantidad.addKeyListener(saltarFocoEnEnter(campoCostoUnitario));
        campoCantidad.addKeyListener(recalcularAlEscribir());
        panel.add(campoCantidad);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Costo unitario real (S/.)"));
        estilizar(campoCostoUnitario);
        campoCostoUnitario.addKeyListener(recalcularAlEscribir());
        campoCostoUnitario.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) guardar();
            }
        });
        panel.add(campoCostoUnitario);
        panel.add(Box.createVerticalStrut(10));

        JPanel filaTotal = new JPanel(new BorderLayout());
        filaTotal.setOpaque(false);
        filaTotal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel lblTotalTexto = etiqueta("Total");
        labelTotal.setFont(Theme.FONT_BOLD);
        labelTotal.setForeground(Theme.TEXT_PRIMARY);
        filaTotal.add(lblTotalTexto, BorderLayout.WEST);
        filaTotal.add(labelTotal, BorderLayout.EAST);
        panel.add(filaTotal);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Nota (opcional)"));
        campoObservacion.setLineWrap(true);
        campoObservacion.setWrapStyleWord(true);
        campoObservacion.setFont(Theme.FONT_BASE);
        JScrollPane scrollObs = new JScrollPane(campoObservacion);
        scrollObs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        panel.add(scrollObs);
        panel.add(Box.createVerticalStrut(14));

        labelMensaje.setFont(Theme.FONT_BASE);
        labelMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelMensaje);
        panel.add(Box.createVerticalStrut(6));

        JPanel botones = new JPanel(new GridLayout(1, 2, 8, 0));
        botones.setOpaque(false);
        botones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JButton btnCancelar = new JButton("Cancelar", Icons.get("close", 15, Theme.TEXT_SECONDARY));
        btnCancelar.setIconTextGap(6);
        btnCancelar.setFont(Theme.FONT_BASE);
        btnCancelar.setFocusPainted(false);
        btnCancelar.addActionListener(e -> { salirModoEdicion(); limpiarFormulario(); });
        btnGuardar = new JButton("Registrar", Icons.get("add", 16, Color.WHITE));
        btnGuardar.setIconTextGap(6);
        btnGuardar.setFont(Theme.FONT_BOLD);
        btnGuardar.setBackground(Theme.ACCENT);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> guardar());
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        panel.add(botones);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void onPartidaSeleccionada() {
        Object sel = comboPartida.getSelectedItem();
        if (sel instanceof Partida p) {
            labelUnidad.setText(p.getUnidad() != null ? p.getUnidad() : "-");
            if (p.getCostoUnitario() > 0 && campoCostoUnitario.getText().isBlank()) {
                campoCostoUnitario.setText(String.valueOf(p.getCostoUnitario()));
            }
        } else {
            labelUnidad.setText("-");
        }
        recalcularTotal();
    }

    private void recalcularTotal() {
        try {
            double cantidad = parseDouble(campoCantidad.getText());
            double costo = parseDouble(campoCostoUnitario.getText());
            labelTotal.setText(String.format("S/. %,.2f", cantidad * costo));
        } catch (NumberFormatException e) {
            labelTotal.setText("S/. 0.00");
        }
    }

    /** Registra un movimiento nuevo o guarda los cambios si estamos editando. */
    private void guardar() {
        Obra obra = obraActivaProvider.get();
        if (obra == null) {
            mostrarMensaje("Selecciona una obra activa primero.", Theme.DANGER);
            return;
        }
        Object selPartida = comboPartida.getSelectedItem();
        if (!(selPartida instanceof Partida partida)) {
            mostrarMensaje("Selecciona una partida/material.", Theme.DANGER);
            return;
        }

        double cantidad;
        double costo;
        LocalDate fecha;
        try {
            cantidad = parseDouble(campoCantidad.getText());
            if (cantidad <= 0) {
                mostrarMensaje("La cantidad debe ser mayor a 0", Theme.DANGER);
                return;
            }
        } catch (NumberFormatException e) {
            mostrarMensaje("Cantidad invalida", Theme.DANGER);
            return;
        }
        try {
            costo = parseDouble(campoCostoUnitario.getText());
        } catch (NumberFormatException e) {
            mostrarMensaje("Costo unitario invalido", Theme.DANGER);
            return;
        }
        if (costo == 0) {
            int op = JOptionPane.showConfirmDialog(this,
                    "El costo unitario es S/. 0.00 (¿material donado o sin costo registrado?).\n¿Deseas registrarlo asi?",
                    "Confirmar costo en cero", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op != JOptionPane.YES_OPTION) return;
        }
        try {
            fecha = LocalDate.parse(campoFecha.getText().trim());
        } catch (Exception e) {
            mostrarMensaje("Fecha invalida. Usa el formato AAAA-MM-DD", Theme.DANGER);
            return;
        }
        if (fecha.isAfter(LocalDate.now())) {
            int op = JOptionPane.showConfirmDialog(this,
                    "La fecha " + fecha + " es futura. ¿Deseas registrarla igual?",
                    "Confirmar fecha futura", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op != JOptionPane.YES_OPTION) return;
        } else if (fecha.isBefore(LocalDate.now().minusYears(1))) {
            int op = JOptionPane.showConfirmDialog(this,
                    "La fecha " + fecha + " es de hace mas de un año. ¿Deseas registrarla igual?",
                    "Confirmar fecha antigua", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op != JOptionPane.YES_OPTION) return;
        }

        MovimientoAlmacen mov = new MovimientoAlmacen();
        mov.setObraId(obra.getId());
        mov.setPartidaId(partida.getId());
        mov.setFecha(fecha);
        mov.setTipo((MovimientoAlmacen.Tipo) comboTipo.getSelectedItem());
        mov.setCantidad(cantidad);
        mov.setCostoUnitarioReal(costo);
        mov.setObservacion(campoObservacion.getText().trim());

        try {
            if (editandoId != null) {
                mov.setId(editandoId);
                movimientoService.actualizar(mov);
                mostrarMensaje("Cambios guardados y registrados en el historial.", Theme.SUCCESS);
            } else {
                movimientoService.registrar(mov);
                mostrarMensaje("Movimiento registrado correctamente.", Theme.SUCCESS);
            }
            salirModoEdicion();
            limpiarFormulario();
            alRegistrar.run();
        } catch (SQLException e) {
            mostrarMensaje("No se pudo guardar. Verifica que la base de datos no este en uso.", Theme.DANGER);
        } catch (IllegalArgumentException e) {
            mostrarMensaje(e.getMessage(), Theme.DANGER);
        }
    }

    private MovimientoAlmacen movimientoSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= movimientosActuales.size()) return null;
        return movimientosActuales.get(fila);
    }

    private void editarSeleccionado() {
        MovimientoAlmacen m = movimientoSeleccionado();
        if (m == null) {
            mostrarMensaje("Selecciona un movimiento de la tabla para editar.", Theme.WARNING);
            return;
        }
        entrarModoEdicion(m);
    }

    private void entrarModoEdicion(MovimientoAlmacen m) {
        editandoId = m.getId();
        comboTipo.setSelectedItem(m.getTipo());
        campoFecha.setText(m.getFecha().toString());
        for (int i = 0; i < comboPartida.getItemCount(); i++) {
            Partida p = comboPartida.getItemAt(i);
            if (p != null && p.getId() != null && p.getId().equals(m.getPartidaId())) {
                comboPartida.setSelectedIndex(i);
                break;
            }
        }
        campoCantidad.setText(fmtNum(m.getCantidad()));
        campoCostoUnitario.setText(fmtNum(m.getCostoUnitarioReal()));
        campoObservacion.setText(m.getObservacion() != null ? m.getObservacion() : "");
        recalcularTotal();

        labelFormTitulo.setText("Editar movimiento");
        btnGuardar.setText("Guardar cambios");
        btnGuardar.setBackground(Theme.PRIMARY);
        String creado = m.getCreadoEn() != null ? m.getCreadoEn() : "-";
        mostrarMensaje("Editando movimiento registrado el " + creado + ".", Theme.TEXT_SECONDARY);
    }

    private void salirModoEdicion() {
        editandoId = null;
        labelFormTitulo.setText("Registrar ingreso/egreso");
        btnGuardar.setText("Registrar");
        btnGuardar.setBackground(Theme.ACCENT);
    }

    private void eliminarSeleccionado() {
        MovimientoAlmacen m = movimientoSeleccionado();
        if (m == null) {
            mostrarMensaje("Selecciona un movimiento de la tabla para eliminar.", Theme.WARNING);
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este movimiento?\n\n" + m.getFecha() + " · " + m.getPartidaCodigo()
                        + " · " + m.getTipo() + " · S/. " + String.format("%,.2f", m.getCostoTotalReal())
                        + "\n\nQuedara registrado en el historial de cambios.",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        try {
            movimientoService.eliminar(m.getId());
            salirModoEdicion();
            limpiarFormulario();
            mostrarMensaje("Movimiento eliminado y registrado en el historial.", Theme.SUCCESS);
            alRegistrar.run();
        } catch (SQLException e) {
            mostrarMensaje("No se pudo eliminar: " + e.getMessage(), Theme.DANGER);
        }
    }

    private void mostrarHistorial() {
        Obra obra = obraActivaProvider.get();
        if (obra == null) {
            mostrarMensaje("Selecciona una obra activa primero.", Theme.DANGER);
            return;
        }
        String[] cols = {"Fecha y hora", "Accion", "Detalle", "Usuario"};
        DefaultTableModel modelo = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        try {
            List<MovimientoAuditoria> historial = movimientoService.listarAuditoria(obra.getId());
            for (MovimientoAuditoria a : historial) {
                modelo.addRow(new Object[]{a.getFechaHora(), a.getAccion(), a.getDetalle(), a.getUsuario()});
            }
        } catch (SQLException e) {
            mostrarMensaje("No se pudo cargar el historial: " + e.getMessage(), Theme.DANGER);
            return;
        }

        JTable tablaHist = new JTable(modelo);
        tablaHist.setFont(Theme.FONT_BASE);
        tablaHist.setRowHeight(24);
        tablaHist.getTableHeader().setFont(Theme.FONT_BOLD);
        tablaHist.getColumnModel().getColumn(0).setPreferredWidth(140);
        tablaHist.getColumnModel().getColumn(2).setPreferredWidth(420);

        JScrollPane scroll = new JScrollPane(tablaHist);
        scroll.setPreferredSize(new Dimension(820, 380));

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Historial de cambios — " + obra.getNombre(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setLayout(new BorderLayout(0, 8));
        JLabel info = new JLabel("  Registro de creaciones, ediciones y eliminaciones (no se puede alterar).");
        info.setFont(Theme.FONT_BASE);
        dialog.getContentPane().add(info, BorderLayout.NORTH);
        dialog.getContentPane().add(scroll, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        tablaModelo.setRowCount(0);
        movimientosActuales.clear();
        salirModoEdicion();

        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa");
            comboPartida.removeAllItems();
            comboObraId = null;
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre());

        try {
            // El combo de partidas solo se reconstruye si cambio la obra (son cientos de items).
            if (!java.util.Objects.equals(comboObraId, obra.getId())) {
                comboPartida.removeAllItems();
                List<Partida> partidas = partidaService.listarEjecutablesPorObra(obra.getId());
                for (Partida p : partidas) comboPartida.addItem(p);
                comboObraId = obra.getId();
                mostrarMensaje(partidas.isEmpty()
                        ? "Esta obra no tiene partidas importadas todavia. Ve a 'Obras' y carga su Excel de presupuesto."
                        : " ", partidas.isEmpty() ? Theme.WARNING : Theme.TEXT_SECONDARY);
            }

            List<MovimientoAlmacen> movimientos = movimientoService.listarPorObra(obra.getId());
            for (MovimientoAlmacen m : movimientos) {
                movimientosActuales.add(m);
                tablaModelo.addRow(new Object[]{
                        m.getFecha(),
                        m.getPartidaCodigo() + " - " + m.getPartidaDescripcion(),
                        m.getTipo(),
                        m.getCantidad(),
                        String.format("%,.2f", m.getCostoUnitarioReal()),
                        String.format("%,.2f", m.getCostoTotalReal()),
                        m.getObservacion()
                });
            }
        } catch (SQLException e) {
            mostrarMensaje("No se pudieron cargar los datos: " + e.getMessage(), Theme.DANGER);
        }
    }

    private void limpiarFormulario() {
        campoCantidad.setText("");
        campoCostoUnitario.setText("");
        campoObservacion.setText("");
        campoFecha.setText(LocalDate.now().toString());
        labelTotal.setText("S/. 0.00");
        comboTipo.setSelectedIndex(0);
    }

    private void mostrarMensaje(String texto, Color color) {
        labelMensaje.setText(texto);
        labelMensaje.setForeground(color);
    }

    private double parseDouble(String texto) throws NumberFormatException {
        if (texto == null || texto.isBlank()) return 0;
        return Double.parseDouble(texto.trim().replace(",", "."));
    }

    private String fmtNum(double v) {
        return (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // ---------- helpers UI ----------

    private KeyAdapter saltarFocoEnEnter(Component siguiente) {
        return new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) siguiente.requestFocus();
            }
        };
    }

    private KeyAdapter recalcularAlEscribir() {
        return new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { recalcularTotal(); }
        };
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Theme.FONT_BASE);
        l.setForeground(Theme.TEXT_SECONDARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void estilizar(JTextField campo) {
        campo.setFont(Theme.FONT_BASE);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void estilizarCombo(JComboBox<?> combo) {
        combo.setFont(Theme.FONT_BASE);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void estilizarBotonSecundario(JButton btn) {
        btn.setFont(Theme.FONT_BASE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
