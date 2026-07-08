package com.obratrack.service;

import com.obratrack.model.Usuario;

/**
 * Guarda el usuario que inicio sesion, para que el resto de la app (por ejemplo
 * el historial de auditoria) sepa QUIEN realiza cada accion.
 */
public final class SesionActual {

    private static Usuario usuario;

    private SesionActual() {}

    public static void iniciar(Usuario u) { usuario = u; }

    public static void cerrar() { usuario = null; }

    public static Usuario getUsuario() { return usuario; }

    /** Nombre del usuario activo para registrar en auditoria; "Sistema" si no hay sesion. */
    public static String nombre() {
        return usuario != null ? usuario.getNombreParaMostrar() : "Sistema";
    }
}
