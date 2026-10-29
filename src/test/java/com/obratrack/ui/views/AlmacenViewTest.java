package com.obratrack.ui.views;

import com.obratrack.model.*;
import com.obratrack.service.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AlmacenViewTest {
    @Test void consultaSinBloquearLaVentana() throws Exception { verificar(false); }
    @Test void descartaLaObraAnterior() throws Exception { verificar(true); }

    private void verificar(boolean quitarObra) throws Exception {
        CountDownLatch iniciada = new CountDownLatch(1), liberar = new CountDownLatch(1);
        CountDownLatch publicada = new CountDownLatch(1), terminada = new CountDownLatch(1);
        AtomicReference<Obra> activa = new AtomicReference<>(new Obra());
        activa.get().setId(1L); activa.get().setNombre("Prueba");
        AtomicReference<AlmacenView> vista = new AtomicReference<>();
        AtomicReference<JTable> tabla = new AtomicReference<>();
        PartidaService partidas = new PartidaService() {
            @Override public List<Partida> listarEjecutablesPorObra(long id) {
                assertFalse(SwingUtilities.isEventDispatchThread());
                iniciada.countDown();
                long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (liberar.getCount() > 0 && System.nanoTime() < limite) {
                    try { liberar.await(1, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                }
                terminada.countDown();
                Partida p = new Partida("01.01", "Concreto", "m3", 1, 100); p.setId(1L);
                return List.of(p);
            }
        };
        MovimientoService movimientos = new MovimientoService() {
            @Override public List<MovimientoAlmacen> listarPorObra(long id) {
                assertFalse(SwingUtilities.isEventDispatchThread());
                MovimientoAlmacen m = new MovimientoAlmacen();
                m.setId(1L); m.setPartidaId(1L); m.setFecha(LocalDate.of(2026, 10, 27));
                m.setTipo(MovimientoAlmacen.Tipo.EGRESO); m.setCantidad(2); m.setCostoUnitarioReal(20);
                return List.of(m);
            }
        };
        try {
            SwingUtilities.invokeAndWait(() -> {
                vista.set(new AlmacenView(activa::get, () -> {}, partidas, movimientos));
                tabla.set((JTable) campo(vista.get(), "tabla"));
                tabla.get().getModel().addTableModelListener(e -> {
                    if (tabla.get().getRowCount() == 1) publicada.countDown();
                });
                vista.get().refrescar();
            });
            assertTrue(iniciada.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(0, tabla.get().getRowCount());
                assertFalse(((JButton) campo(vista.get(), "btnGuardar")).isEnabled());
                if (quitarObra) { activa.set(null); vista.get().refrescar(); }
            });
            liberar.countDown();
            assertTrue(terminada.await(5, TimeUnit.SECONDS));
            if (!quitarObra) assertTrue(publicada.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(quitarObra ? 0 : 1, tabla.get().getRowCount());
                JComboBox<?> combo = (JComboBox<?>) campo(vista.get(), "comboPartida");
                assertEquals(quitarObra ? 0 : 1, combo.getItemCount());
                assertTrue(combo.isEnabled());
            });
        } finally { liberar.countDown(); }
    }

    private Object campo(Object vista, String nombre) {
        try {
            var f = AlmacenView.class.getDeclaredField(nombre); f.setAccessible(true); return f.get(vista);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
