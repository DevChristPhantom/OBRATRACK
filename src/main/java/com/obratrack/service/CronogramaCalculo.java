package com.obratrack.service;

import com.obratrack.model.Actividad;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica pura del cronograma (sin base de datos): avance programado por fecha,
 * estado de cada actividad y curva S (programado vs. real) de la obra completa.
 *
 * <p>El avance programado de una actividad se interpola linealmente entre su fecha
 * de inicio y fin programadas (0% antes de empezar, 100% al terminar). El avance
 * real de la obra se pondera por {@code pesoPorcentual}: la suma de pesos de las
 * actividades debe acercarse a 100 para que el % acumulado sea representativo,
 * pero no es obligatorio (se normaliza sobre el peso total registrado).
 *
 * <p>No existe un historico dia a dia del avance real (solo se guarda el valor
 * actual), asi que la curva real hacia el pasado es una aproximacion: para cada
 * actividad iniciada se interpola linealmente entre su fecha de inicio real y hoy,
 * llegando a su {@code avanceReal} actual en el dia de hoy.
 */
public final class CronogramaCalculo {

    private CronogramaCalculo() {}

    public enum Estado { PENDIENTE, EN_PROCESO, COMPLETADA, ATRASADA }

    /** Puntos porcentuales de tolerancia antes de marcar una actividad como atrasada. */
    private static final double MARGEN_ATRASO = 5.0;

    /** % de avance programado de una actividad en una fecha dada (0-100). */
    public static double avanceProgramado(Actividad a, LocalDate fecha) {
        LocalDate ini = a.getFechaInicioProg();
        LocalDate fin = a.getFechaFinProg();
        if (ini == null || fin == null) return 0;
        if (!fin.isAfter(ini)) {
            return fecha.isBefore(ini) ? 0 : 100;
        }
        if (!fecha.isAfter(ini)) return 0;
        if (!fecha.isBefore(fin)) return 100;
        long total = ChronoUnit.DAYS.between(ini, fin);
        long transcurridos = ChronoUnit.DAYS.between(ini, fecha);
        return (transcurridos * 100.0) / total;
    }

    /** Estado operativo de la actividad a una fecha (para colorear la tabla y el Gantt). */
    public static Estado estado(Actividad a, LocalDate hoy) {
        if (a.getAvanceReal() >= 100 || a.getFechaFinReal() != null) return Estado.COMPLETADA;
        double progHoy = avanceProgramado(a, hoy);
        if (a.getAvanceReal() + MARGEN_ATRASO < progHoy) return Estado.ATRASADA;
        if (a.getFechaInicioReal() != null || a.getAvanceReal() > 0) return Estado.EN_PROCESO;
        return Estado.PENDIENTE;
    }

    /** % acumulado PROGRAMADO de toda la obra a una fecha, ponderado por peso de actividad. */
    public static double pctProgramadoAcumulado(List<Actividad> actividades, LocalDate fecha) {
        double pesoTotal = actividades.stream().mapToDouble(Actividad::getPesoPorcentual).sum();
        if (pesoTotal <= 0) return 0;
        double acumulado = 0;
        for (Actividad a : actividades) {
            acumulado += a.getPesoPorcentual() * (avanceProgramado(a, fecha) / 100.0);
        }
        return (acumulado / pesoTotal) * 100.0;
    }

    /** % acumulado REAL de toda la obra a una fecha (aproximado, ver clase). */
    public static double pctRealAcumulado(List<Actividad> actividades, LocalDate fecha, LocalDate hoy) {
        double pesoTotal = actividades.stream().mapToDouble(Actividad::getPesoPorcentual).sum();
        if (pesoTotal <= 0) return 0;
        double acumulado = 0;
        for (Actividad a : actividades) {
            acumulado += a.getPesoPorcentual() * (avanceRealAFecha(a, fecha, hoy) / 100.0);
        }
        return (acumulado / pesoTotal) * 100.0;
    }

    /** Avance real de una actividad en una fecha, interpolado entre su inicio real y hoy. */
    private static double avanceRealAFecha(Actividad a, LocalDate fecha, LocalDate hoy) {
        if (fecha.isAfter(hoy)) return 0; // no se proyecta el futuro en la curva real
        LocalDate iniReal = a.getFechaInicioReal();
        // Si no se registro fecha de inicio real pero si hay avance cargado, se asume que
        // empezo segun lo programado: evita que un avance real> 0 quede en 0% en el resumen
        // y la curva S solo porque el campo (opcional) de fecha real no se lleno.
        if (iniReal == null && a.getAvanceReal() > 0) iniReal = a.getFechaInicioProg();
        if (iniReal == null || fecha.isBefore(iniReal)) return 0;
        if (a.getFechaFinReal() != null && !fecha.isBefore(a.getFechaFinReal())) return 100;
        long total = ChronoUnit.DAYS.between(iniReal, hoy);
        if (total <= 0) return a.getAvanceReal(); // empezo hoy mismo: no hay tramo que interpolar
        long transcurridos = ChronoUnit.DAYS.between(iniReal, fecha);
        double proporcion = Math.min(1.0, transcurridos / (double) total);
        return proporcion * a.getAvanceReal();
    }

    /** Un punto de la curva S: fecha + % acumulado programado y real. */
    public static final class PuntoCurva {
        public final LocalDate fecha;
        public final double pctProgramado;
        public final double pctReal;

        PuntoCurva(LocalDate fecha, double pctProgramado, double pctReal) {
            this.fecha = fecha;
            this.pctProgramado = pctProgramado;
            this.pctReal = pctReal;
        }
    }

    /**
     * Curva S completa entre {@code desde} y {@code hasta}, muestreada en {@code puntos}
     * fechas equiespaciadas (minimo 2: inicio y fin).
     */
    public static List<PuntoCurva> curvaS(List<Actividad> actividades, LocalDate desde, LocalDate hasta,
                                           int puntos, LocalDate hoy) {
        List<PuntoCurva> resultado = new ArrayList<>();
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            return resultado;
        }
        int n = Math.max(2, puntos);
        long totalDias = ChronoUnit.DAYS.between(desde, hasta);
        for (int i = 0; i < n; i++) {
            long offset = Math.round(totalDias * (i / (double) (n - 1)));
            LocalDate fecha = desde.plusDays(offset);
            double prog = pctProgramadoAcumulado(actividades, fecha);
            double real = pctRealAcumulado(actividades, fecha, hoy);
            resultado.add(new PuntoCurva(fecha, prog, real));
        }
        return resultado;
    }
}
