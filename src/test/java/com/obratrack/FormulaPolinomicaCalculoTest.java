package com.obratrack;

import com.obratrack.model.MonomioPolinomico;
import com.obratrack.service.FormulaPolinomicaCalculo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura de la formula polinomica (FormulaPolinomicaCalculo):
 * cuadre de coeficientes y calculo del factor de reajuste. Sin base de datos.
 */
class FormulaPolinomicaCalculoTest {

    private MonomioPolinomico monomio(long id, String desc, double coefPct, double indiceBase) {
        MonomioPolinomico m = new MonomioPolinomico(desc, coefPct / 100.0, indiceBase);
        m.setId(id);
        return m;
    }

    @Test
    void sumaCoeficientesSumaTodosLosMonomios() {
        List<MonomioPolinomico> monomios = List.of(
                monomio(1, "Mano de obra", 40, 100),
                monomio(2, "Materiales", 60, 100));
        assertEquals(1.0, FormulaPolinomicaCalculo.sumaCoeficientes(monomios), 0.0001);
    }

    @Test
    void coeficientesCuadranCuandoSuman100Porciento() {
        List<MonomioPolinomico> monomios = List.of(
                monomio(1, "Mano de obra", 40, 100),
                monomio(2, "Materiales", 60, 100));
        assertTrue(FormulaPolinomicaCalculo.coeficientesCuadran(monomios));
    }

    @Test
    void coeficientesNoCuadranSiNoSuman100Porciento() {
        List<MonomioPolinomico> monomios = List.of(
                monomio(1, "Mano de obra", 40, 100),
                monomio(2, "Materiales", 50, 100));
        assertFalse(FormulaPolinomicaCalculo.coeficientesCuadran(monomios));
    }

    @Test
    void coeficientesNoCuadranSinMonomios() {
        assertFalse(FormulaPolinomicaCalculo.coeficientesCuadran(List.of()));
    }

    @Test
    void factorReajusteEsUnoSiLosIndicesNoCambiaron() {
        List<MonomioPolinomico> monomios = List.of(
                monomio(1, "Mano de obra", 40, 100),
                monomio(2, "Materiales", 60, 100));
        Map<Long, Double> indices = Map.of(1L, 100.0, 2L, 100.0);
        assertEquals(1.0, FormulaPolinomicaCalculo.calcularFactorReajuste(monomios, indices), 0.0001);
    }

    @Test
    void factorReajusteSubeSiLosIndicesSubieron() {
        List<MonomioPolinomico> monomios = List.of(
                monomio(1, "Mano de obra", 40, 100),
                monomio(2, "Materiales", 60, 100));
        // Mano de obra sube 10%, materiales sube 20%: Fr = 0.4*1.10 + 0.6*1.20 = 0.44 + 0.72 = 1.16
        Map<Long, Double> indices = Map.of(1L, 110.0, 2L, 120.0);
        assertEquals(1.16, FormulaPolinomicaCalculo.calcularFactorReajuste(monomios, indices), 0.0001);
    }

    @Test
    void factorReajusteUsaIndiceBaseSiFaltaElRevisado() {
        List<MonomioPolinomico> monomios = List.of(monomio(1, "Mano de obra", 100, 100));
        double factor = FormulaPolinomicaCalculo.calcularFactorReajuste(monomios, new HashMap<>());
        assertEquals(1.0, factor, 0.0001);
    }

    @Test
    void montoReajustadoMultiplicaPorElFactor() {
        assertEquals(11_600, FormulaPolinomicaCalculo.calcularMontoReajustado(10_000, 1.16), 0.01);
    }
}
