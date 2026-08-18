package com.obratrack.ui.views;

import com.obratrack.model.MonomioPolinomico;
import com.obratrack.model.Obra;
import com.obratrack.service.FormulaPolinomicaCalculo;
import com.obratrack.service.IFormulaPolinomicaService;
import com.obratrack.service.Permisos;
import com.obratrack.service.ServiceFactory;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Formula polinomica de reajuste de precios: elementos (mano de obra, materiales,
 * equipo...) con su coeficiente de incidencia e indice base, y una calculadora
 * para el factor de reajuste Fr = suma(coeficiente x indiceRevisado / indiceBase)
 * a partir de los indices INEI del mes que corresponda.
 */
public class FormulaPolinomicaView extends JPanel {

    private final IFormulaPolinomicaService formulaService = ServiceFactory.formulaPolinomica();
    private final Supplier<Obra> obraActivaProvider;

    private final JLabel tituloObra = new JLabel();
    private final JLabel resumenCoeficientes = new JLabel();
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final List<MonomioPolinomico> actuales = new ArrayList<>();

    private final JTextField campoMontoBase = new JTextField("0.00");
    private final JLabel resultadoLabel = new JLabel(" ");

    private static final String[] COLS = {"Elemento", "Coeficiente %", "Indice base (I0)", "Indice revisado (Ir)"};
    private static final int COL_INDICE_REVISADO = 3;

    public FormulaPolinomicaView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Formula Polinomica");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        header.add(titulo, BorderLayout.WEST);
        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        header.add(tituloObra, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(construirBarra(), BorderLayout.NORTH);

        modelo = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == COL_INDICE_REVISADO; }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), construirCalculadora());
        split.setResizeWeight(0.6);
        split.setBorder(null);
        centro.add(split, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }

    private JPanel construirBarra() {
        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        JButton btnAgregar = new JButton("Agregar elemento");
        btnAgregar.setFont(Theme.FONT_BOLD);
        btnAgregar.setBackground(Theme.ACCENT);
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFocusPainted(false);
        btnAgregar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAgregar.addActionListener(e -> agregarMonomio());
        JButton btnEditar = new JButton("Editar");
        btnEditar.setFocusPainted(false);
        btnEditar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEditar.addActionListener(e -> editarMonomio());
        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.setForeground(Theme.DANGER);
        btnEliminar.setFocusPainted(false);
        btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEliminar.addActionListener(e -> eliminarMonomio());
        if (!Permisos.puedeEscribir()) {
            btnAgregar.setEnabled(false);
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
        botones.add(btnAgregar);
        botones.add(btnEditar);
        botones.add(btnEliminar);
        wrap.add(botones, BorderLayout.NORTH);

        resumenCoeficientes.setFont(Theme.FONT_BOLD);
        wrap.add(resumenCoeficientes, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel construirCalculadora() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 0, 0, 0));
        panel.setOpaque(false);

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila.setOpaque(false);
        JLabel lbl = new JLabel("Monto base a reajustar (S/.):");
        lbl.setFont(Theme.FONT_BASE);
        campoMontoBase.setPreferredSize(new Dimension(140, 28));
        JButton btnCalcular = new JButton("Calcular reajuste");
        btnCalcular.setFont(Theme.FONT_BOLD);
        btnCalcular.setFocusPainted(false);
        btnCalcular.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCalcular.addActionListener(e -> calcular());
        fila.add(lbl);
        fila.add(campoMontoBase);
        fila.add(btnCalcular);
        panel.add(fila, BorderLayout.NORTH);

        resultadoLabel.setFont(Theme.FONT_BOLD);
        resultadoLabel.setForeground(Theme.TEXT_SECONDARY);
        panel.add(resultadoLabel, BorderLayout.CENTER);
        return panel;
    }

    // ============================================================
    //  Carga y calculo
    // ============================================================

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        modelo.setRowCount(0);
        actuales.clear();
        resultadoLabel.setText(" ");

        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa para ver su formula polinomica");
            resumenCoeficientes.setText("");
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre());

        try {
            List<MonomioPolinomico> monomios = formulaService.listarPorObra(obra.getId());
            actuales.addAll(monomios);
            for (MonomioPolinomico m : monomios) {
                modelo.addRow(new Object[]{
                        m.getDescripcion(),
                        String.format("%.2f", m.getCoeficienteIncidencia() * 100),
                        String.format("%.4f", m.getIndiceBase()),
                        String.format("%.4f", m.getIndiceBase()) // por defecto, el revisado arranca igual al base
                });
            }
            actualizarResumenCoeficientes(monomios);
        } catch (SQLException e) {
            mostrarError("No se pudo cargar la formula polinomica: " + e.getMessage());
        }
    }

    private void actualizarResumenCoeficientes(List<MonomioPolinomico> monomios) {
        double suma = FormulaPolinomicaCalculo.sumaCoeficientes(monomios) * 100;
        boolean cuadra = FormulaPolinomicaCalculo.coeficientesCuadran(monomios);
        resumenCoeficientes.setForeground(cuadra ? Theme.SUCCESS : Theme.WARNING);
        resumenCoeficientes.setText(String.format("Suma de coeficientes: %.2f%%   ·   %s",
                suma, cuadra ? "Cuadra con 100%" : "No cuadra: debe sumar 100%"));
    }

    private void calcular() {
        if (actuales.isEmpty()) {
            mostrarError("Agrega al menos un elemento antes de calcular.");
            return;
        }
        Double montoBase = parsearDouble(campoMontoBase.getText());
        if (montoBase == null || montoBase < 0) {
            mostrarError("El monto base debe ser un numero mayor o igual a 0.");
            return;
        }

        Map<Long, Double> indicesRevisados = new HashMap<>();
        for (int fila = 0; fila < actuales.size(); fila++) {
            Double ir = parsearDouble(String.valueOf(modelo.getValueAt(fila, COL_INDICE_REVISADO)));
            if (ir == null) {
                mostrarError("El indice revisado de \"" + actuales.get(fila).getDescripcion() + "\" no es un numero valido.");
                return;
            }
            indicesRevisados.put(actuales.get(fila).getId(), ir);
        }

        double factor = FormulaPolinomicaCalculo.calcularFactorReajuste(actuales, indicesRevisados);
        double montoReajustado = FormulaPolinomicaCalculo.calcularMontoReajustado(montoBase, factor);
        double diferencia = montoReajustado - montoBase;

        boolean cuadra = FormulaPolinomicaCalculo.coeficientesCuadran(actuales);
        resultadoLabel.setForeground(cuadra ? Theme.TEXT_PRIMARY : Theme.WARNING);
        resultadoLabel.setText(String.format(
                "<html>Factor de reajuste Fr = %.4f &nbsp;·&nbsp; Monto reajustado: S/. %,.2f "
                        + "&nbsp;·&nbsp; Diferencia por reajuste: S/. %,.2f%s</html>",
                factor, montoReajustado, diferencia,
                cuadra ? "" : " &nbsp;·&nbsp; (los coeficientes no suman 100%, revisa la formula)"));
    }

    // ============================================================
    //  Alta / edicion / eliminacion de monomios
    // ============================================================

    private MonomioPolinomico seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= actuales.size()) return null;
        return actuales.get(fila);
    }

    private void agregarMonomio() {
        Obra obra = obraActivaProvider.get();
        if (obra == null) { mostrarError("Selecciona una obra activa primero."); return; }
        MonomioPolinomico nuevo = new MonomioPolinomico();
        nuevo.setObraId(obra.getId());
        if (mostrarFormulario(nuevo, "Agregar elemento")) {
            try {
                formulaService.crear(nuevo);
                refrescar();
            } catch (SQLException e) {
                mostrarError("No se pudo guardar: " + e.getMessage());
            }
        }
    }

    private void editarMonomio() {
        MonomioPolinomico m = seleccionado();
        if (m == null) { mostrarError("Selecciona un elemento de la tabla."); return; }
        if (mostrarFormulario(m, "Editar elemento")) {
            try {
                formulaService.actualizar(m);
                refrescar();
            } catch (SQLException e) {
                mostrarError("No se pudo guardar: " + e.getMessage());
            }
        }
    }

    private void eliminarMonomio() {
        MonomioPolinomico m = seleccionado();
        if (m == null) { mostrarError("Selecciona un elemento de la tabla."); return; }
        int op = JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + m.getDescripcion() + "\"?",
                "Eliminar elemento", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            formulaService.eliminar(m.getId());
            refrescar();
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar: " + e.getMessage());
        }
    }

    private boolean mostrarFormulario(MonomioPolinomico m, String titulo) {
        JTextField campoDescripcion = new JTextField(m.getDescripcion() != null ? m.getDescripcion() : "");
        JTextField campoCoeficiente = new JTextField(
                m.getId() != null ? String.valueOf(m.getCoeficienteIncidencia() * 100) : "0");
        JTextField campoIndiceBase = new JTextField(m.getId() != null ? String.valueOf(m.getIndiceBase()) : "100");

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Elemento * (ej. Mano de obra, Cemento, Acero, Equipo)"));
        form.add(campoDescripcion);
        form.add(new JLabel("Coeficiente de incidencia % *"));
        form.add(campoCoeficiente);
        form.add(new JLabel("Indice base I0 (del mes de la propuesta) *"));
        form.add(campoIndiceBase);

        int op = JOptionPane.showConfirmDialog(this, form, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return false;

        if (campoDescripcion.getText().trim().isEmpty()) { mostrarError("El elemento es obligatorio."); return false; }
        Double coefPct = parsearDouble(campoCoeficiente.getText());
        if (coefPct == null || coefPct < 0 || coefPct > 100) { mostrarError("El coeficiente debe estar entre 0 y 100."); return false; }
        Double indiceBase = parsearDouble(campoIndiceBase.getText());
        if (indiceBase == null || indiceBase <= 0) { mostrarError("El indice base debe ser mayor a 0."); return false; }

        m.setDescripcion(campoDescripcion.getText().trim());
        m.setCoeficienteIncidencia(coefPct / 100.0);
        m.setIndiceBase(indiceBase);
        return true;
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
