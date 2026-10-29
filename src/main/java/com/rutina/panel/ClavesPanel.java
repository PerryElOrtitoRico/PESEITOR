package com.rutina.panel;

import com.rutina.model.Ejercicio;

import java.util.Comparator;
import java.util.List;

/** Las claves de 1RM del perfil, para usarlas al crear metas en el panel. */
public final class ClavesPanel {

    private ClavesPanel() {
    }

    /** Una línea por ejercicio con 1RM, ordenada por id. */
    public static List<String> lista(List<Ejercicio> delPerfil) {
        return delPerfil.stream()
                .filter(e -> e.rm() != null)
                .sorted(Comparator.comparingLong(Ejercicio::id))
                .map(e -> linea(e, delPerfil))
                .toList();
    }

    private static String linea(Ejercicio e, List<Ejercicio> delPerfil) {
        String clave = ClaveEjercicio.de(e.nombre());
        if (clave.isEmpty()) return "(sin clave utilizable) — " + e.nombre();
        String base = "1rm:" + clave + " — " + e.nombre();
        return RmAPanel.esRepresentante(e.id(), e.nombre(), delPerfil) ? base : base + " (repetida: no se manda)";
    }
}
