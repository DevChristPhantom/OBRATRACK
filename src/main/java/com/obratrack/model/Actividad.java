package com.obratrack.model;

import java.time.LocalDate;

/**
 * Representa una actividad del cronograma de obra: una tarea con fecha de inicio y
 * fin programadas, su peso dentro del avance total de la obra, y su avance real.
 * Puede vincularse opcionalmente a una partida del presupuesto para relacionar
 * cronograma y costo.
 */
public class Actividad {

    private Long id;
    private Long obraId;
    private Long partidaId;
    private String codigo;
    private String descripcion;
    private LocalDate fechaInicioProg;
    private LocalDate fechaFinProg;
    private LocalDate fechaInicioReal;
    private LocalDate fechaFinReal;
    private double pesoPorcentual;
    private double avanceReal;
    private int orden;

    public Actividad() {}

    public Actividad(String codigo, String descripcion, LocalDate fechaInicioProg,
                      LocalDate fechaFinProg, double pesoPorcentual) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.fechaInicioProg = fechaInicioProg;
        this.fechaFinProg = fechaFinProg;
        this.pesoPorcentual = pesoPorcentual;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public Long getPartidaId() { return partidaId; }
    public void setPartidaId(Long partidaId) { this.partidaId = partidaId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFechaInicioProg() { return fechaInicioProg; }
    public void setFechaInicioProg(LocalDate fechaInicioProg) { this.fechaInicioProg = fechaInicioProg; }

    public LocalDate getFechaFinProg() { return fechaFinProg; }
    public void setFechaFinProg(LocalDate fechaFinProg) { this.fechaFinProg = fechaFinProg; }

    public LocalDate getFechaInicioReal() { return fechaInicioReal; }
    public void setFechaInicioReal(LocalDate fechaInicioReal) { this.fechaInicioReal = fechaInicioReal; }

    public LocalDate getFechaFinReal() { return fechaFinReal; }
    public void setFechaFinReal(LocalDate fechaFinReal) { this.fechaFinReal = fechaFinReal; }

    public double getPesoPorcentual() { return pesoPorcentual; }
    public void setPesoPorcentual(double pesoPorcentual) { this.pesoPorcentual = pesoPorcentual; }

    public double getAvanceReal() { return avanceReal; }
    public void setAvanceReal(double avanceReal) { this.avanceReal = avanceReal; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    @Override
    public String toString() {
        return (codigo != null && !codigo.isBlank() ? codigo + " - " : "") + descripcion;
    }
}
