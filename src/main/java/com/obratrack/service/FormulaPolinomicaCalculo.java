package com.obratrack.service;

import com.obratrack.model.MonomioPolinomico;

import java.util.List;
import java.util.Map;

/**
 * Logica pura de la formula polinomica de reajuste de precios (sin base de
 * datos): Fr = suma( coeficiente_i * indiceRevisado_i / indiceBase_i ). Los
 * coeficientes de incidencia de todos los monomios deben sumar 1 (100%) para
 * que la formula sea valida, igual que en un expediente tecnico real.
 */
public final class FormulaPolinomicaCalculo {

    private FormulaPolinomicaCalculo() {}

    private static final double TOLERANCIA = 0.005; // 0.5 puntos porcentuales

    public static double sumaCoeficientes(List<MonomioPolinomico> monomios) {
        return monomios.stream().mapToDouble(MonomioPolinomico::getCoeficienteIncidencia).sum();
    }

    /** true si los coeficientes de incidencia suman 1 (100%), dentro de la tolerancia. */
    public static boolean coeficientesCuadran(List<MonomioPolinomico> monomios) {
        if (monomios.isEmpty()) return false;
        return Math.abs(sumaCoeficientes(monomios) - 1.0) <= TOLERANCIA;
    }

    /**
     * Factor de reajuste Fr = suma(coeficiente_i * indiceRevisado_i / indiceBase_i).
     * Los indices revisados se pasan por id de monomio; si a un monomio no le
     * llega indice revisado, se usa su propio indice base (factor 1 para ese termino).
     */
    public static double calcularFactorReajuste(List<MonomioPolinomico> monomios, Map<Long, Double> indicesRevisados) {
        double factor = 0;
        for (MonomioPolinomico m : monomios) {
            if (m.getIndiceBase() <= 0) continue;
            Double ir = indicesRevisados.get(m.getId());
            double indiceRevisado = (ir != null) ? ir : m.getIndiceBase();
            factor += m.getCoeficienteIncidencia() * (indiceRevisado / m.getIndiceBase());
        }
        return factor;
    }

    /** Monto reajustado = monto base (bruto) x factor de reajuste. */
    public static double calcularMontoReajustado(double montoBase, double factorReajuste) {
        return montoBase * factorReajuste;
    }
}
