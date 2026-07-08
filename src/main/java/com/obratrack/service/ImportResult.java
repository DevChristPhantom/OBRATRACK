package com.obratrack.service;

import com.obratrack.model.Partida;

import java.util.ArrayList;
import java.util.List;

/** Resultado detallado de una importacion de Excel, para mostrar al usuario. */
public class ImportResult {

    private final List<Partida> partidasImportadas = new ArrayList<>();
    private final List<String> advertencias = new ArrayList<>();
    private final List<String> errores = new ArrayList<>();
    private int filasOmitidas = 0;
    private double presupuestoTotal = 0;
    private boolean exitoso = false;

    public List<Partida> getPartidasImportadas() { return partidasImportadas; }
    public List<String> getAdvertencias() { return advertencias; }
    public List<String> getErrores() { return errores; }

    public int getFilasOmitidas() { return filasOmitidas; }
    public void incrementarFilasOmitidas() { filasOmitidas++; }

    public double getPresupuestoTotal() { return presupuestoTotal; }
    public void setPresupuestoTotal(double presupuestoTotal) { this.presupuestoTotal = presupuestoTotal; }

    public boolean isExitoso() { return exitoso; }
    public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }

    public void agregarAdvertencia(String msg) { advertencias.add(msg); }
    public void agregarError(String msg) { errores.add(msg); }
}
