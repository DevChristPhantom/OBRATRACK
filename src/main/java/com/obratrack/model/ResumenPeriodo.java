package com.obratrack.model;

import java.time.LocalDate;

/**
 * Resumen de ejecucion de obra agregado a un periodo de tiempo (un dia, una
 * semana o un mes). Es el bloque que alimenta el comparativo temporal
 * diario / semanal / mensual a lo largo de toda la duracion de la obra.
 *
 * neto = egresos - ingresos (lo realmente consumido en el periodo).
 * acumulado = suma corrida de neto desde el primer periodo hasta este.
 */
public class ResumenPeriodo {

    private final String etiqueta;   // texto legible del periodo (ej "2026-06", "Sem 26 (22/06 a 28/06)")
    private final LocalDate inicio;  // primer dia del periodo
    private final LocalDate fin;     // ultimo dia del periodo
    private double egresos;
    private double ingresos;
    private double neto;
    private double acumulado;
    private int numMovimientos;

    public ResumenPeriodo(String etiqueta, LocalDate inicio, LocalDate fin) {
        this.etiqueta = etiqueta;
        this.inicio = inicio;
        this.fin = fin;
    }

    /** Suma los egresos de un dia a este periodo. */
    public void agregarEgresos(double valor) { this.egresos += valor; }

    /** Suma los ingresos de un dia a este periodo. */
    public void agregarIngresos(double valor) { this.ingresos += valor; }

    /** Suma la cantidad de movimientos de un dia a este periodo. */
    public void agregarMovimientos(int n) { this.numMovimientos += n; }

    // --- Getters / setters ---

    public String getEtiqueta() { return etiqueta; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getFin() { return fin; }

    public double getEgresos() { return egresos; }
    public double getIngresos() { return ingresos; }

    public double getNeto() { return neto; }
    public void setNeto(double neto) { this.neto = neto; }

    public double getAcumulado() { return acumulado; }
    public void setAcumulado(double acumulado) { this.acumulado = acumulado; }

    public int getNumMovimientos() { return numMovimientos; }
    public void setNumMovimientos(int numMovimientos) { this.numMovimientos = numMovimientos; }
}
