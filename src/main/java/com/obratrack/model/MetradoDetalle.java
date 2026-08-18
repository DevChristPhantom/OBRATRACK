package com.obratrack.model;

/**
 * Una linea del metrado desagregado de una partida: la cantidad que corresponde
 * a un sector, bloque o frente de trabajo especifico (p. ej. "Bloque A", "Sector 2").
 * La suma de las lineas de una partida deberia cuadrar con su cantidad presupuestada.
 */
public class MetradoDetalle {

    private Long id;
    private Long partidaId;
    private Long obraId;
    private String sector;
    private double cantidad;
    private String observacion;

    public MetradoDetalle() {}

    public MetradoDetalle(String sector, double cantidad) {
        this.sector = sector;
        this.cantidad = cantidad;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPartidaId() { return partidaId; }
    public void setPartidaId(Long partidaId) { this.partidaId = partidaId; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
