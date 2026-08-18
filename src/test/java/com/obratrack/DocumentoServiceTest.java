package com.obratrack;

import com.obratrack.model.Documento;
import com.obratrack.model.Obra;
import com.obratrack.service.DocumentoService;
import com.obratrack.service.ObraService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de punta a punta la categoria ESTUDIO (estudios de ingenieria: geotecnico,
 * hidrologico, de impacto ambiental...): sube un archivo real a la carpeta de
 * documentos de la obra y verifica que la fila y el archivo fisico queden
 * correctamente asociados, igual que el resto de categorias de Documento.
 */
class DocumentoServiceTest {

    private final ObraService obraService = new ObraService();
    private final DocumentoService documentoService = new DocumentoService();
    private final long nano = System.nanoTime();
    private Long obraId;

    @Test
    void subeYRecuperaUnEstudioDeIngenieriaConSuArchivoReal() throws Exception {
        Obra o = new Obra("OBRA_ESTUDIO_" + nano, "", LocalDate.now(), null);
        o.setEstado(Obra.Estado.ACTIVA);
        obraService.crear(o);
        obraId = o.getId();

        Path origen = Files.createTempFile("estudio-geotecnico-", ".pdf");
        byte[] contenido = "contenido de prueba del estudio geotecnico".getBytes();
        Files.write(origen, contenido);

        Documento meta = new Documento();
        meta.setObraId(obraId);
        meta.setCategoria(Documento.Categoria.ESTUDIO);
        meta.setNombre("Estudio geotecnico");
        meta.setDescripcion("Sondeo de suelos, sector A");

        Documento guardado = documentoService.subir(meta, origen);
        Files.deleteIfExists(origen);

        assertNotNull(guardado.getId());
        assertEquals(Documento.Categoria.ESTUDIO, guardado.getCategoria());
        assertEquals(contenido.length, guardado.getTamanoBytes());

        Path archivo = documentoService.archivoAbsoluto(guardado);
        assertTrue(Files.isRegularFile(archivo), "el archivo del estudio debe existir en el almacen de documentos");
        assertArrayEquals(contenido, Files.readAllBytes(archivo), "el contenido debe conservarse byte a byte");
        assertTrue(archivo.toString().contains("ESTUDIO"), "debe guardarse en la subcarpeta de su categoria");

        List<Documento> listado = documentoService.listarPorObra(obraId, Documento.Categoria.ESTUDIO);
        assertEquals(1, listado.size());
        assertEquals("Estudio geotecnico", listado.get(0).getNombre());

        documentoService.eliminar(guardado);
        assertFalse(Files.exists(archivo), "eliminar debe borrar tambien el archivo fisico");
        assertTrue(documentoService.listarPorObra(obraId, Documento.Categoria.ESTUDIO).isEmpty());
    }

    @AfterEach
    void limpiar() throws Exception {
        if (obraId != null) {
            obraService.eliminar(obraId);
        }
    }
}
