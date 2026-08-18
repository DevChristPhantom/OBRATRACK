package com.obratrack;

import com.obratrack.model.AdicionalDeductivo;
import com.obratrack.model.AdicionalDeductivo.Tipo;
import com.obratrack.service.PresupuestoAnaliticoCalculo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura del presupuesto analitico (PresupuestoAnaliticoCalculo):
 * gastos generales, utilidad, IGV sobre el costo directo, y el efecto de los
 * adicionales/deductivos sobre el presupuesto actualizado. Sin base de datos.
 */
class PresupuestoAnaliticoCalculoTest {

    private AdicionalDeductivo item(Tipo tipo, double monto) {
        AdicionalDeductivo ad = new AdicionalDeductivo();
        ad.setTipo(tipo);
        ad.setMonto(monto);
        ad.setDescripcion("prueba");
        return ad;
    }

    @Test
    void calculaGastosGeneralesYUtilidadSobreElCostoDirecto() {
        PresupuestoAnaliticoCalculo.Resultado r = PresupuestoAnaliticoCalculo.calcular(
                100_000, 10, 5, 18, List.of());

        assertEquals(100_000, r.costoDirecto, 0.01);
        assertEquals(10_000, r.montoGastosGenerales, 0.01);
        assertEquals(5_000, r.montoUtilidad, 0.01);
        assertEquals(115_000, r.subtotal, 0.01); // 100.000 + 10.000 + 5.000
    }

    @Test
    void calculaElIgvSobreElSubtotalNoSobreElCostoDirecto() {
        PresupuestoAnaliticoCalculo.Resultado r = PresupuestoAnaliticoCalculo.calcular(
                100_000, 10, 5, 18, List.of());

        // subtotal = 115.000; IGV 18% = 20.700
        assertEquals(20_700, r.montoIgv, 0.01);
        assertEquals(135_700, r.presupuestoContractual, 0.01);
    }

    @Test
    void sinGastosGeneralesNiUtilidadElContractualEsSoloCostoDirectoMasIgv() {
        PresupuestoAnaliticoCalculo.Resultado r = PresupuestoAnaliticoCalculo.calcular(
                100_000, 0, 0, 18, List.of());

        assertEquals(100_000, r.subtotal, 0.01);
        assertEquals(18_000, r.montoIgv, 0.01);
        assertEquals(118_000, r.presupuestoContractual, 0.01);
    }

    @Test
    void sumaAdicionalesYRestaDeductivosDelPresupuestoActualizado() {
        List<AdicionalDeductivo> items = List.of(
                item(Tipo.ADICIONAL, 5_000),
                item(Tipo.ADICIONAL, 3_000),
                item(Tipo.DEDUCTIVO, 2_000));

        PresupuestoAnaliticoCalculo.Resultado r = PresupuestoAnaliticoCalculo.calcular(
                100_000, 0, 0, 0, items);

        assertEquals(8_000, r.totalAdicionales, 0.01);
        assertEquals(2_000, r.totalDeductivos, 0.01);
        // contractual = 100.000 (sin GG/utilidad/IGV); actualizado = 100.000 + 8.000 - 2.000
        assertEquals(100_000, r.presupuestoContractual, 0.01);
        assertEquals(106_000, r.presupuestoActualizado, 0.01);
    }

    @Test
    void sinAdicionalesNiDeductivosElActualizadoEsIgualAlContractual() {
        PresupuestoAnaliticoCalculo.Resultado r = PresupuestoAnaliticoCalculo.calcular(
                50_000, 8, 10, 18, List.of());

        assertEquals(r.presupuestoContractual, r.presupuestoActualizado, 0.01);
    }
}
