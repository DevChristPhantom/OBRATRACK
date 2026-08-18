package com.obratrack.model;

import java.time.LocalDate;

/**
 * Un asiento del cuaderno de obra digital: la anotacion diaria que registra el
 * residente, el supervisor u otro actor en obra (incidencias, clima, personal
 * presente). Es un registro legal de la obra: una vez creado no se edita ni se
 * borra (append-only), igual que la auditoria de movimientos de almacen.
 */
public class AsientoCuaderno {

    public enum Tipo { RESIDENTE, SUPERVISOR, INSPECTOR, OTRO }

    public enum Clima { SOLEADO, NUBLADO, LLUVIOSO, OTRO }

    private Long id;
    private Long obraId;
    private int numero;
    private LocalDate fecha;
    private Tipo tipo;
    private Clima clima;
    private int personalObra;
    private String texto;
    private String usuarioRegistro;
    private String creadoEn;

    public AsientoCuaderno() {
        this.fecha = LocalDate.now();
        this.tipo = Tipo.RESIDENTE;
        this.clima = Clima.SOLEADO;
    }

    // --- Getters y setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public Clima getClima() { return clima; }
    public void setClima(Clima clima) { this.clima = clima; }

    public int getPersonalObra() { return personalObra; }
    public void setPersonalObra(int personalObra) { this.personalObra = personalObra; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    @Override
    public String toString() {
        return "Asiento N°" + numero + " - " + fecha;
    }
}
