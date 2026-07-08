package com.obratrack;

import com.obratrack.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica el hashing de contrasenas (PBKDF2 + salt). */
class PasswordUtilTest {

    @Test
    void hashSeVerificaConLaMismaClave() {
        String almacenado = PasswordUtil.hash("Secreta123");
        assertTrue(PasswordUtil.verificar("Secreta123", almacenado));
    }

    @Test
    void hashFallaConClaveIncorrecta() {
        String almacenado = PasswordUtil.hash("Secreta123");
        assertFalse(PasswordUtil.verificar("otraClave", almacenado));
    }

    @Test
    void dosHashesDeLaMismaClaveSonDistintos() {
        // El salt aleatorio garantiza que no se repita el hash.
        assertNotEquals(PasswordUtil.hash("misma"), PasswordUtil.hash("misma"));
    }

    @Test
    void formatoAlmacenadoInvalidoDevuelveFalse() {
        assertFalse(PasswordUtil.verificar("x", "no-es-un-hash-valido"));
        assertFalse(PasswordUtil.verificar("x", null));
    }
}
