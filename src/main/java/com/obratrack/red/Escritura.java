package com.obratrack.red;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca, en la interfaz de un servicio, los metodos que mutan datos (crear/actualizar/
 * eliminar/registrar/...). El despachador RPC del host exige
 * {@link com.obratrack.service.Permisos#puedeEscribir()} antes de invocar un metodo
 * anotado, cerrando en el borde de la red el mismo permiso que hoy solo deshabilita
 * botones en la UI de escritorio (ver {@code ServidorHttp}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Escritura {
}
