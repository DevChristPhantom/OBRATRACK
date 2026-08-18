package com.obratrack;

import com.google.gson.reflect.TypeToken;
import com.obratrack.model.Obra;
import com.obratrack.red.JsonUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica los adaptadores de Gson que el transporte RPC necesita y que Gson no
 * trae por defecto: fechas a texto ISO, {@code char[]} a texto y
 * {@link Optional}. Sin red ni base de datos.
 */
class JsonUtilTest {

    @Test
    void localDateVaYVuelveIgual() {
        LocalDate original = LocalDate.of(2026, 3, 15);
        String json = JsonUtil.GSON.toJson(original, LocalDate.class);
        assertEquals("\"2026-03-15\"", json);
        LocalDate recuperada = JsonUtil.GSON.fromJson(json, LocalDate.class);
        assertEquals(original, recuperada);
    }

    @Test
    void localDateTimeVaYVuelveIgual() {
        LocalDateTime original = LocalDateTime.of(2026, 3, 15, 8, 30, 0);
        String json = JsonUtil.GSON.toJson(original, LocalDateTime.class);
        LocalDateTime recuperada = JsonUtil.GSON.fromJson(json, LocalDateTime.class);
        assertEquals(original, recuperada);
    }

    @Test
    void charArrayViajaComoTextoNoComoArreglo() {
        char[] clave = "Clave123".toCharArray();
        String json = JsonUtil.GSON.toJson(clave, char[].class);
        assertEquals("\"Clave123\"", json, "una contrasena no debe viajar como [\"C\",\"l\",...]");
        char[] recuperada = JsonUtil.GSON.fromJson(json, char[].class);
        assertEquals("Clave123", new String(recuperada));
    }

    @Test
    void optionalPresenteVaYVuelveConValor() {
        Type tipo = new TypeToken<Optional<String>>() { }.getType();
        Optional<String> original = Optional.of("hola");
        String json = JsonUtil.GSON.toJson(original, tipo);
        assertEquals("\"hola\"", json);
        Optional<String> recuperada = JsonUtil.GSON.fromJson(json, tipo);
        assertTrue(recuperada.isPresent());
        assertEquals("hola", recuperada.get());
    }

    @Test
    void optionalVacioViajaComoNull() {
        Type tipo = new TypeToken<Optional<String>>() { }.getType();
        String json = JsonUtil.GSON.toJson(Optional.empty(), tipo);
        assertEquals("null", json);
        Optional<String> recuperada = JsonUtil.GSON.fromJson(json, tipo);
        assertFalse(recuperada.isPresent());
    }

    @Test
    void pojoConEnumAnidadoVaYVuelveIgual() {
        Obra original = new Obra("Obra de prueba", "desc", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        original.setEstado(Obra.Estado.PAUSADA);
        original.setPresupuestoTotal(12345.67);

        String json = JsonUtil.GSON.toJson(original, Obra.class);
        Obra recuperada = JsonUtil.GSON.fromJson(json, Obra.class);

        assertEquals(original.getNombre(), recuperada.getNombre());
        assertEquals(original.getEstado(), recuperada.getEstado());
        assertEquals(original.getFechaInicio(), recuperada.getFechaInicio());
        assertEquals(original.getFechaFinEstimada(), recuperada.getFechaFinEstimada());
        assertEquals(original.getPresupuestoTotal(), recuperada.getPresupuestoTotal(), 0.001);
    }

    @Test
    void nullVaComoJsonNull() {
        Obra recuperada = JsonUtil.GSON.fromJson("null", Obra.class);
        assertNull(recuperada);
    }
}
