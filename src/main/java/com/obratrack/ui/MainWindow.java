package com.obratrack.ui;

import com.obratrack.core.AppInfo;
import com.obratrack.model.Obra;
import com.obratrack.model.Usuario;
import com.obratrack.service.ObraService;
import com.obratrack.service.Permisos;
import com.obratrack.service.SesionActual;
import com.obratrack.ui.views.AjustesView;
import com.obratrack.ui.views.AlmacenView;
import com.obratrack.ui.views.ComparativoView;
import com.obratrack.ui.views.DashboardView;
import com.obratrack.ui.views.ObrasView;
import com.obratrack.ui.views.PartidasView;
import com.obratrack.ui.views.ReportesView;
import com.obratrack.ui.views.UsuariosView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ventana principal: barra superior con obra activa + sidebar de navegacion + area de contenido. */
public class MainWindow extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contenido = new JPanel(cardLayout);
    private final JComboBox<Obra> comboObraActiva = new JComboBox<>();
    private final ObraService obraService = new ObraService();

    /** Botones de navegacion por clave de vista, para poder resaltar el activo. */
    private final Map<String, JButton> botonesNav = new LinkedHashMap<>();
    private final Map<String, String> iconosNav = new LinkedHashMap<>();
    private String vistaActual = "dashboard";

    private DashboardView dashboardView;
    private ObrasView obrasView;
    private PartidasView partidasView;
    private AlmacenView almacenView;
    private ComparativoView comparativoView;
    private ReportesView reportesView;
    private UsuariosView usuariosView;
    private AjustesView ajustesView;

    private final JLabel statusObra = new JLabel();

    private Obra obraActiva;
    private boolean listo = false; // evita refrescos durante la construccion inicial

    public MainWindow() {
        super("ObraTrack — Gestion de Obras");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.net.URL icono = getClass().getResource("/img/appicon.png");
        if (icono != null) setIconImage(new ImageIcon(icono).getImage());
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);
        // Se abre maximizada para adaptarse al tamano de la pantalla del usuario;
        // el boton de restaurar la vuelve al tamano anterior (1280x800).
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(construirBarraSuperior(), BorderLayout.NORTH);
        getContentPane().add(construirSidebar(), BorderLayout.WEST);
        getContentPane().add(contenido, BorderLayout.CENTER);
        getContentPane().add(construirBarraEstado(), BorderLayout.SOUTH);

        construirVistas();
        cargarObrasEnCombo();
        registrarAtajos();
        listo = true;
        // Se difiere la primera carga de datos para que la ventana aparezca al
        // instante tras el login y el contenido entre un tick despues.
        SwingUtilities.invokeLater(() -> mostrarVista("dashboard"));
    }

    private JPanel construirBarraEstado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Theme.BG_SECONDARY);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(4, 14, 4, 14)));
        Usuario u = SesionActual.getUsuario();
        JLabel izq = new JLabel(AppInfo.NOMBRE + " v" + AppInfo.VERSION
                + (u != null ? "   ·   " + u.getNombreParaMostrar() + " (" + u.getRol() + ")" : ""));
        izq.setFont(Theme.FONT_SMALL);
        izq.setForeground(Theme.TEXT_SECONDARY);
        statusObra.setFont(Theme.FONT_SMALL);
        statusObra.setForeground(Theme.TEXT_SECONDARY);
        statusObra.setHorizontalAlignment(SwingConstants.RIGHT);
        barra.add(izq, BorderLayout.WEST);
        barra.add(statusObra, BorderLayout.EAST);
        return barra;
    }

    private JPanel construirBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Theme.BG_SECONDARY);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(10, 16, 10, 16)));

        JLabel titulo = new JLabel("  ObraTrack", Icons.get("obras", 22, Theme.PRIMARY), SwingConstants.LEFT);
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        titulo.setIconTextGap(8);
        barra.add(titulo, BorderLayout.WEST);

        JPanel obraPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        obraPanel.setOpaque(false);
        JLabel lbl = new JLabel("Obra activa:");
        lbl.setForeground(Theme.TEXT_SECONDARY);
        lbl.setFont(Theme.FONT_BASE);
        comboObraActiva.setFont(Theme.FONT_BASE);
        comboObraActiva.setPreferredSize(new Dimension(280, 32));
        comboObraActiva.addActionListener(e -> onObraActivaCambiada());
        obraPanel.add(lbl);
        obraPanel.add(comboObraActiva);
        barra.add(obraPanel, BorderLayout.EAST);

        return barra;
    }

    private JPanel construirSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.BG_SECONDARY);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                new EmptyBorder(14, 12, 14, 12)));

        sidebar.add(botonNav("Dashboard", "home", "dashboard"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(botonNav("Obras", "obras", "obras"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(botonNav("Partidas", "partidas", "partidas"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(botonNav("Almacen", "almacen", "almacen"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(botonNav("Comparativo", "comparativo", "comparativo"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(botonNav("Reportes", "reportes", "reportes"));
        if (Permisos.puedeGestionarUsuarios()) {
            sidebar.add(Box.createVerticalStrut(4));
            sidebar.add(botonNav("Usuarios", "admin", "usuarios"));
        }
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(botonNav("Ajustes", "settings", "ajustes"));

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(seccionAtajos());
        sidebar.add(seccionAtajosPanel());

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(botonPie("Acerca de", "info", Theme.TEXT_SECONDARY, () -> AcercaDe.mostrar(this)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(tarjetaUsuario());
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(botonPie("Cerrar sesion", "logout", Theme.TEXT_SECONDARY, this::cerrarSesion));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(botonPie("Salir", "close", Theme.DANGER, this::confirmarSalida));

        return sidebar;
    }

    /** Boton generico del pie del sidebar (cerrar sesion / salir). */
    private JButton botonPie(String texto, String icono, Color color, Runnable accion) {
        JButton btn = new JButton(texto);
        btn.setIcon(Icons.get(icono, 17, color));
        btn.setIconTextGap(10);
        btn.setFont(Theme.FONT_BOLD);
        btn.setForeground(color);
        btn.setBackground(Theme.BG_SECONDARY);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.getModel().addChangeListener(e ->
                btn.setBackground(btn.getModel().isRollover() ? Theme.NAV_HOVER : Theme.BG_SECONDARY));
        btn.addActionListener(e -> accion.run());
        return btn;
    }

    /** Cierra la sesion actual y vuelve a la pantalla de login. */
    private void cerrarSesion() {
        int op = JOptionPane.showConfirmDialog(this,
                "¿Cerrar la sesion actual?",
                "Cerrar sesion", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (op == JOptionPane.YES_OPTION) {
            SesionActual.cerrar();
            dispose();
            new LoginView(() -> new MainWindow().setVisible(true)).setVisible(true);
        }
    }

    /** Boton para cerrar la aplicacion, con confirmacion previa. */
    private JButton botonSalir() {
        JButton btn = new JButton("Salir");
        btn.setIcon(Icons.get("logout", 18, Theme.DANGER));
        btn.setIconTextGap(10);
        btn.setFont(Theme.FONT_BOLD);
        btn.setForeground(Theme.DANGER);
        btn.setBackground(Theme.BG_SECONDARY);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.getModel().addChangeListener(e ->
                btn.setBackground(btn.getModel().isRollover() ? Theme.NAV_HOVER : Theme.BG_SECONDARY));
        btn.addActionListener(e -> confirmarSalida());
        return btn;
    }

    private void confirmarSalida() {
        int op = JOptionPane.showConfirmDialog(this,
                "¿Seguro que deseas salir de ObraTrack?",
                "Salir", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (op == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    private JLabel seccionAtajos() {
        JLabel titulo = new JLabel("ATAJOS RAPIDOS");
        titulo.setFont(Theme.FONT_SMALL);
        titulo.setForeground(Theme.TEXT_SECONDARY);
        titulo.setBorder(new EmptyBorder(0, 10, 6, 0));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        return titulo;
    }

    private JButton botonNav(String texto, String icono, String vista) {
        JButton btn = new JButton(texto);
        btn.setIcon(Icons.get(icono, 20, Theme.TEXT_SECONDARY));
        btn.setIconTextGap(12);
        btn.setFont(Theme.FONT_BASE);
        btn.setForeground(Theme.TEXT_SECONDARY);
        btn.setBackground(Theme.BG_SECONDARY);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> mostrarVista(vista));
        btn.getModel().addChangeListener(e -> {
            if (!vista.equals(vistaActual)) {
                btn.setBackground(btn.getModel().isRollover() ? Theme.NAV_HOVER : Theme.BG_SECONDARY);
            }
        });
        botonesNav.put(vista, btn);
        iconosNav.put(vista, icono);
        return btn;
    }

    /** Botones de "Atajos rapidos": abren vistas de creacion rapida. */
    private JButton botonAtajo(String texto, String vista) {
        JButton btn = new JButton(texto);
        btn.setIcon(Icons.get("add", 16, Theme.PRIMARY));
        btn.setIconTextGap(8);
        btn.setFont(Theme.FONT_BASE);
        btn.setForeground(Theme.TEXT_SECONDARY);
        btn.setBackground(Theme.BG_SECONDARY);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> mostrarVista(vista));
        btn.getModel().addChangeListener(e ->
                btn.setBackground(btn.getModel().isRollover() ? Theme.NAV_HOVER : Theme.BG_SECONDARY));
        return btn;
    }

    private JPanel seccionAtajosPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(botonAtajo("Nueva obra", "obras"));
        panel.add(botonAtajo("Nueva partida", "partidas"));
        panel.add(botonAtajo("Nuevo reporte", "reportes"));
        return panel;
    }

    private JPanel tarjetaUsuario() {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel avatar = new JLabel(Icons.get("account", 30, Theme.TEXT_PRIMARY));
        card.add(avatar, BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        Usuario u = SesionActual.getUsuario();
        JLabel nombre = new JLabel(u != null ? u.getNombreParaMostrar() : "Usuario");
        nombre.setFont(Theme.FONT_BOLD);
        nombre.setForeground(Theme.TEXT_PRIMARY);
        JLabel rol = new JLabel(u != null ? rolLegible(u.getRol()) : "Gestion de obras");
        rol.setFont(Theme.FONT_SMALL);
        rol.setForeground(Theme.TEXT_SECONDARY);
        textos.add(nombre);
        textos.add(rol);
        card.add(textos, BorderLayout.CENTER);

        return card;
    }

    private String rolLegible(Usuario.Rol rol) {
        return switch (rol) {
            case ADMIN -> "Administrador";
            case JEFE_OBRA -> "Jefe de obra";
            case ALMACENERO -> "Almacenero";
        };
    }

    private void construirVistas() {
        dashboardView = new DashboardView(this::obtenerObraActiva);
        obrasView = new ObrasView(this::onObraCreada, this::onObrasModificadas);
        partidasView = new PartidasView(this::obtenerObraActiva);
        almacenView = new AlmacenView(this::obtenerObraActiva, this::refrescarVistaActual);
        comparativoView = new ComparativoView(this::obtenerObraActiva);
        reportesView = new ReportesView(this::obtenerObraActiva);
        usuariosView = new UsuariosView();
        ajustesView = new AjustesView();

        contenido.add(dashboardView, "dashboard");
        contenido.add(obrasView, "obras");
        contenido.add(partidasView, "partidas");
        contenido.add(almacenView, "almacen");
        contenido.add(comparativoView, "comparativo");
        contenido.add(reportesView, "reportes");
        contenido.add(usuariosView, "usuarios");
        contenido.add(ajustesView, "ajustes");
    }

    public void mostrarVista(String nombre) {
        vistaActual = nombre;
        cardLayout.show(contenido, nombre);
        resaltarNavActivo();
        refrescarVistaActual();
    }

    /** Aplica el estilo activo/inactivo a cada boton del sidebar. */
    private void resaltarNavActivo() {
        for (Map.Entry<String, JButton> entry : botonesNav.entrySet()) {
            boolean activo = entry.getKey().equals(vistaActual);
            JButton btn = entry.getValue();
            btn.setBackground(activo ? Theme.NAV_ACTIVE : Theme.BG_SECONDARY);
            btn.setForeground(activo ? Theme.PRIMARY : Theme.TEXT_SECONDARY);
            btn.setFont(activo ? Theme.FONT_BOLD : Theme.FONT_BASE);
            Color colorIcono = activo ? Theme.PRIMARY : Theme.TEXT_SECONDARY;
            btn.setIcon(Icons.get(iconosNav.get(entry.getKey()), 20, colorIcono));
        }
    }

    /**
     * Refresca UNICAMENTE la vista visible. Antes se refrescaban las 5 vistas en cada
     * clic o cambio de obra (5x consultas a la BD), lo que hacia lenta la navegacion.
     * Las demas vistas se actualizan de forma perezosa cuando se muestran.
     */
    private void refrescarVistaActual() {
        switch (vistaActual) {
            case "dashboard" -> dashboardView.refrescar();
            case "partidas" -> partidasView.refrescar();
            case "almacen" -> almacenView.refrescar();
            case "comparativo" -> comparativoView.refrescar();
            case "reportes" -> reportesView.refrescar();
            case "usuarios" -> usuariosView.refrescar();
            case "ajustes" -> ajustesView.refrescar();
            default -> { /* la vista de obras se refresca sola */ }
        }
    }

    /** Atajos de teclado globales (Ctrl+1..6 vistas, F1 acerca, Ctrl+L salir sesion, Ctrl+Q salir). */
    private void registrarAtajos() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();
        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_1, ctrl), "v1", () -> mostrarVista("dashboard"));
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_2, ctrl), "v2", () -> mostrarVista("obras"));
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_3, ctrl), "v3", () -> mostrarVista("partidas"));
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_4, ctrl), "v4", () -> mostrarVista("almacen"));
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_5, ctrl), "v5", () -> mostrarVista("comparativo"));
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_6, ctrl), "v6", () -> mostrarVista("reportes"));
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "acerca", () -> AcercaDe.mostrar(this));
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_L, ctrl), "logout", this::cerrarSesion);
        atajo(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ctrl), "salir", this::confirmarSalida);
    }

    private void atajo(InputMap im, ActionMap am, KeyStroke ks, String id, Runnable accion) {
        im.put(ks, id);
        am.put(id, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { accion.run(); }
        });
    }

    private Obra obtenerObraActiva() {
        return obraActiva;
    }

    private void onObraActivaCambiada() {
        Object seleccion = comboObraActiva.getSelectedItem();
        obraActiva = (seleccion instanceof Obra) ? (Obra) seleccion : null;
        statusObra.setText(obraActiva != null ? "Obra activa: " + obraActiva.getNombre() : "Sin obra activa");
        if (listo) refrescarVistaActual(); // no refresca durante la carga inicial del combo
    }

    private void onObraCreada(Obra nuevaObra) {
        cargarObrasEnCombo();
        comboObraActiva.setSelectedItem(nuevaObra);
        mostrarVista("partidas");
    }

    /** Tras editar o eliminar una obra: recarga el selector superior y refresca la vista. */
    private void onObrasModificadas() {
        cargarObrasEnCombo();
        refrescarVistaActual();
    }

    public void cargarObrasEnCombo() {
        try {
            List<Obra> activas = obraService.listarActivas();
            Obra seleccionPrevia = obraActiva;
            comboObraActiva.removeAllItems();
            for (Obra o : activas) {
                comboObraActiva.addItem(o);
            }
            if (seleccionPrevia != null && activas.contains(seleccionPrevia)) {
                comboObraActiva.setSelectedItem(seleccionPrevia);
            } else if (!activas.isEmpty()) {
                comboObraActiva.setSelectedIndex(0);
            } else {
                obraActiva = null;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudieron cargar las obras: " + e.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }
}
