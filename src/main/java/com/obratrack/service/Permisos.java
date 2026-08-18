package com.obratrack.service;

import com.obratrack.model.Usuario;

/**
 * Reglas de permisos por rol, basadas en el usuario que inicio sesion (SesionActual).
 *   - ADMIN: administra todo (usuarios, obras).
 *   - JEFE_OBRA: gestiona obras y partidas, registra almacen.
 *   - RESIDENTE, SUPERVISOR, OFICINA_TECNICA: gestionan el dia a dia de la obra
 *     (cronograma, cuaderno de obra, documentos, cumplimiento, almacen) pero no
 *     crean/eliminan obras ni gestionan usuarios.
 *   - ALMACENERO: registra movimientos de almacen y consulta.
 *   - GERENCIA: solo lectura en toda la app (ver {@link #puedeEscribir()}).
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

    /**
     * Permiso general de escritura (crear/editar/eliminar) para los modulos de
     * obra: cronograma, cuaderno de obra, documentos, cumplimiento, valorizaciones,
     * metrados y APU. Unico rol excluido: GERENCIA (solo lectura por diseño).
     */
    public static boolean puedeEscribir() {
        Usuario.Rol r = rol();
        return r != null && r != Usuario.Rol.GERENCIA;
    }
}
