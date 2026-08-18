package com.obratrack.red;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.obratrack.core.AppLog;
import com.obratrack.core.Database;
import com.obratrack.model.Documento;
import com.obratrack.model.Usuario;
import com.obratrack.service.AdicionalDeductivoService;
import com.obratrack.service.ApuService;
import com.obratrack.service.CronogramaService;
import com.obratrack.service.CuadernoService;
import com.obratrack.service.CumplimientoService;
import com.obratrack.service.DocumentoService;
import com.obratrack.service.FormulaPolinomicaService;
import com.obratrack.service.IAdicionalDeductivoService;
import com.obratrack.service.IApuService;
import com.obratrack.service.ICronogramaService;
import com.obratrack.service.ICuadernoService;
import com.obratrack.service.ICumplimientoService;
import com.obratrack.service.IDocumentoService;
import com.obratrack.service.IFormulaPolinomicaService;
import com.obratrack.service.IMetradoService;
import com.obratrack.service.IMovimientoService;
import com.obratrack.service.IObraService;
import com.obratrack.service.IPartidaService;
import com.obratrack.service.IReportePdf;
import com.obratrack.service.IReporteService;
import com.obratrack.service.IUsuarioService;
import com.obratrack.service.IValorizacionService;
import com.obratrack.service.MetradoService;
import com.obratrack.service.MovimientoService;
import com.obratrack.service.ObraService;
import com.obratrack.service.PartidaService;
import com.obratrack.service.Permisos;
import com.obratrack.service.ReportePdf;
import com.obratrack.service.ReporteService;
import com.obratrack.service.SesionActual;
import com.obratrack.service.UsuarioService;
import com.obratrack.service.ValorizacionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Servidor HTTP embebido de la PC anfitriona: expone los servicios existentes de
 * ObraTrack (sin cambiarles una linea de SQL) a las demas PC de la obra en la red
 * local.
 *
 * <p>Un unico endpoint generico {@code POST /rpc} despacha por reflexion hacia la
 * clase local real de cada servicio, usando el {@code Type} declarado en la
 * interfaz {@code IXxxService} para (de)serializar con Gson. La identidad del
 * usuario remoto se "presta" a {@link SesionActual} solo durante el tiempo que
 * dura cada request, dentro de {@link Database#LOCK} (que ya serializa toda la
 * app), asi que no hace falta rehacer SesionActual para multiples usuarios.
 *
 * <p>Los metodos que devuelven un archivo en disco ({@code Path}) no pasan por
 * {@code /rpc} (un {@code Path} no tiene sentido fuera de la maquina que lo genero):
 * usan el endpoint paralelo {@code POST /rpc-archivo}, que hace el mismo despacho
 * por reflexion pero transmite el CONTENIDO del archivo como respuesta binaria. La
 * subida de documentos ({@code DocumentoService.subir}) tiene su propio endpoint
 * {@code POST /archivos/subir} porque recibe bytes en vez de JSON.
 */
public final class ServidorHttp {

    private static final Logger LOG = AppLog.get(ServidorHttp.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final Map<String, Registro> registro = new LinkedHashMap<>();
    private final Map<String, Registro> registroArchivos = new LinkedHashMap<>();
    private final Map<String, Usuario> sesiones = new ConcurrentHashMap<>();
    private HttpServer server;

    private record Registro(Object instancia, Class<?> interfaz) { }

    public ServidorHttp() {
        registro.put("UsuarioService", new Registro(new UsuarioService(), IUsuarioService.class));
        registro.put("ObraService", new Registro(new ObraService(), IObraService.class));
        registro.put("PartidaService", new Registro(new PartidaService(), IPartidaService.class));
        registro.put("MovimientoService", new Registro(new MovimientoService(), IMovimientoService.class));
        registro.put("CronogramaService", new Registro(new CronogramaService(), ICronogramaService.class));
        registro.put("CuadernoService", new Registro(new CuadernoService(), ICuadernoService.class));
        registro.put("CumplimientoService", new Registro(new CumplimientoService(), ICumplimientoService.class));
        registro.put("ValorizacionService", new Registro(new ValorizacionService(), IValorizacionService.class));
        registro.put("MetradoService", new Registro(new MetradoService(), IMetradoService.class));
        registro.put("ApuService", new Registro(new ApuService(), IApuService.class));
        registro.put("FormulaPolinomicaService",
                new Registro(new FormulaPolinomicaService(), IFormulaPolinomicaService.class));
        registro.put("DocumentoService", new Registro(new DocumentoService(), IDocumentoService.class));
        registro.put("AdicionalDeductivoService",
                new Registro(new AdicionalDeductivoService(), IAdicionalDeductivoService.class));

        registroArchivos.put("DocumentoService", new Registro(new DocumentoService(), IDocumentoService.class));
        registroArchivos.put("ReporteService", new Registro(new ReporteService(), IReporteService.class));
        registroArchivos.put("ReportePdf", new Registro(new ReportePdf(), IReportePdf.class));
    }

    public synchronized void iniciar(int puerto) throws IOException {
        if (server != null) return;
        server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/login", this::manejarLogin);
        server.createContext("/logout", this::manejarLogout);
        server.createContext("/rpc", this::manejarRpc);
        server.createContext("/rpc-archivo", this::manejarRpcArchivo);
        server.createContext("/archivos/subir", this::manejarSubirArchivo);
        server.start();
        LOG.info("Servidor de red local iniciado en el puerto " + puerto);
    }

    public synchronized void detener() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    // ---------- /login ----------

    private void manejarLogin(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            responder(ex, 405, error("Metodo no permitido"));
            return;
        }
        try {
            JsonObject cuerpo = leerCuerpoJson(ex);
            String username = cuerpo.get("username").getAsString();
            char[] password = cuerpo.get("password").getAsString().toCharArray();
            Optional<Usuario> autenticado;
            synchronized (Database.LOCK) {
                autenticado = new UsuarioService().autenticar(username, password);
            }
            if (autenticado.isEmpty()) {
                responder(ex, 401, error("Usuario o contrasena incorrectos"));
                return;
            }
            String token = generarToken();
            sesiones.put(token, autenticado.get());
            JsonObject resp = new JsonObject();
            resp.add("usuario", JsonUtil.GSON.toJsonTree(autenticado.get()));
            resp.addProperty("token", token);
            responder(ex, 200, JsonUtil.GSON.toJson(resp));
        } catch (SQLException e) {
            responder(ex, 500, error(e.getMessage()));
        } catch (Exception e) {
            responder(ex, 400, error("Solicitud invalida: " + e.getMessage()));
        }
    }

    private void manejarLogout(HttpExchange ex) throws IOException {
        String token = ex.getRequestHeaders().getFirst("X-ObraTrack-Token");
        if (token != null) sesiones.remove(token);
        responder(ex, 200, "{\"ok\":true}");
    }

    // ---------- /rpc ----------

    private void manejarRpc(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            responder(ex, 405, error("Metodo no permitido"));
            return;
        }
        Usuario usuarioRemoto = usuarioDelToken(ex);
        if (usuarioRemoto == null) {
            responder(ex, 401, error("Sesion invalida o expirada; inicia sesion de nuevo"));
            return;
        }

        JsonObject cuerpo;
        String servicioNombre;
        String metodoNombre;
        JsonArray argsJson;
        try {
            cuerpo = leerCuerpoJson(ex);
            servicioNombre = cuerpo.get("servicio").getAsString();
            metodoNombre = cuerpo.get("metodo").getAsString();
            argsJson = cuerpo.has("args") ? cuerpo.getAsJsonArray("args") : new JsonArray();
        } catch (Exception e) {
            responder(ex, 400, error("Solicitud invalida: " + e.getMessage()));
            return;
        }

        Registro reg = registro.get(servicioNombre);
        if (reg == null) {
            responder(ex, 404, error("Servicio desconocido: " + servicioNombre));
            return;
        }
        Method metodo = buscarMetodo(reg.interfaz(), metodoNombre, argsJson.size());
        if (metodo == null) {
            responder(ex, 404, error("Metodo desconocido: " + servicioNombre + "." + metodoNombre));
            return;
        }

        synchronized (Database.LOCK) {
            Usuario anterior = SesionActual.getUsuario();
            try {
                SesionActual.iniciar(usuarioRemoto);
                if (metodo.isAnnotationPresent(Escritura.class) && !Permisos.puedeEscribir()) {
                    responder(ex, 403, error("Tu rol (" + usuarioRemoto.getRol().etiqueta()
                            + ") no tiene permiso de escritura"));
                    return;
                }
                Object[] args = deserializarArgs(metodo, argsJson);
                Object resultado;
                try {
                    resultado = metodo.invoke(reg.instancia(), args);
                } catch (InvocationTargetException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    int codigo = (causa instanceof IllegalArgumentException) ? 400 : 500;
                    responder(ex, codigo, error(causa.getMessage() != null ? causa.getMessage() : causa.toString()));
                    return;
                }
                JsonObject resp = new JsonObject();
                resp.addProperty("ok", true);
                resp.add("data", metodo.getReturnType() == void.class
                        ? com.google.gson.JsonNull.INSTANCE
                        : JsonUtil.GSON.toJsonTree(resultado, metodo.getGenericReturnType()));
                responder(ex, 200, JsonUtil.GSON.toJson(resp));
            } catch (Exception e) {
                responder(ex, 500, error("Error interno del host: " + e.getMessage()));
            } finally {
                SesionActual.iniciar(anterior);
            }
        }
    }

    // ---------- /rpc-archivo (descarga: documentos guardados y reportes generados) ----------

    private void manejarRpcArchivo(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            responder(ex, 405, error("Metodo no permitido"));
            return;
        }
        Usuario usuarioRemoto = usuarioDelToken(ex);
        if (usuarioRemoto == null) {
            responder(ex, 401, error("Sesion invalida o expirada; inicia sesion de nuevo"));
            return;
        }

        JsonObject cuerpo;
        String servicioNombre;
        String metodoNombre;
        JsonArray argsJson;
        try {
            cuerpo = leerCuerpoJson(ex);
            servicioNombre = cuerpo.get("servicio").getAsString();
            metodoNombre = cuerpo.get("metodo").getAsString();
            argsJson = cuerpo.has("args") ? cuerpo.getAsJsonArray("args") : new JsonArray();
        } catch (Exception e) {
            responder(ex, 400, error("Solicitud invalida: " + e.getMessage()));
            return;
        }

        Registro reg = registroArchivos.get(servicioNombre);
        if (reg == null) {
            responder(ex, 404, error("Servicio de archivos desconocido: " + servicioNombre));
            return;
        }
        Method metodo = buscarMetodo(reg.interfaz(), metodoNombre, argsJson.size());
        if (metodo == null) {
            responder(ex, 404, error("Metodo desconocido: " + servicioNombre + "." + metodoNombre));
            return;
        }

        Path archivo;
        synchronized (Database.LOCK) {
            Usuario anterior = SesionActual.getUsuario();
            try {
                SesionActual.iniciar(usuarioRemoto);
                Object[] args = deserializarArgs(metodo, argsJson);
                Object resultado;
                try {
                    resultado = metodo.invoke(reg.instancia(), args);
                } catch (InvocationTargetException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    responder(ex, 500, error(causa.getMessage() != null ? causa.getMessage() : causa.toString()));
                    return;
                }
                archivo = (Path) resultado;
            } catch (Exception e) {
                responder(ex, 500, error("Error interno del host: " + e.getMessage()));
                return;
            } finally {
                SesionActual.iniciar(anterior);
            }
        }

        if (archivo == null || !Files.isRegularFile(archivo)) {
            responder(ex, 404, error("El archivo solicitado no existe en el host"));
            return;
        }
        ex.getResponseHeaders().add("Content-Type", "application/octet-stream");
        ex.sendResponseHeaders(200, Files.size(archivo));
        try (OutputStream out = ex.getResponseBody()) {
            Files.copy(archivo, out);
        }
    }

    // ---------- /archivos/subir (subida: documentos nuevos) ----------

    private void manejarSubirArchivo(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            responder(ex, 405, error("Metodo no permitido"));
            return;
        }
        Usuario usuarioRemoto = usuarioDelToken(ex);
        if (usuarioRemoto == null) {
            responder(ex, 401, error("Sesion invalida o expirada; inicia sesion de nuevo"));
            return;
        }

        Path temporal = null;
        try {
            Map<String, String> params = parseQuery(ex.getRequestURI().getRawQuery());
            String nombreOriginal = params.getOrDefault("nombreOriginal", "archivo");
            String sufijo = "-" + nombreOriginal.replaceAll("[\\\\/:*?\"<>|]", "_");
            Path crudo = Files.createTempFile("obratrack-subida-", sufijo);
            try (InputStream in = ex.getRequestBody()) {
                Files.copy(in, crudo, StandardCopyOption.REPLACE_EXISTING);
            }
            // Se renombra para que conserve el nombre/extension original: DocumentoService.subir()
            // los necesita para calcular la extension del archivo guardado.
            temporal = crudo.resolveSibling(System.nanoTime() + "-" + nombreOriginal);
            Files.move(crudo, temporal, StandardCopyOption.REPLACE_EXISTING);

            Documento meta = new Documento();
            meta.setObraId(Long.parseLong(params.get("obraId")));
            meta.setCategoria(Documento.Categoria.valueOf(params.get("categoria")));
            meta.setNombre(params.getOrDefault("nombre", ""));
            meta.setDescripcion(params.get("descripcion"));
            String partidaIdStr = params.get("partidaId");
            if (partidaIdStr != null && !partidaIdStr.isBlank()) {
                meta.setPartidaId(Long.parseLong(partidaIdStr));
            }

            Documento guardado;
            synchronized (Database.LOCK) {
                Usuario anterior = SesionActual.getUsuario();
                try {
                    SesionActual.iniciar(usuarioRemoto);
                    if (!Permisos.puedeEscribir()) {
                        responder(ex, 403, error("Tu rol (" + usuarioRemoto.getRol().etiqueta()
                                + ") no tiene permiso de escritura"));
                        return;
                    }
                    guardado = new DocumentoService().subir(meta, temporal);
                } finally {
                    SesionActual.iniciar(anterior);
                }
            }
            JsonObject resp = new JsonObject();
            resp.addProperty("ok", true);
            resp.add("data", JsonUtil.GSON.toJsonTree(guardado));
            responder(ex, 200, JsonUtil.GSON.toJson(resp));
        } catch (SQLException | IOException | RuntimeException e) {
            responder(ex, 400, error("No se pudo subir el archivo: " + e.getMessage()));
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ignored) {
                    // el archivo real ya quedo copiado al almacen de documentos; esto es solo el temporal
                }
            }
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return params;
        for (String par : rawQuery.split("&")) {
            int eq = par.indexOf('=');
            if (eq < 0) continue;
            String k = URLDecoder.decode(par.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(par.substring(eq + 1), StandardCharsets.UTF_8);
            params.put(k, v);
        }
        return params;
    }

    // ---------- utilidades comunes ----------

    private Usuario usuarioDelToken(HttpExchange ex) {
        String token = ex.getRequestHeaders().getFirst("X-ObraTrack-Token");
        return token != null ? sesiones.get(token) : null;
    }

    private Object[] deserializarArgs(Method metodo, JsonArray argsJson) {
        Type[] tipos = metodo.getGenericParameterTypes();
        Object[] args = new Object[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            args[i] = JsonUtil.GSON.fromJson(argsJson.get(i), tipos[i]);
        }
        return args;
    }

    /** Busca por nombre y cantidad de parametros (estos servicios no tienen sobrecargas con la misma aridad). */
    private Method buscarMetodo(Class<?> interfaz, String nombre, int cantidadArgs) {
        for (Method m : interfaz.getMethods()) {
            if (m.getName().equals(nombre) && m.getParameterCount() == cantidadArgs) {
                return m;
            }
        }
        return null;
    }

    private JsonObject leerCuerpoJson(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            String texto = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonUtil.GSON.fromJson(texto, JsonObject.class);
        }
    }

    private void responder(HttpExchange ex, int codigo, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private String error(String mensaje) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", false);
        o.addProperty("error", mensaje);
        return JsonUtil.GSON.toJson(o);
    }

    private String generarToken() {
        byte[] buf = new byte[24];
        ALEATORIO.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
