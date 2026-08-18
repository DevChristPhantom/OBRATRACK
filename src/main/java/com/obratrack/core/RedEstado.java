package com.obratrack.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Estado de red de esta instancia: modo activo (local/anfitriona/cliente), puerto del
 * servidor local y, en modo cliente, la URL del host y el token de sesion remota.
 *
 * <p>Vive en {@code core} (no en {@code service} ni {@code red}) para que
 * {@link Database#get()} pueda negarse a tocar SQLite en modo cliente sin que {@code core}
 * pase a depender de la capa de servicios. Tambien persiste el modo/puerto/host elegidos
 * en {@code red.properties} bajo {@link Rutas#base()}, para no volver a preguntar en cada
 * arranque.
 */
public final class RedEstado {

    private static final Logger LOG = AppLog.get(RedEstado.class);
    public static final int PUERTO_DEFECTO = 8420;

    private static Modo modo = Modo.LOCAL;
    private static int puerto = PUERTO_DEFECTO;
    private static String urlHost;
    private static String tokenRemoto;

    private RedEstado() {}

    public static Modo modo() { return modo; }
    public static void modo(Modo m) { modo = m != null ? m : Modo.LOCAL; }

    public static int puerto() { return puerto; }
    public static void puerto(int p) { puerto = p > 0 ? p : PUERTO_DEFECTO; }

    public static String urlHost() { return urlHost; }
    public static void urlHost(String url) { urlHost = url; }

    public static String tokenRemoto() { return tokenRemoto; }
    public static void tokenRemoto(String token) { tokenRemoto = token; }

    /** true si nunca se guardo una configuracion de red (primer arranque). */
    public static boolean existeConfigGuardada() {
        return Files.exists(Rutas.configRed());
    }

    /** Carga modo/puerto/host desde red.properties; si no existe, deja los valores por defecto. */
    public static void cargarPersistida() {
        Path p = Rutas.configRed();
        if (!Files.exists(p)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
        } catch (IOException e) {
            LOG.warning("No se pudo leer red.properties, se usa modo local: " + e.getMessage());
            return;
        }
        try {
            modo = Modo.valueOf(props.getProperty("modo", "LOCAL"));
        } catch (IllegalArgumentException e) {
            modo = Modo.LOCAL;
        }
        try {
            puerto = Integer.parseInt(props.getProperty("puerto", String.valueOf(PUERTO_DEFECTO)));
        } catch (NumberFormatException ignored) {
            puerto = PUERTO_DEFECTO;
        }
        urlHost = props.getProperty("urlHost", null);
    }

    /** Guarda modo/puerto/host para que el proximo arranque no vuelva a preguntar. */
    public static void guardarPersistida() {
        Properties props = new Properties();
        props.setProperty("modo", modo.name());
        props.setProperty("puerto", String.valueOf(puerto));
        props.setProperty("urlHost", urlHost != null ? urlHost : "");
        try (OutputStream out = Files.newOutputStream(Rutas.configRed())) {
            props.store(out, "ObraTrack - configuracion de red");
        } catch (IOException e) {
            LOG.warning("No se pudo guardar red.properties: " + e.getMessage());
        }
    }
}
