package com.obratrack.ui.views;

import com.obratrack.model.ItemCumplimiento;
import com.obratrack.model.ItemCumplimiento.Categoria;
import com.obratrack.model.ItemCumplimiento.Estado;
import com.obratrack.model.ItemCumplimiento.Impacto;
import com.obratrack.model.ItemCumplimiento.Probabilidad;
import com.obratrack.model.ItemCumplimiento.Severidad;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.CumplimientoCalculo;
import com.obratrack.service.ICumplimientoService;
import com.obratrack.service.IPartidaService;
import com.obratrack.service.Permisos;
import com.obratrack.service.ServiceFactory;
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
 * Gestion y cumplimiento de la obra: matriz de riesgos, compromisos ambientales,
 * control de calidad (ensayos/no conformidades) y seguridad y salud en el
 * trabajo (SST). Las cuatro categorias comparten la misma tabla reutilizada:
 * algo que se identifica, se le hace seguimiento y se cierra.
 */
public class CumplimientoView extends JPanel {

    private final Supplier<Obra> obraActivaProvider;
    private final JLabel tituloObra = new JLabel();
    private final JTabbedPane tabs = new JTabbedPane();

    public CumplimientoView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Gestion y Cumplimiento");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        header.add(titulo, BorderLayout.WEST);
        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        header.add(tituloObra, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tabs.addTab("Matriz de Riesgos", new CategoriaPanel(Categoria.RIESGO));
        tabs.addTab("Compromisos Ambientales", new CategoriaPanel(Categoria.AMBIENTAL));
        tabs.addTab("Control de Calidad", new CategoriaPanel(Categoria.CALIDAD));
        tabs.addTab("Seguridad y Salud (SST)", new CategoriaPanel(Categoria.SST));
        add(tabs, BorderLayout.CENTER);
    }

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        tituloObra.setText(obra != null ? "Obra: " + obra.getNombre() : "Selecciona una obra activa");
        for (Component c : tabs.getComponents()) {
            if (c instanceof CategoriaPanel p) p.cargar();
        }
    }

    // ============================================================
    //  Panel reutilizable por categoria
    // ============================================================

    private class CategoriaPanel extends JPanel {
        private final Categoria categoria;
        private final ICumplimientoService cumplimientoService = ServiceFactory.cumplimiento();
        private final IPartidaService partidaService = ServiceFactory.partida();

        private final DefaultTableModel modelo;
        private final JTable tabla;
        private final List<ItemCumplimiento> actuales = new ArrayList<>();
        private final JLabel resumen = new JLabel();

        private static final String[] COLS = {
                "Descripcion", "Severidad", "Estado", "Fecha", "Fecha limite", "Responsable", "Dias abierto"
        };
        private static final Color FONDO_VENCIDO =
                new Color(Theme.DANGER.getRed(), Theme.DANGER.getGreen(), Theme.DANGER.getBlue(), 22);
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        CategoriaPanel(Categoria categoria) {
            this.categoria = categoria;
            setLayout(new BorderLayout(0, 8));
            setBorder(new EmptyBorder(12, 0, 0, 0));
            setOpaque(false);

            JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            barra.setOpaque(false);
            JButton btnNuevo = new JButton("Nuevo item");
            btnNuevo.setFont(Theme.FONT_BOLD);
            btnNuevo.setBackground(Theme.ACCENT);
            btnNuevo.setForeground(Color.WHITE);
            btnNuevo.setFocusPainted(false);
            btnNuevo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnNuevo.addActionListener(e -> nuevoItem());
            JButton btnEditar = new JButton("Editar");
            btnEditar.setFocusPainted(false);
            btnEditar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEditar.addActionListener(e -> editarItem());
            JButton btnCerrar = new JButton("Marcar cerrado");
            btnCerrar.setFocusPainted(false);
            btnCerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnCerrar.addActionListener(e -> cerrarItem());
            JButton btnEliminar = new JButton("Eliminar");
            btnEliminar.setForeground(Theme.DANGER);
            btnEliminar.setFocusPainted(false);
            btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEliminar.addActionListener(e -> eliminarItem());
            if (!Permisos.puedeEscribir()) {
                btnNuevo.setEnabled(false);
                btnEditar.setEnabled(false);
                btnCerrar.setEnabled(false);
                btnEliminar.setEnabled(false);
            }
            barra.add(btnNuevo);
            barra.add(btnEditar);
            barra.add(btnCerrar);
            barra.add(btnEliminar);

            JPanel norte = new JPanel(new BorderLayout());
            norte.setOpaque(false);
            norte.add(barra, BorderLayout.WEST);
            resumen.setFont(Theme.FONT_BOLD);
            norte.add(resumen, BorderLayout.EAST);
            add(norte, BorderLayout.NORTH);

            modelo = new DefaultTableModel(COLS, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            tabla = new JTable(modelo);
            tabla.setRowHeight(26);
            tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabla.setDefaultRenderer(Object.class, new FilaEstadoRenderer());
            add(new JScrollPane(tabla), BorderLayout.CENTER);
        }

        void cargar() {
            modelo.setRowCount(0);
            actuales.clear();
            Obra obra = obraActivaProvider.get();
            if (obra == null) { resumen.setText(""); return; }
            try {
                List<ItemCumplimiento> items = cumplimientoService.listarPorObra(obra.getId(), categoria);
                actuales.addAll(items);
                LocalDate hoy = LocalDate.now();
                for (ItemCumplimiento i : items) {
                    modelo.addRow(new Object[]{
                            i.getDescripcion(),
                            textoSeveridad(i.getSeveridad()),
                            textoEstado(i.getEstado()),
                            i.getFecha() != null ? i.getFecha().format(fmt) : "",
                            i.getFechaLimite() != null ? i.getFechaLimite().format(fmt) : "",
                            i.getResponsable() != null ? i.getResponsable() : "",
                            CumplimientoCalculo.diasAbierto(i, hoy)
                    });
                }
                long abiertos = CumplimientoCalculo.conteoAbiertos(items);
                long vencidos = CumplimientoCalculo.conteoVencidos(items, hoy);
                resumen.setForeground(vencidos > 0 ? Theme.DANGER : Theme.TEXT_SECONDARY);
                resumen.setText(String.format("%d en total   ·   %d abiertos   ·   %d vencidos", items.size(), abiertos, vencidos));
            } catch (SQLException e) {
                mostrarError("No se pudo cargar: " + e.getMessage());
            }
        }

        private ItemCumplimiento seleccionado() {
            int fila = tabla.getSelectedRow();
            if (fila < 0 || fila >= actuales.size()) return null;
            return actuales.get(fila);
        }

        private void nuevoItem() {
            Obra obra = obraActivaProvider.get();
            if (obra == null) { mostrarError("Selecciona una obra activa primero."); return; }
            ItemCumplimiento nuevo = new ItemCumplimiento();
            nuevo.setObraId(obra.getId());
            nuevo.setCategoria(categoria);
            if (mostrarFormulario(obra, nuevo, "Nuevo item")) {
                try {
                    cumplimientoService.crear(nuevo);
                    cargar();
                } catch (SQLException e) {
                    mostrarError("No se pudo guardar: " + e.getMessage());
                }
            }
        }

        private void editarItem() {
            ItemCumplimiento i = seleccionado();
            if (i == null) { mostrarError("Selecciona un item de la tabla."); return; }
            Obra obra = obraActivaProvider.get();
            if (mostrarFormulario(obra, i, "Editar item")) {
                try {
                    cumplimientoService.actualizar(i);
                    cargar();
                } catch (SQLException e) {
                    mostrarError("No se pudo guardar: " + e.getMessage());
                }
            }
        }

        private void cerrarItem() {
            ItemCumplimiento i = seleccionado();
            if (i == null) { mostrarError("Selecciona un item de la tabla."); return; }
            if (i.getEstado() == Estado.CERRADO) { mostrarError("Ese item ya esta cerrado."); return; }
            int op = JOptionPane.showConfirmDialog(CumplimientoView.this,
                    "¿Marcar \"" + i.getDescripcion() + "\" como cerrado hoy?",
                    "Cerrar item", JOptionPane.YES_NO_OPTION);
            if (op != JOptionPane.YES_OPTION) return;
            i.setEstado(Estado.CERRADO);
            i.setFechaCierre(LocalDate.now());
            try {
                cumplimientoService.actualizar(i);
                cargar();
            } catch (SQLException e) {
                mostrarError("No se pudo cerrar: " + e.getMessage());
            }
        }

        private void eliminarItem() {
            ItemCumplimiento i = seleccionado();
            if (i == null) { mostrarError("Selecciona un item de la tabla."); return; }
            int op = JOptionPane.showConfirmDialog(CumplimientoView.this,
                    "¿Eliminar \"" + i.getDescripcion() + "\"?", "Eliminar", JOptionPane.YES_NO_OPTION);
            if (op != JOptionPane.YES_OPTION) return;
            try {
                cumplimientoService.eliminar(i.getId());
                cargar();
            } catch (SQLException e) {
                mostrarError("No se pudo eliminar: " + e.getMessage());
            }
        }

        /** Formulario compartido de alta/edicion. Devuelve true si el usuario confirmo con datos validos. */
        private boolean mostrarFormulario(Obra obra, ItemCumplimiento i, String titulo) {
            boolean esRiesgo = categoria == Categoria.RIESGO;

            JTextArea campoDescripcion = new JTextArea(i.getDescripcion() != null ? i.getDescripcion() : "", 3, 24);
            campoDescripcion.setLineWrap(true);
            campoDescripcion.setWrapStyleWord(true);
            JComboBox<Probabilidad> comboProbabilidad = new JComboBox<>(Probabilidad.values());
            JComboBox<Impacto> comboImpacto = new JComboBox<>(Impacto.values());
            if (i.getProbabilidad() != null) comboProbabilidad.setSelectedItem(i.getProbabilidad());
            if (i.getImpacto() != null) comboImpacto.setSelectedItem(i.getImpacto());
            JComboBox<Severidad> comboSeveridad = new JComboBox<>(Severidad.values());
            comboSeveridad.setSelectedItem(i.getSeveridad());
            JTextField campoFecha = new JTextField(i.getFecha() != null ? i.getFecha().toString() : LocalDate.now().toString());
            JTextField campoFechaLimite = new JTextField(i.getFechaLimite() != null ? i.getFechaLimite().toString() : "");
            JComboBox<Estado> comboEstado = new JComboBox<>(Estado.values());
            comboEstado.setSelectedItem(i.getEstado());
            JTextField campoFechaCierre = new JTextField(i.getFechaCierre() != null ? i.getFechaCierre().toString() : "");
            JTextField campoResponsable = new JTextField(i.getResponsable() != null ? i.getResponsable() : "");
            JTextArea campoAccion = new JTextArea(i.getAccionSeguimiento() != null ? i.getAccionSeguimiento() : "", 3, 24);
            campoAccion.setLineWrap(true);
            campoAccion.setWrapStyleWord(true);

            JComboBox<Partida> comboPartida = new JComboBox<>();
            comboPartida.addItem(null);
            try {
                if (obra != null) {
                    for (Partida p : partidaService.listarEjecutablesPorObra(obra.getId())) comboPartida.addItem(p);
                }
            } catch (SQLException ignored) {
                // el formulario sigue funcionando sin la lista de partidas
            }
            comboPartida.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                boolean isSelected, boolean cellHasFocus) {
                    Object mostrar = (value == null) ? "— Sin partida vinculada —" : value;
                    return super.getListCellRendererComponent(list, mostrar, index, isSelected, cellHasFocus);
                }
            });
            if (i.getPartidaId() != null) {
                for (int idx = 0; idx < comboPartida.getItemCount(); idx++) {
                    Partida p = comboPartida.getItemAt(idx);
                    if (p != null && p.getId().equals(i.getPartidaId())) { comboPartida.setSelectedIndex(idx); break; }
                }
            }

            JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
            form.add(new JLabel("Descripcion *"));
            form.add(new JScrollPane(campoDescripcion));
            if (esRiesgo) {
                form.add(new JLabel("Probabilidad"));
                form.add(comboProbabilidad);
                form.add(new JLabel("Impacto"));
                form.add(comboImpacto);
            } else {
                form.add(new JLabel("Severidad"));
                form.add(comboSeveridad);
            }
            form.add(new JLabel("Partida vinculada (opcional)"));
            form.add(comboPartida);
            form.add(new JLabel("Fecha * (AAAA-MM-DD)"));
            form.add(campoFecha);
            form.add(new JLabel("Fecha limite (opcional)"));
            form.add(campoFechaLimite);
            form.add(new JLabel("Estado"));
            form.add(comboEstado);
            form.add(new JLabel("Fecha de cierre (si esta cerrado)"));
            form.add(campoFechaCierre);
            form.add(new JLabel("Responsable (opcional)"));
            form.add(campoResponsable);
            form.add(new JLabel(esRiesgo ? "Plan de mitigacion" : "Accion / seguimiento"));
            form.add(new JScrollPane(campoAccion));

            JScrollPane scrollForm = new JScrollPane(form);
            scrollForm.setBorder(BorderFactory.createEmptyBorder());
            scrollForm.setPreferredSize(new Dimension(420, 460));

            int op = JOptionPane.showConfirmDialog(CumplimientoView.this, scrollForm, titulo,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return false;

            String descripcion = campoDescripcion.getText().trim();
            if (descripcion.isEmpty()) { mostrarError("La descripcion es obligatoria."); return false; }
            LocalDate fecha = parsearFecha(campoFecha.getText(), null);
            if (fecha == null) { mostrarError("Fecha invalida. Usa el formato AAAA-MM-DD."); return false; }
            LocalDate fechaLimite = parsearFecha(campoFechaLimite.getText(), null);
            if (!campoFechaLimite.getText().trim().isEmpty() && fechaLimite == null) {
                mostrarError("Fecha limite invalida. Usa AAAA-MM-DD o dejala vacia."); return false;
            }
            Estado estado = (Estado) comboEstado.getSelectedItem();
            LocalDate fechaCierre = parsearFecha(campoFechaCierre.getText(), null);
            if (!campoFechaCierre.getText().trim().isEmpty() && fechaCierre == null) {
                mostrarError("Fecha de cierre invalida. Usa AAAA-MM-DD o dejala vacia."); return false;
            }
            if (estado == Estado.CERRADO && fechaCierre == null) fechaCierre = LocalDate.now();

            Partida partidaElegida = (Partida) comboPartida.getSelectedItem();
            i.setDescripcion(descripcion);
            i.setPartidaId(partidaElegida != null ? partidaElegida.getId() : null);
            i.setFecha(fecha);
            i.setFechaLimite(fechaLimite);
            i.setEstado(estado);
            i.setFechaCierre(fechaCierre);
            i.setResponsable(campoResponsable.getText().trim());
            i.setAccionSeguimiento(campoAccion.getText().trim());
            if (esRiesgo) {
                Probabilidad prob = (Probabilidad) comboProbabilidad.getSelectedItem();
                Impacto imp = (Impacto) comboImpacto.getSelectedItem();
                i.setProbabilidad(prob);
                i.setImpacto(imp);
                i.setSeveridad(CumplimientoCalculo.calcularSeveridadRiesgo(prob, imp));
            } else {
                i.setProbabilidad(null);
                i.setImpacto(null);
                i.setSeveridad((Severidad) comboSeveridad.getSelectedItem());
            }
            return true;
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

        private void mostrarError(String msg) {
            JOptionPane.showMessageDialog(CumplimientoView.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }

        private String textoSeveridad(Severidad s) {
            return switch (s) {
                case BAJA -> "Baja";
                case MEDIA -> "Media";
                case ALTA -> "Alta";
                case CRITICA -> "Critica";
            };
        }

        private String textoEstado(Estado e) {
            return switch (e) {
                case ABIERTO -> "Abierto";
                case EN_PROCESO -> "En proceso";
                case CERRADO -> "Cerrado";
            };
        }

        private Color colorSeveridad(String texto) {
            return switch (texto) {
                case "Critica" -> Theme.DANGER;
                case "Alta" -> Theme.WARNING;
                case "Media" -> Theme.PRIMARY;
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
                boolean vencido = row < actuales.size() && CumplimientoCalculo.estaVencido(actuales.get(row), LocalDate.now());
                c.setBackground(isSelected ? Theme.BORDER : (vencido ? FONDO_VENCIDO : fondo));
                String severidadTexto = String.valueOf(table.getValueAt(row, 1));
                String estadoTexto = String.valueOf(table.getValueAt(row, 2));
                if (column == 1) {
                    c.setForeground(colorSeveridad(severidadTexto));
                } else if (column == 2) {
                    c.setForeground("Cerrado".equals(estadoTexto) ? Theme.SUCCESS : Theme.TEXT_PRIMARY);
                } else {
                    c.setForeground(Theme.TEXT_PRIMARY);
                }
                return c;
            }
        }
    }
}
