package com.obratrack;

import com.obratrack.service.ValorizacionCalculo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura de valorizaciones (ValorizacionCalculo): retencion,
 * neto a pagar, % de avance financiero y saldo por valorizar. Sin base de datos.
 */
class ValorizacionCalculoTest {

    @Test
    void calcularRetencionAplicaElPorcentajeSobreElBruto() {
        assertEquals(1000, ValorizacionCalculo.calcularRetencion(10_000, 10), 0.01);
    }

    @Test
    void calcularRetencionEsCeroSinMontoOSinPorcentaje() {
        assertEquals(0, ValorizacionCalculo.calcularRetencion(0, 10), 0.01);
        assertEquals(0, ValorizacionCalculo.calcularRetencion(10_000, 0), 0.01);
    }

    @Test
    void calcularNetoPagarRestaRetencionYAmortizacion() {
        double neto = ValorizacionCalculo.calcularNetoPagar(10_000, 1000, 2000);
        assertEquals(7000, neto, 0.01);
    }

    @Test
    void calcularPctAvanceFinancieroSobreElPresupuesto() {
        double pct = ValorizacionCalculo.calcularPctAvanceFinanciero(25_000, 100_000);
        assertEquals(25, pct, 0.01);
    }

    @Test
    void calcularPctAvanceFinancieroEsCeroSinPresupuesto() {
        assertEquals(0, ValorizacionCalculo.calcularPctAvanceFinanciero(25_000, 0), 0.01);
    }

    @Test
    void calcularSaldoPorValorizarEsLaDiferenciaConElPresupuesto() {
        double saldo = ValorizacionCalculo.calcularSaldoPorValorizar(25_000, 100_000);
        assertEquals(75_000, saldo, 0.01);
    }

    @Test
    void calcularSaldoPorValorizarNoBajaDeCeroSiSeSupereElPresupuesto() {
        double saldo = ValorizacionCalculo.calcularSaldoPorValorizar(120_000, 100_000);
        assertEquals(0, saldo, 0.01);
    }
}
