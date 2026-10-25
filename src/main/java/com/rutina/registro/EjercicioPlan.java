package com.rutina.registro;

import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;

import java.util.List;

/**
 * Un ejercicio de una sesión del plan del celular.
 *
 * @param bloque "dia-posicion" del bloque; identifica el ejercicio dentro de la sesión
 * @param rm     1RM usado para los kg; null si no tiene
 */
public record EjercicioPlan(String bloque, String nombre, Tipo tipo, Aproximacion modo, Double rm, List<FilaPlan> filas) {
}
