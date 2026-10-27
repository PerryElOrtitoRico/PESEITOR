package com.rutina.app.logica;

import java.time.LocalDate;

/** Fechas del registro en formato AAAA-MM-DD y cómo se muestran (igual que fechaLinda de la app web). */
public final class Fechas {

    private Fechas() {
    }

    /** "2026-10-27" → "27/10/2026"; si no tiene forma de fecha, el texto tal cual. */
    public static String larga(String iso) {
        if (iso == null) return "";
        String[] p = iso.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] + "/" + p[0] : iso;
    }

    /** "2026-10-27" → "27/10"; si no tiene forma de fecha, el texto tal cual. */
    public static String corta(String iso) {
        if (iso == null) return "";
        String[] p = iso.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] : iso;
    }

    /** La fecha de hoy como AAAA-MM-DD. */
    public static String hoy() {
        return LocalDate.now().toString();
    }
}
