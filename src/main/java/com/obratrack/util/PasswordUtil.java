package com.obratrack.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Hashing de contrasenas con PBKDF2 (incluido en el JDK, sin dependencias externas).
 *
 * Nunca se guarda la contrasena en claro. Cada contrasena usa un salt aleatorio y
 * muchas iteraciones, de modo que dos usuarios con la misma clave tienen hashes
 * distintos y un ataque por fuerza bruta es costoso.
 *
 * Formato almacenado:  pbkdf2$&lt;iteraciones&gt;$&lt;saltBase64&gt;$&lt;hashBase64&gt;
 */
public final class PasswordUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int TAM_SALT = 16;    // bytes
    private static final int TAM_CLAVE = 256;  // bits
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    /** Genera el hash almacenable a partir de una contrasena en claro. */
    public static String hash(char[] password) {
        byte[] salt = new byte[TAM_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERACIONES);
        return "pbkdf2$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static String hash(String password) {
        return hash(password.toCharArray());
    }

    /** Verifica una contrasena en claro contra el hash almacenado (comparacion en tiempo constante). */
    public static boolean verificar(char[] password, String almacenado) {
        if (almacenado == null) return false;
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) return false;
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = pbkdf2(password, salt, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean verificar(String password, String almacenado) {
        return verificar(password.toCharArray(), almacenado);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iteraciones) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iteraciones, TAM_CLAVE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contrasena", e);
        }
    }
}
