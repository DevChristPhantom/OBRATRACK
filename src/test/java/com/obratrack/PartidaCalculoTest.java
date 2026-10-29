package com.obratrack;
import com.obratrack.model.Partida;
import com.obratrack.service.PartidaCalculo;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PartidaCalculoTest {
    @Test void acumulaDescendientesSinContarAgrupadorasNiPrefijosSimilares() {
        List<Partida> lista = new ArrayList<>();
        lista.add(partida(1, "01", true));
        lista.add(partida(2, "01.02", true));
        lista.add(partida(3, " 01.02.01 ", false));
        lista.add(partida(4, "01.02.02", false));
        lista.add(partida(5, "010.01", false));
        lista.add(partida(6, null, false));
        Map<Long,Double> ejecutado = Map.of(1L, 999.0, 2L, 999.0, 3L, 100.0, 4L, -25.0, 5L, 8.0);
        var resultado = PartidaCalculo.ejecutadoPorSeccion(lista, ejecutado);
        assertEquals(75.0, resultado.get("01"));
        assertEquals(75.0, resultado.get("01.02"));
        assertEquals(8.0, resultado.get("010"));
    }

    @Test void coincideConElCalculoAnteriorEnUnaObraGrande() {
        List<Partida> lista = new ArrayList<>(); Map<Long,Double> montos = new HashMap<>();
        long id = 1;
        for(int grupo=0; grupo<100; grupo++) {
            lista.add(partida(id++, "G" + grupo, true));
            for(int hoja=0; hoja<50; hoja++) {
                Partida p=partida(id++, "G"+grupo+".01."+hoja, false);
                lista.add(p); montos.put(p.getId(), (double)(hoja-10));
            }
        }
        var nuevos=PartidaCalculo.ejecutadoPorSeccion(lista,montos);
        for(Partida padre:lista) if(padre.isEsPadre()) {
            double anterior=lista.stream().filter(p->!p.isEsPadre() && p.getCodigo().startsWith(padre.getCodigo()+"."))
                    .mapToDouble(p->montos.getOrDefault(p.getId(),0.0)).sum();
            assertEquals(anterior,nuevos.getOrDefault(padre.getCodigo(),0.0));
        }
    }

    @Test void admiteDatosVacios() {
        assertTrue(PartidaCalculo.ejecutadoPorSeccion(List.of(),Map.of()).isEmpty());
        assertTrue(PartidaCalculo.ejecutadoPorSeccion(List.of(partida(1,"",false)),Map.of()).isEmpty());
    }

    private Partida partida(long id,String codigo,boolean padre) {
        Partida p=new Partida(); p.setId(id);p.setCodigo(codigo);p.setEsPadre(padre); return p;
    }
}
