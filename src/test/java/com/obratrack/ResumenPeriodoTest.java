package com.obratrack;

import com.obratrack.core.Database;
import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.model.ResumenPeriodo;
import com.obratrack.service.Granularidad;
import com.obratrack.service.MovimientoService;
import com.obratrack.service.ObraService;
import com.obratrack.service.PartidaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la agregacion temporal (diario / semanal / mensual) de
 * MovimientoService.resumenPorPeriodo: agrupacion correcta por bucket,
 * neto = egresos - ingresos y acumulado corrido. Replica el escenario
 * validado de forma independiente con datos sinteticos.
 *
 * El test crea una obra y una partida propias, las puebla y las elimina
 * al terminar (CASCADE), de modo que no contamina la base real.
 */
class ResumenPeriodoTest {

    private final ObraService obraService = new ObraService();
    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();

    private long obraId;
    private long partidaId;

    @BeforeEach
    void preparar() throws Exception {
        Obra obra = new Obra();
        obra.setNombre("OBRA_TEST_RESUMEN_" + System.nanoTime());
        obra.setPresupuestoTotal(10_000);
        obra.setEstado(Obra.Estado.ACTIVA);
        obra = obraService.crear(obra);
        obraId = obra.getId();

        Partida p = new Partida("01.01", "Partida de prueba", "und", 1, 1);
        partidaService.guardarTodas(obraId, List.of(p));
        partidaId = partidaService.listarPorObra(obraId).get(0).getId();

        registrar("2026-06-22", MovimientoAlmacen.Tipo.EGRESO, 1000);
        registrar("2026-06-22", MovimientoAlmacen.Tipo.EGRESO, 500);
        registrar("2026-06-24", MovimientoAlmacen.Tipo.EGRESO, 300);
        registrar("2026-06-24", MovimientoAlmacen.Tipo.INGRESO, 100); // devolucion
        registrar("2026-06-28", MovimientoAlmacen.Tipo.EGRESO, 200);  // domingo, aun semana 26
        registrar("2026-06-29", MovimientoAlmacen.Tipo.EGRESO, 400);  // lunes, semana 27
        registrar("2026-07-02", MovimientoAlmacen.Tipo.EGRESO, 600);  // julio
    }

    private void registrar(String fecha, MovimientoAlmacen.Tipo tipo, double monto) throws Exception {
        MovimientoAlmacen m = new MovimientoAlmacen();
        m.setObraId(obraId);
        m.setPartidaId(partidaId);
        m.setFecha(LocalDate.parse(fecha));
        m.setTipo(tipo);
        m.setCantidad(1);
        m.setCostoUnitarioReal(monto); // total = 1 * monto
        movimientoService.registrar(m);
    }

    @Test
    void agrupacionDiaria() throws Exception {
        List<ResumenPeriodo> dias = movimientoService.resumenPorPeriodo(obraId, Granularidad.DIARIO);
        assertEquals(5, dias.size(), "deben ser 5 dias distintos");
        assertEquals(1500, dias.get(0).getNeto(), 1e-6);
        assertEquals(1500, dias.get(0).getAcumulado(), 1e-6);
        assertEquals(2900, dias.get(4).getAcumulado(), 1e-6, "acumulado final = neto global");
    }

    @Test
    void agrupacionSemanalIso() throws Exception {
        List<ResumenPeriodo> semanas = movimientoService.resumenPorPeriodo(obraId, Granularidad.SEMANAL);
        assertEquals(2, semanas.size(), "lun-dom: 22 a 28 = semana 26; 29 = semana 27");
        assertEquals(1900, semanas.get(0).getNeto(), 1e-6);  // 2000 egresos - 100 ingresos
        assertEquals(1000, semanas.get(1).getNeto(), 1e-6);  // 400 + 600
        assertEquals(2900, semanas.get(1).getAcumulado(), 1e-6);
    }

    @Test
    void agrupacionMensual() throws Exception {
        List<ResumenPeriodo> meses = movimientoService.resumenPorPeriodo(obraId, Granularidad.MENSUAL);
        assertEquals(2, meses.size());
        assertEquals("2026-06", meses.get(0).getEtiqueta());
        assertEquals(2300, meses.get(0).getNeto(), 1e-6);    // 2400 egresos - 100 ingresos
        assertEquals(600, meses.get(1).getNeto(), 1e-6);
        assertEquals(2900, meses.get(1).getAcumulado(), 1e-6);
    }

    @Test
    void obraSinMovimientosDevuelveListaVacia() throws Exception {
        Obra vacia = new Obra();
        vacia.setNombre("OBRA_TEST_VACIA_" + System.nanoTime());
        vacia.setEstado(Obra.Estado.ACTIVA);
        vacia = obraService.crear(vacia);
        try {
            assertTrue(movimientoService.resumenPorPeriodo(vacia.getId(), Granularidad.MENSUAL).isEmpty());
        } finally {
            borrarObra(vacia.getId());
        }
    }

    @AfterEach
    void limpiar() throws Exception {
        borrarObra(obraId);
    }

    private void borrarObra(long id) throws Exception {
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM obra WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        // por si las claves foraneas no estan en cascada, limpiamos las tablas hijas explicitamente
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM movimiento_almacen WHERE obra_id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM partida WHERE obra_id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
