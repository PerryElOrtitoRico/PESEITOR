package com.rutina.registro;

import java.util.List;

/**
 * Resumen de una sesión anotada: escalas y comparación por tipo.
 *
 * @param esfuerzoPromedio promedio de los esfuerzos anotados; null si no hay ninguno
 */
public record ResumenSesion(int microciclo, int dia, String fecha, Integer descanso, Integer sensacionFinal,
                            Double esfuerzoPromedio, List<ComparacionTipo> porTipo) {
}
