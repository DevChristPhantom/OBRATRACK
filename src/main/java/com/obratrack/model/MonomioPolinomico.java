package com.obratrack.model;

/**
 * Un monomio de la formula polinomica de reajuste de precios de la obra: un
 * elemento representativo del costo (mano de obra, cemento, acero, equipo...)
 * con su coeficiente de incidencia (0-1) y su indice de precios base (I0, del
 * mes de la propuesta). Al valorizar se compara contra el indice revisado (Ir)
 * del mes correspondiente para calcular el factor de reajuste.
 */
public class MonomioPolinomico {

    private Long id;
    private Long obraId;
    private String descripcion;
    private double coeficienteIncidencia;
    private double indiceBase;

    public MonomioPolinomico() {}

    public MonomioPolinomico(String descripcion, double coeficienteIncidencia, double indiceBase) {
        this.descripcion = descripcion;
        this.coeficienteIncidencia = coeficienteIncidencia;
        this.indiceBase = indiceBase;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getCoeficienteIncidencia() { return coeficienteIncidencia; }
    public void setCoeficienteIncidencia(double coeficienteIncidencia) { this.coeficienteIncidencia = coeficienteIncidencia; }

    public double getIndiceBase() { return indiceBase; }
    public void setIndiceBase(double indiceBase) { this.indiceBase = indiceBase; }
}
