package com.obratrack;

import com.obratrack.model.MetradoDetalle;
import com.obratrack.service.MetradoCalculo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura del metrado desagregado (MetradoCalculo): suma por
 * sector y cuadre contra la cantidad presupuestada de la partida. Sin base de datos.
 */
class MetradoCalculoTest {

    @Test
    void totalDesagregadoSumaTodasLasLineas() {
        List<MetradoDetalle> lineas = List.of(
                new MetradoDetalle("Bloque A", 400),
                new MetradoDetalle("Bloque B", 350),
                new MetradoDetalle("Bloque C", 250));
        assertEquals(1000, MetradoCalculo.totalDesagregado(lineas), 0.01);
    }

    @Test
    void cuadraCuandoElTotalCoincideConLoPresupuestado() {
        List<MetradoDetalle> lineas = List.of(
                new MetradoDetalle("Bloque A", 600),
                new MetradoDetalle("Bloque B", 400));
        assertTrue(MetradoCalculo.cuadra(lineas, 1000));
        assertEquals(0, MetradoCalculo.diferencia(lineas, 1000), 0.01);
    }

    @Test
    void noCuadraCuandoFaltaMetradoPorAsignar() {
        List<MetradoDetalle> lineas = List.of(new MetradoDetalle("Bloque A", 600));
        assertFalse(MetradoCalculo.cuadra(lineas, 1000));
        assertEquals(400, MetradoCalculo.diferencia(lineas, 1000), 0.01);
    }

    @Test
    void cuadraConListaVaciaSiLoPresupuestadoEsCero() {
        assertTrue(MetradoCalculo.cuadra(List.of(), 0));
    }
}
