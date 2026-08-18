package com.obratrack.service;

import com.obratrack.model.Usuario;
import com.obratrack.red.Escritura;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de {@link UsuarioService}: las mismas firmas que la implementacion local,
 * para poder sustituirla por {@link UsuarioServiceRemoto} (RPC) cuando esta PC es
 * cliente en la red de la obra, sin que la UI que la usa cambie una linea.
 */
public interface IUsuarioService {

    @Escritura
    Usuario crear(String username, String nombre, char[] password, Usuario.Rol rol) throws SQLException;

    Optional<Usuario> autenticar(String username, char[] password) throws SQLException;

    boolean estaBloqueado(String username);

    long segundosBloqueoRestantes(String username);

    boolean existe(String username) throws SQLException;

    int contar() throws SQLException;

    List<Usuario> listar() throws SQLException;

    @Escritura
    void cambiarPassword(long usuarioId, char[] nuevaPassword) throws SQLException;

    @Escritura
    void requerirCambioPassword(long usuarioId, boolean requerido) throws SQLException;

    @Escritura
    void actualizar(Usuario u) throws SQLException;

    int contarAdminsActivos() throws SQLException;

    @Escritura
    void sembrarAdminSiVacio() throws SQLException;
}
