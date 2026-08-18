package com.obratrack.service;

import com.obratrack.model.AdicionalDeductivo;

import java.util.List;

/**
 * Logica pura (sin base de datos) del presupuesto analitico: a partir del costo directo
 * (suma de partidas) y los porcentajes contractuales de gastos generales, utilidad e IGV,
 * calcula el presupuesto contractual; y a partir de los adicionales/deductivos aprobados,
 * el presupuesto actualizado vigente de la obra.
 */
public final class PresupuestoAnaliticoCalculo {

    private PresupuestoAnaliticoCalculo() {}

    public static final class Resultado {
        public final double costoDirecto;
        public final double pctGastosGenerales;
        public final double montoGastosGenerales;
        public final double pctUtilidad;
        public final double montoUtilidad;
        public final double subtotal;
        public final double pctIgv;
        public final double montoIgv;
        public final double presupuestoContractual;
        public final double totalAdicionales;
        public final double totalDeductivos;
        public final double presupuestoActualizado;

        Resultado(double costoDirecto, double pctGastosGenerales, double montoGastosGenerales,
                   double pctUtilidad, double montoUtilidad, double subtotal, double pctIgv, double montoIgv,
                   double presupuestoContractual, double totalAdicionales, double totalDeductivos,
                   double presupuestoActualizado) {
            this.costoDirecto = costoDirecto;
            this.pctGastosGenerales = pctGastosGenerales;
            this.montoGastosGenerales = montoGastosGenerales;
            this.pctUtilidad = pctUtilidad;
            this.montoUtilidad = montoUtilidad;
            this.subtotal = subtotal;
            this.pctIgv = pctIgv;
            this.montoIgv = montoIgv;
            this.presupuestoContractual = presupuestoContractual;
            this.totalAdicionales = totalAdicionales;
            this.totalDeductivos = totalDeductivos;
            this.presupuestoActualizado = presupuestoActualizado;
        }
    }

    public static Resultado calcular(double costoDirecto, double pctGastosGenerales, double pctUtilidad,
                                      double pctIgv, List<AdicionalDeductivo> adicionalesDeductivos) {
        double montoGG = costoDirecto * pctGastosGenerales / 100.0;
        double montoUtilidad = costoDirecto * pctUtilidad / 100.0;
        double subtotal = costoDirecto + montoGG + montoUtilidad;
        double montoIgv = subtotal * pctIgv / 100.0;
        double presupuestoContractual = subtotal + montoIgv;

        double totalAdicionales = adicionalesDeductivos.stream()
                .filter(ad -> ad.getTipo() == AdicionalDeductivo.Tipo.ADICIONAL)
                .mapToDouble(AdicionalDeductivo::getMonto)
                .sum();
        double totalDeductivos = adicionalesDeductivos.stream()
                .filter(ad -> ad.getTipo() == AdicionalDeductivo.Tipo.DEDUCTIVO)
                .mapToDouble(AdicionalDeductivo::getMonto)
                .sum();
        double presupuestoActualizado = presupuestoContractual + totalAdicionales - totalDeductivos;

        return new Resultado(costoDirecto, pctGastosGenerales, montoGG, pctUtilidad, montoUtilidad,
                subtotal, pctIgv, montoIgv, presupuestoContractual, totalAdicionales, totalDeductivos,
                presupuestoActualizado);
    }
}
