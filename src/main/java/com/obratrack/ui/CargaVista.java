package com.obratrack.ui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Consultas de una pantalla fuera del EDT; solo la solicitud vigente publica resultados. */
public final class CargaVista {
    private final Container vista;
    private final Map<Component, Boolean> estados = new LinkedHashMap<>();
    private SwingWorker<?, ?> actual;
    private int revision;
    private Cursor cursorAnterior;

    public CargaVista(Container vista) { this.vista = vista; }

    public void cancelar() {
        revision++;
        if (actual != null) actual.cancel(false);
        actual = null;
        restaurar();
    }

    public <T> void ejecutar(Callable<T> consulta, Consumer<T> aplicar, Consumer<Throwable> error) {
        cancelar();
        int solicitud = revision;
        cursorAnterior = vista.getCursor();
        recordarEstados(vista);
        estados.keySet().forEach(c -> c.setEnabled(false));
        vista.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override protected T doInBackground() throws Exception { return consulta.call(); }
            @Override protected void done() {
                if (solicitud != revision || isCancelled()) return;
                try {
                    aplicar.accept(get());
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    error.accept(e.getCause());
                } finally {
                    if (solicitud == revision) {
                        actual = null;
                        restaurar();
                    }
                }
            }
        };
        actual = worker;
        worker.execute();
    }

    private void recordarEstados(Container contenedor) {
        for (Component c : contenedor.getComponents()) {
            if (c instanceof AbstractButton || c instanceof JTextComponent || c instanceof JComboBox<?> || c instanceof JTable) {
                estados.put(c, c.isEnabled());
            }
            if (c instanceof Container hijo) recordarEstados(hijo);
        }
    }

    private void restaurar() {
        estados.forEach(Component::setEnabled);
        estados.clear();
        if (cursorAnterior != null) {
            vista.setCursor(cursorAnterior);
            cursorAnterior = null;
        }
    }
}
