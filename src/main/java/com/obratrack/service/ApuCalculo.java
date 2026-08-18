package com.obratrack.service;

import com.obratrack.model.ApuInsumo;

import java.util.List;

/**
 * Logica pura del Analisis de Precios Unitarios (sin base de datos): total por
 * insumo, cuadre contra el costo unitario de la partida y desglose % por tipo
 * de insumo (mano de obra, materiales, equipo, subcontrato).
 */
public final class ApuCalculo {

    private ApuCalculo() {}

    private static final double TOLERANCIA = 0.01;

    public static double totalApu(List<ApuInsumo> insumos) {
        return insumos.stream().mapToDouble(ApuInsumo::getParcial).sum();
    }

    public static double diferencia(List<ApuInsumo> insumos, double costoUnitarioPartida) {
        return costoUnitarioPartida - totalApu(insumos);
    }

    /** true si el total del APU cuadra (dentro de la tolerancia) con el costo unitario de la partida. */
    public static boolean cuadra(List<ApuInsumo> insumos, double costoUnitarioPartida) {
        return Math.abs(diferencia(insumos, costoUnitarioPartida)) <= TOLERANCIA;
    }

    /** % del total del APU que corresponde a un tipo de insumo dado (0 si no hay insumos). */
    public static double pctPorTipo(List<ApuInsumo> insumos, ApuInsumo.Tipo tipo) {
        double total = totalApu(insumos);
        if (total <= 0) return 0;
        double sumaTipo = insumos.stream()
                .filter(i -> i.getTipo() == tipo)
                .mapToDouble(ApuInsumo::getParcial)
                .sum();
        return (sumaTipo / total) * 100.0;
    }
}
