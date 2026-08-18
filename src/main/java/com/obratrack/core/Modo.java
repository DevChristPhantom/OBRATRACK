package com.obratrack.core;

/**
 * Modo de red en el que corre esta instancia de ObraTrack.
 *
 * <p>LOCAL es el comportamiento de siempre (una PC, un archivo SQLite). ANFITRIONA
 * ademas expone los servicios por HTTP en la red local para que otras PC de la
 * misma obra se conecten. CLIENTE no toca SQLite en absoluto: todo pasa por RPC
 * hacia la PC anfitriona. Vive en {@code core} (no en {@code red}) porque
 * {@link Database} necesita conocerlo sin depender de la capa de servicios.
 */
public enum Modo {
    LOCAL, ANFITRIONA, CLIENTE
}
