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
        tabla = new JTable(tablaModelo);
        tabla.setFont(Theme.FONT_BASE);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setFont(Theme.FONT_BOLD);
        tabla.setDefaultRenderer(Object.class, new FilaColorRenderer());
        tabla.setAutoCreateRowSorter(true);

        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    public void refrescar() {
        Obra obra = obraActivaProvider.get();
        tablaModelo.setRowCount(0);

        if (obra == null) {
            tituloObra.setText("Selecciona una obra activa para ver sus partidas");
            return;
        }
        tituloObra.setText("Obra: " + obra.getNombre());

        try {
            List<Partida> partidas = partidaService.listarPorObra(obra.getId());
            Map<Long, Double> ejecutadoPorPartida = movimientoService.totalEjecutadoPorPartida(obra.getId());

            for (Partida p : partidas) {
                double ejecutado = p.isEsPadre() ? 0 : ejecutadoPorPartida.getOrDefault(p.getId(), 0.0);
                double presupuestado = p.getCostoTotalPresupuestado();
                double diferencia = presupuestado - ejecutado;
                double pctAvance = presupuestado > 0 ? (ejecutado / presupuestado) * 100 : 0;

                tablaModelo.addRow(new Object[]{
                        p.getCodigo() != null ? p.getCodigo() : "",
                        sangria(p) + p.getDescripcion(),
                        p.getUnidad() != null ? p.getUnidad() : "",
                        p.isEsPadre() ? "" : String.format("%,.2f", presupuestado),
                        p.isEsPadre() ? "" : String.format("%,.2f", ejecutado),
                        p.isEsPadre() ? "" : String.format("%,.2f", diferencia),
                        p.isEsPadre() ? "" : String.format("%.1f%%", pctAvance)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las partidas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String sangria(Partida p) {
        return "  ".repeat(Math.max(0, p.getNivel() - 1));
    }

    /** Colorea la fila completa segun el % de avance (columna 6), siguiendo ux-guidelines.md. */
    private class FilaColorRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

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
