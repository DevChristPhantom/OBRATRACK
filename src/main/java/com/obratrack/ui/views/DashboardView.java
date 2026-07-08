package com.obratrack.ui.views;

import com.obratrack.model.Obra;
import com.obratrack.model.Partida;
import com.obratrack.model.ResumenPeriodo;
import com.obratrack.service.Granularidad;
import com.obratrack.service.IndicadorSalud;
import com.obratrack.service.MovimientoService;
import com.obratrack.service.PartidaService;
import com.obratrack.ui.Icons;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Dashboard con KPIs (presupuesto, ejecutado, diferencia, % avance), dos graficos
 * dibujados con Java2D a partir de datos reales (dona de distribucion del presupuesto
 * por grupo y barras presupuesto vs ejecutado) y el panel de alertas por partida.
 * Todo el layout es adaptable al tamano de la ventana.
 */
public class DashboardView extends JPanel {

    private final PartidaService partidaService = new PartidaService();
    private final MovimientoService movimientoService = new MovimientoService();
    private final Supplier<Obra> obraActivaProvider;

    private final JLabel tituloObra = new JLabel();
    private final JPanel kpiContainer = new JPanel(new GridLayout(1, 4, 16, 0));
    private final KpiCard cardPresupuesto = new KpiCard("PRESUPUESTO TOTAL", "money", Theme.PRIMARY);
    private final KpiCard cardEjecutado = new KpiCard("EJECUTADO HASTA HOY", "almacen", Theme.SUCCESS);
    private final KpiCard cardDiferencia = new KpiCard("DIFERENCIA DISPONIBLE", "comparativo", Theme.PURPLE);
    private final KpiCard cardAvance = new KpiCard("% AVANCE FINANCIERO", "partidas", Theme.WARNING);

    private final DonutChart donut = new DonutChart();
    private final BarsChart barras = new BarsChart();
    private final LineChart linea = new LineChart();
    private final JPanel alertasPanel = new JPanel();

    // Banner de salud (semaforo de ritmo de gasto)
    private final JPanel bannerSalud = new JPanel(new BorderLayout(12, 0));
    private final JLabel bannerTitulo = new JLabel();
    private final JLabel bannerDetalle = new JLabel();

    // Carga asincrona: velo + spinner mientras se consultan los datos de la obra
    private volatile boolean cargando = false;
    private int spinnerAngulo = 0;
    private int generacion = 0;
    private final javax.swing.Timer spinnerTimer =
            new javax.swing.Timer(80, e -> { spinnerAngulo = (spinnerAngulo + 30) % 360; repaint(); });
    /** Serializa el acceso a la unica conexion SQLite entre workers en segundo plano. */
    private static final Object DB_LOCK = new Object();

    /** Paleta ciclica para los grupos de los graficos. */
    private static final Color[] PALETA = {
            Theme.PRIMARY, Theme.SUCCESS, Theme.PURPLE, Theme.WARNING, Theme.ACCENT,
            new Color(0x1a, 0xbc, 0x9c), new Color(0xe6, 0x7e, 0x22)
    };

    public DashboardView(Supplier<Obra> obraActivaProvider) {
        this.obraActivaProvider = obraActivaProvider;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Dashboard");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        header.add(titulo, BorderLayout.WEST);
        tituloObra.setFont(Theme.FONT_BASE);
        tituloObra.setForeground(Theme.TEXT_SECONDARY);
        header.add(tituloObra, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        kpiContainer.setOpaque(false);
        kpiContainer.add(cardPresupuesto);
        kpiContainer.add(cardEjecutado);
        kpiContainer.add(cardDiferencia);
        kpiContainer.add(cardAvance);

        // Banner de salud arriba de los KPIs
        configurarBanner();
        JPanel topPanel = new JPanel(new BorderLayout(0, 14));
        topPanel.setOpaque(false);
        topPanel.add(bannerSalud, BorderLayout.NORTH);
        topPanel.add(kpiContainer, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 16));
        centro.setOpaque(false);
        centro.add(topPanel, BorderLayout.NORTH);

        // Graficos: distribucion (dona), ejecucion acumulada (linea) y presupuesto vs ejecutado (barras)
        JPanel graficos = new JPanel(new GridLayout(1, 3, 16, 0));
        graficos.setOpaque(false);
        graficos.add(tarjetaChart("Distribucion del presupuesto", donut));
        graficos.add(tarjetaChart("Ejecucion acumulada vs. ritmo esperado", linea));
        graficos.add(tarjetaChart("Presupuesto vs Ejecutado", barras));
        centro.add(graficos, BorderLayout.CENTER);

        // Alertas debajo de los graficos
        alertasPanel.setLayout(new BoxLayout(alertasPanel, BoxLayout.Y_AXIS));
        alertasPanel.setOpaque(false);
        alertasPanel.setBorder(new EmptyBorder(4, 0, 0, 0));
        JScrollPane scrollAlertas = new JScrollPane(alertasPanel);
        scrollAlertas.setBorder(BorderFactory.createEmptyBorder());
        scrollAlertas.getViewport().setOpaque(false);
        scrollAlertas.setOpaque(false);
        scrollAlertas.setPreferredSize(new Dimension(10, 120));
        centro.add(scrollAlertas, BorderLayout.SOUTH);

        add(centro, BorderLayout.CENTER);
        spinnerTimer.setCoalesce(true);
    }

    // ============================================================
    //  Overlay de carga (velo translucido + spinner Java2D)
    // ============================================================

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!cargando) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        Color bg = Theme.BG_PRIMARY;
        g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 200));
        g2.fillRect(0, 0, w, h);

        int d = 48, cx = w / 2, cy = h / 2;
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.BORDER);
        g2.drawArc(cx - d / 2, cy - d / 2, d, d, 0, 360);
        g2.setColor(Theme.PRIMARY);
        g2.drawArc(cx - d / 2, cy - d / 2, d, d, spinnerAngulo, 100);

        g2.setColor(Theme.TEXT_SECONDARY);
        g2.setFont(Theme.FONT_BASE);
        String msg = "Cargando datos de la obra...";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + d);
        g2.dispose();
    }

    private void iniciarCarga() {
        cargando = true;
        if (!spinnerTimer.isRunning()) spinnerTimer.start();
        repaint();
    }

    private void terminarCarga() {
        cargando = false;
        spinnerTimer.stop();
        repaint();
    }

    private JPanel tarjetaChart(String titulo, JComponent chart) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.BG_SECONDARY);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        card.add(lbl, BorderLayout.NORTH);
        chart.setOpaque(false);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private void configurarBanner() {
        bannerSalud.setBackground(Theme.BG_SECONDARY);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        bannerTitulo.setFont(Theme.FONT_TITLE.deriveFont(16f));
        bannerTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        bannerDetalle.setFont(Theme.FONT_BASE);
        bannerDetalle.setForeground(Theme.TEXT_SECONDARY);
        bannerDetalle.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(bannerTitulo);
        textos.add(Box.createVerticalStrut(2));
        textos.add(bannerDetalle);
        bannerSalud.add(textos, BorderLayout.CENTER);
        aplicarBanner(Theme.TEXT_SECONDARY, "Salud de la obra", "Selecciona una obra activa.");
    }

    private void aplicarBanner(Color color, String titulo, String detalle) {
        bannerSalud.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, color),
                new EmptyBorder(12, 14, 12, 14)));
        bannerTitulo.setForeground(color);
        bannerTitulo.setText(titulo);
        bannerDetalle.setText(detalle);
    }

    private void actualizarBanner(IndicadorSalud.Salud salud) {
        Color color = switch (salud.nivel) {
            case VERDE -> Theme.SUCCESS;
            case AMARILLO -> Theme.WARNING;
            case ROJO -> Theme.DANGER;
        };
        aplicarBanner(color, salud.titulo, salud.detalle);
    }

    public void refrescar() {
        Obra obra = obraActivaProvider.get();

        if (obra == null) {
            generacion++;               // invalida cualquier carga en curso
            terminarCarga();
            mostrarVacio();
            return;
        }

        tituloObra.setText("Obra: " + obra.getNombre());
        final int gen = ++generacion;   // marca esta solicitud como la mas reciente
        iniciarCarga();

        SwingWorker<DatosDashboard, Void> worker = new SwingWorker<>() {
            @Override
            protected DatosDashboard doInBackground() throws Exception {
                // Una sola conexion SQLite compartida: serializamos el trabajo de fondo.
                synchronized (DB_LOCK) {
                    return calcular(obra);
                }
            }

            @Override
            protected void done() {
                if (gen != generacion) return;   // llego una carga mas nueva: descartamos esta
                try {
                    aplicar(get());
                } catch (Exception ex) {
                    Throwable causa = (ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null)
                            ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(DashboardView.this,
                            "No se pudo calcular el dashboard: " + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    terminarCarga();
                }
            }
        };
        worker.execute();
    }

    /** Estado "sin obra": limpia KPIs, graficos y alertas en el hilo de la interfaz. */
    private void mostrarVacio() {
        tituloObra.setText("Selecciona una obra activa");
        cardPresupuesto.setValor("S/. 0.00");
        cardEjecutado.setValor("S/. 0.00");
        cardDiferencia.setValor("S/. 0.00");
        cardDiferencia.setColorValor(Theme.TEXT_PRIMARY);
        cardAvance.setValor("0.0%");
        cardAvance.setColorValor(Theme.TEXT_PRIMARY);
        donut.setDatos(List.of());
        barras.setDatos(List.of());
        linea.setDatos(List.of(), 0);
        aplicarBanner(Theme.TEXT_SECONDARY, "Salud de la obra", "Selecciona una obra activa.");
        alertasPanel.removeAll();
        repintar();
    }

    /** Trabajo pesado (consultas y calculos), ejecutado en segundo plano. */
    private DatosDashboard calcular(Obra obra) throws SQLException {
        DatosDashboard d = new DatosDashboard();
        d.partidas = partidaService.listarPorObra(obra.getId());
        d.ejecutadoPorPartida = movimientoService.totalEjecutadoPorPartida(obra.getId());

        d.presupuestoTotal = d.partidas.stream()
                .filter(p -> !p.isEsPadre())
                .mapToDouble(Partida::getCostoTotalPresupuestado)
                .sum();
        d.ejecutado = movimientoService.totalEjecutadoObra(obra.getId());
        d.diferencia = d.presupuestoTotal - d.ejecutado;
        d.pctAvance = d.presupuestoTotal > 0 ? (d.ejecutado / d.presupuestoTotal) * 100 : 0;

        d.grupos = agruparPorNivel1(d.partidas, d.ejecutadoPorPartida);

        List<ResumenPeriodo> periodos = movimientoService.resumenPorPeriodo(obra.getId(), Granularidad.MENSUAL);
        d.acumulados = new ArrayList<>();
        for (ResumenPeriodo rp : periodos) d.acumulados.add(rp.getAcumulado());

        d.salud = IndicadorSalud.evaluar(obra, d.partidas, d.ejecutadoPorPartida, d.ejecutado);
        return d;
    }

    /** Vuelca los datos ya calculados sobre la interfaz (hilo de eventos). */
    private void aplicar(DatosDashboard d) {
        cardPresupuesto.setValor(String.format("S/. %,.2f", d.presupuestoTotal));
        cardEjecutado.setValor(String.format("S/. %,.2f", d.ejecutado));
        cardDiferencia.setValor(String.format("S/. %,.2f", d.diferencia));
        cardDiferencia.setColorValor(d.diferencia < 0 ? Theme.DANGER : Theme.TEXT_PRIMARY);
        cardAvance.setValor(String.format("%.1f%%", d.pctAvance));
        cardAvance.setColorValor(Theme.colorPorAvance(d.pctAvance));

        donut.setDatos(d.grupos);
        barras.setDatos(d.grupos);
        linea.setDatos(d.acumulados, d.presupuestoTotal);
        actualizarBanner(d.salud);

        alertasPanel.removeAll();
        construirAlertas(d.partidas, d.ejecutadoPorPartida);
        repintar();
    }

    /**
     * Agrupa las partidas ejecutables por su codigo de nivel 1 (ej "01", "02"),
     * usando la descripcion de la partida padre como nombre del grupo. Suma
     * presupuesto y ejecutado por grupo, ordena de mayor a menor y condensa la
     * cola en "Otros" para no saturar los graficos.
     */
    private List<Grupo> agruparPorNivel1(List<Partida> partidas, Map<Long, Double> ejecutadoPorPartida) {
        Map<String, String> nombrePorGrupo = new LinkedHashMap<>();
        for (Partida p : partidas) {
            if (p.getNivel() == 1 && p.getCodigo() != null && !p.getCodigo().isBlank()) {
                nombrePorGrupo.putIfAbsent(p.getCodigo().trim().split("\\.")[0], p.getDescripcion());
            }
        }

        Map<String, double[]> acumulado = new LinkedHashMap<>(); // clave -> [presupuesto, ejecutado]
        for (Partida p : partidas) {
            if (p.isEsPadre() || p.getCostoTotalPresupuestado() <= 0) continue;
            String clave = (p.getCodigo() != null && !p.getCodigo().isBlank())
                    ? p.getCodigo().trim().split("\\.")[0] : "-";
            double ej = ejecutadoPorPartida.getOrDefault(p.getId(), 0.0);
            double[] a = acumulado.computeIfAbsent(clave, k -> new double[2]);
            a[0] += p.getCostoTotalPresupuestado();
            a[1] += ej;
        }

        List<Grupo> grupos = new ArrayList<>();
        for (Map.Entry<String, double[]> e : acumulado.entrySet()) {
            String nombre = nombrePorGrupo.getOrDefault(e.getKey(), "Grupo " + e.getKey());
            grupos.add(new Grupo(nombre, e.getValue()[0], e.getValue()[1]));
        }
        grupos.sort((a, b) -> Double.compare(b.presupuesto, a.presupuesto));

        // Condensa a maximo 6 grupos + "Otros"
        int limite = 6;
        List<Grupo> resultado = new ArrayList<>();
        if (grupos.size() > limite + 1) {
            double otrosP = 0, otrosE = 0;
            for (int i = 0; i < grupos.size(); i++) {
                if (i < limite) {
                    resultado.add(grupos.get(i));
                } else {
                    otrosP += grupos.get(i).presupuesto;
                    otrosE += grupos.get(i).ejecutado;
                }
            }
            resultado.add(new Grupo("Otros", otrosP, otrosE));
        } else {
            resultado.addAll(grupos);
        }

        for (int i = 0; i < resultado.size(); i++) {
            resultado.get(i).color = PALETA[i % PALETA.length];
        }
        return resultado;
    }

    private void construirAlertas(List<Partida> partidas, Map<Long, Double> ejecutadoPorPartida) {
        boolean hayAlertas = false;
        for (Partida p : partidas) {
            if (p.isEsPadre() || p.getCostoTotalPresupuestado() <= 0) continue;
            double ej = ejecutadoPorPartida.getOrDefault(p.getId(), 0.0);
            double pct = (ej / p.getCostoTotalPresupuestado()) * 100;
            if (pct >= 80) {
                hayAlertas = true;
                alertasPanel.add(crearAlerta(p, pct));
                alertasPanel.add(Box.createVerticalStrut(8));
            }
        }
        if (!hayAlertas) {
            JLabel sinAlertas = new JLabel("Sin alertas de presupuesto. Todo va segun lo planificado.");
            sinAlertas.setFont(Theme.FONT_BASE);
            sinAlertas.setForeground(Theme.SUCCESS);
            sinAlertas.setAlignmentX(Component.LEFT_ALIGNMENT);
            alertasPanel.add(sinAlertas);
        }
    }

    private void repintar() {
        alertasPanel.revalidate();
        alertasPanel.repaint();
        donut.repaint();
        barras.repaint();
        linea.repaint();
    }

    private JPanel crearAlerta(Partida p, double pct) {
        JPanel alerta = new JPanel(new BorderLayout(10, 0));
        Color color = Theme.colorPorAvance(pct);
        alerta.setBackground(Theme.BG_SECONDARY);
        alerta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                new EmptyBorder(10, 12, 10, 12)
        ));
        alerta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        alerta.setAlignmentX(Component.LEFT_ALIGNMENT);

        String estado = pct > 100 ? "SUPERADO  " : "POR AGOTARSE  ";
        JLabel texto = new JLabel(estado + p.getCodigo() + " - " + p.getDescripcion());
        texto.setFont(Theme.FONT_BASE);
        texto.setForeground(Theme.TEXT_PRIMARY);
        alerta.add(texto, BorderLayout.CENTER);

        JLabel pctLabel = new JLabel(String.format("%.1f%%", pct));
        pctLabel.setFont(Theme.FONT_BOLD);
        pctLabel.setForeground(color);
        alerta.add(pctLabel, BorderLayout.EAST);

        return alerta;
    }

    // ============================================================
    //  Modelo interno de grupo
    // ============================================================

    private static class Grupo {
        final String nombre;
        final double presupuesto;
        final double ejecutado;
        Color color = Theme.PRIMARY;

        Grupo(String nombre, double presupuesto, double ejecutado) {
            this.nombre = nombre;
            this.presupuesto = presupuesto;
            this.ejecutado = ejecutado;
        }
    }

    /** Resultado del calculo en segundo plano; se aplica luego en el hilo de eventos. */
    private static class DatosDashboard {
        List<Partida> partidas = List.of();
        Map<Long, Double> ejecutadoPorPartida = Map.of();
        double presupuestoTotal, ejecutado, diferencia, pctAvance;
        List<Grupo> grupos = List.of();
        List<Double> acumulados = List.of();
        IndicadorSalud.Salud salud;
    }

    // ============================================================
    //  Tarjeta KPI con icono
    // ============================================================

    private static class KpiCard extends JPanel {
        private final JLabel valorLabel = new JLabel("S/. 0.00");

        KpiCard(String titulo, String iconName, Color accent) {
            setLayout(new BorderLayout(14, 0));
            setBackground(Theme.BG_CARD);
            setBorder(new EmptyBorder(16, 16, 16, 16));

            add(new IconBadge(Icons.get(iconName, 24, accent), accent), BorderLayout.WEST);

            JPanel textos = new JPanel();
            textos.setOpaque(false);
            textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

            JLabel tituloLabel = new JLabel(titulo);
            tituloLabel.setFont(Theme.FONT_SMALL);
            tituloLabel.setForeground(Theme.TEXT_SECONDARY);
            tituloLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            valorLabel.setFont(Theme.FONT_KPI);
            valorLabel.setForeground(Theme.TEXT_PRIMARY);
            valorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            textos.add(tituloLabel);
            textos.add(Box.createVerticalStrut(6));
            textos.add(valorLabel);
            add(textos, BorderLayout.CENTER);
        }

        /** Ajusta el tamano de fuente segun la longitud para que el monto no se corte. */
        void setValor(String texto) {
            valorLabel.setText(texto);
            int len = texto.length();
            float size = len > 15 ? 17f : (len > 12 ? 19f : (len > 9 ? 22f : 25f));
            valorLabel.setFont(Theme.FONT_KPI.deriveFont(Font.BOLD, size));
        }

        void setColorValor(Color c) { valorLabel.setForeground(c); }
    }

    /** Recuadro redondeado con fondo tintado del color de acento y el icono centrado. */
    private static class IconBadge extends JComponent {
        private final Icon icon;
        private final Color accent;

        IconBadge(Icon icon, Color accent) {
            this.icon = icon;
            this.accent = accent;
            setPreferredSize(new Dimension(46, 46));
            setMinimumSize(new Dimension(46, 46));
            setMaximumSize(new Dimension(46, 46));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            int x = (getWidth() - icon.getIconWidth()) / 2;
            int y = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, x, y);
            g2.dispose();
        }
    }

    // ============================================================
    //  Grafico de dona (distribucion del presupuesto por grupo)
    // ============================================================

    private static class DonutChart extends JComponent {
        private List<Grupo> datos = List.of();

        void setDatos(List<Grupo> datos) { this.datos = datos; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            double total = datos.stream().mapToDouble(d -> d.presupuesto).sum();
            if (datos.isEmpty() || total <= 0) {
                dibujarSinDatos(g2, w, h);
                g2.dispose();
                return;
            }

            int size = Math.max(80, Math.min(h - 10, (int) (w * 0.42)));
            int cx = 6;
            int cy = (h - size) / 2;

            double angulo = 90;
            for (Grupo gr : datos) {
                double ext = -360.0 * (gr.presupuesto / total);
                g2.setColor(gr.color);
                g2.fill(new Arc2D.Double(cx, cy, size, size, angulo, ext, Arc2D.PIE));
                angulo += ext;
            }

            // Agujero central (color de la tarjeta)
            int hole = (int) (size * 0.60);
            double hx = cx + (size - hole) / 2.0;
            double hy = cy + (size - hole) / 2.0;
            g2.setColor(Theme.BG_SECONDARY);
            g2.fill(new Ellipse2D.Double(hx, hy, hole, hole));

            // Texto central
            String etiqueta = "Total";
            String monto = String.format("S/. %,.0f", total);
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_SECONDARY);
            centrarTexto(g2, etiqueta, cx + size / 2.0, cy + size / 2.0 - 8);
            g2.setFont(Theme.FONT_BOLD);
            g2.setColor(Theme.TEXT_PRIMARY);
            centrarTexto(g2, monto, cx + size / 2.0, cy + size / 2.0 + 10);

            // Leyenda a la derecha
            int lx = cx + size + 22;
            int ly = cy + 14;
            g2.setFont(Theme.FONT_BASE);
            FontMetrics fm = g2.getFontMetrics();
            int anchoLeyenda = Math.max(60, w - lx - 6);
            for (Grupo gr : datos) {
                double pct = 100.0 * gr.presupuesto / total;
                g2.setColor(gr.color);
                g2.fillRoundRect(lx, ly - 10, 12, 12, 3, 3);
                g2.setColor(Theme.TEXT_PRIMARY);
                String nombre = ajustar(fm, gr.nombre, anchoLeyenda - 130);
                g2.drawString(nombre, lx + 20, ly);
                String cifra = String.format("S/. %,.0f (%.1f%%)", gr.presupuesto, pct);
                g2.setColor(Theme.TEXT_SECONDARY);
                int xCifra = lx + anchoLeyenda - fm.stringWidth(cifra);
                g2.drawString(cifra, Math.max(lx + 20, xCifra), ly);
                ly += 26;
                if (ly > cy + size + 10) break;
            }

            g2.dispose();
        }
    }

    // ============================================================
    //  Grafico de barras (presupuesto vs ejecutado por grupo)
    // ============================================================

    private static class BarsChart extends JComponent {
        private List<Grupo> datos = List.of();

        void setDatos(List<Grupo> datos) { this.datos = datos; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            double maxPresup = datos.stream().mapToDouble(d -> d.presupuesto).max().orElse(0);
            if (datos.isEmpty() || maxPresup <= 0) {
                dibujarSinDatos(g2, w, h);
                g2.dispose();
                return;
            }

            g2.setFont(Theme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            int labelW = 120;
            int cifraW = 92;
            int x0 = labelW + 6;
            int barMax = Math.max(40, w - x0 - cifraW);
            int filas = datos.size();
            int alturaFila = Math.max(26, Math.min(46, (h - 16) / Math.max(1, filas)));
            int y = 8;

            for (Grupo gr : datos) {
                double pct = gr.presupuesto > 0 ? (gr.ejecutado / gr.presupuesto) * 100 : 0;
                int barPresup = (int) Math.round(barMax * (gr.presupuesto / maxPresup));
                int barEjec = (int) Math.round(barPresup * Math.min(1.0, gr.presupuesto > 0 ? gr.ejecutado / gr.presupuesto : 0));
                int alturaBarra = Math.min(16, alturaFila - 12);
                int by = y + (alturaFila - alturaBarra) / 2;

                // Etiqueta del grupo
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.drawString(ajustar(fm, gr.nombre, labelW), 0, by + alturaBarra - 3);

                // Barra de presupuesto (fondo)
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(x0, by, barPresup, alturaBarra, 6, 6);
                // Barra de ejecutado (color por avance)
                g2.setColor(Theme.colorPorAvance(pct));
                g2.fillRoundRect(x0, by, Math.max(2, barEjec), alturaBarra, 6, 6);

                // Cifra a la derecha
                g2.setColor(Theme.TEXT_SECONDARY);
                String cifra = String.format("%.0f%%", pct);
                g2.drawString(cifra, x0 + barMax + 8, by + alturaBarra - 3);

                y += alturaFila;
                if (y > h - 8) break;
            }

            g2.dispose();
        }
    }

    // ============================================================
    //  Grafico de linea (ejecucion acumulada vs. ritmo esperado)
    // ============================================================

    private static class LineChart extends JComponent {
        private List<Double> acumulados = List.of();
        private double presupuesto = 0;

        void setDatos(List<Double> acumulados, double presupuesto) {
            this.acumulados = acumulados;
            this.presupuesto = presupuesto;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            if (acumulados.isEmpty() || presupuesto <= 0) {
                dibujarSinDatos(g2, w, h);
                g2.dispose();
                return;
            }

            int x0 = 12, x1 = w - 12, yBase = h - 26, yTop = 12;
            double maxAcum = acumulados.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double maxY = Math.max(presupuesto, maxAcum);
            if (maxY <= 0) maxY = 1;

            // Ejes
            g2.setColor(Theme.BORDER);
            g2.drawLine(x0, yBase, x1, yBase);
            g2.drawLine(x0, yTop, x0, yBase);

            // Linea guia (ritmo esperado): de 0 al presupuesto a lo largo del periodo
            double yPresup = yBase - (presupuesto / maxY) * (yBase - yTop);
            Stroke original = g2.getStroke();
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{5f, 4f}, 0));
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawLine(x0, yBase, x1, (int) Math.round(yPresup));
            g2.setStroke(original);

            // Curva de ejecucion acumulada (empieza en el origen)
            int n = acumulados.size();
            int[] xs = new int[n + 1];
            int[] ys = new int[n + 1];
            xs[0] = x0;
            ys[0] = yBase;
            for (int i = 0; i < n; i++) {
                double fx = (n == 1) ? 1.0 : (double) (i + 1) / n;
                xs[i + 1] = (int) Math.round(x0 + fx * (x1 - x0));
                ys[i + 1] = (int) Math.round(yBase - (acumulados.get(i) / maxY) * (yBase - yTop));
            }
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.PRIMARY);
            g2.drawPolyline(xs, ys, n + 1);
            g2.setStroke(original);
            g2.fillOval(xs[n] - 4, ys[n] - 4, 8, 8);

            // Leyenda
            g2.setFont(Theme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            int ly = h - 8;
            g2.setColor(Theme.PRIMARY);
            g2.fillRect(x0, ly - 8, 14, 3);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString("Ejecutado", x0 + 18, ly);
            int x2 = x0 + 18 + fm.stringWidth("Ejecutado") + 16;
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.fillRect(x2, ly - 6, 14, 2);
            g2.drawString("Ritmo esperado", x2 + 18, ly);

            g2.dispose();
        }
    }

    // ---------- utilidades de dibujo compartidas ----------

    private static void dibujarSinDatos(Graphics2D g2, int w, int h) {
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.setFont(Theme.FONT_BASE);
        String msg = "Sin datos para mostrar";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    private static void centrarTexto(Graphics2D g2, String texto, double cx, double cy) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(texto, (int) (cx - fm.stringWidth(texto) / 2.0), (int) cy);
    }

    /** Trunca el texto con puntos suspensivos si no cabe en el ancho dado. */
    private static String ajustar(FontMetrics fm, String texto, int maxAncho) {
        if (texto == null) return "";
        if (fm.stringWidth(texto) <= maxAncho) return texto;
        String puntos = "...";
        int i = texto.length();
        while (i > 0 && fm.stringWidth(texto.substring(0, i) + puntos) > maxAncho) {
            i--;
        }
        return i <= 0 ? puntos : texto.substring(0, i) + puntos;
    }
}