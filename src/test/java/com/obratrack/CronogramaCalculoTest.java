package com.obratrack;

import com.obratrack.model.Actividad;
import com.obratrack.service.CronogramaCalculo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura del cronograma (CronogramaCalculo): avance programado por
 * fecha, estado de una actividad y los acumulados ponderados (curva S). Sin base de datos.
 */
class CronogramaCalculoTest {

    private Actividad actividad(String codigo, LocalDate inicioProg, LocalDate finProg, double peso) {
        Actividad a = new Actividad(codigo, "Actividad " + codigo, inicioProg, finProg, peso);
        a.setId(1L);
        return a;
    }

    @Test
    void avanceProgramadoEsCeroAntesDeEmpezar() {
        Actividad a = actividad("A1", LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20), 10);
        assertEquals(0, CronogramaCalculo.avanceProgramado(a, LocalDate.of(2026, 3, 5)));
    }

    @Test
    void avanceProgramadoEsCienDespuesDeTerminar() {
        Actividad a = actividad("A1", LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20), 10);
        assertEquals(100, CronogramaCalculo.avanceProgramado(a, LocalDate.of(2026, 3, 25)));
    }

    @Test
    void avanceProgramadoInterpolaLinealmenteAMitadDeCamino() {
        Actividad a = actividad("A1", LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20), 10);
        double pct = CronogramaCalculo.avanceProgramado(a, LocalDate.of(2026, 3, 15));
        assertEquals(50, pct, 0.01);
    }

    @Test
    void estadoPendienteCuandoNoHaEmpezadoYSinAvance() {
        Actividad a = actividad("A1", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), 10);
        CronogramaCalculo.Estado estado = CronogramaCalculo.estado(a, LocalDate.of(2026, 5, 20));
        assertEquals(CronogramaCalculo.Estado.PENDIENTE, estado);
    }

    @Test
    void estadoEnProcesoCuandoVaAlRitmo() {
        Actividad a = actividad("A1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 11), 10);
        a.setFechaInicioReal(LocalDate.of(2026, 3, 1));
        a.setAvanceReal(50); // programado tambien ~50% a mitad de camino
        CronogramaCalculo.Estado estado = CronogramaCalculo.estado(a, LocalDate.of(2026, 3, 6));
        assertEquals(CronogramaCalculo.Estado.EN_PROCESO, estado);
    }

    @Test
    void estadoAtrasadaCuandoElAvanceRealQuedaMuyPorDebajoDelProgramado() {
        Actividad a = actividad("A1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 11), 10);
        a.setFechaInicioReal(LocalDate.of(2026, 3, 1));
        a.setAvanceReal(10); // programado ~50% a mitad de camino, real muy por detras
        CronogramaCalculo.Estado estado = CronogramaCalculo.estado(a, LocalDate.of(2026, 3, 6));
        assertEquals(CronogramaCalculo.Estado.ATRASADA, estado);
    }

    @Test
    void estadoCompletadaCuandoElAvanceRealLlegaA100() {
        Actividad a = actividad("A1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 11), 10);
        a.setAvanceReal(100);
        assertEquals(CronogramaCalculo.Estado.COMPLETADA,
                CronogramaCalculo.estado(a, LocalDate.of(2026, 3, 6)));
    }

    @Test
    void pctProgramadoAcumuladoPonderaPorPeso() {
        Actividad a1 = actividad("A1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 11), 50);
        Actividad a2 = actividad("A2", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 11), 50);
        // a mitad de camino ambas van al 50%, entre las dos deberia dar 50% del total
        double pct = CronogramaCalculo.pctProgramadoAcumulado(List.of(a1, a2), LocalDate.of(2026, 1, 6));
        assertEquals(50, pct, 0.01);
    }

    @Test
    void pctRealAcumuladoUsaElAvanceRealActualDeCadaActividad() {
        Actividad a1 = actividad("A1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 11), 60);
        a1.setFechaInicioReal(LocalDate.of(2026, 1, 1));
        a1.setAvanceReal(100);
        Actividad a2 = actividad("A2", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 11), 40);
        a2.setFechaInicioReal(LocalDate.of(2026, 1, 1));
        a2.setAvanceReal(0);
        LocalDate hoy = LocalDate.of(2026, 1, 6);
        double pct = CronogramaCalculo.pctRealAcumulado(List.of(a1, a2), hoy, hoy);
        assertEquals(60, pct, 0.01); // 60% * 100 + 40% * 0, sobre peso total 100
    }

    @Test
    void pctRealAcumuladoNoIgnoraElAvanceSiFaltaLaFechaInicioReal() {
        // Caso real detectado en prueba manual: se carga "avance real" en el formulario
        // pero se deja vacio el campo (opcional) de fecha de inicio real.
        Actividad a = actividad("A1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 11), 100);
        a.setAvanceReal(40);
        LocalDate hoy = LocalDate.of(2026, 1, 1);
        double pct = CronogramaCalculo.pctRealAcumulado(List.of(a), hoy, hoy);
        assertEquals(40, pct, 0.01);
    }

    @Test
    void curvaSCubreDesdeElInicioHastaElFinConValoresCrecientes() {
        Actividad a = actividad("A1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 21), 100);
        List<CronogramaCalculo.PuntoCurva> curva = CronogramaCalculo.curvaS(
                List.of(a), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 21), 5, LocalDate.of(2026, 1, 21));
        assertEquals(5, curva.size());
        assertEquals(0, curva.get(0).pctProgramado, 0.01);
        assertEquals(100, curva.get(curva.size() - 1).pctProgramado, 0.01);
        for (int i = 1; i < curva.size(); i++) {
            assertTrue(curva.get(i).pctProgramado >= curva.get(i - 1).pctProgramado,
                    "la curva programada no deberia decrecer");
        }
    }
}
