package com.obratrack;

import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.MovimientoAuditoria;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.MovimientoService;
import com.obratrack.service.ObraService;
import com.obratrack.service.PartidaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba el neteo EGRESO - INGRESO del ejecutado y que cada movimiento deje su
 * rastro de auditoria. Usa una obra de nombre unico y la elimina al terminar.
 */
class MovimientoServiceTest {

    private final ObraService obraService = new ObraService();
    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();
    private final long nano = System.nanoTime();
    private Long obraId;

    private long crearObraConPartida() throws Exception {
        Obra o = new Obra("MOV_TEST_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();
        partidaService.guardarTodas(obraId, List.of(new Partida("01.01", "Cemento", "bls", 10, 25)));
        return partidaService.listarPorObra(obraId).get(0).getId();
    }

    private void registrar(long partidaId, MovimientoAlmacen.Tipo tipo, double cantidad, double unitario) throws Exception {
        MovimientoAlmacen m = new MovimientoAlmacen();
        m.setObraId(obraId);
        m.setPartidaId(partidaId);
        m.setFecha(LocalDate.now());
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setCostoUnitarioReal(unitario);
        m.setCostoTotalReal(cantidad * unitario);
        movimientoService.registrar(m);
    }

    @Test
    void netaEgresosMenosIngresos() throws Exception {
        long partidaId = crearObraConPartida();
        registrar(partidaId, MovimientoAlmacen.Tipo.EGRESO, 2, 50);   // +100
        registrar(partidaId, MovimientoAlmacen.Tipo.INGRESO, 1, 30);  // -30  (devolucion)

        assertEquals(70.0, movimientoService.totalEjecutadoObra(obraId), 0.001,
                "el ejecutado debe ser EGRESO - INGRESO");

        Map<Long, Double> porPartida = movimientoService.totalEjecutadoPorPartida(obraId);
        assertEquals(70.0, porPartida.get(partidaId), 0.001);
    }

    @Test
    void registrarDejaRastroDeAuditoria() throws Exception {
        long partidaId = crearObraConPartida();
        registrar(partidaId, MovimientoAlmacen.Tipo.EGRESO, 1, 25);

        List<MovimientoAuditoria> auditoria = movimientoService.listarAuditoria(obraId);
        assertFalse(auditoria.isEmpty(), "registrar debe generar una entrada de auditoria");
        assertEquals(MovimientoAuditoria.Accion.CREACION, auditoria.get(0).getAccion());
    }

    @Test
    void eliminarBorraElMovimiento() throws Exception {
        long partidaId = crearObraConPartida();
        registrar(partidaId, MovimientoAlmacen.Tipo.EGRESO, 1, 25);
        MovimientoAlmacen m = movimientoService.listarPorObra(obraId).get(0);

        movimientoService.eliminar(m.getId());

        assertTrue(movimientoService.listarPorObra(obraId).isEmpty(),
                "el movimiento debe quedar eliminado");
    }

    @AfterEach
    void limpiar() throws Exception {
        if (obraId != null) {
            obraService.eliminar(obraId); // borra partidas y movimientos en cascada
        }
    }
}
