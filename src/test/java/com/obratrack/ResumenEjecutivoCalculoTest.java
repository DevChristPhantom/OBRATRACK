package com.obratrack;

import com.obratrack.model.Actividad;
import com.obratrack.model.ItemCumplimiento;
import com.obratrack.model.ItemCumplimiento.Categoria;
import com.obratrack.model.ItemCumplimiento.Estado;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.model.Valorizacion;
import com.obratrack.service.ResumenEjecutivoCalculo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura de consolidacion de la ficha ejecutiva (ResumenEjecutivoCalculo):
 * junta salud, avance economico, avance fisico, valorizaciones y cumplimiento en un solo
 * objeto, sin tocar base de datos.
 */
class ResumenEjecutivoCalculoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    private Obra obra() {
        Obra o = new Obra();
        o.setNombre("Obra de prueba");
        o.setFechaInicio(HOY.minusDays(45));
        o.setFechaFinEstimada(HOY.plusDays(55));
        return o;
    }

    private Partida partida(long id, double presupuesto) {
        Partida p = new Partida("01.01", "Partida", "und", 1, presupuesto);
        p.setId(id);
        return p;
    }

    private Actividad actividad(String codigo, LocalDate inicio, LocalDate fin, double peso, double avanceReal) {
        Actividad a = new Actividad(codigo, "Actividad " + codigo, inicio, fin, peso);
        a.setId(1L);
        a.setFechaInicioReal(inicio);
        a.setAvanceReal(avanceReal);
        return a;
    }

    private Valorizacion valorizacion(int numero, double montoEjecutado, double montoNeto) {
        Valorizacion v = new Valorizacion();
        v.setNumero(numero);
        v.setPeriodoDesde(HOY.minusDays(30));
        v.setPeriodoHasta(HOY);
        v.setMontoEjecutadoPeriodo(montoEjecutado);
        v.setMontoNetoPagar(montoNeto);
        return v;
    }

    private ItemCumplimiento item(Categoria categoria, Estado estado, LocalDate fechaLimite) {
        ItemCumplimiento i = new ItemCumplimiento();
        i.setCategoria(categoria);
        i.setDescripcion("prueba");
        i.setFecha(HOY.minusDays(10));
        i.setFechaLimite(fechaLimite);
        i.setEstado(estado);
        return i;
    }

    /** itemsPorCategoria con todas las categorias presentes (algunas vacias), como espera construir(). */
    private Map<Categoria, List<ItemCumplimiento>> itemsPorCategoriaVacio() {
        Map<Categoria, List<ItemCumplimiento>> m = new EnumMap<>(Categoria.class);
        for (Categoria c : Categoria.values()) m.put(c, List.of());
        return m;
    }

    @Test
    void calculaElAvanceEconomicoSobreLaSumaDePartidas() {
        Obra obra = obra();
        List<Partida> partidas = List.of(partida(1, 60_000), partida(2, 40_000));
        Map<Long, Double> ejecutadoPorPartida = new HashMap<>();
        ejecutadoPorPartida.put(1L, 30_000.0);
        ejecutadoPorPartida.put(2L, 20_000.0);

        ResumenEjecutivoCalculo.Resumen r = ResumenEjecutivoCalculo.construir(obra, partidas, ejecutadoPorPartida,
                50_000, List.of(), List.of(), itemsPorCategoriaVacio(), HOY);

        assertEquals(100_000, r.presupuestoTotal, 0.01);
        assertEquals(50_000, r.ejecutadoTotal, 0.01);
        assertEquals(50.0, r.pctAvanceEconomico, 0.01);
    }

    @Test
    void cuentaActividadesCompletadasYAtrasadasYPromediaElAvanceFisico() {
        Obra obra = obra();
        Actividad completada = actividad("A1", HOY.minusDays(20), HOY.minusDays(10), 50, 100);
        Actividad atrasada = actividad("A2", HOY.minusDays(20), HOY.minusDays(10), 50, 10); // muy por detras
        List<Actividad> actividades = List.of(completada, atrasada);

        ResumenEjecutivoCalculo.Resumen r = ResumenEjecutivoCalculo.construir(obra, List.of(), Map.of(),
                0, actividades, List.of(), itemsPorCategoriaVacio(), HOY);

        assertEquals(2, r.actividadesTotal);
        assertEquals(1, r.actividadesCompletadas);
        assertEquals(1, r.actividadesAtrasadas);
        // avance real ponderado: 50%*100 + 50%*10 = 55
        assertEquals(55.0, r.pctAvanceFisicoReal, 0.01);
        assertEquals(100.0, r.pctAvanceFisicoProgramado, 0.01);
    }

    @Test
    void sinActividadesElAvanceFisicoQuedaEnCero() {
        Obra obra = obra();
        ResumenEjecutivoCalculo.Resumen r = ResumenEjecutivoCalculo.construir(obra, List.of(), Map.of(),
                0, List.of(), List.of(), itemsPorCategoriaVacio(), HOY);

        assertEquals(0, r.actividadesTotal);
        assertEquals(0, r.pctAvanceFisicoReal, 0.01);
        assertEquals(0, r.pctAvanceFisicoProgramado, 0.01);
    }

    @Test
    void tomaLaPrimeraValorizacionComoLaUltimaEmitida() {
        // listarPorObra() del servicio real ordena por numero DESCENDENTE: la mas reciente primero.
        Valorizacion v2 = valorizacion(2, 20_000, 17_000);
        Valorizacion v1 = valorizacion(1, 15_000, 12_500);
        List<Valorizacion> valorizaciones = List.of(v2, v1);

        ResumenEjecutivoCalculo.Resumen r = ResumenEjecutivoCalculo.construir(obra(), List.of(), Map.of(),
                0, List.of(), valorizaciones, itemsPorCategoriaVacio(), HOY);

        assertEquals(2, r.numeroValorizaciones);
        assertNotNull(r.ultimaValorizacion);
        assertEquals(2, r.ultimaValorizacion.getNumero());
        assertEquals(35_000, r.totalValorizadoAcumulado, 0.01);
    }

    @Test
    void sinValorizacionesLaUltimaEsNulaYElTotalEsCero() {
        ResumenEjecutivoCalculo.Resumen r = ResumenEjecutivoCalculo.construir(obra(), List.of(), Map.of(),
                0, List.of(), List.of(), itemsPorCategoriaVacio(), HOY);

        assertEquals(0, r.numeroValorizaciones);
        assertNull(r.ultimaValorizacion);
        assertEquals(0, r.totalValorizadoAcumulado, 0.01);
    }

    @Test
    void cuentaItemsDeCumplimientoPorCategoriaAbiertosYVencidos() {
        Map<Categoria, List<ItemCumplimiento>> itemsPorCategoria = itemsPorCategoriaVacio();
        itemsPorCategoria.put(Categoria.RIESGO, List.of(
                item(Categoria.RIESGO, Estado.ABIERTO, HOY.minusDays(1)),   // vencido
                item(Categoria.RIESGO, Estado.EN_PROCESO, HOY.plusDays(5)), // no vencido
                item(Categoria.RIESGO, Estado.CERRADO, HOY.minusDays(1)))); // cerrado, no cuenta como vencido

        ResumenEjecutivoCalculo.Resumen r = ResumenEjecutivoCalculo.construir(obra(), List.of(), Map.of(),
                0, List.of(), List.of(), itemsPorCategoria, HOY);

        assertEquals(4, r.cumplimiento.size()); // las 4 categorias siempre presentes
        ResumenEjecutivoCalculo.ConteoCategoria riesgo = r.cumplimiento.stream()
                .filter(c -> c.categoria == Categoria.RIESGO).findFirst().orElseThrow();
        assertEquals(3, riesgo.total);
        assertEquals(2, riesgo.abiertos); // ABIERTO + EN_PROCESO
        assertEquals(1, riesgo.vencidos);

        ResumenEjecutivoCalculo.ConteoCategoria ambiental = r.cumplimiento.stream()
                .filter(c -> c.categoria == Categoria.AMBIENTAL).findFirst().orElseThrow();
        assertEquals(0, ambiental.total);
    }

    @Test
    void incluyeElSemaforoDeSaludDeLaObra() {
        Obra obra = obra();
        List<Partida> partidas = List.of(partida(1, 100_000));
        Map<Long, Double> ejecutadoPorPartida = Map.of(1L, 30_000.0);

        ResumenEjecutivoCalculo.Resumen r = ResumenEjecutivoCalculo.construir(obra, partidas, ejecutadoPorPartida,
                30_000, List.of(), List.of(), itemsPorCategoriaVacio(), HOY);

        assertNotNull(r.salud);
        assertNotNull(r.salud.titulo);
    }
}
