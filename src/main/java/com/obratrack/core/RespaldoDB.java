package com.obratrack.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Respaldo automatico de la base de datos, como hacen las apps profesionales para
 * proteger los datos del usuario. Usa {@code VACUUM INTO}, que produce una copia
 * consistente en un solo archivo aunque el modo WAL este activo. Se conservan los
 * ultimos {@value #MAX_RESPALDOS} respaldos y se purgan los mas antiguos.
 */
public final class RespaldoDB {

    private static final Logger LOG = AppLog.get(RespaldoDB.class);
    private static final int MAX_RESPALDOS = 10;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private RespaldoDB() {}

    public static void respaldarAlIniciar() {
        try {
            LOG.info("Respaldo de base de datos creado: " + respaldarAhora());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo crear el respaldo de la base de datos", e);
        }
    }

    /** Crea un respaldo ahora mismo y devuelve su ruta (lanza excepcion si falla). */
    public static Path respaldarAhora() throws Exception {
        Path dir = Rutas.backups();
        Path destino = dir.resolve("obratrack_" + LocalDateTime.now().format(FMT) + ".db");
        String ruta = destino.toString().replace("\\", "/");
        try (PreparedStatement ps = Database.get().prepareStatement("VACUUM INTO ?")) {
            ps.setString(1, ruta);
            ps.execute();
        }
        purgarAntiguos(dir);
        return destino;
    }

    private static void purgarAntiguos(Path dir) {
        List<Path> respaldos = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> {
                String n = p.getFileName().toString();
                return n.startsWith("obratrack_") && n.endsWith(".db");
            }).sorted().forEach(respaldos::add);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudieron listar los respaldos", e);
            return;
        }
        int sobran = respaldos.size() - MAX_RESPALDOS;
        for (int i = 0; i < sobran; i++) {
            try {
                Files.deleteIfExists(respaldos.get(i));
            } catch (IOException ignored) {
                // si no se puede borrar uno, se intenta la proxima vez
            }
        }
    }
}
