package com.obratrack.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Cargador central de iconos SVG (Material Symbols). Los SVG viven en
 * {@code src/main/resources/icons/} y se empaquetan dentro del .jar, por lo que
 * se cargan por classpath (funciona igual en IntelliJ y en el .exe generado).
 *
 * Los SVG originales son de color oscuro (#1f1f1f); como la UI es oscura, se les
 * aplica un filtro de color para pintarlos del tono que necesite cada lugar.
 *
 * Los iconos se cachean por (nombre, tamano, color): parsear/crear el SVG solo
 * ocurre una vez y luego se reutiliza la misma instancia (un FlatSVGIcon es
 * reutilizable entre componentes), lo que agiliza el repintado de la navegacion.
 */
public final class Icons {

    private static final Map<String, FlatSVGIcon> CACHE = new HashMap<>();

    private Icons() {}

    /** Icono del tamano indicado, recoloreado al color dado (cacheado). */
    public static FlatSVGIcon get(String nombre, int tamano, Color color) {
        String clave = nombre + "|" + tamano + "|" + (color != null ? color.getRGB() : "n");
        FlatSVGIcon cacheado = CACHE.get(clave);
        if (cacheado != null) return cacheado;

        FlatSVGIcon icon = new FlatSVGIcon("icons/" + nombre + ".svg", tamano, tamano);
        if (color != null) {
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(origen -> color));
        }
        CACHE.put(clave, icon);
        return icon;
    }

    /** Icono en el color secundario del tema (para elementos en reposo). */
    public static FlatSVGIcon get(String nombre, int tamano) {
        return get(nombre, tamano, Theme.TEXT_SECONDARY);
    }
}
