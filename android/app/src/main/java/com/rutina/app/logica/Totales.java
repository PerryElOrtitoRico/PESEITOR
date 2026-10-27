package com.rutina.app.logica;

import com.rutina.registro.FilaPlan;
import com.rutina.registro.NotacionRegistro;
import com.rutina.registro.SerieReal;

import java.util.List;
import java.util.Map;

/** Total de repeticiones de un ejercicio (traducción de celular/logica.js). */
public final class Totales {

    private Totales() {
    }

    /** Suma de las reps de una lista de series; lista vacía → 0. */
    public static int totalReps(List<SerieReal> series) {
        int total = 0;
        for (SerieReal s : series) {
            total += s.reps();
        }
        return total;
    }

    /**
     * @param filas   filas del ejercicio en el plan
     * @param textos  etiqueta de la fila → lo que se anotó
     * @return {planificadas, hechas}; hechas = -1 si alguna casilla es inválida
     */
    public static int[] totalesEjercicio(List<FilaPlan> filas, Map<String, String> textos) {
        int planificadas = 0;
        int hechas = 0;
        for (FilaPlan fila : filas) {
            int plan = totalReps(fila.series());
            planificadas += plan;
            if (hechas == -1) continue;
            String texto = textos.get(fila.etiqueta());
            if (texto == null || texto.isBlank()) {
                hechas += plan;
            } else if (!NotacionRegistro.esValida(texto, fila.series())) {
                hechas = -1;
            } else {
                hechas += totalReps(NotacionRegistro.parse(texto, fila.series()));
            }
        }
        return new int[]{planificadas, hechas};
    }
}
