package com.obratrack.model;

/**
 * Usuario del sistema. La contrasena NO se guarda en este objeto: solo vive en la
 * base de datos como hash. Los roles definen permisos futuros (por ahora informativos).
 */
public class Usuario {

    public enum Rol { ADMIN, JEFE_OBRA, ALMACENERO }

    private Long id;
    private String username;
    private String nombre;
    private Rol rol = Rol.ADMIN;
    private boolean activo = true;
    private String creadoEn;

    public Usuario() {}

    public Usuario(String username, String nombre, Rol rol) {
        this.username = username;
        this.nombre = nombre;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    /** Nombre para mostrar: el nombre completo si existe, si no el username. */
    public String getNombreParaMostrar() {
        return (nombre != null && !nombre.isBlank()) ? nombre : username;
    }

    @Override
    public String toString() {
        return getNombreParaMostrar();
    }
}
