package com.rutina.registro;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Una sesión anotada en el celular.
 *
 * @param fecha          AAAA-MM-DD
 * @param descanso       valor de {@link EscalaDescanso}; null si no se anotó
 * @param sensacionFinal valor de {@link EscalaFinal}; null si no se anotó (en el JSON la clave es "final")
 */
public record SesionRegistro(int microciclo, int dia, String fecha, Integer descanso,
                             @SerializedName("final") Integer sensacionFinal,
                             List<EjercicioRegistro> ejercicios) {
}
