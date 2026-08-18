package com.obratrack.ui.views;

import com.obratrack.core.AppInfo;
import com.obratrack.core.Modo;
import com.obratrack.core.RedEstado;
import com.obratrack.core.RespaldoDB;
import com.obratrack.core.Rutas;
import com.obratrack.model.Usuario;
import com.obratrack.service.IUsuarioService;
import com.obratrack.service.ServiceFactory;
import com.obratrack.service.SesionActual;
import com.obratrack.ui.ModoInicioView;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/** Pantalla de ajustes: cambiar la propia contrasena, respaldos y carpetas de datos. */
public class AjustesView extends JPanel {

    private final IUsuarioService usuarioService = ServiceFactory.usuario();
    private final JLabel mensaje = new JLabel(" ");

    public AjustesView() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Ajustes");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        add(titulo, BorderLayout.NORTH);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(tarjetaPassword());
        col.add(Box.createVerticalStrut(14));
        col.add(tarjetaDatos());
        col.add(Box.createVerticalStrut(14));
        col.add(tarjetaRed());
        col.add(Box.createVerticalStrut(14));
        col.add(tarjetaInfo());
        col.add(Box.createVerticalStrut(10));
        mensaje.setFont(Theme.FONT_BASE);
        mensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(mensaje);
        col.add(Box.createVerticalGlue());

        add(new JScrollPane(col) {{
            setBorder(null);
            getViewport().setOpaque(false);
            setOpaque(false);
        }}, BorderLayout.CENTER);
    }

    private JPanel tarjeta(String titulo) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_SECONDARY);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        JLabel t = new JLabel(titulo);
        t.setFont(Theme.FONT_BOLD);
        t.setForeground(Theme.TEXT_PRIMARY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(t);
        card.add(Box.createVerticalStrut(10));
        return card;
    }

    private JPanel tarjetaPassword() {
        JPanel card = tarjeta("Cambiar mi contrasena");
        JPasswordField actual = pass();
        JPasswordField nueva = pass();
        JPasswordField confirma = pass();
        card.add(fila("Contrasena actual", actual));
        card.add(Box.createVerticalStrut(8));
        card.add(fila("Nueva contrasena", nueva));
        card.add(Box.createVerticalStrut(8));
        card.add(fila("Confirmar nueva", confirma));
        card.add(Box.createVerticalStrut(12));

        JButton btn = boton("Cambiar contrasena", Theme.PRIMARY);
        btn.addActionListener(e -> {
            Usuario u = SesionActual.getUsuario();
            if (u == null) { mostrar("No hay sesion activa.", Theme.DANGER); return; }
            char[] act = actual.getPassword();
            char[] nue = nueva.getPassword();
            char[] con = confirma.getPassword();
            try {
                Optional<Usuario> ok = usuarioService.autenticar(u.getUsername(), act);
                if (ok.isEmpty()) { mostrar("La contrasena actual no es correcta.", Theme.DANGER); return; }
                if (nue.length == 0) { mostrar("La nueva contrasena no puede estar vacia.", Theme.DANGER); return; }
                if (!java.util.Arrays.equals(nue, con)) { mostrar("La confirmacion no coincide.", Theme.DANGER); return; }
                usuarioService.cambiarPassword(u.getId(), nue);
                actual.setText(""); nueva.setText(""); confirma.setText("");
                mostrar("Contrasena actualizada correctamente.", Theme.SUCCESS);
            } catch (Exception ex) {
                mostrar("No se pudo cambiar la contrasena: " + ex.getMessage(), Theme.DANGER);
            } finally {
                java.util.Arrays.fill(act, '\0');
                java.util.Arrays.fill(nue, '\0');
                java.util.Arrays.fill(con, '\0');
            }
        });
        card.add(fila("", btn));
        return card;
    }

    private JPanel tarjetaDatos() {
        JPanel card = tarjeta("Datos y respaldos");
        JLabel info = new JLabel("Ubicacion: " + Rutas.base());
        info.setFont(Theme.FONT_SMALL);
        info.setForeground(Theme.TEXT_SECONDARY);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(info);
        card.add(Box.createVerticalStrut(10));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.setAlignmentX(Component.LEFT_ALIGNMENT);
        botones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton respaldo = boton("Crear respaldo ahora", Theme.SUCCESS);
        respaldo.addActionListener(e -> {
            try {
                Path p = RespaldoDB.respaldarAhora();
                mostrar("Respaldo creado: " + p, Theme.SUCCESS);
            } catch (Exception ex) {
                mostrar("No se pudo respaldar: " + ex.getMessage(), Theme.DANGER);
            }
        });
        botones.add(respaldo);
        botones.add(botonAbrir("Carpeta de datos", Rutas.data().toFile()));
        botones.add(botonAbrir("Respaldos", Rutas.backups().toFile()));
        botones.add(botonAbrir("Exportaciones", Rutas.exports().toFile()));
        botones.add(botonAbrir("Logs", Rutas.logs().toFile()));
        card.add(botones);
        return card;
    }

    private JPanel tarjetaRed() {
        JPanel card = tarjeta("Modo de red");
        JLabel info = new JLabel(descripcionModo());
        info.setFont(Theme.FONT_SMALL);
        info.setForeground(Theme.TEXT_SECONDARY);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(info);
        card.add(Box.createVerticalStrut(10));

        JButton btn = boton("Cambiar modo...", Theme.BG_CARD);
        btn.addActionListener(e -> new ModoInicioView(() -> {
            JOptionPane.showMessageDialog(this,
                    "Modo guardado. Cierra y vuelve a abrir ObraTrack para que el cambio tome efecto.",
                    "Reinicia para aplicar el cambio", JOptionPane.INFORMATION_MESSAGE);
        }).setVisible(true));
        card.add(btn);
        return card;
    }

    private String descripcionModo() {
        Modo m = RedEstado.modo();
        return switch (m) {
            case LOCAL -> "Local — esta PC trabaja sola, sin red.";
            case ANFITRIONA -> "Anfitriona — sirve los datos de esta obra en el puerto " + RedEstado.puerto() + ".";
            case CLIENTE -> "Cliente — conectada a " + RedEstado.urlHost();
        };
    }

    private JPanel tarjetaInfo() {
        JPanel card = tarjeta("Acerca de");
        JLabel l1 = new JLabel(AppInfo.NOMBRE + " v" + AppInfo.VERSION);
        l1.setForeground(Theme.TEXT_PRIMARY);
        l1.setFont(Theme.FONT_BASE);
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l2 = new JLabel(AppInfo.EMPRESA);
        l2.setForeground(Theme.TEXT_SECONDARY);
        l2.setFont(Theme.FONT_SMALL);
        l2.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(l1);
        card.add(l2);
        return card;
    }

    private JButton botonAbrir(String texto, File carpeta) {
        JButton b = boton(texto, Theme.BG_CARD);
        b.addActionListener(e -> {
            try {
                if (Desktop.isDesktopSupported() && carpeta.exists()) {
                    Desktop.getDesktop().open(carpeta);
                } else {
                    mostrar("No se pudo abrir: " + carpeta, Theme.WARNING);
                }
            } catch (Exception ex) {
                mostrar("No se pudo abrir la carpeta: " + ex.getMessage(), Theme.DANGER);
            }
        });
        return b;
    }

    private JButton boton(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setFont(Theme.FONT_BOLD);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPasswordField pass() {
        JPasswordField f = new JPasswordField();
        f.setFont(Theme.FONT_BASE);
        f.putClientProperty("JPasswordField.showRevealButton", true);
        return f;
    }

    private JPanel fila(String etiqueta, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!etiqueta.isEmpty()) {
            JLabel l = new JLabel(etiqueta);
            l.setPreferredSize(new Dimension(150, 30));
            l.setFont(Theme.FONT_BASE);
            l.setForeground(Theme.TEXT_SECONDARY);
            p.add(l, BorderLayout.WEST);
        }
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    private void mostrar(String texto, Color color) {
        mensaje.setText(texto);
        mensaje.setForeground(color);
    }

    /** Llamado por MainWindow al mostrar la vista. */
    public void refrescar() {
        mostrar(" ", Theme.TEXT_SECONDARY);
    }
}
