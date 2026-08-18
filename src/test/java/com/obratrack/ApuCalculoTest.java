package com.obratrack;

import com.obratrack.model.ApuInsumo;
import com.obratrack.service.ApuCalculo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura del APU (ApuCalculo): total de insumos, cuadre contra
 * el costo unitario de la partida y desglose % por tipo de insumo. Sin base de datos.
 */
class ApuCalculoTest {

    @Test
    void totalApuSumaLosParcialesDeCadaInsumo() {
        List<ApuInsumo> insumos = List.of(
                new ApuInsumo(ApuInsumo.Tipo.MANO_DE_OBRA, "Operario", "hh", 0.8, 25),
                new ApuInsumo(ApuInsumo.Tipo.MATERIAL, "Cemento", "bls", 0.5, 30));
        // 0.8*25 + 0.5*30 = 20 + 15 = 35
        assertEquals(35, ApuCalculo.totalApu(insumos), 0.01);
    }

    @Test
    void cuadraCuandoElTotalCoincideConElCostoUnitario() {
        List<ApuInsumo> insumos = List.of(new ApuInsumo(ApuInsumo.Tipo.MATERIAL, "Cemento", "bls", 1, 30));
        assertTrue(ApuCalculo.cuadra(insumos, 30));
    }

    @Test
    void noCuadraCuandoElApuNoAlcanzaElCostoUnitario() {
        List<ApuInsumo> insumos = List.of(new ApuInsumo(ApuInsumo.Tipo.MATERIAL, "Cemento", "bls", 1, 20));
        assertFalse(ApuCalculo.cuadra(insumos, 30));
        assertEquals(10, ApuCalculo.diferencia(insumos, 30), 0.01);
    }

    @Test
    void pctPorTipoRepartePorcentualmenteElTotal() {
        List<ApuInsumo> insumos = List.of(
                new ApuInsumo(ApuInsumo.Tipo.MANO_DE_OBRA, "Operario", "hh", 1, 30), // 30
                new ApuInsumo(ApuInsumo.Tipo.MATERIAL, "Cemento", "bls", 1, 70));    // 70, total 100
        assertEquals(30, ApuCalculo.pctPorTipo(insumos, ApuInsumo.Tipo.MANO_DE_OBRA), 0.01);
        assertEquals(70, ApuCalculo.pctPorTipo(insumos, ApuInsumo.Tipo.MATERIAL), 0.01);
        assertEquals(0, ApuCalculo.pctPorTipo(insumos, ApuInsumo.Tipo.EQUIPO), 0.01);
    }

    @Test
    void pctPorTipoEsCeroSinInsumos() {
        assertEquals(0, ApuCalculo.pctPorTipo(List.of(), ApuInsumo.Tipo.MATERIAL), 0.01);
    }
}
