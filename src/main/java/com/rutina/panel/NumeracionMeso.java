package com.rutina.panel;

import com.rutina.model.Mesociclo;

import java.util.Comparator;
import java.util.List;

/** Número de macrociclo (año de inicio) y de mesociclo dentro de ese año. */
public final class NumeracionMeso {

    private NumeracionMeso() {
    }

    /** {macro, meso}: el meso es la posición (desde 1) entre los mesociclos del mismo año, por (inicio, id). */
    public static int[] de(List<Mesociclo> delPerfil, Mesociclo m) {
        int anio = m.inicio().getYear();
        List<Mesociclo> delAnio = delPerfil.stream()
                .filter(x -> x.inicio().getYear() == anio)
                .sorted(Comparator.comparing(Mesociclo::inicio).thenComparingLong(Mesociclo::id))
                .toList();
        int posicion = 1;
        for (Mesociclo x : delAnio) {
            if (x.id() == m.id()) break;
            posicion++;
        }
        return new int[]{anio, posicion};
    }
}
