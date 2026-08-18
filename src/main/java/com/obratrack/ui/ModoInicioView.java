package com.obratrack.ui;

import com.obratrack.core.Modo;
import com.obratrack.core.RedEstado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Pantalla de primer arranque: elige si esta PC trabaja sola, es la PC anfitriona
 * de la obra (aloja los datos y los sirve por red local) o se conecta como cliente
 * a la PC anfitriona de la obra. Se muestra ANTES de tocar SQLite (ver {@code Main}),
 * para que el modo cliente nunca cree un archivo local.
 */
public class ModoInicioView extends JFrame {

    private final Runnable alElegir;
    private final JRadioButton optLocal = new JRadioButton("Este equipo trabaja solo (como hasta ahora)");
    private final JRadioButton optAnfitriona = new JRadioButton("Este equipo es la PC anfitriona de la obra");
    private final JRadioButton optCliente = new JRadioButton("Conectarme a la PC anfitriona de esta obra");
    private final JTextField campoPuerto = new JTextField(String.valueOf(RedEstado.PUERTO_DEFECTO));
    private final JTextField campoHost = new JTextField();
    private final JLabel labelMensaje = new JLabel(" ");

    public ModoInicioView(Runnable alElegir) {
        super("ObraTrack — Modo de esta PC");
        this.alElegir = alElegir;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.net.URL icono = getClass().getResource("/img/appicon.png");
        if (icono != null) setIconImage(new ImageIcon(icono).getImage());
        setSize(560, 580);
        setMinimumSize(new Dimension(520, 540));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.BG_PRIMARY);
        root.setBorder(new EmptyBorder(28, 32, 20, 32));

        JLabel titulo = new JLabel("¿Como trabaja esta PC en esta obra?");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        JLabel sub = new JLabel("<html><div style='width:460px'>Elige una opcion; se recuerda para los "
                + "proximos arranques (se puede cambiar despues desde Ajustes).</div></html>");
        sub.setFont(Theme.FONT_BASE);
        sub.setForeground(Theme.TEXT_SECONDARY);
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(titulo);
        header.add(Box.createVerticalStrut(6));
        header.add(sub);
        root.add(header, BorderLayout.NORTH);

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(optLocal);
        grupo.add(optAnfitriona);
        grupo.add(optCliente);
        optLocal.setSelected(true);
        for (JRadioButton r : new JRadioButton[]{optLocal, optAnfitriona, optCliente}) {
            r.setOpaque(false);
            r.setFont(Theme.FONT_BOLD);
            r.setForeground(Theme.TEXT_PRIMARY);
            r.setAlignmentX(Component.LEFT_ALIGNMENT);
            r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(optLocal);
        col.add(explicacion("No expone ni consume datos por red. Igual que la version de escritorio de siempre."));
        col.add(Box.createVerticalStrut(14));

        col.add(optAnfitriona);
        col.add(explicacion("Guarda los datos de esta obra y los sirve por red local a las demas PC de la obra."));
        JPanel filaPuerto = new JPanel(new BorderLayout(8, 0));
        filaPuerto.setOpaque(false);
        filaPuerto.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        filaPuerto.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaPuerto.setBorder(new EmptyBorder(4, 24, 0, 0));
        JLabel lblPuerto = new JLabel("Puerto:");
        lblPuerto.setFont(Theme.FONT_SMALL);
        lblPuerto.setForeground(Theme.TEXT_SECONDARY);
        estilizarCampo(campoPuerto);
        filaPuerto.add(lblPuerto, BorderLayout.WEST);
        filaPuerto.add(campoPuerto, BorderLayout.CENTER);
        col.add(filaPuerto);
        col.add(Box.createVerticalStrut(14));

        col.add(optCliente);
        col.add(explicacion("No guarda datos aqui: todo lo pide a la PC anfitriona de esta obra."));
        JPanel filaHost = new JPanel(new BorderLayout(8, 0));
        filaHost.setOpaque(false);
        filaHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        filaHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaHost.setBorder(new EmptyBorder(4, 24, 0, 0));
        JLabel lblHost = new JLabel("PC anfitriona:");
        lblHost.setFont(Theme.FONT_SMALL);
        lblHost.setForeground(Theme.TEXT_SECONDARY);
        estilizarCampo(campoHost);
        campoHost.putClientProperty("JTextField.placeholderText", "192.168.1.10:" + RedEstado.PUERTO_DEFECTO);
        filaHost.add(lblHost, BorderLayout.WEST);
        filaHost.add(campoHost, BorderLayout.CENTER);
        col.add(filaHost);

        col.add(Box.createVerticalStrut(18));
        labelMensaje.setFont(Theme.FONT_SMALL);
        labelMensaje.setForeground(Theme.DANGER);
        labelMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(labelMensaje);
        col.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(col);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        root.add(scroll, BorderLayout.CENTER);

        JButton btnContinuar = new JButton("Continuar");
        btnContinuar.setFont(Theme.FONT_BOLD);
        btnContinuar.setBackground(Theme.PRIMARY);
        btnContinuar.setForeground(Color.WHITE);
        btnContinuar.setFocusPainted(false);
        btnContinuar.setBorderPainted(false);
        btnContinuar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnContinuar.setPreferredSize(new Dimension(140, 38));
        btnContinuar.addActionListener(e -> continuar());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pie.setOpaque(false);
        pie.add(btnContinuar);
        root.add(pie, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JLabel explicacion(String texto) {
        JLabel l = new JLabel("<html><div style='width:420px'>" + texto + "</div></html>");
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_SECONDARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2, 24, 0, 0));
        return l;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setFont(Theme.FONT_BASE);
        campo.setMaximumSize(new Dimension(260, 28));
        campo.setPreferredSize(new Dimension(220, 28));
    }

    private void continuar() {
        Modo modo = optAnfitriona.isSelected() ? Modo.ANFITRIONA
                : optCliente.isSelected() ? Modo.CLIENTE : Modo.LOCAL;
        int puerto = RedEstado.PUERTO_DEFECTO;
        String urlHost = null;

        if (modo == Modo.ANFITRIONA) {
            try {
                puerto = Integer.parseInt(campoPuerto.getText().trim());
                if (puerto < 1024 || puerto > 65535) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                mostrarError("Puerto invalido. Usa un numero entre 1024 y 65535.");
                return;
            }
        } else if (modo == Modo.CLIENTE) {
            String direccion = campoHost.getText().trim();
            if (direccion.isEmpty()) {
                mostrarError("Escribe la direccion de la PC anfitriona (ej: 192.168.1.10:8420).");
                return;
            }
            urlHost = direccion.startsWith("http://") || direccion.startsWith("https://")
                    ? direccion : "http://" + direccion;
        }

        RedEstado.modo(modo);
        RedEstado.puerto(puerto);
        RedEstado.urlHost(urlHost);
        RedEstado.guardarPersistida();

        dispose();
        alElegir.run();
    }

    private void mostrarError(String texto) {
        labelMensaje.setText(texto);
    }
}
