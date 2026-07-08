package com.obratrack.service;

/**
 * Nivel de agrupacion temporal para el comparativo de ejecucion de obra.
 * Permite ver el avance dia a dia, semana a semana o mes a mes,
 * cubriendo toda la duracion de la obra.
 */
public enum Granularidad {

    DIARIO("Diario"),
    SEMANAL("Semanal"),
    MENSUAL("Mensual");

    private final String etiqueta;

    Granularidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
