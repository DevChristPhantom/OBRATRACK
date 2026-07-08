package com.obratrack;

import com.formdev.flatlaf.FlatDarkLaf;
import com.obratrack.core.AppInfo;
import com.obratrack.core.AppLog;
import com.obratrack.core.Database;
import com.obratrack.core.ManejadorErrores;
import com.obratrack.core.RespaldoDB;
import com.obratrack.service.UsuarioService;
import com.obratrack.ui.LoginView;
import com.obratrack.ui.MainWindow;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Punto de entrada de ObraTrack. */
public class Main {

    public static void main(String[] args) {
        AppLog.configurar();
        ManejadorErrores.instalar();
        Logger log = AppLog.get(Main.class);
        log.info("Iniciando " + AppInfo.NOMBRE + " " + AppInfo.VERSION);

        FlatDarkLaf.setup();
        UIManager.put("defaultFont", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));

        SwingUtilities.invokeLater(() -> {
            try {
                Database.get();                          // crea/migra el esquema al iniciar
                RespaldoDB.respaldarAlIniciar();         // copia de seguridad automatica
                new UsuarioService().sembrarAdminSiVacio(); // admin/admin123 en el primer arranque
                new LoginView(Main::abrirPrincipal).setVisible(true);
            } catch (Exception e) {
                log.log(Level.SEVERE, "No se pudo iniciar la aplicacion", e);
                JOptionPane.showMessageDialog(null,
                        "No se pudo iniciar ObraTrack.\n\nDetalle: " + e.getMessage(),
                        "Error fatal", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AppLog.get(Main.class).info("Cerrando " + AppInfo.NOMBRE);
            Database.cerrar();
        }));
    }

    /** Abre la ventana principal tras un login correcto. */
    private static void abrirPrincipal() {
        new MainWindow().setVisible(true);
    }
}
