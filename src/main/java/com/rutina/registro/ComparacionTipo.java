package com.rutina.registro;

import com.rutina.model.Tipo;

/**
 * Planilla (lo planificado) contra Entrenado (lo realizado) de un tipo en una sesión.
 *
 * @param imrPlan IMR planificado; null si ninguna serie tiene intensidad conocida
 * @param imrReal IMR realizado; null si ninguna serie tiene intensidad conocida
 */
public record ComparacionTipo(Tipo tipo, int volumenPlan, int volumenReal, Double imrPlan, Double imrReal) {
}
