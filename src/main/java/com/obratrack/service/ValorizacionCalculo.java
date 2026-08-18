package com.obratrack.service;

/**
 * Logica pura de valorizaciones (sin base de datos): retencion de garantia,
 * monto neto a pagar y % de avance financiero acumulado frente al contrato.
 */
public final class ValorizacionCalculo {

    private ValorizacionCalculo() {}

    /** Monto retenido como garantia sobre el bruto ejecutado del periodo. */
    public static double calcularRetencion(double montoBruto, double pctRetencion) {
        if (montoBruto <= 0 || pctRetencion <= 0) return 0;
        return montoBruto * (pctRetencion / 100.0);
    }

    /** Neto a pagar al contratista: bruto menos retencion menos amortizacion de adelantos. */
    public static double calcularNetoPagar(double montoBruto, double montoRetencion, double montoAmortizacionAdelanto) {
        return montoBruto - montoRetencion - montoAmortizacionAdelanto;
    }

    /** % del presupuesto contractual ya valorizado (acumulado total / presupuesto). */
    public static double calcularPctAvanceFinanciero(double montoAcumuladoTotal, double presupuestoTotal) {
        if (presupuestoTotal <= 0) return 0;
        return (montoAcumuladoTotal / presupuestoTotal) * 100.0;
    }

    /** Saldo del contrato que todavia no se ha valorizado. */
    public static double calcularSaldoPorValorizar(double montoAcumuladoTotal, double presupuestoTotal) {
        return Math.max(0, presupuestoTotal - montoAcumuladoTotal);
    }
}
