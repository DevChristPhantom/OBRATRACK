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

    /** Modalidad de ejecucion de la obra publica (memoria descriptiva del expediente tecnico). */
    public enum ModalidadEjecucion {
        CONTRATA, ADMINISTRACION_DIRECTA, CONCURSO_OFERTA, NUCLEO_EJECUTOR
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

    // --- Memoria descriptiva: ficha ampliada del expediente tecnico ---
    private String ubicacion;
    private String entidadContratante;
    private ModalidadEjecucion modalidadEjecucion;
    private String sectoresBloques;

    // --- Presupuesto analitico: porcentajes contractuales sobre el costo directo ---
    private double pctGastosGenerales;
    private double pctUtilidad;
    private double pctIgv = 18.0; // tasa vigente en Peru

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

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getEntidadContratante() { return entidadContratante; }
    public void setEntidadContratante(String entidadContratante) { this.entidadContratante = entidadContratante; }

    public ModalidadEjecucion getModalidadEjecucion() { return modalidadEjecucion; }
    public void setModalidadEjecucion(ModalidadEjecucion modalidadEjecucion) { this.modalidadEjecucion = modalidadEjecucion; }

    public String getSectoresBloques() { return sectoresBloques; }
    public void setSectoresBloques(String sectoresBloques) { this.sectoresBloques = sectoresBloques; }

    public double getPctGastosGenerales() { return pctGastosGenerales; }
    public void setPctGastosGenerales(double pctGastosGenerales) { this.pctGastosGenerales = pctGastosGenerales; }

    public double getPctUtilidad() { return pctUtilidad; }
    public void setPctUtilidad(double pctUtilidad) { this.pctUtilidad = pctUtilidad; }

    public double getPctIgv() { return pctIgv; }
    public void setPctIgv(double pctIgv) { this.pctIgv = pctIgv; }

    @Override
    public String toString() {
        return nombre; // para que se vea limpio en JComboBox
    }
}
