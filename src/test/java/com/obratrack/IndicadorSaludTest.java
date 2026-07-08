package com.obratrack;

import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.IndicadorSalud;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica el semaforo de salud (IndicadorSalud): ritmo de gasto vs. tiempo
 * transcurrido y sobregiro de partidas. La logica es pura (sin base de datos).
 */
class IndicadorSaludTest {

    /** Obra cuyo % de tiempo transcurrido es aproximadamente el indicado. */
    private Obra obraConTiempo(int pctTiempo) {
        Obra o = new Obra();
        o.setNombre("Test");
        o.setFechaInicio(LocalDate.now().minusDays(pctTiempo));
        o.setFechaFinEstimada(LocalDate.now().plusDays(100 - pctTiempo));
        return o;
    }

    /** Una partida ejecutable (hoja) con presupuesto dado. */
    private Partida partida(long id, double presupuesto) {
        Partida p = new Partida("01.01", "Partida", "und", 1, presupuesto);
        p.setId(id);
        return p;
    }

    private Map<Long, Double> ejec(long id, double valor) {
        Map<Long, Double> m = new HashMap<>();
        m.put(id, valor);
        return m;
    }

    @Test
    void verdeCuandoGastaPorDebajoDelRitmo() {
        Obra obra = obraConTiempo(45);
        List<Partida> partidas = List.of(partida(1, 100_000));
        IndicadorSalud.Salud s = IndicadorSalud.evaluar(obra, partidas, ejec(1, 38_000), 38_000);
        assertEquals(IndicadorSalud.Nivel.VERDE, s.nivel);
    }

    @Test
    void amarilloCuandoGastaAlgoPorDelante() {
        Obra obra = obraConTiempo(45);
        List<Partida> partidas = List.of(partida(1, 100_000));
        IndicadorSalud.Salud s = IndicadorSalud.evaluar(obra, partidas, ejec(1, 58_000), 58_000);
        assertEquals(IndicadorSalud.Nivel.AMARILLO, s.nivel);
    }

    @Test
    void rojoCuandoGastaMuyPorDelante() {
        Obra obra = obraConTiempo(45);
        List<Partida> partidas = List.of(partida(1, 100_000));
        IndicadorSalud.Salud s = IndicadorSalud.evaluar(obra, partidas, ejec(1, 82_000), 82_000);
        assertEquals(IndicadorSalud.Nivel.ROJO, s.nivel);
    }

    @Test
    void rojoCuandoSuperaElPresupuestoTotal() {
        Obra obra = obraConTiempo(45);
        List<Partida> partidas = List.of(partida(1, 100_000));
        IndicadorSalud.Salud s = IndicadorSalud.evaluar(obra, partidas, ejec(1, 101_000), 101_000);
        assertEquals(IndicadorSalud.Nivel.ROJO, s.nivel);
    }

    @Test
    void rojoPorSobregiroDePartidasAunConTiempoAmplio() {
        // Va al 90% del tiempo, gasto global moderado, pero una partida se paso >5% del total.
        Obra obra = obraConTiempo(90);
        List<Partida> partidas = List.of(partida(1, 40_000), partida(2, 60_000));
        Map<Long, Double> ejecutado = new HashMap<>();
        ejecutado.put(1L, 46_000.0); // 6.000 de sobrecosto = 6% de 100.000
        ejecutado.put(2L, 4_000.0);
        IndicadorSalud.Salud s = IndicadorSalud.evaluar(obra, partidas, ejecutado, 50_000);
        assertEquals(IndicadorSalud.Nivel.ROJO, s.nivel);
        assertTrue(s.pctSobregiro >= 5.0);
    }

    @Test
    void modoSinFechaUsaSoloSobregiroYGasto() {
        Obra obra = new Obra(); // sin fechas
        obra.setNombre("Sin fechas");
        List<Partida> partidas = List.of(partida(1, 100_000));
        IndicadorSalud.Salud s = IndicadorSalud.evaluar(obra, partidas, ejec(1, 20_000), 20_000);
        assertEquals(IndicadorSalud.Nivel.VERDE, s.nivel);
        assertEquals(-1, s.pctTiempo, 0.001, "sin fechas, pctTiempo debe ser -1");
    }
}
