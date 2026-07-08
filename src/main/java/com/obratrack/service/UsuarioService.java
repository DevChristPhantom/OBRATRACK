package com.obratrack.service;

import com.obratrack.core.AppLog;
import com.obratrack.core.Database;
import com.obratrack.model.Usuario;
import com.obratrack.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** Alta, autenticacion y listado de usuarios del sistema. */
public class UsuarioService {

    private static final Logger LOG = AppLog.get(UsuarioService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Bloqueo temporal tras varios intentos fallidos (en memoria, por usuario). */
    private static final int MAX_INTENTOS = 5;
    private static final long BLOQUEO_MS = 5 * 60 * 1000L; // 5 minutos
    private static final Map<String, long[]> INTENTOS = new ConcurrentHashMap<>(); // usuario -> [fallos, bloqueadoHasta]

    /** Credenciales del administrador por defecto (cambiar tras el primer ingreso). */
    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASS_INICIAL = "admin123";

    /**
     * Crea un usuario con su contrasena hasheada. Devuelve el usuario con id asignado.
     * Lanza IllegalArgumentException si el username ya existe o falta algun dato.
     */
    public Usuario crear(String username, String nombre, char[] password, Usuario.Rol rol) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("La contrasena es obligatoria");
        }
        if (existe(username)) {
            throw new IllegalArgumentException("El usuario '" + username + "' ya existe");
        }
        Usuario u = new Usuario(username.trim(), nombre, rol != null ? rol : Usuario.Rol.ADMIN);
        u.setCreadoEn(LocalDateTime.now().format(TS));
        String hash = PasswordUtil.hash(password);

        String sql = """
            INSERT INTO usuario (username, nombre, password_hash, rol, activo, creado_en)
            VALUES (?, ?, ?, ?, 1, ?)
        """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getNombre());
            ps.setString(3, hash);
            ps.setString(4, u.getRol().name());
            ps.setString(5, u.getCreadoEn());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) u.setId(rs.getLong(1));
            }
        }
        return u;
    }

    /**
     * Verifica usuario + contrasena. Devuelve el usuario si las credenciales son
     * correctas y esta activo; Optional.empty() en cualquier otro caso.
     */
    public Optional<Usuario> autenticar(String username, char[] password) throws SQLException {
        if (estaBloqueado(username)) {
            LOG.warning("Login bloqueado temporalmente por intentos fallidos: " + username);
            return Optional.empty();
        }
        String sql = "SELECT * FROM usuario WHERE username = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, username != null ? username.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    LOG.warning("Login fallido (usuario inexistente): " + username);
                    registrarFallo(username);
                    return Optional.empty();
                }
                boolean activo = rs.getInt("activo") == 1;
                String hash = rs.getString("password_hash");
                if (!activo) {
                    LOG.warning("Login fallido (usuario inactivo): " + username);
                    return Optional.empty();
                }
                if (!PasswordUtil.verificar(password, hash)) {
                    LOG.warning("Login fallido (contrasena incorrecta): " + username);
                    registrarFallo(username);
                    return Optional.empty();
                }
                limpiarFallos(username);
                Usuario u = mapear(rs);
                LOG.info("Login correcto: " + u.getUsername() + " (" + u.getRol() + ")");
                return Optional.of(u);
            }
        }
    }

    // ---------- bloqueo por intentos fallidos ----------

    public boolean estaBloqueado(String username) {
        long[] e = INTENTOS.get(clave(username));
        return e != null && e[1] > System.currentTimeMillis();
    }

    public long segundosBloqueoRestantes(String username) {
        long[] e = INTENTOS.get(clave(username));
        if (e == null) return 0;
        long restante = e[1] - System.currentTimeMillis();
        return restante > 0 ? (restante / 1000) + 1 : 0;
    }

    private void registrarFallo(String username) {
        long[] e = INTENTOS.computeIfAbsent(clave(username), k -> new long[2]);
        e[0]++;
        if (e[0] >= MAX_INTENTOS) {
            e[1] = System.currentTimeMillis() + BLOQUEO_MS;
            e[0] = 0;
            LOG.warning("Usuario bloqueado por " + (BLOQUEO_MS / 60000) + " min: " + username);
        }
    }

    private void limpiarFallos(String username) {
        INTENTOS.remove(clave(username));
    }

    private String clave(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    public boolean existe(String username) throws SQLException {
        String sql = "SELECT 1 FROM usuario WHERE username = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, username != null ? username.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contar() throws SQLException {
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuario")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public List<Usuario> listar() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuario ORDER BY username";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public void cambiarPassword(long usuarioId, char[] nuevaPassword) throws SQLException {
        if (nuevaPassword == null || nuevaPassword.length == 0) {
            throw new IllegalArgumentException("La contrasena es obligatoria");
        }
        String sql = "UPDATE usuario SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(nuevaPassword));
            ps.setLong(2, usuarioId);
            ps.executeUpdate();
        }
    }

    /** Actualiza nombre, rol y estado (activo) de un usuario. El username no cambia. */
    public void actualizar(Usuario u) throws SQLException {
        if (u.getId() == null) {
            throw new IllegalArgumentException("El usuario a editar no tiene id");
        }
        String sql = "UPDATE usuario SET nombre = ?, rol = ?, activo = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getRol().name());
            ps.setInt(3, u.isActivo() ? 1 : 0);
            ps.setLong(4, u.getId());
            ps.executeUpdate();
        }
    }

    /** Cuenta administradores activos (para no dejar el sistema sin ningun ADMIN). */
    public int contarAdminsActivos() throws SQLException {
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuario WHERE rol = 'ADMIN' AND activo = 1")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Si no hay ningun usuario, crea el administrador por defecto (admin / admin123).
     * Se llama al arrancar la app para que siempre exista una forma de entrar.
     */
    public void sembrarAdminSiVacio() throws SQLException {
        if (contar() == 0) {
            crear(ADMIN_USER, "Administrador", ADMIN_PASS_INICIAL.toCharArray(), Usuario.Rol.ADMIN);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setNombre(rs.getString("nombre"));
        try {
            u.setRol(Usuario.Rol.valueOf(rs.getString("rol")));
        } catch (Exception e) {
            u.setRol(Usuario.Rol.ADMIN);
        }
        u.setActivo(rs.getInt("activo") == 1);
        u.setCreadoEn(rs.getString("creado_en"));
        return u;
    }
}
