package com.obratrack.ui;

import com.obratrack.core.AppInfo;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/** Ventana "Acerca de" con logo, nombre, versión y empresa. */
public final class AcercaDe {

    private AcercaDe() {}

    public static void mostrar(Component padre) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        URL logo = AcercaDe.class.getResource("/img/logoTitan.png");
        if (logo != null) {
            Image img = new ImageIcon(logo).getImage().getScaledInstance(190, -1, Image.SCALE_SMOOTH);
            JLabel lblLogo = new JLabel(new ImageIcon(img));
            lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(lblLogo);
            p.add(Box.createVerticalStrut(12));
        }

        JLabel titulo = new JLabel(AppInfo.NOMBRE + "  v" + AppInfo.VERSION);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(titulo);

        JLabel empresa = new JLabel(AppInfo.EMPRESA);
        empresa.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(4));
        p.add(empresa);

        JLabel desc = new JLabel("<html><div style='text-align:center;width:300px'>"
                + "Sistema integral de gestión de obras: control de partidas, precios, almacén, "
                + "comparativos temporales y reportes.</div></html>");
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(12));
        p.add(desc);

        JOptionPane.showMessageDialog(padre, p, "Acerca de " + AppInfo.NOMBRE, JOptionPane.PLAIN_MESSAGE);
    }
}
