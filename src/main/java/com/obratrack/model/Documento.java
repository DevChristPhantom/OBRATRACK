package com.obratrack.model;

import java.time.LocalDate;

/**
 * Un documento de la obra: plano (con version), especificacion tecnica (ligada
 * a una partida), foto de avance, anexo o estudio de ingenieria (geotecnico,
 * hidrologico, de impacto ambiental, etc.). El archivo real vive en el disco
 * (carpeta de datos de la app, ver {@link com.obratrack.core.Rutas#documentos()});
 * esta fila solo guarda su metadata y la ruta relativa hacia el.
 */
public class Documento {

    public enum Categoria { PLANO, ESPECIFICACION_TECNICA, FOTO, ANEXO, ESTUDIO }

    private Long id;
    private Long obraId;
    private Long partidaId;
    private Categoria categoria;
    private String nombre;
    private int version;
    private String rutaArchivo;
    private String nombreArchivoOriginal;
    private long tamanoBytes;
    private LocalDate fecha;
    private String descripcion;
    private String usuarioRegistro;
    private String creadoEn;

    public Documento() {
        this.fecha = LocalDate.now();
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

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public String getNombreArchivoOriginal() { return nombreArchivoOriginal; }
    public void setNombreArchivoOriginal(String nombreArchivoOriginal) { this.nombreArchivoOriginal = nombreArchivoOriginal; }

    public long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    /** Version formateada como en un plano real: REV001, REV002... */
    public String getVersionFormateada() {
        return String.format("REV%03d", version);
    }
}
