package com.rutina.registro;

import java.util.Map;

/**
 * Lo anotado de un ejercicio en el celular.
 *
 * @param bloque   "dia-posicion", el mismo del plan
 * @param esfuerzo valor de {@link EscalaEsfuerzo}; null si no se anotó
 * @param filas    etiqueta de la fila → texto de la casilla; solo las casillas con texto
 * @param nota     texto libre (hasta 500 caracteres); null si no hay
 */
public record EjercicioRegistro(String bloque, Integer esfuerzo, Map<String, String> filas, String nota) {
}
