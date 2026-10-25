package com.rutina.registro;

import java.util.List;

/** Una sesión (microciclo, día) del plan del celular; puede no tener ejercicios. */
public record SesionPlan(int microciclo, int dia, List<EjercicioPlan> ejercicios) {
}
