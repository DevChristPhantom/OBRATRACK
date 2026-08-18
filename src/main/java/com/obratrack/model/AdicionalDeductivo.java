package com.obratrack.model;

import java.time.LocalDate;

/**
 * Modificacion aprobada al presupuesto contractual de la obra: un adicional (suma)
 * o un deductivo (resta), con su resolucion de aprobacion. Junto con el costo
 * directo, gastos generales, utilidad e IGV, forma el presupuesto analitico.
 */
public class AdicionalDeductivo {

    public enum Tipo { ADICIONAL, DEDUCTIVO }

    private Long id;
    private Long obraId;
    private int numero;
    private Tipo tipo;
    private String descripcion;
    private double monto;
    private LocalDate fechaAprobacion;
    private String resolucionAprobacion;
    private String usuarioRegistro;
    private String creadoEn;

    public AdicionalDeductivo() {
        this.fechaAprobacion = LocalDate.now();
        this.tipo = Tipo.ADICIONAL;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public LocalDate getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(LocalDate fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }

    public String getResolucionAprobacion() { return resolucionAprobacion; }
    public void setResolucionAprobacion(String resolucionAprobacion) { this.resolucionAprobacion = resolucionAprobacion; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    @Override
    public String toString() {
        return tipo + " N°" + numero + " - " + descripcion;
    }
}
