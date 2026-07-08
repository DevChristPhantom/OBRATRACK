package com.obratrack.model;

/**
 * Representa una partida del presupuesto, importada desde el Excel de licitacion.
 * Las partidas pueden ser "padre" (agrupadoras, sin metrado propio) o "hoja"
 * (con cantidad/precio/total, las que realmente se ejecutan en obra).
 */
public class Partida {

    private Long id;
    private Long obraId;
    private String codigo;          // ej: 01.01.01.04
    private String descripcion;
    private String unidad;          // m2, m3, kg, gln, und, ml...
    private double cantidadPresupuestada;
    private double costoUnitario;
    private double costoTotalPresupuestado;
    private boolean esPadre;        // true = fila agrupadora (no se registra material directamente)
    private int nivel;              // profundidad jerarquica (1 = "01", 2 = "01.01", etc.)

    public Partida() {}

    public Partida(String codigo, String descripcion, String unidad,
                    double cantidadPresupuestada, double costoUnitario) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.unidad = unidad;
        this.cantidadPresupuestada = cantidadPresupuestada;
        this.costoUnitario = costoUnitario;
        this.costoTotalPresupuestado = cantidadPresupuestada * costoUnitario;
        this.esPadre = (unidad == null || unidad.isBlank());
        this.nivel = codigo != null ? codigo.split("\\.").length : 1;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public double getCantidadPresupuestada() { return cantidadPresupuestada; }
    public void setCantidadPresupuestada(double cantidadPresupuestada) { this.cantidadPresupuestada = cantidadPresupuestada; }

    public double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(double costoUnitario) { this.costoUnitario = costoUnitario; }

    public double getCostoTotalPresupuestado() { return costoTotalPresupuestado; }
    public void setCostoTotalPresupuestado(double costoTotalPresupuestado) { this.costoTotalPresupuestado = costoTotalPresupuestado; }

    public boolean isEsPadre() { return esPadre; }
    public void setEsPadre(boolean esPadre) { this.esPadre = esPadre; }

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    @Override
    public String toString() {
        return (codigo != null ? codigo + " - " : "") + descripcion;
    }
}
