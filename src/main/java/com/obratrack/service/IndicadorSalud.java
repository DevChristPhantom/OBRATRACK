package com.obratrack.service;

import com.obratrack.model.Obra;
import com.obratrack.model.Partida;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Semaforo de salud de la obra basado en el RITMO de gasto, no solo en el % consumido.
 *
 * Combina dos senales y toma la mas severa:
 *   A) Ritmo gasto vs. tiempo: desvio = %presupuesto_gastado - %tiempo_transcurrido.
 *      Gastar mas rapido de lo que avanza el plazo es la senal temprana de descontrol.
 *      (Requiere fecha de inicio y fecha fin estimada de la obra.)
 *   B) Sobregiro de partidas: cuanto se pasaron las partidas que ya superaron su tope,
 *      como % del presupuesto total. Detecta descontrol fino aunque el global se vea bien.
 *
 * Umbrales:
 *   ROJO     si ejecutado >= presupuesto total, o desvio >= +15 pts, o sobregiro >= 5%.
 *   AMARILLO si desvio en [+5, +15) pts, o %gastado >= 85%, o hay alguna partida sobregirada.
 *   VERDE    en el resto (gasto a la par o por debajo del tiempo, sin sobregiros relevantes).
 *
 * Sin fecha fin estimada se usa el "modo sin fecha": solo senal B + %gastado.
 */
public final class IndicadorSalud {

    private IndicadorSalud() {}

    public enum Nivel { VERDE, AMARILLO, ROJO }

    public static final class Salud {
        public final Nivel nivel;
        public final String titulo;
        public final String detalle;
        public final double pctGastado;
        public final double pctTiempo;   // -1 si no hay fechas
        public final double desvio;      // pctGastado - pctTiempo (0 si no hay fechas)
        public final double pctSobregiro;

        Salud(Nivel nivel, String titulo, String detalle,
              double pctGastado, double pctTiempo, double desvio, double pctSobregiro) {
            this.nivel = nivel;
            this.titulo = titulo;
            this.detalle = detalle;
            this.pctGastado = pctGastado;
            this.pctTiempo = pctTiempo;
            this.desvio = desvio;
            this.pctSobregiro = pctSobregiro;
        }
    }

    private static final double ROJO_DESVIO = 15.0;
    private static final double AMARILLO_DESVIO = 5.0;
    private static final double ROJO_SOBREGIRO = 5.0;   // % del presupuesto total
    private static final double AMARILLO_GASTADO = 85.0; // % del presupuesto total

    public static Salud evaluar(Obra obra, List<Partida> partidas,
                                Map<Long, Double> ejecutadoPorPartida, double ejecutadoTotal) {
        double presupuesto = partidas.stream()
                .filter(p -> !p.isEsPadre())
                .mapToDouble(Partida::getCostoTotalPresupuestado)
                .sum();

        if (presupuesto <= 0) {
            return new Salud(Nivel.VERDE, "Sin presupuesto",
                    "Carga el Excel de presupuesto para evaluar la salud de la obra.",
                    0, -1, 0, 0);
        }

        double pctGastado = (ejecutadoTotal / presupuesto) * 100;

        // Senal B: sobregiro de partidas
        double sobrecosto = 0;
        boolean algunaSobregirada = false;
        for (Partida p : partidas) {
            if (p.isEsPadre() || p.getCostoTotalPresupuestado() <= 0) continue;
            double ej = ejecutadoPorPartida.getOrDefault(p.getId(), 0.0);
            double exceso = ej - p.getCostoTotalPresupuestado();
            if (exceso > 0) {
                sobrecosto += exceso;
                algunaSobregirada = true;
            }
        }
        double pctSobregiro = (sobrecosto / presupuesto) * 100;

        // Senal A: ritmo gasto vs. tiempo
        double pctTiempo = calcularPctTiempo(obra);
        boolean conFecha = pctTiempo >= 0;
        double desvio = conFecha ? (pctGastado - pctTiempo) : 0;

        // Determinar nivel (la senal mas severa manda)
        Nivel nivel;
        if (ejecutadoTotal >= presupuesto || pctSobregiro >= ROJO_SOBREGIRO
                || (conFecha && desvio >= ROJO_DESVIO)) {
            nivel = Nivel.ROJO;
        } else if ((conFecha && desvio >= AMARILLO_DESVIO)
                || pctGastado >= AMARILLO_GASTADO || algunaSobregirada) {
            nivel = Nivel.AMARILLO;
        } else {
            nivel = Nivel.VERDE;
        }

        String titulo = switch (nivel) {
            case VERDE -> "En control";
            case AMARILLO -> "Requiere atencion";
            case ROJO -> "Fuera de control";
        };

        String detalle = construirDetalle(nivel, conFecha, pctGastado, pctTiempo, desvio,
                pctSobregiro, ejecutadoTotal >= presupuesto);

        return new Salud(nivel, titulo, detalle, pctGastado, conFecha ? pctTiempo : -1, desvio, pctSobregiro);
    }

    /** % de tiempo transcurrido segun fechas de la obra, o -1 si no se puede calcular. */
    private static double calcularPctTiempo(Obra obra) {
        LocalDate inicio = obra.getFechaInicio();
        LocalDate fin = obra.getFechaFinEstimada();
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            return -1;
        }
        long total = ChronoUnit.DAYS.between(inicio, fin);
        long transcurridos = ChronoUnit.DAYS.between(inicio, LocalDate.now());
        if (transcurridos < 0) transcurridos = 0;
        if (transcurridos > total) transcurridos = total;
        return (transcurridos * 100.0) / total;
    }

    private static String construirDetalle(Nivel nivel, boolean conFecha, double pctGastado,
                                           double pctTiempo, double desvio, double pctSobregiro,
                                           boolean superoTotal) {
        StringBuilder sb = new StringBuilder();
        if (conFecha) {
            sb.append(String.format("Gasto %.0f%% vs. %.0f%% de tiempo transcurrido", pctGastado, pctTiempo));
            if (desvio >= 0) {
                sb.append(String.format(" (vas %.0f pts por delante del ritmo)", desvio));
            } else {
                sb.append(String.format(" (vas %.0f pts por debajo del ritmo)", -desvio));
            }
        } else {
            sb.append(String.format("Gasto %.0f%% del presupuesto", pctGastado));
        }
        sb.append(". ");
        if (superoTotal) {
            sb.append("El ejecutado ya supero el presupuesto total.");
        } else if (pctSobregiro >= ROJO_SOBREGIRO) {
            sb.append(String.format("Sobregiro de partidas: %.1f%% del presupuesto.", pctSobregiro));
        } else if (pctSobregiro > 0) {
            sb.append(String.format("Hay partidas sobregiradas (%.1f%% del presupuesto).", pctSobregiro));
        } else {
            sb.append("Sin partidas sobregiradas.");
        }
        if (!conFecha) {
            sb.append(" Carga la fecha fin estimada de la obra para el analisis de ritmo.");
        }
        return sb.toString();
    }
}
