package com.obratrack.ui;

import java.awt.Color;
import java.awt.Font;

/** Paleta y tipografia centralizada, segun ux-guidelines.md de la skill. */
public final class Theme {

    public static final Color BG_PRIMARY = new Color(0x1a, 0x1a, 0x2e);
    public static final Color BG_SECONDARY = new Color(0x16, 0x21, 0x3e);
    public static final Color BG_CARD = new Color(0x0f, 0x34, 0x60);
    public static final Color ACCENT = new Color(0xe9, 0x45, 0x60);
    public static final Color SUCCESS = new Color(0x2e, 0xcc, 0x71);
    public static final Color WARNING = new Color(0xf3, 0x9c, 0x12);
    public static final Color DANGER = new Color(0xe7, 0x4c, 0x3c);
    public static final Color PRIMARY = new Color(0x3b, 0x82, 0xf6);   // azul de accion / item de menu activo
    public static final Color PURPLE = new Color(0x8b, 0x5c, 0xf6);    // acento para tarjetas
    public static final Color TEXT_PRIMARY = new Color(0xff, 0xff, 0xff);
    public static final Color TEXT_SECONDARY = new Color(0xa0, 0xa0, 0xb0);
    public static final Color BORDER = new Color(0x2a, 0x2a, 0x4a);
    public static final Color NAV_HOVER = new Color(0x1f, 0x2b, 0x50);
    public static final Color NAV_ACTIVE = new Color(0x22, 0x3a, 0x66);

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
}
