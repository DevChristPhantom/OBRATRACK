package com.obratrack;

import com.obratrack.model.Documento;
import com.obratrack.service.DocumentoCalculo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la logica pura de documentos (DocumentoCalculo): siguiente numero de
 * version de un plano y formato legible de tamano de archivo. Sin base de datos.
 */
class DocumentoCalculoTest {

    private Documento version(int v) {
        Documento d = new Documento();
        d.setVersion(v);
        return d;
    }

    @Test
    void siguienteVersionEsUnoSiNoHayHistorial() {
        assertEquals(1, DocumentoCalculo.siguienteVersion(List.of()));
    }

    @Test
    void siguienteVersionContinuaLaMasAltaDelHistorial() {
        List<Documento> historial = List.of(version(1), version(2), version(3));
        assertEquals(4, DocumentoCalculo.siguienteVersion(historial));
    }

    @Test
    void siguienteVersionNoDependeDelOrdenDeLaLista() {
        List<Documento> historial = List.of(version(3), version(1), version(2));
        assertEquals(4, DocumentoCalculo.siguienteVersion(historial));
    }

    @Test
    void formatoTamanoUsaBytesParaArchivosPequenos() {
        assertEquals("500 B", DocumentoCalculo.formatoTamano(500));
    }

    @Test
    void formatoTamanoUsaKilobytes() {
        assertEquals("2.0 KB", DocumentoCalculo.formatoTamano(2048));
    }

    @Test
    void formatoTamanoUsaMegabytes() {
        assertEquals("5.0 MB", DocumentoCalculo.formatoTamano(5L * 1024 * 1024));
    }

    @Test
    void formatoTamanoUsaGigabytesParaArchivosMuyGrandes() {
        assertEquals("1.50 GB", DocumentoCalculo.formatoTamano((long) (1.5 * 1024 * 1024 * 1024)));
    }
}
