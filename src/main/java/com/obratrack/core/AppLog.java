package com.obratrack.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configuracion central de logs (java.util.logging, incluido en el JDK, sin dependencias).
 * Escribe a consola y a un archivo rotativo en la carpeta {@code logs/}, para poder
 * diagnosticar problemas en produccion como hace cualquier app de escritorio profesional.
 */
public final class AppLog {

    private static final String CARPETA = "logs";
    private static boolean configurado = false;

    private AppLog() {}

    /** Debe llamarse una sola vez al arrancar la aplicacion. */
    public static synchronized void configurar() {
        if (configurado) return;
        // Formato de una sola linea: fecha hora NIVEL clase - mensaje
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$-7s %3$s - %5$s%6$s%n");
        try {
            Path dir = Rutas.logs();
            Logger root = Logger.getLogger("");
            root.setLevel(Level.INFO);
            for (Handler h : root.getHandlers()) {
                root.removeHandler(h);
            }

            ConsoleHandler consola = new ConsoleHandler();
            consola.setLevel(Level.INFO);
            consola.setFormatter(new SimpleFormatter());
            root.addHandler(consola);

            // Archivo rotativo: hasta 5 archivos de ~2 MB, con append entre ejecuciones.
            FileHandler archivo = new FileHandler(dir.resolve("obratrack.%g.log").toString(), 2_000_000, 5, true);
            archivo.setLevel(Level.INFO);
            archivo.setFormatter(new SimpleFormatter());
            root.addHandler(archivo);

            configurado = true;
        } catch (IOException e) {
            System.err.println("No se pudo configurar el log en archivo: " + e.getMessage());
        }
    }

    /** Logger para una clase concreta. */
    public static Logger get(Class<?> clase) {
        return Logger.getLogger(clase.getName());
    }
}
