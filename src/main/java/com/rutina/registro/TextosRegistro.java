package com.rutina.registro;

import java.util.ArrayList;
import java.util.List;

/** Textos que muestra la pestaña Registro de la PC (sin Swing, para poder testearlos). */
public final class TextosRegistro {

    private TextosRegistro() {
    }

    /** "75 min" para 75; "" si no se midió (null). */
    public static String duracion(Integer duracionMin) {
        if (duracionMin == null) return "";
        return duracionMin + " min";
    }

    /**
     * Notas de los ejercicios de una sesión, en el orden de los ejercicios del plan: "Nombre: nota" separadas por " | ".
     * Solo entran los ejercicios del plan que tienen nota no vacía en el registro (se empareja por bloque).
     * Los saltos de línea y espacios repetidos de la nota se reemplazan por un solo espacio. "" si no hay ninguna.
     */
    public static String notas(SesionPlan plan, SesionRegistro registro) {
        if (registro.ejercicios() == null) return "";
        List<String> partes = new ArrayList<>();
        for (EjercicioPlan ej : plan.ejercicios()) {
            for (EjercicioRegistro er : registro.ejercicios()) {
                if (ej.bloque().equals(er.bloque()) && er.nota() != null && !er.nota().isBlank()) {
                    partes.add(ej.nombre() + ": " + er.nota().strip().replaceAll("\\s+", " "));
                    break;
                }
            }
        }
        return String.join(" | ", partes);
    }
}
