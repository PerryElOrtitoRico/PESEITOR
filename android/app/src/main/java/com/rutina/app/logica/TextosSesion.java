package com.rutina.app.logica;

import com.rutina.registro.NotacionRegistro;
import com.rutina.registro.SerieReal;

import java.util.List;

/** Textos que muestra la pantalla de la sesión (iguales a los de la app web). */
public final class TextosSesion {

    private TextosSesion() {
    }

    /** "Microciclo 1 · Día 2". */
    public static String tituloSesion(int micro, int dia) {
        return "Microciclo " + micro + " · Día " + dia;
    }

    /** "Anotada el 27/10/2026", o "Sin anotar todavía" si la fecha es null. */
    public static String lineaFecha(String fecha) {
        if (fecha == null) {
            return "Sin anotar todavía";
        }
        return "Anotada el " + Fechas.larga(fecha);
    }

    /** "<hechas> / <planificadas> reps" a partir de {planificadas, hechas}; "— / <planificadas> reps" si hechas es -1. */
    public static String repsTexto(int[] totales) {
        String hechas = totales[1] == -1 ? "—" : String.valueOf(totales[1]);
        return hechas + " / " + totales[0] + " reps";
    }

    /** Mensaje de error de la casilla; null si está vacía o es válida. */
    public static String errorCasilla(String texto, List<SerieReal> planificadas) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            NotacionRegistro.parse(texto, planificadas);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /** Aviso gris "Ojo: N series (planificadas: M)…"; null si no corresponde. */
    public static String ojoCasilla(String texto, List<SerieReal> planificadas) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        List<SerieReal> hechas;
        try {
            hechas = NotacionRegistro.parse(texto, planificadas);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (hechas.size() <= planificadas.size()) {
            return null;
        }
        return "Ojo: " + hechas.size() + " series (planificadas: " + planificadas.size() + "). Los decimales del peso van con coma.";
    }
}
