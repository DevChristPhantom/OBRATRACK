package com.obratrack.model;

/**
 * Una linea del Analisis de Precios Unitarios (APU) de una partida: un insumo
 * (mano de obra, material, equipo o subcontrato) con su cantidad por unidad de
 * la partida y su precio unitario. La suma de los parciales deberia cuadrar
 * con el costo unitario de la partida.
 */
public class ApuInsumo {

    public enum Tipo { MANO_DE_OBRA, MATERIAL, EQUIPO, SUBCONTRATO }

    private Long id;
    private Long partidaId;
    private Long obraId;
    private Tipo tipo;
    private String descripcion;
    private String unidad;
    private double cantidad;
    private double precioUnitario;

    public ApuInsumo() {
        this.tipo = Tipo.MATERIAL;
    }

    public ApuInsumo(Tipo tipo, String descripcion, String unidad, double cantidad, double precioUnitario) {
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.unidad = unidad;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPartidaId() { return partidaId; }
    public void setPartidaId(Long partidaId) { this.partidaId = partidaId; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    /** Parcial de esta linea: cantidad * precio unitario, por unidad de la partida. */
    public double getParcial() {
        return cantidad * precioUnitario;
    }
}
