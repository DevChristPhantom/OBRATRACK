package com.obratrack.model;

import java.time.LocalDate;

/**
 * Registro diario de ingreso o egreso de material/avance contra una partida.
 * Esta es la tabla que usa el almacenero/jefe de obra dia a dia.
 */
public class MovimientoAlmacen {

    public enum Tipo {
        INGRESO, EGRESO
    }

    private Long id;
    private Long obraId;
    private Long partidaId;
    private String partidaCodigo;     // denormalizado para mostrar rapido en tablas
    private String partidaDescripcion;
    private LocalDate fecha;
    private Tipo tipo;
    private double cantidad;
    private double costoUnitarioReal;
    private double costoTotalReal;
    private String observacion;
    private String usuarioRegistro;
    private String creadoEn;        // fecha/hora de creacion (auditoria)
    private String actualizadoEn;   // fecha/hora de la ultima edicion (auditoria)

    public MovimientoAlmacen() {
        this.fecha = LocalDate.now();
        this.tipo = Tipo.EGRESO;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public Long getPartidaId() { return partidaId; }
    public void setPartidaId(Long partidaId) { this.partidaId = partidaId; }

    public String getPartidaCodigo() { return partidaCodigo; }
    public void setPartidaCodigo(String partidaCodigo) { this.partidaCodigo = partidaCodigo; }

    public String getPartidaDescripcion() { return partidaDescripcion; }
    public void setPartidaDescripcion(String partidaDescripcion) { this.partidaDescripcion = partidaDescripcion; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public double getCostoUnitarioReal() { return costoUnitarioReal; }
    public void setCostoUnitarioReal(double costoUnitarioReal) {
        this.costoUnitarioReal = costoUnitarioReal;
        this.costoTotalReal = this.cantidad * costoUnitarioReal;
    }

    public double getCostoTotalReal() { return costoTotalReal; }
    public void setCostoTotalReal(double costoTotalReal) { this.costoTotalReal = costoTotalReal; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    public String getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(String actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    /** Recalcula el total con la cantidad y costo actuales. Llamar tras setCantidad + setCostoUnitarioReal manual. */
    public void recalcularTotal() {
        this.costoTotalReal = this.cantidad * this.costoUnitarioReal;
    }
}
