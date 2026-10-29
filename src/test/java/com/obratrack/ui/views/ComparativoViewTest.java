package com.obratrack.ui.views;
import com.obratrack.model.*;
import com.obratrack.service.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class ComparativoViewTest {
    @Test void consultaSinBloquearSwing() throws Exception { verificar(false); }
    @Test void cambiarGranularidadDescartaLaRespuestaAnterior() throws Exception { verificar(true); }

    private void verificar(boolean cambiar) throws Exception {
        CountDownLatch inicio=new CountDownLatch(1), liberar=new CountDownLatch(1), publicada=new CountDownLatch(1);
        AtomicInteger consultas=new AtomicInteger();
        Obra obra=new Obra();obra.setId(1L);obra.setNombre("Prueba");obra.setPresupuestoTotal(100);
        AtomicReference<ComparativoView> vista=new AtomicReference<>();
        AtomicReference<JTable> tabla=new AtomicReference<>();
        MovimientoService servicio=new MovimientoService(){
            @Override public List<ResumenPeriodo> resumenPorPeriodo(long id,Granularidad g){
                assertFalse(SwingUtilities.isEventDispatchThread());
                if(consultas.incrementAndGet()==1){
                    inicio.countDown();long fin=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                    while(liberar.getCount()>0 && System.nanoTime()<fin){
                        try{liberar.await(1,TimeUnit.SECONDS);}catch(InterruptedException ignored){}
                    }
                }
                ResumenPeriodo p=new ResumenPeriodo(g.name(),LocalDate.of(2026,1,1),LocalDate.of(2026,1,31));
                p.setAcumulado(50);p.setNeto(50);p.agregarMovimientos(2);return List.of(p);
            }
        };
        try{
            SwingUtilities.invokeAndWait(()->{
                vista.set(new ComparativoView(()->obra,servicio));
                tabla.set((JTable)campo(vista.get(),"tabla"));
                tabla.get().getModel().addTableModelListener(e->{
                    if(tabla.get().getRowCount()==1) publicada.countDown();
                });vista.get().refrescar();
            });
            assertTrue(inicio.await(5,TimeUnit.SECONDS));
            CountDownLatch disponible=new CountDownLatch(1);
            SwingUtilities.invokeLater(()->{
                assertEquals(0,tabla.get().getRowCount());
                if(cambiar)((JComboBox<?>)campo(vista.get(),"comboGranularidad")).setSelectedItem(Granularidad.SEMANAL);
                disponible.countDown();
            });
            assertTrue(disponible.await(3,TimeUnit.SECONDS),"El EDT debe responder durante la consulta lenta");
            if(!cambiar) liberar.countDown();
            assertTrue(publicada.await(5,TimeUnit.SECONDS));
            liberar.countDown();
            SwingUtilities.invokeAndWait(()->{
                assertEquals(1,tabla.get().getRowCount());
                assertEquals(cambiar?"SEMANAL":"DIARIO",tabla.get().getValueAt(0,0));
                assertTrue(((JLabel)campo(vista.get(),"labelResumen")).getText().contains("2 movimientos"));
            });
        }finally{liberar.countDown();}
    }
    private Object campo(Object vista,String nombre){
        try{var f=ComparativoView.class.getDeclaredField(nombre);f.setAccessible(true);return f.get(vista);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
}
