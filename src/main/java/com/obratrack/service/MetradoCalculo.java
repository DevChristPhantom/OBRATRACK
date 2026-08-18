package com.obratrack.service;

import com.obratrack.model.MetradoDetalle;

import java.util.List;

/**
 * Logica pura del metrado desagregado (sin base de datos): suma de las lineas
 * por sector y su cuadre contra la cantidad presupuestada de la partida.
 */
public final class MetradoCalculo {

    private MetradoCalculo() {}

    /** Tolerancia relativa antes de marcar el metrado como descuadrado (redondeos normales). */
    private static final double TOLERANCIA = 0.01;

    public static double totalDesagregado(List<MetradoDetalle> lineas) {
        return lineas.stream().mapToDouble(MetradoDetalle::getCantidad).sum();
    }

    public static double diferencia(List<MetradoDetalle> lineas, double cantidadPresupuestada) {
        return cantidadPresupuestada - totalDesagregado(lineas);
    }

    /** true si el desglose por sector cuadra (dentro de la tolerancia) con lo presupuestado. */
    public static boolean cuadra(List<MetradoDetalle> lineas, double cantidadPresupuestada) {
        return Math.abs(diferencia(lineas, cantidadPresupuestada)) <= TOLERANCIA;
    }
}
