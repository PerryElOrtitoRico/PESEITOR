package com.rutina.app.logica;

/**
 * Estado del cronómetro de una sesión.
 *
 * @param acumuladoMs milisegundos ya contados hasta la última pausa
 * @param desde       hora del sistema (epoch ms) en que arrancó; null si está parado
 */
public record EstadoCrono(long acumuladoMs, Long desde) {
}
