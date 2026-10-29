package com.obratrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class CargaVistaTest {
    @Test void consultaFueraDeSwingYRestauraLosPermisosDeLosBotones() throws Exception {
        AtomicReference<CargaVista> carga=new AtomicReference<>();
        JButton editar=new JButton(), restringido=new JButton(), menu=new JButton();
        JComboBox<String> combo=new JComboBox<>(new String[]{"Uno"});combo.setEditable(true);
        CountDownLatch inicio=new CountDownLatch(1), liberar=new CountDownLatch(1), fin=new CountDownLatch(1);
        AtomicBoolean consultaEdt=new AtomicBoolean(true), aplicaEdt=new AtomicBoolean(false);
        try {
            SwingUtilities.invokeAndWait(()->{
                JPanel panel=new JPanel();panel.add(editar);panel.add(restringido);panel.add(combo);restringido.setEnabled(false);
                carga.set(new CargaVista(panel));
                carga.get().ejecutar(()->{consultaEdt.set(SwingUtilities.isEventDispatchThread());inicio.countDown();liberar.await();return 7;},
                        valor->{aplicaEdt.set(SwingUtilities.isEventDispatchThread());assertEquals(7,valor);fin.countDown();},
                        e->{throw new AssertionError(e);});
            });
            assertTrue(inicio.await(5,TimeUnit.SECONDS));
            CountDownLatch responde=new CountDownLatch(1);
            SwingUtilities.invokeLater(()->{assertFalse(editar.isEnabled());assertTrue(menu.isEnabled());responde.countDown();});
            assertTrue(responde.await(3,TimeUnit.SECONDS));
            liberar.countDown();assertTrue(fin.await(5,TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(()->{assertTrue(editar.isEnabled());assertFalse(restringido.isEnabled());assertTrue(combo.isEnabled());assertTrue(combo.getEditor().getEditorComponent().isEnabled());});
            assertFalse(consultaEdt.get());assertTrue(aplicaEdt.get());
        } finally {liberar.countDown();}
    }

    @Test void unaRespuestaCanceladaNoSobrescribeLaNueva() throws Exception {
        AtomicReference<CargaVista> carga=new AtomicReference<>();AtomicInteger resultado=new AtomicInteger();
        CountDownLatch inicio=new CountDownLatch(1), liberar=new CountDownLatch(1), viejaTerminada=new CountDownLatch(1), nueva=new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(()->{
                carga.set(new CargaVista(new JPanel()));
                carga.get().ejecutar(()->{inicio.countDown();liberar.await();viejaTerminada.countDown();return 1;},resultado::set,e->{throw new AssertionError(e);});
            });
            assertTrue(inicio.await(5,TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(()->carga.get().ejecutar(()->2,v->{resultado.set(v);nueva.countDown();},e->{throw new AssertionError(e);}));
            assertTrue(nueva.await(5,TimeUnit.SECONDS));liberar.countDown();assertTrue(viejaTerminada.await(5,TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(()->assertEquals(2,resultado.get()));
        }finally{liberar.countDown();}
    }

    @Test void unErrorRestauraLosControles() throws Exception {
        JButton boton=new JButton();CountDownLatch error=new CountDownLatch(1);AtomicReference<Throwable> causa=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            JPanel p=new JPanel();p.add(boton);
            new CargaVista(p).ejecutar(()->{throw new SQLException("Prueba");},v->fail("No debe aplicar"),e->{causa.set(e);error.countDown();});
        });
        assertTrue(error.await(5,TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(()->assertTrue(boton.isEnabled()));
        assertInstanceOf(SQLException.class,causa.get());
    }
}
