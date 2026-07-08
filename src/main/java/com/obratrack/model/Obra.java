package com.obratrack.model;

import java.time.LocalDate;

/**
 * Representa una obra/proyecto de construccion.
 * Cada obra tiene su propio presupuesto importado desde un Excel de licitacion.
 */
public class Obra {

    public enum Estado {
        ACTIVA, PAUSADA, FINALIZADA
    }

    private Long id;
    private String nombre;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFinEstimada;
    private double presupuestoTotal;
    private Estado estado;
    private String rutaExcelOrigen;
    private LocalDate fechaCreacion;

    public Obra() {
        this.estado = Estado.ACTIVA;
        this.fechaCreacion = LocalDate.now();
    }

    public Obra(String nombre, String descripcion, LocalDate fechaInicio, LocalDate fechaFinEstimada) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFinEstimada = fechaFinEstimada;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFinEstimada() { return fechaFinEstimada; }
    public void setFechaFinEstimada(LocalDate fechaFinEstimada) { this.fechaFinEstimada = fechaFinEstimada; }

    public double getPresupuestoTotal() { return presupuestoTotal; }
    public void setPresupuestoTotal(double presupuestoTotal) { this.presupuestoTotal = presupuestoTotal; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getRutaExcelOrigen() { return rutaExcelOrigen; }
    public void setRutaExcelOrigen(String rutaExcelOrigen) { this.rutaExcelOrigen = rutaExcelOrigen; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    @Override
    public String toString() {
        return nombre; // para que se vea limpio en JComboBox
    }
}
