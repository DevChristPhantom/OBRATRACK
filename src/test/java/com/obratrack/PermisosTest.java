package com.obratrack;

import com.obratrack.model.Usuario;
import com.obratrack.service.Permisos;
import com.obratrack.service.SesionActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica la matriz de permisos por rol (no toca la base de datos). */
class PermisosTest {

    private void sesionCon(Usuario.Rol rol) {
        SesionActual.iniciar(new Usuario("u", "U", rol));
    }

    @Test
    void adminPuedeTodo() {
        sesionCon(Usuario.Rol.ADMIN);
        assertTrue(Permisos.esAdmin());
        assertTrue(Permisos.puedeGestionarUsuarios());
        assertTrue(Permisos.puedeGestionarObras());
    }

    @Test
    void jefeObraGestionaObrasPeroNoUsuarios() {
        sesionCon(Usuario.Rol.JEFE_OBRA);
        assertFalse(Permisos.esAdmin());
        assertFalse(Permisos.puedeGestionarUsuarios());
        assertTrue(Permisos.puedeGestionarObras());
    }

    @Test
    void almaceneroSoloConsultaYRegistra() {
        sesionCon(Usuario.Rol.ALMACENERO);
        assertFalse(Permisos.esAdmin());
        assertFalse(Permisos.puedeGestionarUsuarios());
        assertFalse(Permisos.puedeGestionarObras());
    }

    @Test
    void sinSesionNoTienePermisos() {
        SesionActual.cerrar();
        assertFalse(Permisos.esAdmin());
        assertFalse(Permisos.puedeGestionarUsuarios());
        assertFalse(Permisos.puedeGestionarObras());
    }

    @AfterEach
    void limpiar() {
        SesionActual.cerrar();
    }
}
