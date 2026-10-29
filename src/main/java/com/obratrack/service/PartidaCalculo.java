package com.obratrack.service;

import com.obratrack.model.Partida;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Subtotales por prefijo, calculados una sola vez para toda la tabla. */
public final class PartidaCalculo {
    private PartidaCalculo() {}

    public static Map<String, Double> ejecutadoPorSeccion(List<Partida> partidas, Map<Long, Double> ejecutado) {
        Map<String, Double> totales = new HashMap<>();
        for (Partida p : partidas) {
            if (p.isEsPadre() || p.getCodigo() == null) continue;
            String codigo = p.getCodigo().trim();
            double monto = ejecutado.getOrDefault(p.getId(), 0.0);
            for (int punto = codigo.indexOf('.'); punto >= 0; punto = codigo.indexOf('.', punto + 1)) {
                if (punto > 0) totales.merge(codigo.substring(0, punto), monto, Double::sum);
            }
        }
        return totales;
    }
}
