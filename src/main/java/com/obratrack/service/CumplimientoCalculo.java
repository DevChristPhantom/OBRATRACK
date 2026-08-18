package com.obratrack.service;

import com.obratrack.model.ItemCumplimiento;
import com.obratrack.model.ItemCumplimiento.Estado;
import com.obratrack.model.ItemCumplimiento.Impacto;
import com.obratrack.model.ItemCumplimiento.Probabilidad;
import com.obratrack.model.ItemCumplimiento.Severidad;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Logica pura de gestion y cumplimiento (sin base de datos): matriz de riesgo
 * (probabilidad x impacto -&gt; severidad), vencimiento de plazos y conteos por estado.
 */
public final class CumplimientoCalculo {

    private CumplimientoCalculo() {}

    /**
     * Matriz de riesgo 3x3 estandar: la severidad no es un simple producto, sube
     * mas rapido cuando el impacto es alto aunque la probabilidad sea baja
     * (un riesgo poco probable pero catastrofico igual merece atencion).
     */
    public static Severidad calcularSeveridadRiesgo(Probabilidad probabilidad, Impacto impacto) {
        if (probabilidad == null || impacto == null) return Severidad.MEDIA;
        return switch (probabilidad) {
            case BAJA -> switch (impacto) {
                case BAJO -> Severidad.BAJA;
                case MEDIO -> Severidad.BAJA;
                case ALTO -> Severidad.MEDIA;
            };
            case MEDIA -> switch (impacto) {
                case BAJO -> Severidad.BAJA;
                case MEDIO -> Severidad.MEDIA;
                case ALTO -> Severidad.ALTA;
            };
            case ALTA -> switch (impacto) {
                case BAJO -> Severidad.MEDIA;
                case MEDIO -> Severidad.ALTA;
                case ALTO -> Severidad.CRITICA;
            };
        };
    }

    /** true si el item tiene fecha limite vencida y todavia no esta cerrado. */
    public static boolean estaVencido(ItemCumplimiento item, LocalDate hoy) {
        return item.getEstado() != Estado.CERRADO
                && item.getFechaLimite() != null
                && item.getFechaLimite().isBefore(hoy);
    }

    /** Dias que el item lleva abierto: hasta su cierre si ya cerro, hasta hoy si sigue abierto. */
    public static long diasAbierto(ItemCumplimiento item, LocalDate hoy) {
        if (item.getFecha() == null) return 0;
        LocalDate hasta = item.getEstado() == Estado.CERRADO && item.getFechaCierre() != null
                ? item.getFechaCierre() : hoy;
        return Math.max(0, ChronoUnit.DAYS.between(item.getFecha(), hasta));
    }

    public static long conteoAbiertos(List<ItemCumplimiento> items) {
        return items.stream().filter(i -> i.getEstado() != Estado.CERRADO).count();
    }

    public static long conteoVencidos(List<ItemCumplimiento> items, LocalDate hoy) {
        return items.stream().filter(i -> estaVencido(i, hoy)).count();
    }
}
