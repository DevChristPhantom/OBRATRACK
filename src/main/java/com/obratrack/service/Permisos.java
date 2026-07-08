package com.obratrack.service;

import com.obratrack.model.Usuario;

/**
 * Reglas de permisos por rol, basadas en el usuario que inicio sesion (SesionActual).
 *   - ADMIN: administra todo (usuarios, obras).
 *   - JEFE_OBRA: gestiona obras y partidas, registra almacen.
 *   - ALMACENERO: registra movimientos de almacen y consulta.
 */
public final class Permisos {

    private Permisos() {}

    private static Usuario.Rol rol() {
        Usuario u = SesionActual.getUsuario();
        return u != null ? u.getRol() : null;
    }

    public static boolean esAdmin() {
        return rol() == Usuario.Rol.ADMIN;
    }

    public static boolean puedeGestionarUsuarios() {
        return esAdmin();
    }

    public static boolean puedeGestionarObras() {
        Usuario.Rol r = rol();
        return r == Usuario.Rol.ADMIN || r == Usuario.Rol.JEFE_OBRA;
    }
}
