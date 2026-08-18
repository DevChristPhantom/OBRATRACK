package com.obratrack.ui.views;

import com.obratrack.model.AsientoCuaderno;
import com.obratrack.model.Obra;
import com.obratrack.service.CuadernoCalculo;
import com.obratrack.service.ICuadernoService;
import com.obratrack.service.Permisos;
import com.obratrack.service.ServiceFactory;
import com.obratrack.ui.Icons;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/**
 * Cuaderno de obra digital: bitacora diaria de la obra (incidencias, clima,
 * personal en sitio). Es un registro legal, append-only: solo se agregan
 * asientos nuevos, nunca se editan ni se borran los existentes.
 */
public class CuadernoView extends JPanel {

    private final ICuadernoService cuadernoService = ServiceFactory.cuaderno();
    private final Supplier<Obra> obraActivaProvider;

    private final JLabel tituloObra = new JLabel();
    private final JLabel resumenTexto = new JLabel();
    private final JPanel feed = new JPanel();

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CuadernoView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Cuaderno de Obra");
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

        feed.setLayout(new BoxLayout(feed, BoxLayout.Y_AXIS));
        feed.setOpaque(false);
        JScrollPane scroll = new JScrollPane(feed);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        centro.add(scroll, BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
    }

    private JPanel construirBarra() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);

        JButton btnNuevo = new JButton("Nuevo asiento", Icons.get("add", 16, Color.WHITE));
        btnNuevo.setIconTextGap(6);
        btnNuevo.setFont(Theme.FONT_BOLD);
        btnNuevo.setBackground(Theme.ACCENT);
        btnNuevo.setForeground(Color.WHITE);
        btnNuevo.setFocusPainted(false);
        btnNuevo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNuevo.addActionListener(e -> nuevoAsiento());
        btnNuevo.setEnabled(Permisos.puedeEscribir());
        barra.add(btnNuevo, BorderLayout.WEST);

        resumenTexto.setFont(Theme.FONT_BOLD);
        resumenTexto.setForeground(Theme.TEXT_SECONDARY);
        barra.add(resumenTexto, BorderLayout.EAST);
        return barra;
    }

    // ============================================================
    //  Carga y refresco
    // ============================================================

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        feed.removeAll();

        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa para ver su cuaderno de obra");
            resumenTexto.setText("");
            repintar();
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre());

        try {
            List<AsientoCuaderno> asientos = cuadernoService.listarPorObra(obra.getId());
            LocalDate hoy = LocalDate.now();

            if (asientos.isEmpty()) {
                resumenTexto.setText("Sin asientos registrados");
                JLabel vacio = new JLabel("Todavia no hay anotaciones. Registra el primer asiento del dia.");
                vacio.setFont(Theme.FONT_BASE);
                vacio.setForeground(Theme.TEXT_SECONDARY);
                vacio.setAlignmentX(Component.LEFT_ALIGNMENT);
                feed.add(vacio);
            } else {
                long ultimos7 = CuadernoCalculo.conteoUltimosDias(asientos, hoy, 7);
                double personalProm = CuadernoCalculo.personalPromedio(asientos, hoy, 7);
                String etiquetaAsientos = asientos.size() == 1 ? "asiento" : "asientos";
                resumenTexto.setText(String.format("%d %s   ·   %d en los ultimos 7 dias   ·   Personal promedio (7d): %.0f",
                        asientos.size(), etiquetaAsientos, ultimos7, personalProm));
                for (AsientoCuaderno a : asientos) {
                    feed.add(tarjetaAsiento(a));
                    feed.add(Box.createVerticalStrut(10));
                }
            }
            repintar();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el cuaderno de obra: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void repintar() {
        feed.revalidate();
        feed.repaint();
    }

    // ============================================================
    //  Tarjeta de un asiento
    // ============================================================

    private JPanel tarjetaAsiento(AsientoCuaderno a) {
        // getMaximumSize() se sobreescribe para que el ancho se estire con el contenedor
        // pero el alto se quede en el preferido (el del contenido): sin esto, BoxLayout
        // reparte el espacio sobrante del scroll estirando las tarjetas verticalmente.
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        card.setBackground(Theme.BG_SECONDARY);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, colorTipo(a.getTipo())),
                new EmptyBorder(12, 14, 12, 14)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JLabel titulo = new JLabel("Asiento N° " + a.getNumero() + "   ·   "
                + (a.getFecha() != null ? a.getFecha().format(FMT_FECHA) : "-")
                + "   ·   " + textoTipo(a.getTipo())
                + (a.getClima() != null ? "   ·   " + textoClima(a.getClima()) : ""));
        titulo.setFont(Theme.FONT_BOLD);
        titulo.setForeground(colorTipo(a.getTipo()));
        cabecera.add(titulo, BorderLayout.WEST);

        JLabel personal = new JLabel("Personal en obra: " + a.getPersonalObra());
        personal.setFont(Theme.FONT_SMALL);
        personal.setForeground(Theme.TEXT_SECONDARY);
        cabecera.add(personal, BorderLayout.EAST);
        card.add(cabecera, BorderLayout.NORTH);

        JTextArea texto = new JTextArea(a.getTexto());
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setFont(Theme.FONT_BASE);
        texto.setForeground(Theme.TEXT_PRIMARY);
        texto.setOpaque(false);
        texto.setBorder(null);
        card.add(texto, BorderLayout.CENTER);

        JLabel pie = new JLabel("Registrado por " + (a.getUsuarioRegistro() != null ? a.getUsuarioRegistro() : "-")
                + "   ·   " + formatearCreadoEn(a.getCreadoEn()));
        pie.setFont(Theme.FONT_SMALL);
        pie.setForeground(Theme.TEXT_SECONDARY);
        card.add(pie, BorderLayout.SOUTH);

        return card;
    }

    private String formatearCreadoEn(String creadoEn) {
        if (creadoEn == null || creadoEn.isBlank()) return "";
        try {
            return java.time.LocalDateTime.parse(creadoEn, FMT_TS).format(FMT_HORA);
        } catch (Exception e) {
            return creadoEn;
        }
    }

    private Color colorTipo(AsientoCuaderno.Tipo tipo) {
        return switch (tipo) {
            case RESIDENTE -> Theme.PRIMARY;
            case SUPERVISOR -> Theme.PURPLE;
            case INSPECTOR -> Theme.WARNING;
            case OTRO -> Theme.TEXT_SECONDARY;
        };
    }

    private String textoTipo(AsientoCuaderno.Tipo tipo) {
        return switch (tipo) {
            case RESIDENTE -> "Residente";
            case SUPERVISOR -> "Supervisor";
            case INSPECTOR -> "Inspector";
            case OTRO -> "Otro";
        };
    }

    private String textoClima(AsientoCuaderno.Clima clima) {
        return switch (clima) {
            case SOLEADO -> "Soleado";
            case NUBLADO -> "Nublado";
            case LLUVIOSO -> "Lluvioso";
            case OTRO -> "Otro";
        };
    }

    // ============================================================
    //  Alta (append-only: no hay edicion ni borrado)
    // ============================================================

    private void nuevoAsiento() {
        Obra obra = obraActivaProvider.get();
        if (obra == null) {
            mostrarError("Selecciona una obra activa primero.");
            return;
        }

        JTextField campoFecha = new JTextField(LocalDate.now().toString());
        JComboBox<AsientoCuaderno.Tipo> comboTipo = new JComboBox<>(AsientoCuaderno.Tipo.values());
        comboTipo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                Object mostrar = (value instanceof AsientoCuaderno.Tipo t) ? textoTipo(t) : value;
                return super.getListCellRendererComponent(list, mostrar, index, isSelected, cellHasFocus);
            }
        });
        JComboBox<AsientoCuaderno.Clima> comboClima = new JComboBox<>(AsientoCuaderno.Clima.values());
        comboClima.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                Object mostrar = (value instanceof AsientoCuaderno.Clima c) ? textoClima(c) : value;
                return super.getListCellRendererComponent(list, mostrar, index, isSelected, cellHasFocus);
            }
        });
        JTextField campoPersonal = new JTextField("0");
        JTextArea campoTexto = new JTextArea(8, 30);
        campoTexto.setLineWrap(true);
        campoTexto.setWrapStyleWord(true);

        JPanel form = new JPanel(new BorderLayout(0, 8));
        JPanel datos = new JPanel(new GridLayout(0, 1, 0, 4));
        datos.add(new JLabel("Fecha * (AAAA-MM-DD)"));
        datos.add(campoFecha);
        datos.add(new JLabel("Tipo de anotacion"));
        datos.add(comboTipo);
        datos.add(new JLabel("Clima"));
        datos.add(comboClima);
        datos.add(new JLabel("Personal en obra (numero de trabajadores)"));
        datos.add(campoPersonal);
        form.add(datos, BorderLayout.NORTH);
        JPanel textoWrap = new JPanel(new BorderLayout(0, 4));
        textoWrap.add(new JLabel("Anotacion *"), BorderLayout.NORTH);
        textoWrap.add(new JScrollPane(campoTexto), BorderLayout.CENTER);
        form.add(textoWrap, BorderLayout.CENTER);

        int op = JOptionPane.showConfirmDialog(this, form, "Nuevo asiento del cuaderno de obra",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        LocalDate fecha = parsearFecha(campoFecha.getText());
        if (fecha == null) {
            mostrarError("Fecha invalida. Usa el formato AAAA-MM-DD.");
            return;
        }
        String texto = campoTexto.getText().trim();
        if (texto.isEmpty()) {
            mostrarError("La anotacion no puede estar vacia.");
            return;
        }
        Integer personal = parsearEntero(campoPersonal.getText());
        if (personal == null || personal < 0) {
            mostrarError("El personal en obra debe ser un numero mayor o igual a 0.");
            return;
        }

        AsientoCuaderno a = new AsientoCuaderno();
        a.setObraId(obra.getId());
        a.setFecha(fecha);
        a.setTipo((AsientoCuaderno.Tipo) comboTipo.getSelectedItem());
        a.setClima((AsientoCuaderno.Clima) comboClima.getSelectedItem());
        a.setPersonalObra(personal);
        a.setTexto(texto);

        try {
            cuadernoService.crear(a);
            refrescar();
        } catch (SQLException e) {
            mostrarError("No se pudo guardar el asiento: " + e.getMessage());
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

    private Integer parsearEntero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
