package com.obratrack.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resuelve dónde guardar datos, logs, respaldos y exportaciones.
 *
 * - En desarrollo (ejecutando desde el proyecto) usa la carpeta actual, para no mover la base
 *   de datos existente.
 * - Instalada en "Archivos de programa" (carpeta de solo lectura para usuarios normales) cae
 *   automáticamente a {@code %APPDATA%\ObraTrack}, que sí es escribible.
 * - Se puede forzar con la propiedad de sistema {@code -Dobratrack.home=ruta}.
 */
public final class Rutas {

    private static Path base;

    private Rutas() {}

    public static synchronized Path base() {
        if (base != null) return base;

        String prop = System.getProperty("obratrack.home");
        if (prop != null && !prop.isBlank()) {
            base = crear(Paths.get(prop));
            return base;
        }
        Path cwd = Paths.get("").toAbsolutePath();
        if (esEscribible(cwd)) {
            base = cwd;
            return base;
        }
        String appdata = System.getenv("APPDATA");
        Path destino = (appdata != null && !appdata.isBlank())
                ? Paths.get(appdata, "ObraTrack")
                : Paths.get(System.getProperty("user.home"), "ObraTrack");
        base = crear(destino);
        return base;
    }

    public static Path data()    { return sub("data"); }
    public static Path logs()    { return sub("logs"); }
    public static Path exports() { return sub("exports"); }
    public static Path backups() { return sub("data", "backups"); }

    private static Path sub(String... partes) {
        Path p = base();
        for (String s : partes) p = p.resolve(s);
        crear(p);
        return p;
    }

    private static Path crear(Path p) {
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {
            // si no se puede crear, la operacion que la use fallara y quedara en el log
        }
        return p;
    }

    private static boolean esEscribible(Path dir) {
        try {
            Files.createDirectories(dir);
            Path prueba = dir.resolve(".obratrack_write_test");
            Files.writeString(prueba, "ok");
            Files.deleteIfExists(prueba);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
