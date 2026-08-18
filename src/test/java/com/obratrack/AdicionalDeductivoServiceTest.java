package com.obratrack;

import com.obratrack.model.AdicionalDeductivo;
import com.obratrack.model.AdicionalDeductivo.Tipo;
import com.obratrack.model.Obra;
import com.obratrack.service.AdicionalDeductivoService;
import com.obratrack.service.ObraService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba el CRUD de adicionales/deductivos contra la base real: numeracion
 * correlativa por obra, orden de listado y borrado en cascada al eliminar la obra.
 */
class AdicionalDeductivoServiceTest {

    private final ObraService obraService = new ObraService();
    private final AdicionalDeductivoService adicionalService = new AdicionalDeductivoService();
    private final long nano = System.nanoTime();
    private Long obraId;

    @Test
    void asignaNumeracionCorrelativaYListaEnOrdenDescendente() throws Exception {
        Obra o = new Obra("OBRA_ADIC_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();

        AdicionalDeductivo primero = nuevo(Tipo.ADICIONAL, "Primer adicional", 5_000);
        primero.setObraId(obraId);
        adicionalService.crear(primero);
        assertEquals(1, primero.getNumero());

        AdicionalDeductivo segundo = nuevo(Tipo.DEDUCTIVO, "Primer deductivo", 1_500);
        segundo.setObraId(obraId);
        adicionalService.crear(segundo);
        assertEquals(2, segundo.getNumero());

        List<AdicionalDeductivo> lista = adicionalService.listarPorObra(obraId);
        assertEquals(2, lista.size());
        assertEquals(2, lista.get(0).getNumero(), "debe listar el mas reciente primero");
        assertEquals(1, lista.get(1).getNumero());
    }

    @Test
    void eliminarQuitaElRegistroDeLaLista() throws Exception {
        Obra o = new Obra("OBRA_ADIC_DEL_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();

        AdicionalDeductivo ad = nuevo(Tipo.ADICIONAL, "A borrar", 1_000);
        ad.setObraId(obraId);
        adicionalService.crear(ad);
        assertFalse(adicionalService.listarPorObra(obraId).isEmpty());

        adicionalService.eliminar(ad.getId());
        assertTrue(adicionalService.listarPorObra(obraId).isEmpty());
    }

    @Test
    void eliminarLaObraBorraSusAdicionalesEnCascada() throws Exception {
        Obra o = new Obra("OBRA_ADIC_CASC_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();

        AdicionalDeductivo ad = nuevo(Tipo.ADICIONAL, "En cascada", 1_000);
        ad.setObraId(obraId);
        adicionalService.crear(ad);

        obraService.eliminar(obraId);
        assertTrue(adicionalService.listarPorObra(obraId).isEmpty());
        obraId = null;
    }

    @Test
    void crearSinDescripcionLanzaExcepcion() throws Exception {
        Obra o = new Obra("OBRA_ADIC_VAL_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();

        AdicionalDeductivo ad = nuevo(Tipo.ADICIONAL, "", 1_000);
        ad.setObraId(obraId);
        assertThrows(IllegalArgumentException.class, () -> adicionalService.crear(ad));
    }

    private AdicionalDeductivo nuevo(Tipo tipo, String descripcion, double monto) {
        AdicionalDeductivo ad = new AdicionalDeductivo();
        ad.setTipo(tipo);
        ad.setDescripcion(descripcion);
        ad.setMonto(monto);
        ad.setFechaAprobacion(LocalDate.now());
        return ad;
    }

    @AfterEach
    void limpiar() throws Exception {
        if (obraId != null) {
            obraService.eliminar(obraId);
        }
    }
}
