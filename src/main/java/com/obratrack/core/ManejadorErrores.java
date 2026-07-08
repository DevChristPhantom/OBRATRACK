package com.obratrack.core;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manejo global de errores no controlados: en vez de que la app muera en silencio
 * (o imprima un stacktrace que el usuario no ve), se registra en el log y se muestra
 * un dialogo claro. Cubre tanto hilos normales como el hilo de eventos de Swing (EDT).
 */
public final class ManejadorErrores {

    private static final Logger LOG = AppLog.get(ManejadorErrores.class);

    private ManejadorErrores() {}

    public static void instalar() {
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> manejar(error));

        // Envuelve la cola de eventos de Swing para capturar excepciones en la UI.
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
            @Override
            protected void dispatchEvent(AWTEvent evento) {
                try {
                    super.dispatchEvent(evento);
                } catch (Throwable error) {
                    manejar(error);
                }
            }
        });
    }

    private static void manejar(Throwable error) {
        LOG.log(Level.SEVERE, "Error no controlado", error);
        String detalle = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Ocurrio un error inesperado.\n\nDetalle: " + detalle
                        + "\n\nSe registro en el archivo de log (carpeta 'logs') para su revision.",
                "Error inesperado", JOptionPane.ERROR_MESSAGE));
    }
}
