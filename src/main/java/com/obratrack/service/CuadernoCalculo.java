package com.obratrack.service;

import com.obratrack.model.AsientoCuaderno;

import java.time.LocalDate;
import java.util.List;

/**
 * Logica pura del cuaderno de obra (sin base de datos): estadisticas rapidas
 * sobre los asientos ya cargados, para el resumen de la vista.
 */
public final class CuadernoCalculo {

    private CuadernoCalculo() {}

    /** Cuantos asientos tienen fecha dentro de los ultimos {@code dias} (incluyendo hoy). */
    public static long conteoUltimosDias(List<AsientoCuaderno> asientos, LocalDate hoy, int dias) {
        LocalDate desde = hoy.minusDays(dias - 1L);
        return asientos.stream()
                .filter(a -> a.getFecha() != null && !a.getFecha().isBefore(desde) && !a.getFecha().isAfter(hoy))
                .count();
    }

    /** Promedio de personal en obra registrado en los ultimos {@code dias} (0 si no hay asientos en el rango). */
    public static double personalPromedio(List<AsientoCuaderno> asientos, LocalDate hoy, int dias) {
        LocalDate desde = hoy.minusDays(dias - 1L);
        List<AsientoCuaderno> enRango = asientos.stream()
                .filter(a -> a.getFecha() != null && !a.getFecha().isBefore(desde) && !a.getFecha().isAfter(hoy))
                .toList();
        if (enRango.isEmpty()) return 0;
        return enRango.stream().mapToInt(AsientoCuaderno::getPersonalObra).average().orElse(0);
    }
}
