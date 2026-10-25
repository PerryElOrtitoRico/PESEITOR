package com.rutina.registro;

import java.util.List;

/** Una fila de intensidad de un ejercicio del plan del celular (etiqueta "80-85", "4x12"…) con sus series planificadas. */
public record FilaPlan(String etiqueta, Integer pctMin, Integer pctMax, List<SerieReal> series) {
}
