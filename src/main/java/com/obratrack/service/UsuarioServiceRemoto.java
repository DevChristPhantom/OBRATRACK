package com.obratrack.service;

import com.google.gson.reflect.TypeToken;
import com.obratrack.core.RedEstado;
import com.obratrack.model.Usuario;
import com.obratrack.red.RpcCliente;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion remota de {@link IUsuarioService}: en modo cliente, cada metodo
 * llama por RPC a la PC anfitriona en vez de tocar SQLite. La autenticacion usa el
 * endpoint dedicado {@code POST /login} (no el RPC generico), que ademas deja
 * guardado el token de sesion remota en {@link RedEstado} para el resto de llamadas.
 */
public class UsuarioServiceRemoto implements IUsuarioService {

    private static final String SERVICIO = "UsuarioService";

    @Override
    public Usuario crear(String username, String nombre, char[] password, Usuario.Rol rol) throws SQLException {
        return rpc("crear", Usuario.class, username, nombre, password, rol);
    }

    @Override
    public Optional<Usuario> autenticar(String username, char[] password) throws SQLException {
        String urlBase = RedEstado.urlHost();
        if (urlBase == null || urlBase.isBlank()) {
            throw new SQLException("No se configuro la direccion de la PC anfitriona de esta obra");
        }
        try {
            return RpcCliente.login(urlBase, username, password);
        } catch (IOException e) {
            throw new SQLException("No se pudo conectar con la PC anfitriona: " + e.getMessage(), e);
        }
    }

    /**
     * El bloqueo por intentos fallidos se controla y se aplica en el host (que es quien
     * de verdad ejecuta {@code autenticar}); aqui no se replica ese estado, asi que
     * siempre se informa "no bloqueado" del lado cliente.
     */
    @Override
    public boolean estaBloqueado(String username) {
        return false;
    }

    @Override
    public long segundosBloqueoRestantes(String username) {
        return 0;
    }

    @Override
    public boolean existe(String username) throws SQLException {
        return rpc("existe", boolean.class, username);
    }

    @Override
    public int contar() throws SQLException {
        return rpc("contar", int.class);
    }

    @Override
    public List<Usuario> listar() throws SQLException {
        Type tipo = new TypeToken<List<Usuario>>() { }.getType();
        return rpc("listar", tipo);
    }

    @Override
    public void cambiarPassword(long usuarioId, char[] nuevaPassword) throws SQLException {
        rpc("cambiarPassword", void.class, usuarioId, nuevaPassword);
    }

    @Override
    public void requerirCambioPassword(long usuarioId, boolean requerido) throws SQLException {
        rpc("requerirCambioPassword", void.class, usuarioId, requerido);
    }

    @Override
    public void actualizar(Usuario u) throws SQLException {
        rpc("actualizar", void.class, u);
    }

    @Override
    public int contarAdminsActivos() throws SQLException {
        return rpc("contarAdminsActivos", int.class);
    }

    @Override
    public void sembrarAdminSiVacio() throws SQLException {
        rpc("sembrarAdminSiVacio", void.class);
    }

    private <T> T rpc(String metodo, Type tipoRetorno, Object... args) throws SQLException {
        try {
            return RpcCliente.invocar(SERVICIO, metodo, tipoRetorno, args);
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
