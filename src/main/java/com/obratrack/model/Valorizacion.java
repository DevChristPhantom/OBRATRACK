package com.obratrack.model;

import java.time.LocalDate;

/**
 * Valorización mensual: corte formal del avance ejecutado en un período, con su
 * retención de garantía y monto neto a pagar al contratista. Es un registro legal
 * (base de pago); una vez emitida no se edita, solo se puede eliminar si fue un
 * error antes de presentarla (igual que el resto de registros append-only).
 */
public class Valorizacion {

    private Long id;
    private Long obraId;
    private int numero;
    private LocalDate periodoDesde;
    private LocalDate periodoHasta;
    private LocalDate fechaEmision;
    private double montoEjecutadoPeriodo;
    private double montoAcumuladoAntes;
    private double pctRetencion;
    private double montoRetencion;
    private double montoAmortizacionAdelanto;
    private double montoNetoPagar;
    private String observaciones;
    private String usuarioRegistro;
    private String creadoEn;

    public Valorizacion() {
        this.fechaEmision = LocalDate.now();
        this.pctRetencion = 10.0; // garantia estandar en obras publicas peruanas
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public LocalDate getPeriodoDesde() { return periodoDesde; }
    public void setPeriodoDesde(LocalDate periodoDesde) { this.periodoDesde = periodoDesde; }

    public LocalDate getPeriodoHasta() { return periodoHasta; }
    public void setPeriodoHasta(LocalDate periodoHasta) { this.periodoHasta = periodoHasta; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public double getMontoEjecutadoPeriodo() { return montoEjecutadoPeriodo; }
    public void setMontoEjecutadoPeriodo(double montoEjecutadoPeriodo) { this.montoEjecutadoPeriodo = montoEjecutadoPeriodo; }

    public double getMontoAcumuladoAntes() { return montoAcumuladoAntes; }
    public void setMontoAcumuladoAntes(double montoAcumuladoAntes) { this.montoAcumuladoAntes = montoAcumuladoAntes; }

    public double getPctRetencion() { return pctRetencion; }
    public void setPctRetencion(double pctRetencion) { this.pctRetencion = pctRetencion; }

    public double getMontoRetencion() { return montoRetencion; }
    public void setMontoRetencion(double montoRetencion) { this.montoRetencion = montoRetencion; }

    public double getMontoAmortizacionAdelanto() { return montoAmortizacionAdelanto; }
    public void setMontoAmortizacionAdelanto(double montoAmortizacionAdelanto) { this.montoAmortizacionAdelanto = montoAmortizacionAdelanto; }

    public double getMontoNetoPagar() { return montoNetoPagar; }
    public void setMontoNetoPagar(double montoNetoPagar) { this.montoNetoPagar = montoNetoPagar; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    /** Acumulado total tras esta valorizacion (antes + lo ejecutado en este periodo). */
    public double getMontoAcumuladoTotal() {
        return montoAcumuladoAntes + montoEjecutadoPeriodo;
    }

    @Override
    public String toString() {
        return "Valorizacion N°" + numero + " - " + periodoDesde + " a " + periodoHasta;
    }
}
