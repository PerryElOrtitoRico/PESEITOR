package com.rutina.panel;

import java.text.Normalizer;
import java.util.Locale;

/** Clave de un ejercicio para la métrica del panel: {@code Press banca} → {@code press-banca}. */
public final class ClaveEjercicio {

    private ClaveEjercicio() {
    }

    /** Minúsculas, sin tildes, todo lo que no sea letra o número pasa a un guion; "" si no queda nada. */
    public static String de(String nombre) {
        if (nombre == null) return "";
        String sinTildes = Normalizer.normalize(nombre.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinTildes.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }
}
