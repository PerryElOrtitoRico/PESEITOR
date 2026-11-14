package com.rutina.registro;

import java.util.List;

/** Un día de pliometría del plan del celular: cada ejercicio se hace {@code series} × {@code saltosPorSerie}. */
public record DiaPlioCelular(int dia, int contactos, int series, int saltosPorSerie, List<String> ejercicios) {
    public DiaPlioCelular {
        ejercicios = ejercicios == null ? List.of() : List.copyOf(ejercicios);
    }

    /** Nunca null: Gson en Android arma el objeto sin pasar por el constructor, y un JSON viejo no trae el campo. */
    @Override
    public List<String> ejercicios() {
        return ejercicios == null ? List.of() : ejercicios;
    }
}
