package com.obratrack.ui.views;

import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.ExcelImporter;
import com.obratrack.service.ExcelImporter.HojaImportable;
import com.obratrack.service.IObraService;
import com.obratrack.service.IPartidaService;
import com.obratrack.service.ImportResult;
import com.obratrack.service.Permisos;
import com.obratrack.service.ServiceFactory;
import com.obratrack.ui.Icons;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Vista de gestion de obras: crear nueva obra y cargar su Excel de presupuesto. */
public final class ObrasView extends JPanel {

    private final IObraService obraService = ServiceFactory.obra();
    private final IPartidaService partidaService = ServiceFactory.partida();
    private final ExcelImporter excelImporter = new ExcelImporter();

    private final Consumer<Obra> alCrearObra;
    private final Runnable alModificarObras;

    private final JTextField campoNombre = new JTextField();
    private final JTextArea campoDescripcion = new JTextArea(3, 20);
    private final JTextField campoFechaInicio = new JTextField(LocalDate.now().toString());
    private final JTextField campoFechaFin = new JTextField();
    private final JTextField campoRutaExcel = new JTextField();
    private final JTextField campoUbicacion = new JTextField();
    private final JTextField campoEntidad = new JTextField();
    private final JComboBox<String> campoModalidad = new JComboBox<>(OPCIONES_MODALIDAD);
    private final JTextArea campoSectores = new JTextArea(3, 20);

    /** Primer elemento = "sin definir" (modalidad opcional); el resto son los valores del enum. */
    private static final String SIN_DEFINIR = "(sin definir)";
    private static final String[] OPCIONES_MODALIDAD = construirOpcionesModalidad();

    private static String[] construirOpcionesModalidad() {
        Obra.ModalidadEjecucion[] valores = Obra.ModalidadEjecucion.values();
        String[] opciones = new String[valores.length + 1];
        opciones[0] = SIN_DEFINIR;
        for (int i = 0; i < valores.length; i++) opciones[i + 1] = valores[i].name();
        return opciones;
    }
    private final List<Obra> obrasActuales = new ArrayList<>();
    private JTable tabla;
    private DefaultTableModel tablaModelo;

    public ObrasView(Consumer<Obra> alCrearObra, Runnable alModificarObras) {
        this.alCrearObra = alCrearObra;
        this.alModificarObras = alModificarObras;
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Obras");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 20, 0));
        centro.setOpaque(false);
        centro.add(construirFormularioNuevaObra());
        centro.add(construirListadoObras());
        add(centro, BorderLayout.CENTER);

        cargarListado();
    }

    private JPanel construirFormularioNuevaObra() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        panel.add(seccionTitulo("Nueva obra"));
        panel.add(Box.createVerticalStrut(12));

        panel.add(etiqueta("Nombre de la obra *"));
        estilizar(campoNombre);
        panel.add(campoNombre);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Descripcion"));
        campoDescripcion.setLineWrap(true);
        campoDescripcion.setWrapStyleWord(true);
        campoDescripcion.setFont(Theme.FONT_BASE);
        JScrollPane scrollDesc = new JScrollPane(campoDescripcion);
        scrollDesc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.add(scrollDesc);
        panel.add(Box.createVerticalStrut(10));

        JPanel filaFechas = new JPanel(new GridLayout(1, 2, 10, 0));
        filaFechas.setOpaque(false);
        filaFechas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        filaFechas.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel colInicio = new JPanel();
        colInicio.setLayout(new BoxLayout(colInicio, BoxLayout.Y_AXIS));
        colInicio.setOpaque(false);
        colInicio.add(etiqueta("Fecha de inicio (AAAA-MM-DD)"));
        estilizar(campoFechaInicio);
        colInicio.add(campoFechaInicio);

        JPanel colFin = new JPanel();
        colFin.setLayout(new BoxLayout(colFin, BoxLayout.Y_AXIS));
        colFin.setOpaque(false);
        colFin.add(etiqueta("Fecha fin estimada"));
        estilizar(campoFechaFin);
        campoFechaFin.setToolTipText("Necesaria para el semaforo de ritmo de gasto (opcional).");
        colFin.add(campoFechaFin);

        filaFechas.add(colInicio);
        filaFechas.add(colFin);
        panel.add(filaFechas);
        panel.add(Box.createVerticalStrut(10));

        panel.add(seccionTitulo("Memoria descriptiva"));
        panel.add(Box.createVerticalStrut(8));

        panel.add(etiqueta("Ubicacion (distrito, provincia, departamento)"));
        estilizar(campoUbicacion);
        panel.add(campoUbicacion);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Entidad contratante"));
        estilizar(campoEntidad);
        panel.add(campoEntidad);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Modalidad de ejecucion"));
        campoModalidad.setAlignmentX(Component.LEFT_ALIGNMENT);
        campoModalidad.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        panel.add(campoModalidad);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Sectores / bloques"));
        campoSectores.setLineWrap(true);
        campoSectores.setWrapStyleWord(true);
        campoSectores.setFont(Theme.FONT_BASE);
        JScrollPane scrollSectores = new JScrollPane(campoSectores);
        scrollSectores.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.add(scrollSectores);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Excel de presupuesto (.xlsx / .xls)"));
        JPanel filaArchivo = new JPanel(new BorderLayout(8, 0));
        filaArchivo.setOpaque(false);
        estilizar(campoRutaExcel);
        campoRutaExcel.setEditable(false);
        JButton btnElegir = new JButton("Elegir archivo...", Icons.get("search", 15, Theme.TEXT_SECONDARY));
        btnElegir.setIconTextGap(6);
        estilizarBotonSecundario(btnElegir);
        btnElegir.addActionListener(e -> elegirArchivoExcel());
        filaArchivo.add(campoRutaExcel, BorderLayout.CENTER);
        filaArchivo.add(btnElegir, BorderLayout.EAST);
        filaArchivo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        panel.add(filaArchivo);
        panel.add(Box.createVerticalStrut(18));

        JButton btnCrear = new JButton("Crear obra", Icons.get("add", 17, Color.WHITE));
        btnCrear.setIconTextGap(8);
        estilizarBotonPrimario(btnCrear);
        btnCrear.addActionListener(e -> crearObra());
        if (!Permisos.puedeGestionarObras()) {
            btnCrear.setEnabled(false);
            btnCrear.setToolTipText("Requiere permiso de administrador o jefe de obra");
        }
        panel.add(btnCrear);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel construirListadoObras() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Theme.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.add(seccionTitulo("Obras registradas"), BorderLayout.NORTH);

        String[] columnas = {"Nombre", "Estado", "Presupuesto (S/.)", "Fecha creacion"};
        tablaModelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(tablaModelo);
        tabla.setFont(Theme.FONT_BASE);
        tabla.setRowHeight(26);
        tabla.getTableHeader().setFont(Theme.FONT_BOLD);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        JButton btnEditar = new JButton("Editar", Icons.get("settings", 15, Theme.TEXT_SECONDARY));
        btnEditar.setIconTextGap(6);
        estilizarBotonSecundario(btnEditar);
        btnEditar.addActionListener(e -> editarObraSeleccionada());
        JButton btnEliminar = new JButton("Eliminar", Icons.get("delete", 15, Theme.DANGER));
        btnEliminar.setIconTextGap(6);
        estilizarBotonSecundario(btnEliminar);
        btnEliminar.setForeground(Theme.DANGER);
        btnEliminar.addActionListener(e -> eliminarObraSeleccionada());
        if (!Permisos.puedeGestionarObras()) {
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
        barra.add(btnEditar);
        barra.add(btnEliminar);
        panel.add(barra, BorderLayout.SOUTH);

        return panel;
    }

    private void elegirArchivoExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos Excel (*.xlsx, *.xls)", "xlsx", "xls"));
        int resultado = chooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            campoRutaExcel.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void crearObra() {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarError("El nombre de la obra es obligatorio.");
            return;
        }

        String rutaExcel = campoRutaExcel.getText().trim();
        ImportResult importResult = null;

        if (!rutaExcel.isEmpty()) {
            String hojaElegida;
            try {
                hojaElegida = elegirHojaAImportar(rutaExcel);
            } catch (IOException ex) {
                mostrarError("No se pudo leer el archivo Excel: " + ex.getMessage());
                return;
            }
            if (SELECCION_CANCELADA.equals(hojaElegida)) {
                return; // el usuario cerro el dialogo de seleccion de hoja
            }

            importResult = excelImporter.importar(rutaExcel, hojaElegida);
            if (!importResult.isExitoso()) {
                StringBuilder msg = new StringBuilder("No se pudo importar el Excel:\n\n");
                for (String err : importResult.getErrores()) msg.append("• ").append(err).append("\n");
                msg.append("\n¿Deseas crear la obra de todas formas, sin partidas? Podras importar el Excel despues.");
                int op = JOptionPane.showConfirmDialog(this, msg.toString(), "Error de importacion",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (op != JOptionPane.YES_OPTION) return;
                importResult = null;
            }
        }

        LocalDate fechaInicio = parsearFecha(campoFechaInicio.getText(), LocalDate.now());
        if (fechaInicio == null) {
            mostrarError("Fecha de inicio invalida. Usa el formato AAAA-MM-DD.");
            return;
        }
        LocalDate fechaFin = parsearFecha(campoFechaFin.getText(), null);
        if (!campoFechaFin.getText().trim().isEmpty() && fechaFin == null) {
            mostrarError("Fecha fin invalida. Usa el formato AAAA-MM-DD o dejala vacia.");
            return;
        }
        if (fechaFin != null && !fechaFin.isAfter(fechaInicio)) {
            mostrarError("La fecha fin estimada debe ser posterior a la fecha de inicio.");
            return;
        }

        try {
            Obra obra = new Obra(nombre, campoDescripcion.getText().trim(), fechaInicio, fechaFin);
            obra.setRutaExcelOrigen(rutaExcel.isEmpty() ? null : rutaExcel);
            obra.setUbicacion(vacioComoNull(campoUbicacion.getText()));
            obra.setEntidadContratante(vacioComoNull(campoEntidad.getText()));
            obra.setModalidadEjecucion(modalidadSeleccionada(campoModalidad));
            obra.setSectoresBloques(vacioComoNull(campoSectores.getText()));
            obraService.crear(obra);

            if (importResult != null) {
                List<Partida> partidas = importResult.getPartidasImportadas();
                partidaService.guardarTodas(obra.getId(), partidas);
                obraService.actualizarPresupuestoTotal(obra.getId(), importResult.getPresupuestoTotal());
                obra.setPresupuestoTotal(importResult.getPresupuestoTotal());
                mostrarResumenImportacion(importResult);
            }

            limpiarFormulario();
            cargarListado();
            alCrearObra.accept(obra);

        } catch (SQLException e) {
            mostrarError("No se pudo guardar la obra: " + e.getMessage());
        }
    }

    /** Marca interna para distinguir "el usuario cancelo" de "usar hoja por defecto" (null). */
    private static final String SELECCION_CANCELADA = "__CANCELADA__";

    /**
     * Si el libro trae varias hojas de presupuesto, pide al usuario que elija cual importar
     * (mostrando partidas y total de cada una). Devuelve el nombre de la hoja, null para usar
     * la hoja por defecto (0 o 1 hojas), o SELECCION_CANCELADA si el usuario cerro el dialogo.
     */
    private String elegirHojaAImportar(String rutaExcel) throws IOException {
        List<HojaImportable> hojas = excelImporter.listarHojasImportables(rutaExcel);
        if (hojas.size() <= 1) {
            return null; // deja que el importador use la hoja por defecto
        }
        HojaImportable seleccion = (HojaImportable) JOptionPane.showInputDialog(
                this,
                "El archivo tiene varias hojas de presupuesto.\nElige cual quieres importar:",
                "Seleccionar hoja del Excel",
                JOptionPane.QUESTION_MESSAGE,
                null,
                hojas.toArray(),
                hojas.get(0));
        if (seleccion == null) {
            return SELECCION_CANCELADA;
        }
        return seleccion.nombre;
    }

    private void mostrarResumenImportacion(ImportResult resultado) {
        StringBuilder sb = new StringBuilder();
        sb.append("IMPORTACION COMPLETADA\n");
        sb.append("========================================\n");
        sb.append("Partidas importadas : ").append(resultado.getPartidasImportadas().size()).append("\n");
        sb.append("  - ejecutables     : ").append(resultado.getPartidasEjecutables()).append("\n");
        sb.append("  - agrupadoras     : ").append(resultado.getPartidasPadre()).append("\n");
        sb.append("Filas omitidas      : ").append(resultado.getFilasOmitidas()).append("\n");
        sb.append(String.format("Presupuesto total   : S/. %,.2f%n", resultado.getPresupuestoTotal()));
        sb.append("Subtotales de seccion: ")
          .append(resultado.isSubtotalesCuadran() ? "CUADRAN" : "REVISAR (ver advertencias)")
          .append("\n");

        // Verificacion (mensajes positivos)
        if (!resultado.getInformes().isEmpty()) {
            sb.append("\nVERIFICACION\n----------------------------------------\n");
            resultado.getInformes().forEach(m -> sb.append("• ").append(m).append("\n"));
        }

        // Todas las advertencias, desplazables
        if (!resultado.getAdvertencias().isEmpty()) {
            sb.append("\nADVERTENCIAS (").append(resultado.getAdvertencias().size()).append(")\n");
            sb.append("----------------------------------------\n");
            resultado.getAdvertencias().forEach(a -> sb.append("• ").append(a).append("\n"));
        }

        JTextArea area = new JTextArea(sb.toString(), 22, 70);
        area.setEditable(false);
        area.setCaretPosition(0);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(720, 420));

        int tipo = resultado.isSubtotalesCuadran()
                ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(this, scroll, "Importacion de Excel — resumen y verificacion", tipo);
    }

    private void cargarListado() {
        try {
            tablaModelo.setRowCount(0);
            obrasActuales.clear();
            List<Obra> obras = obraService.listarTodas();
            for (Obra o : obras) {
                obrasActuales.add(o);
                tablaModelo.addRow(new Object[]{
                        o.getNombre(), o.getEstado(),
                        String.format("%,.2f", o.getPresupuestoTotal()),
                        o.getFechaCreacion()
                });
            }
        } catch (SQLException e) {
            mostrarError("No se pudo cargar el listado de obras: " + e.getMessage());
        }
    }

    private Obra obraSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= obrasActuales.size()) return null;
        return obrasActuales.get(fila);
    }

    private void editarObraSeleccionada() {
        Obra o = obraSeleccionada();
        if (o == null) {
            mostrarError("Selecciona una obra de la lista para editar.");
            return;
        }
        mostrarDialogoEditar(o);
    }

    private void eliminarObraSeleccionada() {
        Obra o = obraSeleccionada();
        if (o == null) {
            mostrarError("Selecciona una obra de la lista para eliminar.");
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la obra \"" + o.getNombre() + "\"?\n\n"
                        + "Se borraran TAMBIEN sus partidas, movimientos de almacen e historial.\n"
                        + "Esta accion no se puede deshacer.",
                "Eliminar obra", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            obraService.eliminar(o.getId());
            cargarListado();
            if (alModificarObras != null) alModificarObras.run();
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar la obra: " + e.getMessage());
        }
    }

    /** Dialogo para editar nombre, descripcion, fechas y estado de una obra. */
    private void mostrarDialogoEditar(Obra o) {
        JTextField nombre = new JTextField(o.getNombre());
        JTextArea descripcion = new JTextArea(o.getDescripcion() != null ? o.getDescripcion() : "", 3, 20);
        descripcion.setLineWrap(true);
        descripcion.setWrapStyleWord(true);
        JTextField inicio = new JTextField(o.getFechaInicio() != null ? o.getFechaInicio().toString() : "");
        JTextField fin = new JTextField(o.getFechaFinEstimada() != null ? o.getFechaFinEstimada().toString() : "");
        JComboBox<Obra.Estado> estado = new JComboBox<>(Obra.Estado.values());
        estado.setSelectedItem(o.getEstado());
        JTextField ubicacion = new JTextField(o.getUbicacion() != null ? o.getUbicacion() : "");
        JTextField entidad = new JTextField(o.getEntidadContratante() != null ? o.getEntidadContratante() : "");
        JComboBox<String> modalidad = new JComboBox<>(OPCIONES_MODALIDAD);
        modalidad.setSelectedItem(o.getModalidadEjecucion() != null ? o.getModalidadEjecucion().name() : SIN_DEFINIR);
        JTextArea sectores = new JTextArea(o.getSectoresBloques() != null ? o.getSectoresBloques() : "", 3, 20);
        sectores.setLineWrap(true);
        sectores.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Nombre *"));
        form.add(nombre);
        form.add(new JLabel("Descripcion"));
        form.add(new JScrollPane(descripcion));
        form.add(new JLabel("Fecha de inicio (AAAA-MM-DD)"));
        form.add(inicio);
        form.add(new JLabel("Fecha fin estimada (opcional)"));
        form.add(fin);
        form.add(new JLabel("Estado"));
        form.add(estado);
        form.add(new JLabel("Ubicacion (distrito, provincia, departamento)"));
        form.add(ubicacion);
        form.add(new JLabel("Entidad contratante"));
        form.add(entidad);
        form.add(new JLabel("Modalidad de ejecucion"));
        form.add(modalidad);
        form.add(new JLabel("Sectores / bloques"));
        form.add(new JScrollPane(sectores));

        int op = JOptionPane.showConfirmDialog(this, form, "Editar obra",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        String nuevoNombre = nombre.getText().trim();
        if (nuevoNombre.isEmpty()) {
            mostrarError("El nombre es obligatorio.");
            return;
        }
        LocalDate fi = parsearFecha(inicio.getText(), null);
        if (!inicio.getText().trim().isEmpty() && fi == null) {
            mostrarError("Fecha de inicio invalida. Usa AAAA-MM-DD.");
            return;
        }
        LocalDate ff = parsearFecha(fin.getText(), null);
        if (!fin.getText().trim().isEmpty() && ff == null) {
            mostrarError("Fecha fin invalida. Usa AAAA-MM-DD o dejala vacia.");
            return;
        }
        if (fi != null && ff != null && !ff.isAfter(fi)) {
            mostrarError("La fecha fin debe ser posterior a la fecha de inicio.");
            return;
        }

        o.setNombre(nuevoNombre);
        o.setDescripcion(descripcion.getText().trim());
        o.setFechaInicio(fi);
        o.setFechaFinEstimada(ff);
        o.setEstado((Obra.Estado) estado.getSelectedItem());
        o.setUbicacion(vacioComoNull(ubicacion.getText()));
        o.setEntidadContratante(vacioComoNull(entidad.getText()));
        o.setModalidadEjecucion(modalidadSeleccionada(modalidad));
        o.setSectoresBloques(vacioComoNull(sectores.getText()));
        try {
            obraService.actualizar(o);
            cargarListado();
            if (alModificarObras != null) alModificarObras.run();
        } catch (SQLException e) {
            mostrarError("No se pudo guardar los cambios: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void limpiarFormulario() {
        campoNombre.setText("");
        campoDescripcion.setText("");
        campoFechaInicio.setText(LocalDate.now().toString());
        campoFechaFin.setText("");
        campoRutaExcel.setText("");
        campoUbicacion.setText("");
        campoEntidad.setText("");
        campoModalidad.setSelectedIndex(0);
        campoSectores.setText("");
    }

    private String vacioComoNull(String texto) {
        String t = texto == null ? "" : texto.trim();
        return t.isEmpty() ? null : t;
    }

    private Obra.ModalidadEjecucion modalidadSeleccionada(JComboBox<String> combo) {
        String seleccion = (String) combo.getSelectedItem();
        if (seleccion == null || SIN_DEFINIR.equals(seleccion)) return null;
        return Obra.ModalidadEjecucion.valueOf(seleccion);
    }

    /** Parsea una fecha AAAA-MM-DD; devuelve porDefecto si el texto esta vacio, o null si es invalida. */
    private LocalDate parsearFecha(String texto, LocalDate porDefecto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) return porDefecto;
        try {
            return LocalDate.parse(t);
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------- helpers de estilo ----------

    private JLabel seccionTitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Theme.FONT_BOLD);
        l.setForeground(Theme.TEXT_PRIMARY);
        return l;
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
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void estilizarBotonPrimario(JButton btn) {
        btn.setFont(Theme.FONT_BOLD);
        btn.setBackground(Theme.ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    private void estilizarBotonSecundario(JButton btn) {
        btn.setFont(Theme.FONT_BASE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
