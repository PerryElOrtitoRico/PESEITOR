package com.rutina.app.logica;

import java.util.Locale;

/** Funciones del cronómetro de la sesión (traducción de celular/logica.js). El estado es inmutable. */
public final class Crono {

    private Crono() {
    }

    /** Cronómetro en cero y parado. */
    public static EstadoCrono nuevo() {
        return new EstadoCrono(0, null);
    }

    /** Si ya corre devuelve un estado igual; si no, arranca en {@code ahora}. */
    public static EstadoCrono iniciar(EstadoCrono estado, long ahora) {
        if (estado.desde() != null) return estado;
        return new EstadoCrono(estado.acumuladoMs(), ahora);
    }

    /** Si corre, suma lo corrido a acumuladoMs y lo para; si no, devuelve un estado igual. */
    public static EstadoCrono pausar(EstadoCrono estado, long ahora) {
        if (estado.desde() == null) return estado;
        return new EstadoCrono(estado.acumuladoMs() + (ahora - estado.desde()), null);
    }

    /** Milisegundos contados a la hora {@code ahora}. */
    public static long ms(EstadoCrono estado, long ahora) {
        if (estado.desde() == null) return estado.acumuladoMs();
        return estado.acumuladoMs() + (ahora - estado.desde());
    }

    /** "h:mm:ss" sin redondear: 0 → "0:00:00", 75_500 → "0:01:15", 3_725_000 → "1:02:05". */
    public static String texto(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);
    }

    /** Minutos para guardar: ms <= 0 → null; si no, al minuto más cercano y al menos 1. */
    public static Integer minutosDe(long ms) {
        if (ms <= 0) return null;
        return (int) Math.max(1, Math.round(ms / 60000.0));
    }

    /** Minutos escritos por el usuario → entero de 1 a 600, o null si es inválido ("75", " 5 " sí; "0", "601", "7,5", "" no). */
    public static Integer duracionValida(String texto) {
        if (texto == null) return null;
        String t = texto.strip();
        if (!t.matches("[0-9]+")) return null;
        long n;
        try {
            n = Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
        if (n < 1 || n > 600) return null;
        return (int) n;
    }
}
