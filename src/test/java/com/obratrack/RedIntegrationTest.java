package com.obratrack;

import com.google.gson.reflect.TypeToken;
import com.obratrack.core.Database;
import com.obratrack.core.RedEstado;
import com.obratrack.core.Rutas;
import com.obratrack.model.Actividad;
import com.obratrack.model.Documento;
import com.obratrack.model.MovimientoAlmacen;
import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.model.Usuario;
import com.obratrack.red.RpcCliente;
import com.obratrack.red.ServidorHttp;
import com.obratrack.service.ObraService;
import com.obratrack.service.UsuarioService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de punta a punta del transporte de red (Paso 1, entregas A y B): levanta
 * un {@link ServidorHttp} real en un puerto efimero y usa {@link RpcCliente} real
 * (HTTP por localhost, sin mocks) para autenticar, crear una obra, guardar
 * partidas, registrar un movimiento, crear una actividad de cronograma, subir y
 * descargar un documento (bytes reales, no solo metadata) y generar y descargar
 * un reporte Excel — exactamente como lo haria una PC cliente real. Tambien
 * verifica que un usuario GERENCIA (solo lectura) recibe 403 al intentar un
 * metodo de escritura.
 *
 * <p>Usa la misma base de datos compartida de desarrollo que el resto de los
 * tests (no una aislada), con usuarios/obras de nombre unico y limpieza en
 * {@code @AfterEach}, siguiendo la convencion ya establecida en
 * {@link ObraServiceTest} y {@link UsuarioServiceTest}.
 */
class RedIntegrationTest {

    private static ServidorHttp servidor;
    private static String urlBase;

    private final ObraService obraService = new ObraService();
    private final UsuarioService usuarioService = new UsuarioService();
    private final long nano = System.nanoTime();
    private final String usuarioEscritura = "test_red_w_" + nano;
    private final String usuarioSoloLectura = "test_red_r_" + nano;
    private static final char[] CLAVE = "Clave123".toCharArray();

    private Long obraId;

    @BeforeAll
    static void iniciarServidor() throws Exception {
        int puerto;
        try (ServerSocket s = new ServerSocket(0)) {
            puerto = s.getLocalPort();
        }
        servidor = new ServidorHttp();
        servidor.iniciar(puerto);
        urlBase = "http://127.0.0.1:" + puerto;
    }

    @AfterAll
    static void detenerServidor() {
        if (servidor != null) servidor.detener();
    }

    @AfterEach
    void limpiar() throws Exception {
        RedEstado.urlHost(null);
        RedEstado.tokenRemoto(null);
        if (obraId != null) {
            obraService.eliminar(obraId);
            obraId = null;
        }
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM usuario WHERE username IN (?, ?)")) {
            ps.setString(1, usuarioEscritura);
            ps.setString(2, usuarioSoloLectura);
            ps.executeUpdate();
        }
    }

    @Test
    void loginObraPartidaYMovimientoDePuntaAPunta() throws Exception {
        usuarioService.crear(usuarioEscritura, "Prueba Red", CLAVE, Usuario.Rol.JEFE_OBRA);

        Optional<Usuario> login = RpcCliente.login(urlBase, usuarioEscritura, CLAVE);
        assertTrue(login.isPresent(), "debe autenticar contra el host real por HTTP");
        assertNotNull(RedEstado.tokenRemoto(), "el login remoto debe dejar un token de sesion");

        Obra nueva = new Obra("OBRA_RED_" + nano, "obra de prueba de red", LocalDate.now(), null);
        Obra creada = RpcCliente.invocar("ObraService", "crear", Obra.class, nueva);
        assertNotNull(creada.getId(), "la obra creada por RPC debe volver con id asignado");
        obraId = creada.getId();

        Type tipoListaPartidas = new TypeToken<List<Partida>>() { }.getType();
        Partida partida = new Partida("01.01", "Concreto f'c=210", "m3", 10, 350);
        RpcCliente.invocar("PartidaService", "guardarTodas", void.class, obraId, List.of(partida));
        List<Partida> partidas = RpcCliente.invocar("PartidaService", "listarPorObra", tipoListaPartidas, obraId);
        assertEquals(1, partidas.size(), "la partida guardada por RPC debe poder listarse por RPC");
        assertEquals("Concreto f'c=210", partidas.get(0).getDescripcion());

        MovimientoAlmacen mov = new MovimientoAlmacen();
        mov.setObraId(obraId);
        mov.setPartidaId(partidas.get(0).getId());
        mov.setFecha(LocalDate.now());
        mov.setTipo(MovimientoAlmacen.Tipo.EGRESO);
        mov.setCantidad(2);
        mov.setCostoUnitarioReal(350);
        MovimientoAlmacen registrado = RpcCliente.invocar("MovimientoService", "registrar",
                MovimientoAlmacen.class, mov);
        assertNotNull(registrado.getId(), "el movimiento registrado por RPC debe volver con id asignado");
        assertEquals(700, registrado.getCostoTotalReal(), 0.001);
    }

    @Test
    void usuarioSoloLecturaRecibe403AlIntentarEscribir() throws Exception {
        usuarioService.crear(usuarioSoloLectura, "Prueba Gerencia", CLAVE, Usuario.Rol.GERENCIA);

        Optional<Usuario> login = RpcCliente.login(urlBase, usuarioSoloLectura, CLAVE);
        assertTrue(login.isPresent());

        Obra nueva = new Obra("OBRA_RED_GERENCIA_" + nano, "no deberia crearse", LocalDate.now(), null);
        IOException fallo = assertThrows(IOException.class,
                () -> RpcCliente.invocar("ObraService", "crear", Obra.class, nueva));
        assertTrue(fallo.getMessage().toLowerCase().contains("permiso"),
                "el error debe indicar falta de permiso, no un error generico: " + fallo.getMessage());

        // de solo lectura si sigue funcionando (no todo el acceso remoto se corta, solo la escritura)
        Type tipoListaObras = new TypeToken<List<Obra>>() { }.getType();
        List<Obra> obras = RpcCliente.invocar("ObraService", "listarActivas", tipoListaObras);
        assertFalse(obras == null, "un metodo de solo lectura debe seguir funcionando para GERENCIA");
    }

    @Test
    void cronogramaDePuntaAPunta() throws Exception {
        usuarioService.crear(usuarioEscritura, "Prueba Red", CLAVE, Usuario.Rol.JEFE_OBRA);
        RpcCliente.login(urlBase, usuarioEscritura, CLAVE);

        Obra obra = new Obra("OBRA_RED_CRONO_" + nano, "", LocalDate.now(), null);
        obraService.crear(obra);
        obraId = obra.getId();

        Actividad actividad = new Actividad();
        actividad.setObraId(obraId);
        actividad.setDescripcion("Excavacion masiva");
        actividad.setFechaInicioProg(LocalDate.now());
        actividad.setFechaFinProg(LocalDate.now().plusDays(5));
        actividad.setPesoPorcentual(20);

        Actividad creada = RpcCliente.invocar("CronogramaService", "crear", Actividad.class, actividad);
        assertNotNull(creada.getId(), "la actividad creada por RPC debe volver con id asignado");

        Type tipoListaActividades = new TypeToken<List<Actividad>>() { }.getType();
        List<Actividad> actividades = RpcCliente.invocar("CronogramaService", "listarPorObra",
                tipoListaActividades, obraId);
        assertEquals(1, actividades.size());
        assertEquals("Excavacion masiva", actividades.get(0).getDescripcion());
    }

    @Test
    void documentoSubirDescargarYEliminarDePuntaAPunta() throws Exception {
        usuarioService.crear(usuarioEscritura, "Prueba Red", CLAVE, Usuario.Rol.JEFE_OBRA);
        RpcCliente.login(urlBase, usuarioEscritura, CLAVE);

        Obra obra = new Obra("OBRA_RED_DOC_" + nano, "", LocalDate.now(), null);
        obraService.crear(obra);
        obraId = obra.getId();

        Path origen = Files.createTempFile("prueba-red-", ".txt");
        String contenido = "contenido de prueba " + nano;
        Files.writeString(origen, contenido);
        Path descargado = null;
        try {
            Map<String, String> meta = new LinkedHashMap<>();
            meta.put("obraId", String.valueOf(obraId));
            meta.put("categoria", "ANEXO");
            meta.put("nombre", "Anexo de prueba");
            meta.put("descripcion", "");
            meta.put("nombreOriginal", origen.getFileName().toString());
            Documento subido = RpcCliente.subirArchivo(origen, meta);
            assertNotNull(subido.getId(), "el documento subido debe volver con id asignado");

            Type tipoListaDocs = new TypeToken<List<Documento>>() { }.getType();
            List<Documento> documentos = RpcCliente.invocar("DocumentoService", "listarPorObra", tipoListaDocs,
                    obraId, Documento.Categoria.ANEXO);
            assertEquals(1, documentos.size(), "el documento subido debe poder listarse por RPC");

            descargado = Rutas.cache().resolve("prueba_red_descarga_" + nano + ".txt");
            RpcCliente.descargarArchivo("DocumentoService", "archivoAbsoluto", descargado, subido);
            assertEquals(contenido, Files.readString(descargado),
                    "el archivo descargado debe ser identico, byte a byte, al que se subio");

            RpcCliente.invocar("DocumentoService", "eliminar", void.class, subido);
            List<Documento> despuesDeBorrar = RpcCliente.invocar("DocumentoService", "listarPorObra", tipoListaDocs,
                    obraId, Documento.Categoria.ANEXO);
            assertTrue(despuesDeBorrar.isEmpty(), "tras eliminar por RPC, el documento no debe seguir listado");
        } finally {
            Files.deleteIfExists(origen);
            if (descargado != null) Files.deleteIfExists(descargado);
        }
    }

    @Test
    void reporteExcelGeneradoYDescargadoDePuntaAPunta() throws Exception {
        usuarioService.crear(usuarioEscritura, "Prueba Red", CLAVE, Usuario.Rol.JEFE_OBRA);
        RpcCliente.login(urlBase, usuarioEscritura, CLAVE);

        Obra obra = new Obra("OBRA_RED_REP_" + nano, "", LocalDate.now(), null);
        obraService.crear(obra);
        obraId = obra.getId();

        Path destino = Rutas.cache().resolve("reporte_prueba_" + nano + ".xlsx");
        try {
            Path descargado = RpcCliente.descargarArchivo("ReporteService", "exportarComparativoExcel",
                    destino, obra);
            byte[] contenido = Files.readAllBytes(descargado);
            assertTrue(contenido.length > 0, "el Excel generado y descargado no debe estar vacio");
            assertTrue(contenido.length > 2 && contenido[0] == 'P' && contenido[1] == 'K',
                    "un .xlsx es un ZIP: el archivo descargado debe empezar con la firma PK");
        } finally {
            Files.deleteIfExists(destino);
        }
    }
}
