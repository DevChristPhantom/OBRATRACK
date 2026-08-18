package com.obratrack.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.obratrack.model.Usuario;
import com.obratrack.service.IUsuarioService;
import com.obratrack.service.ServiceFactory;
import com.obratrack.service.SesionActual;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Pantalla de inicio de sesion. Panel izquierdo claro con el formulario y panel
 * derecho con la foto de obra de fondo, el logo de Grupo Titan G&amp;L y los modulos.
 * Al autenticar correctamente, guarda la sesion y abre la ventana principal.
 */
public class LoginView extends JFrame {

    // Paleta del panel claro (independiente del tema oscuro global)
    private static final Color CARD = new Color(0xf7, 0xf8, 0xfb);
    private static final Color TITULO = new Color(0x1a, 0x20, 0x33);
    private static final Color SUB = new Color(0x6b, 0x72, 0x84);
    private static final Color ICONO_CAMPO = new Color(0x9a, 0xa2, 0xb2);
    private static final Color AZUL = Theme.PRIMARY;
    private static final Color CLARO_TXT = new Color(0xd6, 0xdd, 0xea);
    private static final String ESTILO_CAMPO = "arc:14;borderColor:#d8dde6;focusedBorderColor:#3b82f6";

    private final IUsuarioService usuarioService = ServiceFactory.usuario();
    private final Runnable alIngresar;
    private final Preferences prefs = Preferences.userNodeForPackage(LoginView.class);

    private final JTextField campoUsuario = new JTextField();
    private final JPasswordField campoPassword = new JPasswordField();
    private final JCheckBox chkRecordar = new JCheckBox("Recordarme");
    private final JLabel labelMensaje = new JLabel(" ");
    private JButton btnIngresar;
    private boolean autenticando = false;
    private char echoOriginal;

    public LoginView(Runnable alIngresar) {
        super("ObraTrack — Iniciar sesion");
        this.alIngresar = alIngresar;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        URL iconoApp = getClass().getResource("/img/appicon.png");
        if (iconoApp != null) setIconImage(new ImageIcon(iconoApp).getImage());
        setSize(980, 600);
        setMinimumSize(new Dimension(840, 540));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(CARD);
        root.add(construirFormulario());
        root.add(construirMarca());
        setContentPane(root);

        String ultimo = prefs.get("ultimoUsuario", "");
        if (!ultimo.isBlank()) {
            campoUsuario.setText(ultimo);
            chkRecordar.setSelected(true);
            SwingUtilities.invokeLater(campoPassword::requestFocusInWindow);
        } else {
            SwingUtilities.invokeLater(campoUsuario::requestFocusInWindow);
        }
    }

    private JPanel construirFormulario() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CARD);
        wrap.setBorder(new EmptyBorder(26, 46, 26, 46));

        // Logo arriba a la izquierda
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoRow.setOpaque(false);
        logoRow.add(new JLabel(Icons.get("obras", 30, AZUL)));
        JPanel logoTxt = new JPanel();
        logoTxt.setOpaque(false);
        logoTxt.setLayout(new BoxLayout(logoTxt, BoxLayout.Y_AXIS));
        JLabel marca = new JLabel("ObraTrack");
        marca.setFont(new Font("Segoe UI", Font.BOLD, 20));
        marca.setForeground(TITULO);
        JLabel marcaSub = new JLabel("Gestion de Obras");
        marcaSub.setFont(Theme.FONT_SMALL);
        marcaSub.setForeground(SUB);
        logoTxt.add(marca);
        logoTxt.add(marcaSub);
        logoRow.add(logoTxt);
        wrap.add(logoRow, BorderLayout.NORTH);

        // Columna centrada con el formulario
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(avatar());
        col.add(Box.createVerticalStrut(16));

        JLabel bienvenido = new JLabel("¡Bienvenido de nuevo!");
        bienvenido.setFont(new Font("Segoe UI", Font.BOLD, 19));
        bienvenido.setForeground(TITULO);
        bienvenido.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(bienvenido);
        JLabel instr = new JLabel("Inicia sesion para continuar");
        instr.setFont(Theme.FONT_BASE);
        instr.setForeground(SUB);
        instr.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(Box.createVerticalStrut(4));
        col.add(instr);
        col.add(Box.createVerticalStrut(22));

        // Campo usuario (icono a la izquierda + placeholder)
        estilizarCampo(campoUsuario, "account", "Usuario o correo electronico");
        campoUsuario.addKeyListener(enterSalta(campoPassword));
        col.add(campoUsuario);
        col.add(Box.createVerticalStrut(12));

        // Campo contrasena (candado + placeholder + ojo para mostrar/ocultar)
        estilizarCampo(campoPassword, "lock", "Contrasena");
        echoOriginal = campoPassword.getEchoChar();
        campoPassword.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) intentarIngresar();
            }
        });
        JToggleButton verPass = new JToggleButton(Icons.get("eye", 18, ICONO_CAMPO));
        verPass.setToolTipText("Mostrar u ocultar la contrasena");
        verPass.setBorderPainted(false);
        verPass.setContentAreaFilled(false);
        verPass.setFocusPainted(false);
        verPass.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        verPass.addActionListener(e -> {
            boolean mostrar = verPass.isSelected();
            campoPassword.setEchoChar(mostrar ? (char) 0 : echoOriginal);
            verPass.setIcon(Icons.get("eye", 18, mostrar ? AZUL : ICONO_CAMPO));
        });
        campoPassword.putClientProperty("JTextField.trailingComponent", verPass);
        col.add(campoPassword);
        col.add(Box.createVerticalStrut(12));

        // Fila: Recordarme + ¿Olvidaste tu contrasena?
        JPanel opciones = new JPanel(new BorderLayout());
        opciones.setOpaque(false);
        opciones.setMaximumSize(new Dimension(360, 28));
        opciones.setAlignmentX(Component.CENTER_ALIGNMENT);
        chkRecordar.setOpaque(false);
        chkRecordar.setFont(Theme.FONT_BASE);
        chkRecordar.setForeground(SUB);
        JButton olvido = enlace("¿Olvidaste tu contrasena?");
        olvido.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Pide al administrador que restablezca tu contrasena desde la gestion de usuarios.",
                "Recuperar contrasena", JOptionPane.INFORMATION_MESSAGE));
        opciones.add(chkRecordar, BorderLayout.WEST);
        opciones.add(olvido, BorderLayout.EAST);
        col.add(opciones);
        col.add(Box.createVerticalStrut(16));

        // Boton principal
        btnIngresar = new JButton("Iniciar sesion");
        btnIngresar.setFont(Theme.FONT_BOLD);
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setBackground(AZUL);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIngresar.putClientProperty(FlatClientProperties.STYLE, "arc:14");
        btnIngresar.setMaximumSize(new Dimension(360, 46));
        btnIngresar.setPreferredSize(new Dimension(360, 46));
        btnIngresar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnIngresar.addActionListener(e -> intentarIngresar());
        col.add(btnIngresar);

        col.add(Box.createVerticalStrut(8));
        labelMensaje.setFont(Theme.FONT_SMALL);
        labelMensaje.setForeground(Theme.DANGER);
        labelMensaje.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(labelMensaje);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(col);
        wrap.add(center, BorderLayout.CENTER);

        // Pie: informacion protegida
        JLabel pie = new JLabel("Tu informacion esta protegida", Icons.get("admin", 15, SUB), SwingConstants.LEFT);
        pie.setIconTextGap(6);
        pie.setFont(Theme.FONT_SMALL);
        pie.setForeground(SUB);
        pie.setHorizontalAlignment(SwingConstants.CENTER);
        wrap.add(pie, BorderLayout.SOUTH);

        return wrap;
    }

    /** Aplica el estilo de campo claro y redondeado con icono principal y placeholder. */
    private void estilizarCampo(JTextField campo, String icono, String placeholder) {
        campo.setForeground(TITULO);
        campo.setBackground(Color.WHITE);
        campo.setCaretColor(TITULO);
        campo.setFont(Theme.FONT_BASE);
        campo.setMaximumSize(new Dimension(360, 46));
        campo.setPreferredSize(new Dimension(360, 46));
        campo.setAlignmentX(Component.CENTER_ALIGNMENT);
        campo.putClientProperty("JTextField.placeholderText", placeholder);
        campo.putClientProperty("JTextField.leadingIcon", Icons.get(icono, 18, ICONO_CAMPO));
        campo.putClientProperty(FlatClientProperties.STYLE, ESTILO_CAMPO);
    }

    private JButton enlace(String texto) {
        JButton b = new JButton(texto);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(AZUL);
        b.setFont(Theme.FONT_SMALL);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0, 0, 0, 0));
        return b;
    }

    /** Avatar circular azul con el icono de usuario. */
    private JComponent avatar() {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AZUL);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                Icon ic = Icons.get("account", 40, Color.WHITE);
                ic.paintIcon(this, g2, (getWidth() - ic.getIconWidth()) / 2, (getHeight() - ic.getIconHeight()) / 2);
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(78, 78));
        c.setMaximumSize(new Dimension(78, 78));
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    private JPanel construirMarca() {
        URL fondoUrl = getClass().getResource("/img/fondoobra.jpg");
        Image fondo = (fondoUrl != null) ? new ImageIcon(fondoUrl).getImage() : null;

        FondoPanel panel = new FondoPanel(fondo);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(48, 40, 48, 40));

        JLabel logoImg = new JLabel();
        logoImg.setAlignmentX(Component.CENTER_ALIGNMENT);
        URL url = getClass().getResource("/img/logoTitan.png");
        if (url != null) {
            ImageIcon icon = new ImageIcon(url);
            Image escalada = icon.getImage().getScaledInstance(260, -1, Image.SCALE_SMOOTH);
            logoImg.setIcon(new ImageIcon(escalada));
        } else {
            logoImg.setText("GRUPO TITAN G&L S.A.C.");
            logoImg.setForeground(Color.WHITE);
            logoImg.setFont(new Font("Segoe UI", Font.BOLD, 18));
        }
        panel.add(Box.createVerticalGlue());

        JLabel bienvenido = new JLabel("Bienvenido a ObraTrack");
        bienvenido.setFont(new Font("Segoe UI", Font.BOLD, 22));
        bienvenido.setForeground(Color.WHITE);
        bienvenido.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(bienvenido);
        panel.add(Box.createVerticalStrut(16));
        panel.add(logoImg);
        panel.add(Box.createVerticalStrut(16));

        JLabel tagline = new JLabel("<html><div style='text-align:center;width:340px'>"
                + "Sistema integral para el control de partidas, precios de obra y gestion de almacen."
                + "</div></html>");
        tagline.setFont(Theme.FONT_BASE);
        tagline.setForeground(CLARO_TXT);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(tagline);
        panel.add(Box.createVerticalStrut(28));

        JPanel modulos = new JPanel(new GridLayout(1, 4, 12, 0));
        modulos.setOpaque(false);
        modulos.setMaximumSize(new Dimension(470, 96));
        modulos.setAlignmentX(Component.CENTER_ALIGNMENT);
        modulos.add(modulo("partidas", "Control de Partidas"));
        modulos.add(modulo("money", "Precios de Obra"));
        modulos.add(modulo("almacen", "Gestion de Almacen"));
        modulos.add(modulo("reportes", "Reportes y Analisis"));
        panel.add(modulos);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel modulo(String icono, String texto) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel ic = new JLabel(new IconoRecuadro(Icons.get(icono, 24, Color.WHITE)));
        ic.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel tx = new JLabel("<html><div style='text-align:center;width:98px'>" + texto + "</div></html>");
        tx.setFont(Theme.FONT_SMALL);
        tx.setForeground(CLARO_TXT);
        tx.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(ic);
        p.add(Box.createVerticalStrut(6));
        p.add(tx);
        return p;
    }

    private void intentarIngresar() {
        if (autenticando) return; // evita doble envio mientras verifica
        String usuario = campoUsuario.getText().trim();
        char[] pass = campoPassword.getPassword();
        if (usuario.isEmpty() || pass.length == 0) {
            mostrarError("Ingresa tu usuario y contrasena.");
            return;
        }

        // La verificacion (PBKDF2) se hace en segundo plano para no congelar la ventana.
        autenticando = true;
        btnIngresar.setEnabled(false);
        btnIngresar.setText("Verificando...");
        mostrarInfo("Verificando credenciales...");

        new SwingWorker<Optional<Usuario>, Void>() {
            @Override protected Optional<Usuario> doInBackground() throws Exception {
                try {
                    return usuarioService.autenticar(usuario, pass);
                } finally {
                    java.util.Arrays.fill(pass, '\0'); // limpia la contrasena de memoria
                }
            }

            @Override protected void done() {
                autenticando = false;
                btnIngresar.setEnabled(true);
                btnIngresar.setText("Iniciar sesion");
                try {
                    Optional<Usuario> autenticado = get();
                    if (autenticado.isEmpty()) {
                        long seg = usuarioService.segundosBloqueoRestantes(usuario);
                        if (seg > 0) {
                            mostrarError("Cuenta bloqueada por varios intentos fallidos. Espera " + seg + " s.");
                        } else {
                            mostrarError("Usuario o contrasena incorrectos.");
                        }
                        campoPassword.setText("");
                        return;
                    }
                    SesionActual.iniciar(autenticado.get());
                    if (chkRecordar.isSelected()) {
                        prefs.put("ultimoUsuario", usuario);
                    } else {
                        prefs.remove("ultimoUsuario");
                    }
                    dispose();
                    alIngresar.run();
                } catch (Exception e) {
                    mostrarError("No se pudo verificar el acceso: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void mostrarError(String texto) {
        labelMensaje.setForeground(Theme.DANGER);
        labelMensaje.setText(texto);
    }

    private void mostrarInfo(String texto) {
        labelMensaje.setForeground(SUB);
        labelMensaje.setText(texto);
    }

    private KeyAdapter enterSalta(Component siguiente) {
        return new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) siguiente.requestFocus();
            }
        };
    }

    /** Recuadro redondeado semitransparente con un icono centrado (modulos del panel derecho). */
    private static class IconoRecuadro implements Icon {
        private final Icon icono;
        private static final int LADO = 48;

        IconoRecuadro(Icon icono) { this.icono = icono; }

        @Override public int getIconWidth() { return LADO; }
        @Override public int getIconHeight() { return LADO; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(x, y, LADO, LADO, 14, 14);
            icono.paintIcon(c, g2, x + (LADO - icono.getIconWidth()) / 2, y + (LADO - icono.getIconHeight()) / 2);
            g2.dispose();
        }
    }

    /**
     * Panel que pinta la foto de obra cubriendo todo el area y encima una capa
     * azul oscura semitransparente, para que el texto blanco se lea bien.
     */
    private static class FondoPanel extends JPanel {
        private final Image fondo;

        FondoPanel(Image fondo) {
            this.fondo = fondo;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            if (fondo != null && fondo.getWidth(this) > 0) {
                double ir = (double) fondo.getWidth(this) / fondo.getHeight(this);
                double pr = (double) w / h;
                int dw, dh, dx, dy;
                if (pr > ir) { dw = w; dh = (int) (w / ir); dx = 0; dy = (h - dh) / 2; }
                else { dh = h; dw = (int) (h * ir); dy = 0; dx = (w - dw) / 2; }
                g.drawImage(fondo, dx, dy, dw, dh, this);
                g.setColor(new Color(0x0d, 0x1b, 0x38, 200));
                g.fillRect(0, 0, w, h);
            } else {
                g.setColor(Theme.BG_CARD);
                g.fillRect(0, 0, w, h);
            }
        }
    }
}
