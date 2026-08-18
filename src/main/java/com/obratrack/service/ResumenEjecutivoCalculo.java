package com.obratrack.service;

import com.obratrack.model.Actividad;
import com.obratrack.model.ItemCumplimiento;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.model.Valorizacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Logica pura (sin base de datos) que consolida en un solo objeto los indicadores de
 * las distintas areas de la obra -financiero, fisico, valorizaciones, cumplimiento-
 * para la ficha ejecutiva exportable a gerencia. No calcula nada nuevo: cada area ya
 * tiene su logica propia (IndicadorSalud, CronogramaCalculo, CumplimientoCalculo); esta
 * clase solo junta esos resultados en una sola fotografia de la obra a una fecha.
 */
public final class ResumenEjecutivoCalculo {

    private ResumenEjecutivoCalculo() {}

    public static final class ConteoCategoria {
        public final ItemCumplimiento.Categoria categoria;
        public final long total;
        public final long abiertos;
        public final long vencidos;

        ConteoCategoria(ItemCumplimiento.Categoria categoria, long total, long abiertos, long vencidos) {
            this.categoria = categoria;
            this.total = total;
            this.abiertos = abiertos;
            this.vencidos = vencidos;
        }
    }

    public static final class Resumen {
        public final Obra obra;
        public final IndicadorSalud.Salud salud;

        public final double presupuestoTotal;
        public final double ejecutadoTotal;
        public final double pctAvanceEconomico;

        public final int actividadesTotal;
        public final int actividadesCompletadas;
        public final int actividadesAtrasadas;
        public final double pctAvanceFisicoReal;
        public final double pctAvanceFisicoProgramado;

        public final int numeroValorizaciones;
        public final Valorizacion ultimaValorizacion; // null si la obra aun no tiene ninguna
        public final double totalValorizadoAcumulado;

        public final List<ConteoCategoria> cumplimiento;

        Resumen(Obra obra, IndicadorSalud.Salud salud, double presupuestoTotal, double ejecutadoTotal,
                double pctAvanceEconomico, int actividadesTotal, int actividadesCompletadas,
                int actividadesAtrasadas, double pctAvanceFisicoReal, double pctAvanceFisicoProgramado,
                int numeroValorizaciones, Valorizacion ultimaValorizacion, double totalValorizadoAcumulado,
                List<ConteoCategoria> cumplimiento) {
            this.obra = obra;
            this.salud = salud;
            this.presupuestoTotal = presupuestoTotal;
            this.ejecutadoTotal = ejecutadoTotal;
            this.pctAvanceEconomico = pctAvanceEconomico;
            this.actividadesTotal = actividadesTotal;
            this.actividadesCompletadas = actividadesCompletadas;
            this.actividadesAtrasadas = actividadesAtrasadas;
            this.pctAvanceFisicoReal = pctAvanceFisicoReal;
            this.pctAvanceFisicoProgramado = pctAvanceFisicoProgramado;
            this.numeroValorizaciones = numeroValorizaciones;
            this.ultimaValorizacion = ultimaValorizacion;
            this.totalValorizadoAcumulado = totalValorizadoAcumulado;
            this.cumplimiento = cumplimiento;
        }
    }

    /**
     * @param itemsPorCategoria items de cumplimiento ya agrupados por categoria (una entrada
     *                          por cada valor de {@link ItemCumplimiento.Categoria}, aunque este vacia)
     * @param valorizaciones    se asume ordenada por numero descendente (como devuelve
     *                          {@link ValorizacionService#listarPorObra}), asi que la primera es la ultima emitida
     */
    public static Resumen construir(Obra obra, List<Partida> partidas, Map<Long, Double> ejecutadoPorPartida,
                                     double ejecutadoTotal, List<Actividad> actividades,
                                     List<Valorizacion> valorizaciones,
                                     Map<ItemCumplimiento.Categoria, List<ItemCumplimiento>> itemsPorCategoria,
                                     LocalDate hoy) {
        IndicadorSalud.Salud salud = IndicadorSalud.evaluar(obra, partidas, ejecutadoPorPartida, ejecutadoTotal);

        double presupuestoTotal = partidas.stream()
                .filter(p -> !p.isEsPadre())
                .mapToDouble(Partida::getCostoTotalPresupuestado)
                .sum();
        double pctAvanceEconomico = presupuestoTotal > 0 ? (ejecutadoTotal / presupuestoTotal) * 100 : 0;

        int actividadesCompletadas = 0;
        int actividadesAtrasadas = 0;
        for (Actividad a : actividades) {
            CronogramaCalculo.Estado estado = CronogramaCalculo.estado(a, hoy);
            if (estado == CronogramaCalculo.Estado.COMPLETADA) actividadesCompletadas++;
            if (estado == CronogramaCalculo.Estado.ATRASADA) actividadesAtrasadas++;
        }
        double pctFisicoReal = CronogramaCalculo.pctRealAcumulado(actividades, hoy, hoy);
        double pctFisicoProgramado = CronogramaCalculo.pctProgramadoAcumulado(actividades, hoy);

        Valorizacion ultima = valorizaciones.isEmpty() ? null : valorizaciones.get(0);
        double totalValorizado = valorizaciones.stream().mapToDouble(Valorizacion::getMontoEjecutadoPeriodo).sum();

        List<ConteoCategoria> cumplimiento = new ArrayList<>();
        for (ItemCumplimiento.Categoria categoria : ItemCumplimiento.Categoria.values()) {
            List<ItemCumplimiento> items = itemsPorCategoria.getOrDefault(categoria, List.of());
            cumplimiento.add(new ConteoCategoria(categoria, items.size(),
                    CumplimientoCalculo.conteoAbiertos(items), CumplimientoCalculo.conteoVencidos(items, hoy)));
        }

        return new Resumen(obra, salud, presupuestoTotal, ejecutadoTotal, pctAvanceEconomico,
                actividades.size(), actividadesCompletadas, actividadesAtrasadas, pctFisicoReal, pctFisicoProgramado,
                valorizaciones.size(), ultima, totalValorizado, cumplimiento);
    }
}
