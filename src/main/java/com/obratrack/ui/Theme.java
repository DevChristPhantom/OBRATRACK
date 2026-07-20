package com.obratrack.ui;

import java.awt.Color;
import java.awt.Font;

/** Paleta y tipografia centralizada, segun ux-guidelines.md de la skill. */
public final class Theme {

    // Tema CLARO (light) — superficies blancas sobre un lienzo gris muy suave.
    public static final Color BG_PRIMARY = new Color(0xf4, 0xf6, 0xfa);   // lienzo/pagina (gris claro)
    public static final Color BG_SECONDARY = new Color(0xff, 0xff, 0xff);  // sidebar, barra superior, tarjetas de grafico
    public static final Color BG_CARD = new Color(0xef, 0xf3, 0xfa);       // tarjetas KPI, tarjeta de usuario, pista de barras
    public static final Color ACCENT = new Color(0xe9, 0x45, 0x60);
    public static final Color SUCCESS = new Color(0x16, 0xa3, 0x4a);       // verde legible sobre blanco
    public static final Color WARNING = new Color(0xd9, 0x77, 0x06);       // ambar legible sobre blanco
    public static final Color DANGER = new Color(0xdc, 0x26, 0x26);        // rojo legible sobre blanco
    public static final Color PRIMARY = new Color(0x3b, 0x82, 0xf6);       // azul de accion / item de menu activo
    public static final Color PURPLE = new Color(0x8b, 0x5c, 0xf6);        // acento para tarjetas
    public static final Color TEXT_PRIMARY = new Color(0x1e, 0x29, 0x3b);  // texto principal (slate oscuro)
    public static final Color TEXT_SECONDARY = new Color(0x64, 0x74, 0x8b);// texto secundario (gris slate)
    public static final Color BORDER = new Color(0xe2, 0xe8, 0xf0);        // bordes/divisores suaves
    public static final Color NAV_HOVER = new Color(0xf1, 0xf5, 0xf9);     // hover del menu (gris muy claro)
    public static final Color NAV_ACTIVE = new Color(0xe8, 0xf0, 0xfe);    // item de menu activo (azul muy claro)

    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BASE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_KPI = new Font("Segoe UI", Font.BOLD, 26);

    private Theme() {}

    /** Color de estado segun % de presupuesto consumido (verde/amarillo/rojo). */
    public static Color colorPorAvance(double pctConsumido) {
        if (pctConsumido > 100) return DANGER;
        if (pctConsumido >= 80) return WARNING;
        return SUCCESS;
    }

    /**
     * Fondo de una fila separadora (partida agrupadora) segun su nivel jerarquico,
     * para distinguir los titulos de seccion como en el Excel de presupuesto.
     */
    public static Color fondoSeparador(int nivel) {
        switch (nivel) {
            case 1:  return new Color(0xe8, 0xf0, 0xfe); // seccion principal (azul muy claro)
            case 2:  return new Color(0xea, 0xf7, 0xef); // subseccion (verde muy claro)
            default: return new Color(0xf1, 0xf5, 0xf9); // niveles mas profundos (gris claro)
        }
    }

    /** Color del texto de una fila separadora segun su nivel (azul / verde / slate). */
    public static Color textoSeparador(int nivel) {
        switch (nivel) {
            case 1:  return new Color(0x1d, 0x4e, 0xd8); // azul intenso
            case 2:  return new Color(0x15, 0x80, 0x3d); // verde intenso
            default: return new Color(0x47, 0x55, 0x69); // slate
        }
    }
}
