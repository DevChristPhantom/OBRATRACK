package com.obratrack.ui.views;

import com.obratrack.model.Documento;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.DocumentoCalculo;
import com.obratrack.service.IDocumentoService;
import com.obratrack.service.IPartidaService;
import com.obratrack.service.Permisos;
import com.obratrack.service.ServiceFactory;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Documentos de la obra: planos (con control de version), especificaciones
 * tecnicas por partida, estudios de ingenieria, panel fotografico de avance y
 * anexos. Cada pestaña es la misma tabla reutilizada, filtrada por categoria
 * de documento.
 */
public class DocumentosView extends JPanel {

    private final Supplier<Obra> obraActivaProvider;
    private final JLabel tituloObra = new JLabel();
    private final JTabbedPane tabs = new JTabbedPane();

    public DocumentosView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Documentos");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        header.add(titulo, BorderLayout.WEST);
        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        header.add(tituloObra, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tabs.addTab("Planos", new CategoriaPanel(Documento.Categoria.PLANO, false));
        tabs.addTab("Especificaciones Tecnicas", new CategoriaPanel(Documento.Categoria.ESPECIFICACION_TECNICA, true));
        tabs.addTab("Estudios de Ingenieria", new CategoriaPanel(Documento.Categoria.ESTUDIO, false));
        tabs.addTab("Panel Fotografico", new CategoriaPanel(Documento.Categoria.FOTO, false));
        tabs.addTab("Anexos", new CategoriaPanel(Documento.Categoria.ANEXO, false));
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
    //  Panel reutilizable por categoria de documento
    // ============================================================

    private class CategoriaPanel extends JPanel {
        private final Documento.Categoria categoria;
        private final boolean requierePartida;
        private final IDocumentoService documentoService = ServiceFactory.documento();
        private final IPartidaService partidaService = ServiceFactory.partida();

        private final DefaultTableModel modelo;
        private final JTable tabla;
        private final List<Documento> actuales = new ArrayList<>();

        private static final String[] COLS = {"Nombre", "Version", "Partida", "Fecha", "Archivo", "Tamaño", "Subido por"};
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        CategoriaPanel(Documento.Categoria categoria, boolean requierePartida) {
            this.categoria = categoria;
            this.requierePartida = requierePartida;
            setLayout(new BorderLayout(0, 10));
            setBorder(new EmptyBorder(12, 0, 0, 0));
            setOpaque(false);

            JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            barra.setOpaque(false);
            JButton btnSubir = new JButton("Subir documento");
            btnSubir.setFont(Theme.FONT_BOLD);
            btnSubir.setBackground(Theme.ACCENT);
            btnSubir.setForeground(Color.WHITE);
            btnSubir.setFocusPainted(false);
            btnSubir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnSubir.addActionListener(e -> subirDocumento());
            JButton btnAbrir = new JButton("Abrir");
            btnAbrir.setFocusPainted(false);
            btnAbrir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnAbrir.addActionListener(e -> abrirSeleccionado());
            JButton btnEliminar = new JButton("Eliminar");
            btnEliminar.setForeground(Theme.DANGER);
            btnEliminar.setFocusPainted(false);
            btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEliminar.addActionListener(e -> eliminarSeleccionado());
            if (!Permisos.puedeEscribir()) {
                btnSubir.setEnabled(false);
                btnEliminar.setEnabled(false);
            }
            barra.add(btnSubir);
            barra.add(btnAbrir);
            barra.add(btnEliminar);
            add(barra, BorderLayout.NORTH);

            modelo = new DefaultTableModel(COLS, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            tabla = new JTable(modelo);
            tabla.setRowHeight(26);
            tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            add(new JScrollPane(tabla), BorderLayout.CENTER);
        }

        void cargar() {
            modelo.setRowCount(0);
            actuales.clear();
            Obra obra = obraActivaProvider.get();
            if (obra == null) return;
            try {
                List<Documento> documentos = documentoService.listarPorObra(obra.getId(), categoria);
                actuales.addAll(documentos);
                for (Documento d : documentos) {
                    modelo.addRow(new Object[]{
                            d.getNombre(),
                            categoria == Documento.Categoria.PLANO ? d.getVersionFormateada() : "",
                            d.getPartidaId() != null ? nombrePartida(d.getPartidaId()) : "",
                            d.getFecha() != null ? d.getFecha().format(fmt) : "",
                            d.getNombreArchivoOriginal(),
                            DocumentoCalculo.formatoTamano(d.getTamanoBytes()),
                            d.getUsuarioRegistro() != null ? d.getUsuarioRegistro() : ""
                    });
                }
            } catch (SQLException e) {
                mostrarError("No se pudieron cargar los documentos: " + e.getMessage());
            }
        }

        private String nombrePartida(long partidaId) {
            try {
                for (Partida p : partidaService.listarPorObra(obraActivaProvider.get().getId())) {
                    if (p.getId() != null && p.getId() == partidaId) return p.getCodigo();
                }
            } catch (SQLException ignored) {
                // si no se puede resolver el nombre, se deja en blanco; no bloquea la tabla
            }
            return "";
        }

        private Documento seleccionado() {
            int fila = tabla.getSelectedRow();
            if (fila < 0 || fila >= actuales.size()) return null;
            return actuales.get(fila);
        }

        private void abrirSeleccionado() {
            Documento d = seleccionado();
            if (d == null) { mostrarError("Selecciona un documento de la tabla."); return; }
            try {
                File archivo = documentoService.archivoAbsoluto(d).toFile();
                if (!archivo.exists()) { mostrarError("El archivo ya no existe en disco."); return; }
                Desktop.getDesktop().open(archivo);
            } catch (IOException | UnsupportedOperationException e) {
                mostrarError("No se pudo abrir el archivo: " + e.getMessage());
            }
        }

        private void eliminarSeleccionado() {
            Documento d = seleccionado();
            if (d == null) { mostrarError("Selecciona un documento de la tabla."); return; }
            int op = JOptionPane.showConfirmDialog(DocumentosView.this,
                    "¿Eliminar \"" + d.getNombre() + "\" (" + d.getNombreArchivoOriginal() + ")?\n"
                            + "Se borrara tambien el archivo del disco.",
                    "Eliminar documento", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (op != JOptionPane.YES_OPTION) return;
            try {
                documentoService.eliminar(d);
                cargar();
            } catch (SQLException e) {
                mostrarError("No se pudo eliminar: " + e.getMessage());
            }
        }

        private void subirDocumento() {
            Obra obra = obraActivaProvider.get();
            if (obra == null) { mostrarError("Selecciona una obra activa primero."); return; }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Elegir archivo");
            if (chooser.showOpenDialog(DocumentosView.this) != JFileChooser.APPROVE_OPTION) return;
            File archivoElegido = chooser.getSelectedFile();

            String nombreSugerido = archivoElegido.getName().replaceFirst("\\.[^.]+$", "");
            JTextField campoNombre = new JTextField(nombreSugerido);
            JTextField campoFecha = new JTextField(LocalDate.now().toString());
            JTextArea campoDescripcion = new JTextArea(3, 20);
            campoDescripcion.setLineWrap(true);
            campoDescripcion.setWrapStyleWord(true);

            JComboBox<Partida> comboPartida = new JComboBox<>();
            if (requierePartida) {
                comboPartida.addItem(null);
                try {
                    for (Partida p : partidaService.listarEjecutablesPorObra(obra.getId())) comboPartida.addItem(p);
                } catch (SQLException ignored) {
                    // el formulario sigue funcionando sin la lista de partidas
                }
                comboPartida.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                        Object mostrar = (value == null) ? "— Selecciona una partida —" : value;
                        return super.getListCellRendererComponent(list, mostrar, index, isSelected, cellHasFocus);
                    }
                });
            }

            JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
            form.add(new JLabel("Archivo: " + archivoElegido.getName()));
            form.add(new JLabel("Nombre del documento *"));
            form.add(campoNombre);
            if (requierePartida) {
                form.add(new JLabel("Partida *"));
                form.add(comboPartida);
            }
            form.add(new JLabel("Fecha * (AAAA-MM-DD)"));
            form.add(campoFecha);
            form.add(new JLabel("Descripcion (opcional)"));
            form.add(new JScrollPane(campoDescripcion));

            int op = JOptionPane.showConfirmDialog(DocumentosView.this, form, "Subir documento",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;

            if (campoNombre.getText().trim().isEmpty()) { mostrarError("El nombre es obligatorio."); return; }
            LocalDate fecha = parsearFecha(campoFecha.getText());
            if (fecha == null) { mostrarError("Fecha invalida. Usa el formato AAAA-MM-DD."); return; }
            Partida partidaElegida = requierePartida ? (Partida) comboPartida.getSelectedItem() : null;
            if (requierePartida && partidaElegida == null) { mostrarError("Selecciona la partida a la que corresponde."); return; }

            Documento meta = new Documento();
            meta.setObraId(obra.getId());
            meta.setPartidaId(partidaElegida != null ? partidaElegida.getId() : null);
            meta.setCategoria(categoria);
            meta.setNombre(campoNombre.getText().trim());
            meta.setFecha(fecha);
            meta.setDescripcion(campoDescripcion.getText().trim());

            subirEnSegundoPlano(meta, archivoElegido.toPath());
        }

        /** Copia el archivo (puede pesar cientos de MB) en un hilo de fondo, con un dialogo de espera. */
        private void subirEnSegundoPlano(Documento meta, Path origen) {
            JDialog espera = new JDialog(SwingUtilities.getWindowAncestor(DocumentosView.this),
                    "Subiendo documento", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel panel = new JPanel(new BorderLayout(0, 8));
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));
            panel.add(new JLabel("Copiando archivo, un momento..."), BorderLayout.NORTH);
            JProgressBar barra = new JProgressBar();
            barra.setIndeterminate(true);
            panel.add(barra, BorderLayout.CENTER);
            espera.setContentPane(panel);
            espera.pack();
            espera.setSize(Math.max(320, espera.getWidth()), espera.getHeight());
            espera.setLocationRelativeTo(DocumentosView.this);
            espera.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            SwingWorker<Documento, Void> worker = new SwingWorker<>() {
                @Override protected Documento doInBackground() throws Exception {
                    return documentoService.subir(meta, origen);
                }

                @Override protected void done() {
                    espera.dispose();
                    try {
                        get();
                        cargar();
                    } catch (Exception ex) {
                        Throwable causa = (ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null)
                                ? ex.getCause() : ex;
                        mostrarError("No se pudo subir el documento: " + causa.getMessage());
                    }
                }
            };
            worker.execute();
            espera.setVisible(true); // bloquea aqui hasta que done() llame a espera.dispose()
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

        private void mostrarError(String msg) {
            JOptionPane.showMessageDialog(DocumentosView.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
