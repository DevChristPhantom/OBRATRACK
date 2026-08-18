package com.obratrack.ui.views;

import com.obratrack.model.Actividad;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.CronogramaCalculo;
import com.obratrack.service.ICronogramaService;
import com.obratrack.service.IPartidaService;
import com.obratrack.service.Permisos;
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
 * Cronograma de obra: actividades con fechas programadas/reales, tabla de avance,
 * diagrama de Gantt y curva S (programado vs. real), todo dibujado con Java2D a
 * partir de {@link CronogramaCalculo} (logica pura, sin base de datos).
 */
public class CronogramaView extends JPanel {

    private final ICronogramaService cronogramaService = ServiceFactory.cronograma();
    private final IPartidaService partidaService = ServiceFactory.partida();
    private final Supplier<Obra> obraActivaProvider;

    private final JLabel tituloObra = new JLabel();
    private final DefaultTableModel tablaModelo;
    private final JTable tabla;
    private final List<Actividad> actividadesActuales = new ArrayList<>();

    private final JLabel resumenTexto = new JLabel();
    private final GanttChart gantt = new GanttChart();
    private final CurvaSChart curva = new CurvaSChart();

    private static final String[] COLUMNAS = {
            "Codigo", "Actividad", "Inicio prog.", "Fin prog.", "Peso %", "Avance prog.", "Avance real", "Estado"
    };
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CronogramaView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Cronograma");
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
        tabla.setDefaultRenderer(Object.class, new FilaEstadoRenderer());
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(construirBarra(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(tabla), construirPanelGraficos());
        split.setResizeWeight(0.45);
        split.setBorder(null);
        centro.add(split, BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
    }

    private JPanel construirBarra() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        JButton btnNueva = new JButton("Nueva actividad", Icons.get("add", 16, Color.WHITE));
        btnNueva.setIconTextGap(6);
        estilizarBotonPrimario(btnNueva);
        btnNueva.addActionListener(e -> nuevaActividad());
        JButton btnEditar = new JButton("Editar", Icons.get("settings", 15, Theme.TEXT_SECONDARY));
        btnEditar.setIconTextGap(6);
        estilizarBotonSecundario(btnEditar);
        btnEditar.addActionListener(e -> editarSeleccionada());
        JButton btnEliminar = new JButton("Eliminar", Icons.get("delete", 15, Theme.DANGER));
        btnEliminar.setIconTextGap(6);
        btnEliminar.setForeground(Theme.DANGER);
        estilizarBotonSecundario(btnEliminar);
        btnEliminar.addActionListener(e -> eliminarSeleccionada());
        if (!Permisos.puedeEscribir()) {
            btnNueva.setEnabled(false);
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
        botones.add(btnNueva);
        botones.add(btnEditar);
        botones.add(btnEliminar);
        barra.add(botones, BorderLayout.WEST);

        resumenTexto.setFont(Theme.FONT_BOLD);
        resumenTexto.setForeground(Theme.TEXT_SECONDARY);
        barra.add(resumenTexto, BorderLayout.EAST);
        return barra;
    }

    private JPanel construirPanelGraficos() {
        JPanel graficos = new JPanel(new GridLayout(1, 2, 16, 0));
        graficos.setOpaque(false);
        graficos.add(tarjetaChart("Diagrama de Gantt", new JScrollPane(gantt)));
        graficos.add(tarjetaChart("Curva S — avance programado vs. real", curva));
        return graficos;
    }

    private JPanel tarjetaChart(String titulo, JComponent contenido) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.BG_SECONDARY);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        card.add(lbl, BorderLayout.NORTH);
        card.add(contenido, BorderLayout.CENTER);
        return card;
    }

    // ============================================================
    //  Carga y refresco
    // ============================================================

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        tablaModelo.setRowCount(0);
        actividadesActuales.clear();

        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa para ver su cronograma");
            resumenTexto.setText("");
            gantt.setDatos(List.of(), LocalDate.now());
            curva.setDatos(List.of(), LocalDate.now());
            repintarGraficos();
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre());

        try {
            List<Actividad> actividades = cronogramaService.listarPorObra(obra.getId());
            actividadesActuales.addAll(actividades);
            LocalDate hoy = LocalDate.now();

            for (Actividad a : actividades) {
                double avanceProg = CronogramaCalculo.avanceProgramado(a, hoy);
                CronogramaCalculo.Estado estado = CronogramaCalculo.estado(a, hoy);
                tablaModelo.addRow(new Object[]{
                        a.getCodigo() != null ? a.getCodigo() : "",
                        a.getDescripcion(),
                        a.getFechaInicioProg() != null ? a.getFechaInicioProg().format(FMT) : "",
                        a.getFechaFinProg() != null ? a.getFechaFinProg().format(FMT) : "",
                        String.format("%.1f", a.getPesoPorcentual()),
                        String.format("%.0f%%", avanceProg),
                        String.format("%.0f%%", a.getAvanceReal()),
                        textoEstado(estado)
                });
            }

            actualizarResumen(actividades, hoy);
            gantt.setDatos(actividades, hoy);
            curva.setDatos(construirCurva(actividades, obra, hoy), hoy);
            repintarGraficos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el cronograma: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<CronogramaCalculo.PuntoCurva> construirCurva(List<Actividad> actividades, Obra obra, LocalDate hoy) {
        if (actividades.isEmpty()) return List.of();
        LocalDate desde = actividades.stream().map(Actividad::getFechaInicioProg)
                .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(obra.getFechaInicio());
        LocalDate hasta = actividades.stream().map(Actividad::getFechaFinProg)
                .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(hoy);
        if (desde == null || hasta == null || !hasta.isAfter(desde)) return List.of();
        return CronogramaCalculo.curvaS(actividades, desde, hasta, 24, hoy);
    }

    private void actualizarResumen(List<Actividad> actividades, LocalDate hoy) {
        if (actividades.isEmpty()) {
            resumenTexto.setText("Sin actividades registradas");
            resumenTexto.setForeground(Theme.TEXT_SECONDARY);
            return;
        }
        double prog = CronogramaCalculo.pctProgramadoAcumulado(actividades, hoy);
        double real = CronogramaCalculo.pctRealAcumulado(actividades, hoy, hoy);
        double desvio = real - prog;
        String signo = desvio >= 0 ? "+" : "";
        resumenTexto.setText(String.format("Programado: %.0f%%   ·   Real: %.0f%%   ·   Desvio: %s%.0f pts",
                prog, real, signo, desvio));
        resumenTexto.setForeground(desvio < -MARGEN_RESUMEN ? Theme.DANGER
                : (desvio < 0 ? Theme.WARNING : Theme.SUCCESS));
    }

    private static final double MARGEN_RESUMEN = 5.0;

    private String textoEstado(CronogramaCalculo.Estado estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EN_PROCESO -> "En proceso";
            case COMPLETADA -> "Completada";
            case ATRASADA -> "Atrasada";
        };
    }

    private void repintarGraficos() {
        gantt.revalidate();
        gantt.repaint();
        curva.repaint();
    }

    // ============================================================
    //  Alta / edicion / eliminacion
    // ============================================================

    private Actividad seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return null;
        int modelRow = tabla.convertRowIndexToModel(fila);
        if (modelRow < 0 || modelRow >= actividadesActuales.size()) return null;
        return actividadesActuales.get(modelRow);
    }

    private void nuevaActividad() {
        Obra obra = obraActivaProvider.get();
        if (obra == null) {
            mostrarError("Selecciona una obra activa primero.");
            return;
        }
        Actividad nueva = new Actividad();
        nueva.setObraId(obra.getId());
        nueva.setFechaInicioProg(LocalDate.now());
        nueva.setFechaFinProg(LocalDate.now().plusDays(7));
        if (mostrarFormulario(obra, nueva, "Nueva actividad")) {
            try {
                cronogramaService.crear(nueva);
                refrescar();
            } catch (SQLException e) {
                mostrarError("No se pudo guardar la actividad: " + e.getMessage());
            }
        }
    }

    private void editarSeleccionada() {
        Actividad a = seleccionada();
        if (a == null) {
            mostrarError("Selecciona una actividad de la tabla para editar.");
            return;
        }
        Obra obra = obraActivaProvider.get();
        if (mostrarFormulario(obra, a, "Editar actividad")) {
            try {
                cronogramaService.actualizar(a);
                refrescar();
            } catch (SQLException e) {
                mostrarError("No se pudo guardar los cambios: " + e.getMessage());
            }
        }
    }

    private void eliminarSeleccionada() {
        Actividad a = seleccionada();
        if (a == null) {
            mostrarError("Selecciona una actividad de la tabla para eliminar.");
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la actividad \"" + a.getDescripcion() + "\"?",
                "Eliminar actividad", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            cronogramaService.eliminar(a.getId());
            refrescar();
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar la actividad: " + e.getMessage());
        }
    }

    /** Formulario compartido de alta/edicion. Devuelve true si el usuario confirmo con datos validos. */
    private boolean mostrarFormulario(Obra obra, Actividad a, String titulo) {
        JTextField campoCodigo = new JTextField(a.getCodigo() != null ? a.getCodigo() : "");
        JTextField campoDescripcion = new JTextField(a.getDescripcion() != null ? a.getDescripcion() : "");
        JTextField campoInicioProg = new JTextField(str(a.getFechaInicioProg()));
        JTextField campoFinProg = new JTextField(str(a.getFechaFinProg()));
        JTextField campoPeso = new JTextField(String.valueOf(a.getPesoPorcentual()));
        JTextField campoAvanceReal = new JTextField(String.valueOf(a.getAvanceReal()));
        JTextField campoInicioReal = new JTextField(str(a.getFechaInicioReal()));
        JTextField campoFinReal = new JTextField(str(a.getFechaFinReal()));

        JComboBox<Partida> comboPartida = new JComboBox<>();
        comboPartida.addItem(null);
        try {
            if (obra != null) {
                for (Partida p : partidaService.listarEjecutablesPorObra(obra.getId())) {
                    comboPartida.addItem(p);
                }
            }
        } catch (SQLException ignored) {
            // El formulario sigue funcionando sin la lista de partidas.
        }
        comboPartida.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                Object mostrar = (value == null) ? "— Sin partida vinculada —" : value;
                return super.getListCellRendererComponent(list, mostrar, index, isSelected, cellHasFocus);
            }
        });
        if (a.getPartidaId() != null) {
            for (int i = 0; i < comboPartida.getItemCount(); i++) {
                Partida p = comboPartida.getItemAt(i);
                if (p != null && p.getId().equals(a.getPartidaId())) {
                    comboPartida.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Codigo (opcional)"));
        form.add(campoCodigo);
        form.add(new JLabel("Descripcion *"));
        form.add(campoDescripcion);
        form.add(new JLabel("Partida vinculada (opcional)"));
        form.add(comboPartida);
        form.add(new JLabel("Fecha inicio programada * (AAAA-MM-DD)"));
        form.add(campoInicioProg);
        form.add(new JLabel("Fecha fin programada * (AAAA-MM-DD)"));
        form.add(campoFinProg);
        form.add(new JLabel("Peso % dentro de la obra (para la curva S)"));
        form.add(campoPeso);
        form.add(new JLabel("Avance real % (0-100)"));
        form.add(campoAvanceReal);
        form.add(new JLabel("Fecha inicio real (opcional)"));
        form.add(campoInicioReal);
        form.add(new JLabel("Fecha fin real (opcional)"));
        form.add(campoFinReal);

        JScrollPane scrollForm = new JScrollPane(form);
        scrollForm.setBorder(BorderFactory.createEmptyBorder());
        scrollForm.setPreferredSize(new Dimension(420, 420));

        int op = JOptionPane.showConfirmDialog(this, scrollForm, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return false;

        String descripcion = campoDescripcion.getText().trim();
        if (descripcion.isEmpty()) {
            mostrarError("La descripcion de la actividad es obligatoria.");
            return false;
        }
        LocalDate inicioProg = parsearFecha(campoInicioProg.getText(), null);
        LocalDate finProg = parsearFecha(campoFinProg.getText(), null);
        if (inicioProg == null || finProg == null) {
            mostrarError("Las fechas programadas son obligatorias, formato AAAA-MM-DD.");
            return false;
        }
        if (!finProg.isAfter(inicioProg)) {
            mostrarError("La fecha fin programada debe ser posterior a la de inicio.");
            return false;
        }
        Double peso = parsearDouble(campoPeso.getText());
        if (peso == null || peso < 0) {
            mostrarError("El peso % debe ser un numero mayor o igual a 0.");
            return false;
        }
        Double avanceReal = parsearDouble(campoAvanceReal.getText());
        if (avanceReal == null || avanceReal < 0 || avanceReal > 100) {
            mostrarError("El avance real debe estar entre 0 y 100.");
            return false;
        }
        LocalDate inicioReal = parsearFecha(campoInicioReal.getText(), null);
        if (!campoInicioReal.getText().trim().isEmpty() && inicioReal == null) {
            mostrarError("Fecha inicio real invalida. Usa AAAA-MM-DD o dejala vacia.");
            return false;
        }
        LocalDate finReal = parsearFecha(campoFinReal.getText(), null);
        if (!campoFinReal.getText().trim().isEmpty() && finReal == null) {
            mostrarError("Fecha fin real invalida. Usa AAAA-MM-DD o dejala vacia.");
            return false;
        }
        if (inicioReal != null && finReal != null && finReal.isBefore(inicioReal)) {
            mostrarError("La fecha fin real no puede ser anterior a la fecha inicio real.");
            return false;
        }

        Partida partidaElegida = (Partida) comboPartida.getSelectedItem();
        a.setCodigo(campoCodigo.getText().trim());
        a.setDescripcion(descripcion);
        a.setPartidaId(partidaElegida != null ? partidaElegida.getId() : null);
        a.setFechaInicioProg(inicioProg);
        a.setFechaFinProg(finProg);
        a.setPesoPorcentual(peso);
        a.setAvanceReal(avanceReal);
        a.setFechaInicioReal(inicioReal);
        a.setFechaFinReal(finReal);
        return true;
    }

    private String str(LocalDate d) {
        return d != null ? d.toString() : "";
    }

    private LocalDate parsearFecha(String texto, LocalDate porDefecto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) return porDefecto;
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

    private void estilizarBotonPrimario(JButton btn) {
        btn.setFont(Theme.FONT_BOLD);
        btn.setBackground(Theme.ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void estilizarBotonSecundario(JButton btn) {
        btn.setFont(Theme.FONT_BASE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ============================================================
    //  Colores de fila segun estado
    // ============================================================

    private Color colorEstado(String estado) {
        return switch (estado) {
            case "Completada" -> Theme.SUCCESS;
            case "En proceso" -> Theme.PRIMARY;
            case "Atrasada" -> Theme.DANGER;
            default -> Theme.TEXT_SECONDARY;
        };
    }

    private class FilaEstadoRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(Theme.FONT_BASE);
            Color fondo = (row % 2 == 0) ? Theme.BG_PRIMARY : Theme.BG_SECONDARY;
            c.setBackground(isSelected ? Theme.BORDER : fondo);
            String estado = String.valueOf(table.getValueAt(row, 7));
            c.setForeground(column == 7 ? colorEstado(estado) : Theme.TEXT_PRIMARY);
            return c;
        }
    }

    // ============================================================
    //  Diagrama de Gantt (Java2D)
    // ============================================================

    private static class GanttChart extends JComponent implements Scrollable {
        private List<Actividad> datos = List.of();
        private LocalDate hoy = LocalDate.now();
        private LocalDate desde = LocalDate.now();
        private LocalDate hasta = LocalDate.now().plusDays(1);
        private static final int ALTO_FILA = 30;
        private static final int ANCHO_ETIQUETA = 140;

        void setDatos(List<Actividad> datos, LocalDate hoy) {
            this.datos = datos;
            this.hoy = hoy;
            LocalDate min = datos.stream().map(Actividad::getFechaInicioProg)
                    .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(hoy);
            LocalDate max = datos.stream().map(Actividad::getFechaFinProg)
                    .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(hoy.plusDays(1));
            this.desde = min.isBefore(hoy) ? min : hoy.minusDays(1);
            this.hasta = max.isAfter(hoy) ? max : hoy.plusDays(1);
            setPreferredSize(new Dimension(10, Math.max(60, datos.size() * ALTO_FILA + 10)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();

            if (datos.isEmpty()) {
                g2.setColor(Theme.TEXT_SECONDARY);
                g2.setFont(Theme.FONT_BASE);
                g2.drawString("Sin actividades registradas", 8, 20);
                g2.dispose();
                return;
            }

            long totalDias = java.time.temporal.ChronoUnit.DAYS.between(desde, hasta);
            if (totalDias <= 0) totalDias = 1;
            int x0 = ANCHO_ETIQUETA;
            int barrasAncho = Math.max(40, w - x0 - 8);

            g2.setFont(Theme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            int y = 4;
            for (Actividad a : datos) {
                CronogramaCalculo.Estado estado = CronogramaCalculo.estado(a, hoy);
                Color color = switch (estado) {
                    case COMPLETADA -> Theme.SUCCESS;
                    case EN_PROCESO -> Theme.PRIMARY;
                    case ATRASADA -> Theme.DANGER;
                    default -> Theme.TEXT_SECONDARY;
                };

                // Etiqueta
                g2.setColor(Theme.TEXT_PRIMARY);
                String etiqueta = (a.getCodigo() != null && !a.getCodigo().isBlank() ? a.getCodigo() + " " : "")
                        + a.getDescripcion();
                g2.drawString(truncar(fm, etiqueta, ANCHO_ETIQUETA - 6), 0, y + 19);

                // Barra programada (fondo, traza completa)
                int bx0 = x0 + (int) (barrasAncho * diasDesde(a.getFechaInicioProg()) / (double) totalDias);
                int bx1 = x0 + (int) (barrasAncho * diasDesde(a.getFechaFinProg()) / (double) totalDias);
                int barAlto = ALTO_FILA - 12;
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(bx0, y, Math.max(2, bx1 - bx0), barAlto, 5, 5);

                // Relleno de avance real dentro de la barra
                double frac = Math.min(1.0, a.getAvanceReal() / 100.0);
                int anchoAvance = (int) Math.round((bx1 - bx0) * frac);
                g2.setColor(color);
                g2.fillRoundRect(bx0, y, Math.max(0, anchoAvance), barAlto, 5, 5);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(bx0, y, Math.max(2, bx1 - bx0), barAlto, 5, 5);

                y += ALTO_FILA;
            }

            // Linea de "hoy"
            int xHoy = x0 + (int) (barrasAncho * java.time.temporal.ChronoUnit.DAYS.between(desde, hoy) / (double) totalDias);
            g2.setColor(Theme.DANGER);
            Stroke original = g2.getStroke();
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{4f, 3f}, 0));
            g2.drawLine(xHoy, 0, xHoy, y);
            g2.setStroke(original);

            g2.dispose();
        }

        private long diasDesde(LocalDate fecha) {
            if (fecha == null) return 0;
            long d = java.time.temporal.ChronoUnit.DAYS.between(desde, fecha);
            return Math.max(0, d);
        }

        private String truncar(FontMetrics fm, String texto, int maxAncho) {
            if (texto == null) return "";
            if (fm.stringWidth(texto) <= maxAncho) return texto;
            String puntos = "...";
            int i = texto.length();
            while (i > 0 && fm.stringWidth(texto.substring(0, i) + puntos) > maxAncho) i--;
            return i <= 0 ? puntos : texto.substring(0, i) + puntos;
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return ALTO_FILA; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return ALTO_FILA * 4; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ============================================================
    //  Curva S (Java2D): % acumulado programado vs. real
    // ============================================================

    private static class CurvaSChart extends JComponent {
        private List<CronogramaCalculo.PuntoCurva> datos = List.of();
        private LocalDate hoy = LocalDate.now();

        void setDatos(List<CronogramaCalculo.PuntoCurva> datos, LocalDate hoy) {
            this.datos = datos;
            this.hoy = hoy;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (datos.size() < 2) {
                g2.setColor(Theme.TEXT_SECONDARY);
                g2.setFont(Theme.FONT_BASE);
                String msg = "Sin datos suficientes para la curva S";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int x0 = 12, x1 = w - 12, yBase = h - 28, yTop = 12;
            int n = datos.size();

            g2.setColor(Theme.BORDER);
            g2.drawLine(x0, yBase, x1, yBase);
            g2.drawLine(x0, yTop, x0, yBase);

            int[] xs = new int[n];
            int[] ysProg = new int[n];
            int[] ysReal = new int[n];
            for (int i = 0; i < n; i++) {
                double fx = i / (double) (n - 1);
                xs[i] = (int) Math.round(x0 + fx * (x1 - x0));
                ysProg[i] = (int) Math.round(yBase - (datos.get(i).pctProgramado / 100.0) * (yBase - yTop));
                ysReal[i] = (int) Math.round(yBase - (datos.get(i).pctReal / 100.0) * (yBase - yTop));
            }

            // La curva real solo se dibuja hasta hoy: no hay dato de avance real para
            // fechas futuras, y extender la linea ahi la haria caer a 0 (se veria como
            // un retroceso en vez de "todavia no hay dato").
            int realCount = 1;
            for (int i = 1; i < n; i++) {
                if (!datos.get(i).fecha.isAfter(hoy)) realCount = i + 1;
            }

            Stroke original = g2.getStroke();
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{5f, 4f}, 0));
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawPolyline(xs, ysProg, n);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.PRIMARY);
            g2.drawPolyline(xs, ysReal, realCount);
            g2.setStroke(original);
            g2.fillOval(xs[realCount - 1] - 4, ysReal[realCount - 1] - 4, 8, 8);

            g2.setFont(Theme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            int ly = h - 8;
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.fillRect(x0, ly - 6, 14, 2);
            g2.drawString("Programado", x0 + 18, ly);
            int lx2 = x0 + 18 + fm.stringWidth("Programado") + 16;
            g2.setColor(Theme.PRIMARY);
            g2.fillRect(lx2, ly - 8, 14, 3);
            g2.drawString("Real", lx2 + 18, ly);

            g2.dispose();
        }
    }
}
