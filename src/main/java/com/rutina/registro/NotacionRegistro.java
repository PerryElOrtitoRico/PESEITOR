package com.rutina.registro;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee lo que el usuario escribió en una casilla del registro (ej. {@code 3.3-85.3-80}).
 * El punto separa series, el guion marca el kg de esa serie y los decimales del kg van con coma.
 */
public final class NotacionRegistro {

    /** reps 1..99 sin ceros a la izquierda, kg opcional con decimales con coma. */
    private static final Pattern SERIE = Pattern.compile("([1-9][0-9]?)(?:-([0-9]+(?:,[0-9]+)?))?");

    private NotacionRegistro() {
    }

    /**
     * @param texto        lo que escribió el usuario; no puede ser vacío (vacío = "como lo planificado", lo resuelve quien llama)
     * @param planificadas series planificadas de esa fila, en orden (puede ser una lista vacía)
     * @return las series realizadas; lista vacía si el texto es "0"
     * @throws IllegalArgumentException si el texto no cumple la gramática; el mensaje dice qué está mal
     */
    public static List<SerieReal> parse(String texto, List<SerieReal> planificadas) {
        if (texto == null) throw new IllegalArgumentException("casilla vacía");
        String t = texto.replaceAll("\\s+", "");
        if (t.isEmpty()) throw new IllegalArgumentException("casilla vacía");
        if (t.equals("0")) return List.of();

        String[] partes = t.split("\\.", -1);
        List<SerieReal> res = new ArrayList<>();
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i];
            if (p.isEmpty()) throw new IllegalArgumentException("falta una serie entre puntos en \"" + texto + "\"");
            Matcher m = SERIE.matcher(p);
            if (!m.matches()) throw new IllegalArgumentException("serie inválida: \"" + p + "\" (reps de 1 a 99, kg con coma)");
            int reps = Integer.parseInt(m.group(1));
            double kg;
            if (m.group(2) != null) {
                kg = Double.parseDouble(m.group(2).replace(',', '.'));
                if (kg <= 0) throw new IllegalArgumentException("el kg tiene que ser mayor que 0: \"" + p + "\"");
            } else {
                if (planificadas.isEmpty()) throw new IllegalArgumentException("indicá el peso de la serie \"" + p + "\"");
                kg = planificadas.get(Math.min(i, planificadas.size() - 1)).kg();
            }
            res.add(new SerieReal(reps, kg));
        }
        return res;
    }

    /** true si parse no lanzaría excepción. */
    public static boolean esValida(String texto, List<SerieReal> planificadas) {
        try {
            parse(texto, planificadas);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
