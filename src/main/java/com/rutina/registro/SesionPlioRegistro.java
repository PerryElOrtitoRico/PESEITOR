package com.rutina.registro;

import java.util.List;

/**
 * Una sesión de pliometría anotada en el celular. Se identifica por {@code inicio} (del {@link PlanPlio}), semana y día.
 *
 * @param fecha       AAAA-MM-DD, el día en que se apretó "Iniciar"
 * @param duracionMin minutos (1 a 600); null si no se midió
 */
public record SesionPlioRegistro(String inicio, int semana, int dia, String fecha, Integer duracionMin,
                                 List<EjercicioPlioRegistro> ejercicios) {
    public SesionPlioRegistro {
        ejercicios = ejercicios == null ? List.of() : List.copyOf(ejercicios);
    }

    /** Clave de identidad: dos sesiones con la misma clave son la misma. */
    public String clave() {
        return inicio + "|" + semana + "|" + dia;
    }

    /** Nunca null: Gson en Android arma el objeto sin pasar por el constructor, y un JSON viejo no trae el campo. */
    @Override
    public List<EjercicioPlioRegistro> ejercicios() {
        return ejercicios == null ? List.of() : ejercicios;
    }
}
