package com.obratrack;

import com.formdev.flatlaf.FlatLightLaf;
import com.obratrack.core.AppInfo;
import com.obratrack.core.AppLog;
import com.obratrack.core.Database;
import com.obratrack.core.ManejadorErrores;
import com.obratrack.core.Modo;
import com.obratrack.core.RedEstado;
import com.obratrack.core.RespaldoDB;
import com.obratrack.model.Usuario;
import com.obratrack.red.ServidorHttp;
import com.obratrack.service.ServiceFactory;
import com.obratrack.service.SesionActual;
import com.obratrack.ui.CambioPasswordObligatorio;
import com.obratrack.ui.LoginView;
import com.obratrack.ui.MainWindow;
import com.obratrack.ui.ModoInicioView;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Punto de entrada de ObraTrack. */
public class Main {

    private static ServidorHttp servidor;

    public static void main(String[] args) {
        AppLog.configurar();
        ManejadorErrores.instalar();
        Logger log = AppLog.get(Main.class);
        log.info("Iniciando " + AppInfo.NOMBRE + " " + AppInfo.VERSION);

        FlatLightLaf.setup();
        com.obratrack.ui.Theme.configurar();

        SwingUtilities.invokeLater(() -> {
            if (RedEstado.existeConfigGuardada()) {
                RedEstado.cargarPersistida();
                continuarArranque(log);
            } else {
                // Primer arranque: hay que elegir el modo de red ANTES de tocar SQLite,
                // para que el modo cliente nunca llegue a crear un archivo local.
                new ModoInicioView(() -> continuarArranque(log)).setVisible(true);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AppLog.get(Main.class).info("Cerrando " + AppInfo.NOMBRE);
            if (servidor != null) servidor.detener();
            if (RedEstado.modo() != Modo.CLIENTE) Database.cerrar();
        }));
    }

    /** Continua el arranque una vez decidido (o cargado) el modo de red. */
    private static void continuarArranque(Logger log) {
        JFrame progreso = new JFrame("Iniciando ObraTrack");
        progreso.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel estado = new JLabel("Preparando la aplicacion...");
        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);
        panel.add(estado, java.awt.BorderLayout.NORTH);
        panel.add(barra, java.awt.BorderLayout.CENTER);
        progreso.setContentPane(panel);
        progreso.pack();
        progreso.setLocationRelativeTo(null);
        progreso.setVisible(true);

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (RedEstado.modo() != Modo.CLIENTE) {
                    publish("Preparando la base de datos...");
                    Database.get();
                    publish("Creando respaldo de seguridad...");
                    RespaldoDB.respaldarAlIniciar();
                    publish("Preparando usuarios...");
                    ServiceFactory.usuario().sembrarAdminSiVacio();
                }
                if (RedEstado.modo() == Modo.ANFITRIONA) {
                    publish("Iniciando servidor de la obra...");
                    servidor = new ServidorHttp();
                    servidor.iniciar(RedEstado.puerto());
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> mensajes) {
                estado.setText(mensajes.get(mensajes.size() - 1));
                progreso.pack();
            }

            @Override
            protected void done() {
                try {
                    get();
                    new LoginView(Main::abrirPrincipal).setVisible(true);
                } catch (Exception e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    log.log(Level.SEVERE, "No se pudo iniciar la aplicacion", causa);
                    JOptionPane.showMessageDialog(progreso,
                            "No se pudo iniciar ObraTrack.\n\nDetalle: " + causa.getMessage(),
                            "Error fatal", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                } finally {
                    progreso.dispose();
                }
            }
        }.execute();
    }

    /** Abre la ventana principal tras un login correcto. */
    private static void abrirPrincipal() {
        Usuario u = SesionActual.getUsuario();
        // Si la cuenta esta marcada para cambio de clave (p. ej. admin de fabrica),
        // se obliga a definir una nueva antes de entrar.
        if (u != null && u.isDebeCambiarPassword()) {
            boolean cambiada = CambioPasswordObligatorio.mostrar(u);
            if (!cambiada) {
                SesionActual.cerrar();
                new LoginView(Main::abrirPrincipal).setVisible(true);
                return;
            }
        }
        new MainWindow().setVisible(true);
    }
}
