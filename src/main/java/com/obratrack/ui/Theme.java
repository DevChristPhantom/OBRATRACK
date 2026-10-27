package com.obratrack.ui;

import java.awt.Color;
import java.awt.Font;

/** Paleta y tipografia centralizada, segun ux-guidelines.md de la skill. */
public final class Theme {

    // Tema CLARO (light) — superficies blancas sobre un lienzo gris muy suave.
    public static final Color BG_PRIMARY = new Color(0xf3, 0xf6, 0xfa);   // lienzo/pagina (gris claro)
    public static final Color BG_SECONDARY = new Color(0xff, 0xff, 0xff);  // sidebar, barra superior, tarjetas de grafico
    public static final Color BG_CARD = new Color(0xef, 0xf3, 0xfa);       // tarjetas KPI, tarjeta de usuario, pista de barras
    public static final Color ACCENT = new Color(0x0f, 0x76, 0x6e);
    public static final Color SUCCESS = new Color(0x15, 0x80, 0x3d);       // verde legible sobre blanco
    public static final Color WARNING = new Color(0x92, 0x40, 0x0e);       // ambar legible sobre blanco
    public static final Color DANGER = new Color(0xdc, 0x26, 0x26);        // rojo legible sobre blanco
    public static final Color PRIMARY = new Color(0x1d, 0x4e, 0xd8);       // azul de accion / item de menu activo
    public static final Color PURPLE = new Color(0x0e, 0x74, 0x90);        // acento para tarjetas
    public static final Color TEXT_PRIMARY = new Color(0x1e, 0x29, 0x3b);  // texto principal (slate oscuro)
    public static final Color TEXT_SECONDARY = new Color(0x64, 0x74, 0x8b);// texto secundario (gris slate)
    public static final Color BORDER = new Color(0xe2, 0xe8, 0xf0);        // bordes/divisores suaves
    public static final Color NAV_HOVER = new Color(0xf1, 0xf5, 0xf9);     // hover del menu (gris muy claro)
    public static final Color NAV_ACTIVE = new Color(0xe8, 0xf0, 0xfe);    // item de menu activo (azul muy claro)

    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BASE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_KPI = new Font("Segoe UI", Font.BOLD, 26);

    public static final Color SIDEBAR = new Color(0x12, 0x23, 0x3d);
    public static final Color SIDEBAR_TEXT = new Color(0xcf, 0xda, 0xe9);
    public static final Color SIDEBAR_HOVER = new Color(0x20, 0x35, 0x53);
    public static final Color SIDEBAR_ACTIVE = new Color(0x2b, 0x4a, 0x72);

    /** Defaults compartidos por todas las pantallas Swing, incluidos los dialogos. */
    public static void configurar() {
        javax.swing.UIManager.put("defaultFont", FONT_BASE);
        javax.swing.UIManager.put("Component.arc", 12);
        javax.swing.UIManager.put("Button.arc", 12);
        javax.swing.UIManager.put("TextComponent.arc", 10);
        javax.swing.UIManager.put("Component.focusWidth", 1);
        javax.swing.UIManager.put("Component.focusColor", PRIMARY);
        javax.swing.UIManager.put("Component.minimumHeight", 34);
        javax.swing.UIManager.put("Button.minimumHeight", 36);
        javax.swing.UIManager.put("Button.margin", new java.awt.Insets(8, 14, 8, 14));
        javax.swing.UIManager.put("Table.rowHeight", 32);
        javax.swing.UIManager.put("Table.showHorizontalLines", true);
        javax.swing.UIManager.put("Table.showVerticalLines", false);
        javax.swing.UIManager.put("Table.gridColor", BORDER);
        javax.swing.UIManager.put("Table.selectionBackground", NAV_ACTIVE);
        javax.swing.UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        javax.swing.UIManager.put("TableHeader.background", BG_CARD);
        javax.swing.UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        javax.swing.UIManager.put("TableHeader.font", FONT_BOLD);
        javax.swing.UIManager.put("TableHeader.height", 38);
        javax.swing.UIManager.put("ScrollBar.width", 10);
        javax.swing.UIManager.put("ScrollBar.thumbArc", 10);
        javax.swing.UIManager.put("ScrollBar.trackArc", 10);
    }

    public static void pintarSuperficie(java.awt.Graphics g, javax.swing.JComponent c) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(BG_SECONDARY);
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 16, 16);
        g2.setColor(BORDER);
        g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 16, 16);
        g2.dispose();
    }

    public static javax.swing.JPanel superficie(java.awt.LayoutManager layout, int margen) {
        javax.swing.JPanel panel = new javax.swing.JPanel(layout) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                pintarSuperficie(g, this);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(margen, margen, margen, margen));
        return panel;
    }

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
