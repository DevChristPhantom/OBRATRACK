package com.obratrack.ui.views;

import com.obratrack.model.ApuInsumo;
import com.obratrack.model.MetradoDetalle;
import com.obratrack.model.Partida;
import com.obratrack.service.ApuCalculo;
import com.obratrack.service.IApuService;
import com.obratrack.service.IMetradoService;
import com.obratrack.service.MetradoCalculo;
import com.obratrack.service.Permisos;
import com.obratrack.service.ServiceFactory;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Detalle de una partida: metrado desagregado por sector/bloque y analisis de
 * precios unitarios (APU). Ambos son documentos de trabajo (no append-only):
 * se editan a medida que se afina el presupuesto o se corrigen datos de campo.
 */
public class PartidaDetalleDialog extends JDialog {

    private final Partida partida;
    private final IMetradoService metradoService = ServiceFactory.metrado();
    private final IApuService apuService = ServiceFactory.apu();

    // --- Metrados ---
    private final DefaultTableModel metradoModelo;
    private final JTable metradoTabla;
    private final List<MetradoDetalle> metradosActuales = new ArrayList<>();
    private final JLabel metradoResumen = new JLabel();

    // --- APU ---
    private final DefaultTableModel apuModelo;
    private final JTable apuTabla;
    private final List<ApuInsumo> apuActuales = new ArrayList<>();
    private final JLabel apuResumen = new JLabel();

    private static final String[] COLS_METRADO = {"Sector / Bloque", "Cantidad", "Observacion"};
    private static final String[] COLS_APU = {"Tipo", "Descripcion", "Unidad", "Cantidad", "Precio Unit. (S/.)", "Parcial (S/.)"};

    public PartidaDetalleDialog(Window owner, Partida partida) {
        super(owner, "Detalle de partida: " + partida.getCodigo(), ModalityType.APPLICATION_MODAL);
        this.partida = partida;
        setSize(820, 560);
        setLocationRelativeTo(owner);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBorder(new EmptyBorder(14, 16, 8, 16));
        JLabel titulo = new JLabel(partida.getCodigo() + " — " + partida.getDescripcion());
        titulo.setFont(Theme.FONT_BOLD);
        cabecera.add(titulo, BorderLayout.NORTH);
        JLabel sub = new JLabel(String.format("Presupuestado: %.2f %s   ·   Costo unitario: S/. %,.2f",
                partida.getCantidadPresupuestada(), partida.getUnidad() != null ? partida.getUnidad() : "",
                partida.getCostoUnitario()));
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_SECONDARY);
        cabecera.add(sub, BorderLayout.SOUTH);

        metradoModelo = new DefaultTableModel(COLS_METRADO, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        metradoTabla = new JTable(metradoModelo);
        metradoTabla.setRowHeight(26);
        metradoTabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        apuModelo = new DefaultTableModel(COLS_APU, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        apuTabla = new JTable(apuModelo);
        apuTabla.setRowHeight(26);
        apuTabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Metrados por sector", construirPanelMetrados());
        tabs.addTab("Analisis de precios unitarios (APU)", construirPanelApu());

        setLayout(new BorderLayout());
        add(cabecera, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        // Se difiere la carga fuera del constructor (mismo motivo que en MainWindow):
        // la ventana ya queda armada antes de tocar la base de datos.
        SwingUtilities.invokeLater(() -> {
            cargarMetrados();
            cargarApu();
        });
    }

    // ============================================================
    //  Pestaña de metrados
    // ============================================================

    private JPanel construirPanelMetrados() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnAgregar = new JButton("Agregar sector");
        btnAgregar.addActionListener(e -> agregarMetrado());
        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editarMetrado());
        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> eliminarMetrado());
        if (!Permisos.puedeEscribir()) {
            btnAgregar.setEnabled(false);
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
        barra.add(btnAgregar);
        barra.add(btnEditar);
        barra.add(btnEliminar);
        panel.add(barra, BorderLayout.NORTH);
        panel.add(new JScrollPane(metradoTabla), BorderLayout.CENTER);

        metradoResumen.setFont(Theme.FONT_BOLD);
        panel.add(metradoResumen, BorderLayout.SOUTH);
        return panel;
    }

    private void cargarMetrados() {
        metradoModelo.setRowCount(0);
        metradosActuales.clear();
        try {
            List<MetradoDetalle> lineas = metradoService.listarPorPartida(partida.getId());
            metradosActuales.addAll(lineas);
            for (MetradoDetalle m : lineas) {
                metradoModelo.addRow(new Object[]{m.getSector(), String.format("%.2f", m.getCantidad()),
                        m.getObservacion() != null ? m.getObservacion() : ""});
            }
            double total = MetradoCalculo.totalDesagregado(lineas);
            double diferencia = MetradoCalculo.diferencia(lineas, partida.getCantidadPresupuestada());
            boolean cuadra = MetradoCalculo.cuadra(lineas, partida.getCantidadPresupuestada());
            metradoResumen.setForeground(cuadra ? Theme.SUCCESS : Theme.WARNING);
            metradoResumen.setText(String.format("Total desglosado: %.2f   ·   Presupuestado: %.2f   ·   Diferencia: %.2f   ·   %s",
                    total, partida.getCantidadPresupuestada(), diferencia, cuadra ? "Cuadra" : "No cuadra"));
        } catch (SQLException e) {
            mostrarError("No se pudo cargar el metrado: " + e.getMessage());
        }
    }

    private MetradoDetalle metradoSeleccionado() {
        int fila = metradoTabla.getSelectedRow();
        if (fila < 0 || fila >= metradosActuales.size()) return null;
        return metradosActuales.get(fila);
    }

    private void agregarMetrado() {
        JTextField campoSector = new JTextField();
        JTextField campoCantidad = new JTextField("0");
        JTextField campoObs = new JTextField();
        if (!mostrarFormularioMetrado(campoSector, campoCantidad, campoObs, "Agregar linea de metrado")) return;

        Double cantidad = parsearDouble(campoCantidad.getText());
        if (campoSector.getText().trim().isEmpty() || cantidad == null) {
            mostrarError("Sector y cantidad son obligatorios (cantidad numerica).");
            return;
        }
        MetradoDetalle m = new MetradoDetalle(campoSector.getText().trim(), cantidad);
        m.setPartidaId(partida.getId());
        m.setObraId(partida.getObraId());
        m.setObservacion(campoObs.getText().trim());
        try {
            metradoService.crear(m);
            cargarMetrados();
        } catch (SQLException e) {
            mostrarError("No se pudo guardar: " + e.getMessage());
        }
    }

    private void editarMetrado() {
        MetradoDetalle m = metradoSeleccionado();
        if (m == null) { mostrarError("Selecciona una linea para editar."); return; }
        JTextField campoSector = new JTextField(m.getSector());
        JTextField campoCantidad = new JTextField(String.valueOf(m.getCantidad()));
        JTextField campoObs = new JTextField(m.getObservacion() != null ? m.getObservacion() : "");
        if (!mostrarFormularioMetrado(campoSector, campoCantidad, campoObs, "Editar linea de metrado")) return;

        Double cantidad = parsearDouble(campoCantidad.getText());
        if (campoSector.getText().trim().isEmpty() || cantidad == null) {
            mostrarError("Sector y cantidad son obligatorios (cantidad numerica).");
            return;
        }
        m.setSector(campoSector.getText().trim());
        m.setCantidad(cantidad);
        m.setObservacion(campoObs.getText().trim());
        try {
            metradoService.actualizar(m);
            cargarMetrados();
        } catch (SQLException e) {
            mostrarError("No se pudo guardar: " + e.getMessage());
        }
    }

    private void eliminarMetrado() {
        MetradoDetalle m = metradoSeleccionado();
        if (m == null) { mostrarError("Selecciona una linea para eliminar."); return; }
        int op = JOptionPane.showConfirmDialog(this, "¿Eliminar la linea \"" + m.getSector() + "\"?",
                "Eliminar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            metradoService.eliminar(m.getId());
            cargarMetrados();
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar: " + e.getMessage());
        }
    }

    private boolean mostrarFormularioMetrado(JTextField sector, JTextField cantidad, JTextField obs, String titulo) {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Sector / Bloque *"));
        form.add(sector);
        form.add(new JLabel("Cantidad *"));
        form.add(cantidad);
        form.add(new JLabel("Observacion (opcional)"));
        form.add(obs);
        int op = JOptionPane.showConfirmDialog(this, form, titulo, JOptionPane.OK_CANCEL_OPTION);
        return op == JOptionPane.OK_OPTION;
    }

    // ============================================================
    //  Pestaña de APU
    // ============================================================

    private JPanel construirPanelApu() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnAgregar = new JButton("Agregar insumo");
        btnAgregar.addActionListener(e -> agregarApu());
        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editarApu());
        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> eliminarApu());
        if (!Permisos.puedeEscribir()) {
            btnAgregar.setEnabled(false);
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
        barra.add(btnAgregar);
        barra.add(btnEditar);
        barra.add(btnEliminar);
        panel.add(barra, BorderLayout.NORTH);
        panel.add(new JScrollPane(apuTabla), BorderLayout.CENTER);

        JPanel resumenPanel = new JPanel();
        resumenPanel.setLayout(new BoxLayout(resumenPanel, BoxLayout.Y_AXIS));
        apuResumen.setFont(Theme.FONT_BOLD);
        apuResumen.setAlignmentX(Component.LEFT_ALIGNMENT);
        resumenPanel.add(apuResumen);
        panel.add(resumenPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void cargarApu() {
        apuModelo.setRowCount(0);
        apuActuales.clear();
        try {
            List<ApuInsumo> insumos = apuService.listarPorPartida(partida.getId());
            apuActuales.addAll(insumos);
            for (ApuInsumo i : insumos) {
                apuModelo.addRow(new Object[]{textoTipo(i.getTipo()), i.getDescripcion(),
                        i.getUnidad() != null ? i.getUnidad() : "", String.format("%.4f", i.getCantidad()),
                        String.format("%,.2f", i.getPrecioUnitario()), String.format("%,.2f", i.getParcial())});
            }
            double total = ApuCalculo.totalApu(insumos);
            double diferencia = ApuCalculo.diferencia(insumos, partida.getCostoUnitario());
            boolean cuadra = ApuCalculo.cuadra(insumos, partida.getCostoUnitario());
            // En dos lineas via HTML (no una sola cadena larga): con muchos insumos o un dialogo
            // angosto, una linea unica se recorta y el desglose por tipo queda ilegible.
            String resumen = String.format(
                    "<html>Total APU: S/. %,.2f &nbsp;·&nbsp; Costo unitario partida: S/. %,.2f "
                            + "&nbsp;·&nbsp; Diferencia: S/. %,.2f &nbsp;·&nbsp; %s"
                            + "<br>Mano de obra %.0f%% &nbsp; Materiales %.0f%% "
                            + "&nbsp; Equipo %.0f%% &nbsp; Subcontrato %.0f%%</html>",
                    total, partida.getCostoUnitario(), diferencia, cuadra ? "Cuadra" : "No cuadra",
                    ApuCalculo.pctPorTipo(insumos, ApuInsumo.Tipo.MANO_DE_OBRA),
                    ApuCalculo.pctPorTipo(insumos, ApuInsumo.Tipo.MATERIAL),
                    ApuCalculo.pctPorTipo(insumos, ApuInsumo.Tipo.EQUIPO),
                    ApuCalculo.pctPorTipo(insumos, ApuInsumo.Tipo.SUBCONTRATO));
            apuResumen.setForeground(cuadra ? Theme.SUCCESS : Theme.WARNING);
            apuResumen.setText(resumen);
        } catch (SQLException e) {
            mostrarError("No se pudo cargar el APU: " + e.getMessage());
        }
    }

    private ApuInsumo apuSeleccionado() {
        int fila = apuTabla.getSelectedRow();
        if (fila < 0 || fila >= apuActuales.size()) return null;
        return apuActuales.get(fila);
    }

    private void agregarApu() {
        ApuFormResultado r = mostrarFormularioApu(null);
        if (r == null) return;
        ApuInsumo i = new ApuInsumo(r.tipo, r.descripcion, r.unidad, r.cantidad, r.precioUnitario);
        i.setPartidaId(partida.getId());
        i.setObraId(partida.getObraId());
        try {
            apuService.crear(i);
            cargarApu();
        } catch (SQLException e) {
            mostrarError("No se pudo guardar: " + e.getMessage());
        }
    }

    private void editarApu() {
        ApuInsumo i = apuSeleccionado();
        if (i == null) { mostrarError("Selecciona un insumo para editar."); return; }
        ApuFormResultado r = mostrarFormularioApu(i);
        if (r == null) return;
        i.setTipo(r.tipo);
        i.setDescripcion(r.descripcion);
        i.setUnidad(r.unidad);
        i.setCantidad(r.cantidad);
        i.setPrecioUnitario(r.precioUnitario);
        try {
            apuService.actualizar(i);
            cargarApu();
        } catch (SQLException e) {
            mostrarError("No se pudo guardar: " + e.getMessage());
        }
    }

    private void eliminarApu() {
        ApuInsumo i = apuSeleccionado();
        if (i == null) { mostrarError("Selecciona un insumo para eliminar."); return; }
        int op = JOptionPane.showConfirmDialog(this, "¿Eliminar el insumo \"" + i.getDescripcion() + "\"?",
                "Eliminar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            apuService.eliminar(i.getId());
            cargarApu();
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar: " + e.getMessage());
        }
    }

    private static final class ApuFormResultado {
        ApuInsumo.Tipo tipo;
        String descripcion;
        String unidad;
        double cantidad;
        double precioUnitario;
    }

    private ApuFormResultado mostrarFormularioApu(ApuInsumo existente) {
        JComboBox<ApuInsumo.Tipo> comboTipo = new JComboBox<>(ApuInsumo.Tipo.values());
        comboTipo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                Object mostrar = (value instanceof ApuInsumo.Tipo t) ? textoTipo(t) : value;
                return super.getListCellRendererComponent(list, mostrar, index, isSelected, cellHasFocus);
            }
        });
        JTextField campoDescripcion = new JTextField();
        JTextField campoUnidad = new JTextField();
        JTextField campoCantidad = new JTextField("0");
        JTextField campoPrecio = new JTextField("0");
        if (existente != null) {
            comboTipo.setSelectedItem(existente.getTipo());
            campoDescripcion.setText(existente.getDescripcion());
            campoUnidad.setText(existente.getUnidad());
            campoCantidad.setText(String.valueOf(existente.getCantidad()));
            campoPrecio.setText(String.valueOf(existente.getPrecioUnitario()));
        }

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Tipo de insumo"));
        form.add(comboTipo);
        form.add(new JLabel("Descripcion *"));
        form.add(campoDescripcion);
        form.add(new JLabel("Unidad (hh, kg, m3, dia...)"));
        form.add(campoUnidad);
        form.add(new JLabel("Cantidad por unidad de la partida *"));
        form.add(campoCantidad);
        form.add(new JLabel("Precio unitario (S/.) *"));
        form.add(campoPrecio);

        int op = JOptionPane.showConfirmDialog(this, form,
                existente == null ? "Agregar insumo" : "Editar insumo", JOptionPane.OK_CANCEL_OPTION);
        if (op != JOptionPane.OK_OPTION) return null;

        Double cantidad = parsearDouble(campoCantidad.getText());
        Double precio = parsearDouble(campoPrecio.getText());
        if (campoDescripcion.getText().trim().isEmpty() || cantidad == null || precio == null || cantidad < 0 || precio < 0) {
            mostrarError("Descripcion, cantidad y precio unitario son obligatorios (numeros >= 0).");
            return null;
        }
        ApuFormResultado r = new ApuFormResultado();
        r.tipo = (ApuInsumo.Tipo) comboTipo.getSelectedItem();
        r.descripcion = campoDescripcion.getText().trim();
        r.unidad = campoUnidad.getText().trim();
        r.cantidad = cantidad;
        r.precioUnitario = precio;
        return r;
    }

    private String textoTipo(ApuInsumo.Tipo tipo) {
        return switch (tipo) {
            case MANO_DE_OBRA -> "Mano de obra";
            case MATERIAL -> "Material";
            case EQUIPO -> "Equipo";
            case SUBCONTRATO -> "Subcontrato";
        };
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
