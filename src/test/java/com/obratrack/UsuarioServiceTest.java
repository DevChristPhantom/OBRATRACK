package com.obratrack;

import com.obratrack.core.Database;
import com.obratrack.model.Usuario;
import com.obratrack.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba alta y autenticacion de usuarios. Crea un usuario de nombre unico y lo
 * elimina al terminar para no contaminar la base real.
 */
class UsuarioServiceTest {

    private final UsuarioService service = new UsuarioService();
    private final String username = "test_" + System.nanoTime();

    @Test
    void crearYAutenticar() throws Exception {
        service.crear(username, "Usuario Prueba", "Clave123".toCharArray(), Usuario.Rol.ALMACENERO);

        Optional<Usuario> ok = service.autenticar(username, "Clave123".toCharArray());
        assertTrue(ok.isPresent(), "debe autenticar con la clave correcta");
        assertEquals("Usuario Prueba", ok.get().getNombre());
        assertEquals(Usuario.Rol.ALMACENERO, ok.get().getRol());

        assertTrue(service.autenticar(username, "claveMala".toCharArray()).isEmpty(),
                "no debe autenticar con clave incorrecta");
        assertTrue(service.autenticar("no_existe_" + username, "x".toCharArray()).isEmpty(),
                "no debe autenticar un usuario inexistente");
    }

    @Test
    void noPermiteUsuarioDuplicado() throws Exception {
        service.crear(username, "Uno", "Clave123".toCharArray(), Usuario.Rol.ADMIN);
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(username, "Dos", "OtraClave".toCharArray(), Usuario.Rol.ADMIN));
    }

    @Test
    void exigeClaveNoVacia() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(username, "SinClave", new char[0], Usuario.Rol.ADMIN));
    }

    @Test
    void seBloqueaTrasCincoIntentosFallidos() throws Exception {
        service.crear(username, "Bloqueo", "Correcta1".toCharArray(), Usuario.Rol.ALMACENERO);
        for (int i = 0; i < 5; i++) {
            assertTrue(service.autenticar(username, "claveMala".toCharArray()).isEmpty());
        }
        assertTrue(service.estaBloqueado(username), "debe bloquearse tras 5 fallos");
        // aun con la clave correcta, permanece bloqueado
        assertTrue(service.autenticar(username, "Correcta1".toCharArray()).isEmpty());
        assertTrue(service.segundosBloqueoRestantes(username) > 0);
    }

    @Test
    void actualizarCambiaRolYEstado() throws Exception {
        service.crear(username, "Ana", "Clave123".toCharArray(), Usuario.Rol.ALMACENERO);
        Usuario u = service.listar().stream()
                .filter(x -> x.getUsername().equals(username)).findFirst().orElseThrow();
        u.setRol(Usuario.Rol.JEFE_OBRA);
        u.setActivo(false);
        service.actualizar(u);

        Usuario r = service.listar().stream()
                .filter(x -> x.getUsername().equals(username)).findFirst().orElseThrow();
        assertEquals(Usuario.Rol.JEFE_OBRA, r.getRol());
        assertFalse(r.isActivo());
    }

    @AfterEach
    void limpiar() throws Exception {
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM usuario WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
}
