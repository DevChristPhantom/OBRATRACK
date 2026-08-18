package com.obratrack;

import com.obratrack.model.AsientoCuaderno;
import com.obratrack.service.CuadernoCalculo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura del cuaderno de obra (CuadernoCalculo): conteo y
 * promedio de personal sobre una ventana de dias. Sin base de datos.
 */
class CuadernoCalculoTest {

    private AsientoCuaderno asiento(LocalDate fecha, int personal) {
        AsientoCuaderno a = new AsientoCuaderno();
        a.setFecha(fecha);
        a.setPersonalObra(personal);
        a.setTexto("texto de prueba");
        return a;
    }

    @Test
    void conteoUltimosDiasIncluyeSoloElRangoIndicado() {
        LocalDate hoy = LocalDate.of(2026, 3, 15);
        List<AsientoCuaderno> asientos = List.of(
                asiento(hoy, 10),
                asiento(hoy.minusDays(3), 8),
                asiento(hoy.minusDays(6), 12),
                asiento(hoy.minusDays(10), 20) // fuera del rango de 7 dias
        );
        long conteo = CuadernoCalculo.conteoUltimosDias(asientos, hoy, 7);
        assertEquals(3, conteo);
    }

    @Test
    void personalPromedioPromediaSoloLosAsientosEnRango() {
        LocalDate hoy = LocalDate.of(2026, 3, 15);
        List<AsientoCuaderno> asientos = List.of(
                asiento(hoy, 10),
                asiento(hoy.minusDays(2), 20),
                asiento(hoy.minusDays(20), 100) // fuera del rango, no debe afectar el promedio
        );
        double promedio = CuadernoCalculo.personalPromedio(asientos, hoy, 7);
        assertEquals(15, promedio, 0.01);
    }

    @Test
    void personalPromedioEsCeroSinAsientosEnRango() {
        LocalDate hoy = LocalDate.of(2026, 3, 15);
        List<AsientoCuaderno> asientos = List.of(asiento(hoy.minusDays(30), 10));
        assertEquals(0, CuadernoCalculo.personalPromedio(asientos, hoy, 7), 0.01);
    }

    @Test
    void conteoUltimosDiasIgnoraFechasFuturas() {
        LocalDate hoy = LocalDate.of(2026, 3, 15);
        List<AsientoCuaderno> asientos = List.of(asiento(hoy.plusDays(1), 5));
        assertEquals(0, CuadernoCalculo.conteoUltimosDias(asientos, hoy, 7));
    }
}
