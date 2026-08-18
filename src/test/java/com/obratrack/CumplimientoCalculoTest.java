package com.obratrack;

import com.obratrack.model.ItemCumplimiento;
import com.obratrack.model.ItemCumplimiento.Categoria;
import com.obratrack.model.ItemCumplimiento.Estado;
import com.obratrack.model.ItemCumplimiento.Impacto;
import com.obratrack.model.ItemCumplimiento.Probabilidad;
import com.obratrack.model.ItemCumplimiento.Severidad;
import com.obratrack.service.CumplimientoCalculo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura de gestion y cumplimiento (CumplimientoCalculo): matriz
 * de riesgo (probabilidad x impacto), vencimiento de plazos y dias abierto. Sin base de datos.
 */
class CumplimientoCalculoTest {

    private ItemCumplimiento item(LocalDate fecha, LocalDate fechaLimite, Estado estado, LocalDate fechaCierre) {
        ItemCumplimiento i = new ItemCumplimiento();
        i.setCategoria(Categoria.RIESGO);
        i.setDescripcion("prueba");
        i.setFecha(fecha);
        i.setFechaLimite(fechaLimite);
        i.setEstado(estado);
        i.setFechaCierre(fechaCierre);
        return i;
    }

    @Test
    void matrizDeRiesgoProbabilidadBajaImpactoBajoEsBaja() {
        assertEquals(Severidad.BAJA, CumplimientoCalculo.calcularSeveridadRiesgo(Probabilidad.BAJA, Impacto.BAJO));
    }

    @Test
    void matrizDeRiesgoProbabilidadAltaImpactoAltoEsCritica() {
        assertEquals(Severidad.CRITICA, CumplimientoCalculo.calcularSeveridadRiesgo(Probabilidad.ALTA, Impacto.ALTO));
    }

    @Test
    void matrizDeRiesgoProbabilidadMediaImpactoMedioEsMedia() {
        assertEquals(Severidad.MEDIA, CumplimientoCalculo.calcularSeveridadRiesgo(Probabilidad.MEDIA, Impacto.MEDIO));
    }

    @Test
    void matrizDeRiesgoProbabilidadBajaImpactoAltoSubeAMedia() {
        // Poco probable pero catastrofico: no se queda en "baja", merece atencion.
        assertEquals(Severidad.MEDIA, CumplimientoCalculo.calcularSeveridadRiesgo(Probabilidad.BAJA, Impacto.ALTO));
    }

    @Test
    void estaVencidoCuandoLaFechaLimitePasoYSigueAbierto() {
        LocalDate hoy = LocalDate.of(2026, 6, 15);
        ItemCumplimiento i = item(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1), Estado.ABIERTO, null);
        assertTrue(CumplimientoCalculo.estaVencido(i, hoy));
    }

    @Test
    void noEstaVencidoSiYaEstaCerrado() {
        LocalDate hoy = LocalDate.of(2026, 6, 15);
        ItemCumplimiento i = item(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1), Estado.CERRADO, LocalDate.of(2026, 6, 5));
        assertFalse(CumplimientoCalculo.estaVencido(i, hoy));
    }

    @Test
    void noEstaVencidoSinFechaLimite() {
        LocalDate hoy = LocalDate.of(2026, 6, 15);
        ItemCumplimiento i = item(LocalDate.of(2026, 5, 1), null, Estado.ABIERTO, null);
        assertFalse(CumplimientoCalculo.estaVencido(i, hoy));
    }

    @Test
    void diasAbiertoUsaHoyMientrasSigaAbierto() {
        LocalDate hoy = LocalDate.of(2026, 6, 15);
        ItemCumplimiento i = item(LocalDate.of(2026, 6, 1), null, Estado.ABIERTO, null);
        assertEquals(14, CumplimientoCalculo.diasAbierto(i, hoy));
    }

    @Test
    void diasAbiertoUsaFechaDeCierreUnaVezCerrado() {
        LocalDate hoy = LocalDate.of(2026, 6, 15);
        ItemCumplimiento i = item(LocalDate.of(2026, 6, 1), null, Estado.CERRADO, LocalDate.of(2026, 6, 6));
        assertEquals(5, CumplimientoCalculo.diasAbierto(i, hoy));
    }

    @Test
    void conteosAbiertosYVencidos() {
        LocalDate hoy = LocalDate.of(2026, 6, 15);
        List<ItemCumplimiento> items = List.of(
                item(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), Estado.ABIERTO, null),   // vencido
                item(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20), Estado.EN_PROCESO, null), // no vencido
                item(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10), Estado.CERRADO, LocalDate.of(2026, 5, 9)));
        assertEquals(2, CumplimientoCalculo.conteoAbiertos(items));
        assertEquals(1, CumplimientoCalculo.conteoVencidos(items, hoy));
    }
}
