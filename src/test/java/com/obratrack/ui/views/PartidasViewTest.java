package com.obratrack.ui.views;

import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.PartidaService;
import com.obratrack.service.MovimientoService;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class PartidasViewTest {
    @Test
    void cargaSinBloquearSwingYPublicaLaTablaCompleta() throws Exception {
        verificarCarga(false);
    }

    @Test
    void descartaCargaDeUnaObraQueYaNoEstaSeleccionada() throws Exception {
        verificarCarga(true);
    }

    private void verificarCarga(boolean quitarObra) throws Exception {
        CountDownLatch iniciada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch terminada = new CountDownLatch(1);
        CountDownLatch publicada = new CountDownLatch(1);
        AtomicReference<Obra> activa = new AtomicReference<>(new Obra());
        activa.get().setId(1L);
        activa.get().setNombre("Obra de prueba");
        AtomicReference<PartidasView> vista = new AtomicReference<>();
        AtomicReference<JTable> tabla = new AtomicReference<>();
        PartidaService partidas = new PartidaService() {
            @Override public List<Partida> listarPorObra(long id) {
                assertFalse(SwingUtilities.isEventDispatchThread());
                iniciada.countDown();
                // Simula una consulta que no permite interrumpir el trabajo en curso.
                boolean lista = false;
                while (!lista) {
                    try { lista = liberar.await(5, TimeUnit.SECONDS); }
                    catch (InterruptedException ignored) { }
                }
                terminada.countDown();
                Partida p = new Partida("01.01", "Concreto", "m3", 2, 100);
                p.setId(1L);
                return List.of(p);
            }
        };
        MovimientoService movimientos = new MovimientoService() {
            @Override public Map<Long, Double> totalEjecutadoPorPartida(long id) { return Map.of(1L, 50.0); }
        };
        try {
            SwingUtilities.invokeAndWait(() -> {
                vista.set(new PartidasView(activa::get, partidas, movimientos));
                tabla.set(buscarTabla(vista.get()));
                tabla.get().getModel().addTableModelListener(e -> {
                    if (tabla.get().getRowCount() == 1) publicada.countDown();
                });
                vista.get().refrescar();
            });
            assertTrue(iniciada.await(5, TimeUnit.SECONDS));
            // Esta llamada no puede completarse si la consulta esta bloqueando el EDT.
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(0, tabla.get().getRowCount());
                if (quitarObra) {
                    activa.set(null);
                    vista.get().refrescar();
                }
            });
            liberar.countDown();
            assertTrue(terminada.await(5, TimeUnit.SECONDS));
            if (!quitarObra) assertTrue(publicada.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(quitarObra ? 0 : 1, tabla.get().getRowCount());
                if (!quitarObra) assertTrue(tabla.get().getValueAt(0, 1).toString().contains("Concreto"));
            });
        } finally {
            liberar.countDown();
        }
    }

    private JTable buscarTabla(Container contenedor) {
        for (Component c : contenedor.getComponents()) {
            if (c instanceof JTable t) return t;
            if (c instanceof Container hijo) {
                JTable t = buscarTabla(hijo);
                if (t != null) return t;
            }
        }
        return null;
    }
}
