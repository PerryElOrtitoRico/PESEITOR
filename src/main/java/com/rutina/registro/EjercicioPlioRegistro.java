package com.rutina.registro;

/**
 * Lo anotado de un ejercicio de pliometría.
 *
 * @param numero posición del ejercicio en el día (desde 1)
 * @param nota   texto libre (hasta 500 caracteres)
 */
public record EjercicioPlioRegistro(int numero, String nota) {
}
