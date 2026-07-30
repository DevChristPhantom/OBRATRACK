package com.obratrack.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Metadatos de la aplicacion (nombre y version), leidos de version.properties. */
public final class AppInfo {

    private static final Logger LOG = AppLog.get(AppInfo.class);

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
        } catch (IOException e) {
            LOG.log(Level.FINE, "No se pudo leer version.properties; se usa 'dev'", e);
        }
        return "dev";
    }
}
