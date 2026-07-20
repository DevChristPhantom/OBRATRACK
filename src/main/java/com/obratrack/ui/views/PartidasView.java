package com.obratrack.ui.views;

import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.service.MovimientoService;
import com.obratrack.service.PartidaService;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Tabla de partidas de la obra activa: presupuestado vs ejecutado, con color segun avance. */
public class PartidasView extends JPanel {

    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();
    private final Supplier<Obra> obraActivaProvider;

    private final JLabel tituloObra = new JLabel();
    private final DefaultTableModel tablaModelo;
    private final JTable tabla;

    /** Metadatos por fila (indexados por fila del MODELO) para saber si es separador y su nivel. */
    private final List<Boolean> esSeparadorPorFila = new ArrayList<>();
    private final List<Integer> nivelPorFila = new ArrayList<>();

    private static final String[] COLUMNAS = {
            "Codigo", "Descripcion", "Unidad", "Presupuestado (S/.)", "Ejecutado (S/.)", "Diferencia (S/.)", "% Avance"
    };

    public PartidasView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Partidas");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        header.add(titulo, BorderLayout.WEST);

        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        header.add(tituloObra, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tablaModelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        // Tabla con tooltip por celda para poder LEER la partida completa aunque la columna la recorte.
        tabla = new JTable(tablaModelo) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int viewRow = rowAtPoint(e.getPoint());
                if (viewRow < 0) return null;
                int modelRow = convertRowIndexToModel(viewRow);
                Object codigo = tablaModelo.getValueAt(modelRow, 0);
                Object desc = tablaModelo.getValueAt(modelRow, 1);
                Object unidad = tablaModelo.getValueAt(modelRow, 2);
                Object pres = tablaModelo.getValueAt(modelRow, 3);
                // Tooltip HTML con la partida completa (codigo + descripcion sin recortar + unidad + monto).
                StringBuilder sb = new StringBuilder("<html><b>");
                sb.append(escape(String.valueOf(codigo))).append("</b>&nbsp; ");
                sb.append(escape(String.valueOf(desc).trim()));
                if (unidad != null && !String.valueOf(unidad).isBlank()) {
                    sb.append("<br/>Unidad: ").append(escape(String.valueOf(unidad)));
                }
                if (pres != null && !String.valueOf(pres).isBlank()) {
                    sb.append("&nbsp;&nbsp;|&nbsp;&nbsp;Presupuestado: S/. ").append(escape(String.valueOf(pres)));
                }
                sb.append("</html>");
                return sb.toString();
            }
        };
        tabla.setFont(Theme.FONT_BASE);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setFont(Theme.FONT_BOLD);
        tabla.setDefaultRenderer(Object.class, new FilaColorRenderer());
        tabla.setAutoCreateRowSorter(true);
        // No estirar columnas para llenar el ancho: respetamos los anchos calculados y,
        // si la descripcion es muy larga, aparece scroll horizontal en vez de recortar.
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /**
     * Reparte los anchos de columna dando prioridad a "Descripcion", que se autoajusta
     * al texto mas largo (con un tope) para que las partidas se lean completas.
     */
    private void ajustarAnchosColumnas() {
        if (tabla.getColumnModel().getColumnCount() < COLUMNAS.length) return;

        int anchoDescripcion = calcularAnchoDescripcion();
        int[] anchos = {90, anchoDescripcion, 70, 135, 125, 135, 90};
        for (int c = 0; c < anchos.length; c++) {
            tabla.getColumnModel().getColumn(c).setPreferredWidth(anchos[c]);
        }
    }

    /** Ancho ideal de la columna Descripcion segun su contenido, acotado entre un minimo y un maximo. */
    private int calcularAnchoDescripcion() {
        final int columna = 1;
        final int minimo = 240;
        final int maximo = 640;
        FontMetrics fmBase = tabla.getFontMetrics(Theme.FONT_BASE);
        FontMetrics fmBold = tabla.getFontMetrics(Theme.FONT_BOLD);
        int ancho = minimo;
        for (int r = 0; r < tabla.getRowCount(); r++) {
            Object v = tablaModelo.getValueAt(r, columna);
            if (v == null) continue;
            boolean separador = r < esSeparadorPorFila.size() && esSeparadorPorFila.get(r);
            FontMetrics fm = separador ? fmBold : fmBase;
            int w = fm.stringWidth(v.toString()) + 28; // margen interno
            if (w > ancho) ancho = w;
        }
        return Math.min(ancho, maximo);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        tablaModelo.setRowCount(0);
        esSeparadorPorFila.clear();
        nivelPorFila.clear();

        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa para ver sus partidas");
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre());

        try {
            List<Partida> partidas = partidaService.listarPorObra(obra.getId());
            Map<Long, Double> ejecutadoPorPartida = movimientoService.totalEjecutadoPorPartida(obra.getId());

            for (Partida p : partidas) {
                // Ejecutado: para una partida hoja es su propio ejecutado; para un separador
                // (seccion) es la suma del ejecutado de todas sus partidas hijas.
                double ejecutado = p.isEsPadre()
                        ? ejecutadoDeSeccion(p, partidas, ejecutadoPorPartida)
                        : ejecutadoPorPartida.getOrDefault(p.getId(), 0.0);
                double presupuestado = p.getCostoTotalPresupuestado(); // en separadores = subtotal (roll-up)
                double diferencia = presupuestado - ejecutado;
                double pctAvance = presupuestado > 0 ? (ejecutado / presupuestado) * 100 : 0;

                // Los separadores tambien muestran su subtotal; solo se deja en blanco si no hay monto.
                boolean sinMonto = presupuestado == 0 && ejecutado == 0;

                tablaModelo.addRow(new Object[]{
                        p.getCodigo() != null ? p.getCodigo() : "",
                        sangria(p) + p.getDescripcion(),
                        p.getUnidad() != null ? p.getUnidad() : "",
                        sinMonto ? "" : String.format("%,.2f", presupuestado),
                        sinMonto ? "" : String.format("%,.2f", ejecutado),
                        sinMonto ? "" : String.format("%,.2f", diferencia),
                        (sinMonto || presupuestado == 0) ? "" : String.format("%.1f%%", pctAvance)
                });
                esSeparadorPorFila.add(p.isEsPadre());
                nivelPorFila.add(Math.max(1, p.getNivel()));
            }
            ajustarAnchosColumnas();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las partidas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String sangria(Partida p) {
        return "  ".repeat(Math.max(0, p.getNivel() - 1));
    }

    /**
     * Ejecutado de una seccion (partida agrupadora): suma del ejecutado de todas las
     * partidas hoja cuyo codigo cuelga de la del padre (prefijo "codigoPadre.").
     */
    private double ejecutadoDeSeccion(Partida padre, List<Partida> partidas, Map<Long, Double> ejecutadoPorPartida) {
        String codigoPadre = padre.getCodigo();
        if (codigoPadre == null || codigoPadre.isBlank()) return 0.0;
        String prefijo = codigoPadre.trim() + ".";
        double suma = 0;
        for (Partida hija : partidas) {
            if (hija.isEsPadre()) continue;
            String cod = hija.getCodigo();
            if (cod != null && cod.trim().startsWith(prefijo)) {
                suma += ejecutadoPorPartida.getOrDefault(hija.getId(), 0.0);
            }
        }
        return suma;
    }

    private boolean esSeparador(int modelRow) {
        return modelRow >= 0 && modelRow < esSeparadorPorFila.size() && esSeparadorPorFila.get(modelRow);
    }

    private int nivelDe(int modelRow) {
        return (modelRow >= 0 && modelRow < nivelPorFila.size()) ? nivelPorFila.get(modelRow) : 1;
    }

    /**
     * Colorea las filas. Los SEPARADORES (partidas agrupadoras, sin monto) se muestran
     * con fondo y texto distintos segun su nivel jerarquico, como los titulos de seccion
     * del Excel de presupuesto. Las partidas normales conservan el color por % de avance.
     */
    private class FilaColorRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int modelRow = table.convertRowIndexToModel(row);
            boolean separador = esSeparador(modelRow);
            int nivel = nivelDe(modelRow);

            // --- Fila separadora: destaca como titulo de seccion (estilo Excel) ---
            if (separador) {
                c.setFont(Theme.FONT_BOLD);
                if (isSelected) {
                    c.setBackground(Theme.BORDER);
                } else {
                    c.setBackground(Theme.fondoSeparador(nivel));
                }
                c.setForeground(Theme.textoSeparador(nivel));
                return c;
            }

            // --- Partida normal: mismo estilo de siempre (color por avance en la columna %) ---
            c.setFont(Theme.FONT_BASE);
            String pctTexto = String.valueOf(table.getValueAt(row, 6));
            Color colorTexto = Theme.TEXT_PRIMARY;
            Color fondo = (row % 2 == 0) ? Theme.BG_PRIMARY : Theme.BG_SECONDARY;

            if (!pctTexto.isBlank() && pctTexto.endsWith("%")) {
                try {
                    double pct = Double.parseDouble(pctTexto.replace("%", ""));
                    colorTexto = Theme.colorPorAvance(pct);
                } catch (NumberFormatException ignored) {}
            }

            if (isSelected) {
                c.setBackground(Theme.BORDER);
            } else {
                c.setBackground(fondo);
            }
            c.setForeground(column == 6 ? colorTexto : Theme.TEXT_PRIMARY);
            return c;
        }
    }
}
