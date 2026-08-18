package com.obratrack;

import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.MovimientoService;
import com.obratrack.service.ObraService;
import com.obratrack.service.PartidaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba el CRUD de obras (crear/actualizar/eliminar) y que la eliminacion borre
 * en cascada partidas y movimientos. Usa nombres unicos y limpia al terminar.
 */
class ObraServiceTest {

    private final ObraService obraService = new ObraService();
    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();
    private final long nano = System.nanoTime();
    private Long obraId;

    @Test
    void crearActualizarEliminar() throws Exception {
        Obra o = new Obra("OBRA_TEST_" + nano, "descripcion", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();
        assertNotNull(obraId, "crear debe asignar id");

        o.setNombre("OBRA_TEST_EDIT_" + nano);
        o.setEstado(Obra.Estado.FINALIZADA);
        obraService.actualizar(o);

        Obra recargada = obraService.listarTodas().stream()
                .filter(x -> x.getId().equals(obraId)).findFirst().orElseThrow();
        assertEquals("OBRA_TEST_EDIT_" + nano, recargada.getNombre());
        assertEquals(Obra.Estado.FINALIZADA, recargada.getEstado());

        obraService.eliminar(obraId);
        assertTrue(obraService.listarTodas().stream().noneMatch(x -> x.getId().equals(obraId)));
        obraId = null;
    }

    @Test
    void guardaYRecuperaLosCamposDeMemoriaDescriptiva() throws Exception {
        Obra o = new Obra("OBRA_MEMORIA_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        o.setUbicacion("El Algarrobal, Ilo, Moquegua");
        o.setEntidadContratante("Municipalidad Distrital de Ilo");
        o.setModalidadEjecucion(Obra.ModalidadEjecucion.ADMINISTRACION_DIRECTA);
        o.setSectoresBloques("Sector A: Pabellon administrativo\nSector B: Aulas");
        obraService.crear(o);
        obraId = o.getId();

        Obra recargada = obraService.listarTodas().stream()
                .filter(x -> x.getId().equals(obraId)).findFirst().orElseThrow();
        assertEquals("El Algarrobal, Ilo, Moquegua", recargada.getUbicacion());
        assertEquals("Municipalidad Distrital de Ilo", recargada.getEntidadContratante());
        assertEquals(Obra.ModalidadEjecucion.ADMINISTRACION_DIRECTA, recargada.getModalidadEjecucion());
        assertEquals("Sector A: Pabellon administrativo\nSector B: Aulas", recargada.getSectoresBloques());

        recargada.setUbicacion("Otra ubicacion");
        recargada.setModalidadEjecucion(Obra.ModalidadEjecucion.CONTRATA);
        obraService.actualizar(recargada);
        Obra reeditada = obraService.listarTodas().stream()
                .filter(x -> x.getId().equals(obraId)).findFirst().orElseThrow();
        assertEquals("Otra ubicacion", reeditada.getUbicacion());
        assertEquals(Obra.ModalidadEjecucion.CONTRATA, reeditada.getModalidadEjecucion());
        obraId = null;
        obraService.eliminar(o.getId());
    }

    @Test
    void losCamposDeMemoriaDescriptivaQuedanNulosSiNoSeInforman() throws Exception {
        Obra o = new Obra("OBRA_SIN_MEMORIA_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();

        Obra recargada = obraService.listarTodas().stream()
                .filter(x -> x.getId().equals(obraId)).findFirst().orElseThrow();
        assertNull(recargada.getUbicacion());
        assertNull(recargada.getEntidadContratante());
        assertNull(recargada.getModalidadEjecucion());
        assertNull(recargada.getSectoresBloques());
    }

    @Test
    void eliminarBorraPartidasYMovimientosEnCascada() throws Exception {
        Obra o = new Obra("OBRA_CASC_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();

        partidaService.guardarTodas(obraId, List.of(new Partida("01.01", "Concreto", "m3", 1, 100)));
        Partida guardada = partidaService.listarPorObra(obraId).get(0);

        MovimientoAlmacen m = new MovimientoAlmacen();
        m.setObraId(obraId);
        m.setPartidaId(guardada.getId());
        m.setFecha(LocalDate.now());
        m.setTipo(MovimientoAlmacen.Tipo.EGRESO);
        m.setCantidad(1);
        m.setCostoUnitarioReal(100);
        movimientoService.registrar(m);

        assertFalse(partidaService.listarPorObra(obraId).isEmpty());
        assertFalse(movimientoService.listarPorObra(obraId).isEmpty());

        obraService.eliminar(obraId);

        assertTrue(partidaService.listarPorObra(obraId).isEmpty(), "las partidas deben borrarse en cascada");
        assertTrue(movimientoService.listarPorObra(obraId).isEmpty(), "los movimientos deben borrarse en cascada");
        obraId = null;
    }

    @AfterEach
    void limpiar() throws Exception {
        if (obraId != null) {
            obraService.eliminar(obraId);
        }
    }
}
