package com.obratrack.service;

import com.obratrack.model.Documento;

import java.util.List;

/**
 * Logica pura de documentos (sin base de datos): siguiente numero de version
 * de un plano dentro de su propio historial, y formato legible de tamano de archivo.
 */
public final class DocumentoCalculo {

    private DocumentoCalculo() {}

    /** Siguiente version dentro del historial de un mismo documento (plano), 1 si es el primero. */
    public static int siguienteVersion(List<Documento> historialMismoNombre) {
        return historialMismoNombre.stream().mapToInt(Documento::getVersion).max().orElse(0) + 1;
    }

    /** Tamano de archivo en formato legible (B, KB, MB, GB). */
    public static String formatoTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
}
