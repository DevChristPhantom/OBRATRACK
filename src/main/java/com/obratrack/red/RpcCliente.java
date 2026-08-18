package com.obratrack.red;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.obratrack.core.AppLog;
import com.obratrack.core.RedEstado;
import com.obratrack.model.Documento;
import com.obratrack.model.Usuario;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Lado cliente del transporte RPC: usado por las clases {@code *ServiceRemoto} para
 * llamar a la PC anfitriona por HTTP. Un unico {@link HttpClient} compartido por
 * toda la app.
 */
public final class RpcCliente {

    private static final Logger LOG = AppLog.get(RpcCliente.class);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private RpcCliente() {}

    /**
     * POST /login: autentica contra el host. Si es valido, guarda el token de sesion
     * remota en {@link RedEstado} (necesario para el resto de llamadas RPC) y devuelve
     * el usuario autenticado; en caso contrario devuelve Optional.empty().
     */
    public static Optional<Usuario> login(String urlBase, String username, char[] password) throws IOException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("username", username);
        cuerpo.addProperty("password", new String(password));
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(urlBase + "/login"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.GSON.toJson(cuerpo)))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                return Optional.empty();
            }
            JsonObject respuesta = JsonUtil.GSON.fromJson(resp.body(), JsonObject.class);
            Usuario usuario = JsonUtil.GSON.fromJson(respuesta.get("usuario"), Usuario.class);
            String token = respuesta.get("token").getAsString();
            RedEstado.urlHost(urlBase);
            RedEstado.tokenRemoto(token);
            return Optional.of(usuario);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Se interrumpio la conexion con el host de la obra", e);
        }
    }

    /** POST /logout: invalida el token remoto en el host. Se ignoran los errores (solo es limpieza). */
    public static void logout() {
        String urlBase = RedEstado.urlHost();
        String token = RedEstado.tokenRemoto();
        if (urlBase == null || token == null) return;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(urlBase + "/logout"))
                    .timeout(Duration.ofSeconds(5))
                    .header("X-ObraTrack-Token", token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HTTP.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            LOG.fine("No se pudo cerrar la sesion remota (se ignora): " + e.getMessage());
        } finally {
            RedEstado.tokenRemoto(null);
        }
    }

    /**
     * Invoca {@code servicio.metodo(args...)} en la PC anfitriona y deserializa la
     * respuesta al tipo generico indicado (usar {@code TypeToken} para tipos con
     * genericos, p. ej. {@code List<Obra>}).
     */
    public static <T> T invocar(String servicio, String metodo, Type tipoRetorno, Object... args) throws IOException {
        String urlBase = RedEstado.urlHost();
        String token = RedEstado.tokenRemoto();
        if (urlBase == null || token == null) {
            throw new IOException("No hay sesion activa con la PC anfitriona de esta obra.");
        }
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("servicio", servicio);
        cuerpo.addProperty("metodo", metodo);
        JsonArray argsJson = new JsonArray();
        for (Object arg : args) {
            argsJson.add(arg == null ? JsonNull.INSTANCE : JsonUtil.GSON.toJsonTree(arg, arg.getClass()));
        }
        cuerpo.add("args", argsJson);

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(urlBase + "/rpc"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-ObraTrack-Token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.GSON.toJson(cuerpo)))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 403) {
                throw new IOException("No tienes permiso para esta accion (tu rol es de solo lectura).");
            }
            JsonObject respuesta;
            try {
                respuesta = JsonUtil.GSON.fromJson(resp.body(), JsonObject.class);
            } catch (Exception parseEx) {
                throw new IOException("Respuesta invalida del host (HTTP " + resp.statusCode() + ")");
            }
            boolean ok = respuesta != null && respuesta.has("ok") && respuesta.get("ok").getAsBoolean();
            if (resp.statusCode() != 200 || !ok) {
                String err = (respuesta != null && respuesta.has("error") && !respuesta.get("error").isJsonNull())
                        ? respuesta.get("error").getAsString() : ("HTTP " + resp.statusCode());
                throw new IOException("El servidor de la obra respondio con error: " + err);
            }
            JsonElement data = respuesta.get("data");
            if (data == null || data.isJsonNull()) return null;
            return JsonUtil.GSON.fromJson(data, tipoRetorno);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Se interrumpio la conexion con el host de la obra", e);
        }
    }

    /**
     * POST /archivos/subir: envia el archivo local (por streaming, sin cargarlo entero
     * en memoria) a la PC anfitriona junto con sus metadatos, y devuelve el
     * {@link Documento} guardado. Los metadatos van por query string porque el cuerpo
     * de la peticion son los bytes crudos del archivo.
     */
    public static Documento subirArchivo(Path archivoLocal, Map<String, String> metadatos) throws IOException {
        String urlBase = RedEstado.urlHost();
        String token = RedEstado.tokenRemoto();
        if (urlBase == null || token == null) {
            throw new IOException("No hay sesion activa con la PC anfitriona de esta obra.");
        }
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> e : metadatos.entrySet()) {
            if (query.length() > 0) query.append('&');
            query.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(e.getValue() != null ? e.getValue() : "", StandardCharsets.UTF_8));
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(urlBase + "/archivos/subir?" + query))
                    .timeout(Duration.ofMinutes(5))
                    .header("X-ObraTrack-Token", token)
                    .POST(HttpRequest.BodyPublishers.ofFile(archivoLocal))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 403) {
                throw new IOException("No tienes permiso para esta accion (tu rol es de solo lectura).");
            }
            JsonObject respuesta = JsonUtil.GSON.fromJson(resp.body(), JsonObject.class);
            boolean ok = respuesta != null && respuesta.has("ok") && respuesta.get("ok").getAsBoolean();
            if (resp.statusCode() != 200 || !ok) {
                String err = (respuesta != null && respuesta.has("error") && !respuesta.get("error").isJsonNull())
                        ? respuesta.get("error").getAsString() : ("HTTP " + resp.statusCode());
                throw new IOException("El servidor de la obra respondio con error: " + err);
            }
            return JsonUtil.GSON.fromJson(respuesta.get("data"), Documento.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Se interrumpio la conexion con el host de la obra", e);
        }
    }

    /**
     * POST /rpc-archivo: invoca en el host un metodo que devuelve {@code Path} (un
     * documento guardado o un reporte recien generado) y descarga su contenido
     * directamente al archivo local {@code destino} (por streaming, sin pasar por
     * memoria), devolviendo ese mismo {@code destino}.
     */
    public static Path descargarArchivo(String servicio, String metodo, Path destino, Object... args)
            throws IOException {
        String urlBase = RedEstado.urlHost();
        String token = RedEstado.tokenRemoto();
        if (urlBase == null || token == null) {
            throw new IOException("No hay sesion activa con la PC anfitriona de esta obra.");
        }
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("servicio", servicio);
        cuerpo.addProperty("metodo", metodo);
        JsonArray argsJson = new JsonArray();
        for (Object arg : args) {
            argsJson.add(arg == null ? JsonNull.INSTANCE : JsonUtil.GSON.toJsonTree(arg, arg.getClass()));
        }
        cuerpo.add("args", argsJson);
        try {
            Path carpeta = destino.getParent();
            if (carpeta != null) Files.createDirectories(carpeta);
            HttpRequest req = HttpRequest.newBuilder(URI.create(urlBase + "/rpc-archivo"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-ObraTrack-Token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.GSON.toJson(cuerpo)))
                    .build();
            HttpResponse<Path> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofFile(destino));
            if (resp.statusCode() != 200) {
                String detalle = Files.readString(destino, StandardCharsets.UTF_8);
                Files.deleteIfExists(destino);
                throw new IOException(resp.statusCode() == 403
                        ? "No tienes permiso para esta accion (tu rol es de solo lectura)."
                        : "El servidor de la obra respondio con error: " + extraerError(detalle));
            }
            return destino;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Se interrumpio la conexion con el host de la obra", e);
        }
    }

    private static String extraerError(String cuerpoRespuesta) {
        try {
            JsonObject o = JsonUtil.GSON.fromJson(cuerpoRespuesta, JsonObject.class);
            return (o != null && o.has("error") && !o.get("error").isJsonNull())
                    ? o.get("error").getAsString() : cuerpoRespuesta;
        } catch (Exception e) {
            return cuerpoRespuesta;
        }
    }
}
