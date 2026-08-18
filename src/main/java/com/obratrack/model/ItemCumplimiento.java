package com.obratrack.model;

import java.time.LocalDate;

/**
 * Un item de gestion y cumplimiento de la obra: riesgo, compromiso ambiental,
 * hallazgo de control de calidad (ensayo/no conformidad) o incidente de SST.
 * Las cuatro categorias comparten la misma forma real: algo que se identifica,
 * se le hace seguimiento y se cierra. Probabilidad/impacto solo aplican a RIESGO
 * (de ahi se calcula la severidad); en las demas categorias la severidad se elige
 * directamente.
 */
public class ItemCumplimiento {

    public enum Categoria { RIESGO, AMBIENTAL, CALIDAD, SST }

    public enum Probabilidad { BAJA, MEDIA, ALTA }

    public enum Impacto { BAJO, MEDIO, ALTO }

    public enum Severidad { BAJA, MEDIA, ALTA, CRITICA }

    public enum Estado { ABIERTO, EN_PROCESO, CERRADO }

    private Long id;
    private Long obraId;
    private Long partidaId;
    private Categoria categoria;
    private String descripcion;
    private Probabilidad probabilidad;
    private Impacto impacto;
    private Severidad severidad;
    private LocalDate fecha;
    private LocalDate fechaLimite;
    private Estado estado;
    private LocalDate fechaCierre;
    private String responsable;
    private String accionSeguimiento;
    private String usuarioRegistro;
    private String creadoEn;

    public ItemCumplimiento() {
        this.fecha = LocalDate.now();
        this.estado = Estado.ABIERTO;
        this.severidad = Severidad.MEDIA;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public Long getPartidaId() { return partidaId; }
    public void setPartidaId(Long partidaId) { this.partidaId = partidaId; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Probabilidad getProbabilidad() { return probabilidad; }
    public void setProbabilidad(Probabilidad probabilidad) { this.probabilidad = probabilidad; }

    public Impacto getImpacto() { return impacto; }
    public void setImpacto(Impacto impacto) { this.impacto = impacto; }

    public Severidad getSeveridad() { return severidad; }
    public void setSeveridad(Severidad severidad) { this.severidad = severidad; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre = fechaCierre; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public String getAccionSeguimiento() { return accionSeguimiento; }
    public void setAccionSeguimiento(String accionSeguimiento) { this.accionSeguimiento = accionSeguimiento; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }
}
