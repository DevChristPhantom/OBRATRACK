package com.obratrack.red;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Instancia unica de Gson para el transporte RPC (host y cliente), con adaptadores
 * para los tipos que Gson no maneja de forma segura por reflexion: fechas (a texto
 * ISO, igual que ya se guardan en SQLite), contrasenas ({@code char[]}, a texto) y
 * {@link Optional} (a null o al valor envuelto).
 */
public final class JsonUtil {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, type, ctx) ->
                    src != null ? new JsonPrimitive(src.toString()) : JsonNull.INSTANCE)
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, ctx) ->
                    (json == null || json.isJsonNull()) ? null : LocalDate.parse(json.getAsString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                    src != null ? new JsonPrimitive(src.toString()) : JsonNull.INSTANCE)
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                    (json == null || json.isJsonNull()) ? null : LocalDateTime.parse(json.getAsString()))
            .registerTypeAdapter(char[].class, (JsonSerializer<char[]>) (src, type, ctx) ->
                    src != null ? new JsonPrimitive(new String(src)) : JsonNull.INSTANCE)
            .registerTypeAdapter(char[].class, (JsonDeserializer<char[]>) (json, type, ctx) ->
                    (json == null || json.isJsonNull()) ? null : json.getAsString().toCharArray())
            .registerTypeAdapterFactory(new OptionalTypeAdapterFactory())
            .create();

    private JsonUtil() {}

    /** Gson no trae adaptador para java.util.Optional; este lo serializa como el valor envuelto o null. */
    private static final class OptionalTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> tipoToken) {
            if (tipoToken.getRawType() != Optional.class) return null;
            Type tipoContenido = ((ParameterizedType) tipoToken.getType()).getActualTypeArguments()[0];
            TypeAdapter<Object> adaptadorContenido =
                    (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(tipoContenido));
            return (TypeAdapter<T>) new TypeAdapter<Optional<Object>>() {
                @Override
                public void write(JsonWriter out, Optional<Object> valor) throws IOException {
                    if (valor == null || valor.isEmpty()) {
                        out.nullValue();
                    } else {
                        adaptadorContenido.write(out, valor.get());
                    }
                }

                @Override
                public Optional<Object> read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return Optional.empty();
                    }
                    return Optional.ofNullable(adaptadorContenido.read(in));
                }
            };
        }
    }
}
