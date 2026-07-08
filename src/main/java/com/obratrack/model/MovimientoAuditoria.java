package com.obratrack.model;

/**
 * Entrada del historial de auditoria de un movimiento de almacen.
 * Registra QUE paso (creacion / edicion / eliminacion), CUANDO y con que detalle,
 * para poder rastrear cambios y evitar fraudes. La tabla es append-only.
 */
public class MovimientoAuditoria {

    public enum Accion { CREACION, EDICION, ELIMINACION }

    private Long id;
    private Long obraId;
    private Long movimientoId;
    private Accion accion;
    private String detalle;
    private String usuario;
    private String fechaHora;   // "yyyy-MM-dd HH:mm:ss"

    public MovimientoAuditoria() {}

    public MovimientoAuditoria(Long obraId, Long movimientoId, Accion accion,
                               String detalle, String usuario, String fechaHora) {
        this.obraId = obraId;
        this.movimientoId = movimientoId;
        this.accion = accion;
        this.detalle = detalle;
        this.usuario = usuario;
        this.fechaHora = fechaHora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public Long getMovimientoId() { return movimientoId; }
    public void setMovimientoId(Long movimientoId) { this.movimientoId = movimientoId; }

    public Accion getAccion() { return accion; }
    public void setAccion(Accion accion) { this.accion = accion; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
}
