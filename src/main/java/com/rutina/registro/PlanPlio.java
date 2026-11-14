package com.rutina.registro;

import java.util.List;

/** Un plan de pliometría (4 semanas) dentro del plan del celular; {@code semanas} es [semana][día]. */
public record PlanPlio(int nivel, String inicio, List<List<DiaPlioCelular>> semanas) {
    public PlanPlio {
        semanas = semanas == null ? List.of() : List.copyOf(semanas);
    }

    /** Nunca null: Gson en Android arma el objeto sin pasar por el constructor, y un JSON viejo no trae el campo. */
    @Override
    public List<List<DiaPlioCelular>> semanas() {
        return semanas == null ? List.of() : semanas;
    }
}
