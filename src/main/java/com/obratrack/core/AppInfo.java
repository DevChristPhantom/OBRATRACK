package com.obratrack.core;

import java.io.InputStream;
import java.util.Properties;

/** Metadatos de la aplicacion (nombre y version), leidos de version.properties. */
public final class AppInfo {

    public static final String NOMBRE = "ObraTrack";
    public static final String EMPRESA = "Grupo Titan G&L S.A.C.";
    public static final String VERSION = cargarVersion();

    private AppInfo() {}

    private static String cargarVersion() {
        try (InputStream in = AppInfo.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                return p.getProperty("app.version", "dev");
            }
        } catch (Exception ignored) {
            // si no se puede leer, se usa "dev"
        }
        return "dev";
    }
}
