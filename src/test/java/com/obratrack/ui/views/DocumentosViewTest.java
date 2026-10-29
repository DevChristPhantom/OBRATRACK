package com.obratrack.ui.views;

import com.obratrack.model.*;
import com.obratrack.service.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class DocumentosViewTest {
    @Test void cargaSoloLaPestanaVisibleYResuelveLasPartidasUnaVez() throws Exception {
        AtomicInteger consultasDoc=new AtomicInteger(),consultasPartidas=new AtomicInteger();
        AtomicReference<DocumentosView> vista=new AtomicReference<>();CountDownLatch aplicada=new CountDownLatch(1);
        Obra obra=new Obra();obra.setId(1L);obra.setNombre("Prueba");
        DocumentoService documentos=new DocumentoService(){
            @Override public List<Documento> listarPorObra(long id,Documento.Categoria categoria){
                assertFalse(SwingUtilities.isEventDispatchThread());consultasDoc.incrementAndGet();
                Documento d=new Documento();d.setNombre("Plano");d.setPartidaId(1L);d.setCategoria(categoria);
                return List.of(d,d,d);
            }
        };
        PartidaService partidas=new PartidaService(){
            @Override public List<Partida> listarPorObra(long id){
                assertFalse(SwingUtilities.isEventDispatchThread());consultasPartidas.incrementAndGet();
                Partida p=new Partida();p.setId(1L);p.setCodigo("01.01");return List.of(p);
            }
        };
        AtomicReference<JTable> tabla=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            vista.set(new DocumentosView(()->obra,documentos,partidas));
            tabla.set(buscarTabla(vista.get()));
            tabla.get().getModel().addTableModelListener(e->{if(tabla.get().getRowCount()==3) aplicada.countDown();});
            vista.get().refrescar();
        });
        assertTrue(aplicada.await(5,TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(()->{
            assertEquals("01.01",tabla.get().getValueAt(0,2));
            assertEquals(1,consultasDoc.get(),"No debe consultar las otras cuatro categorias");
            assertEquals(1,consultasPartidas.get(),"No debe repetir la consulta por documento");
        });
    }
    private JTable buscarTabla(Container c){
        for(Component hijo:c.getComponents()){
            if(hijo instanceof JTable t)return t;
            if(hijo instanceof Container cont){JTable t=buscarTabla(cont);if(t!=null)return t;}
        }return null;
    }
}
