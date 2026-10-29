package com.rutina.panel;

import com.rutina.model.Ejercicio;
import com.rutina.service.datos.EjercicioRepo.RmRegistrado;
import panel.core.Registro;

import java.util.List;
import java.util.Optional;

/** Convierte un 1RM guardado en el registro de resultado del panel. */
public final class RmAPanel {

    private RmAPanel() {
    }

    /** Vacío si el nombre no da una clave utilizable. */
    public static Optional<Registro> aRegistro(RmRegistrado rm, String dispositivo, long ahoraMs) {
        String clave = ClaveEjercicio.de(rm.nombre());
        if (clave.isEmpty()) return Optional.empty();
        String fecha = rm.fecha().toString();
        return Optional.of(new Registro("resultado", "rutineitor", "rm:" + rm.ejercicioId() + ":" + fecha, "gimnasio",
                fecha, null, "1RM " + rm.nombre() + ": " + FormatoSeries.kg(rm.rm()) + " kg", null, null,
                "1rm:" + clave, rm.rm(), dispositivo, ahoraMs, false));
    }

    /** El ejercicio de menor id es el dueño de la clave; los demás con la misma clave no se mandan. */
    public static boolean esRepresentante(long ejercicioId, String nombre, List<Ejercicio> delPerfil) {
        String clave = ClaveEjercicio.de(nombre);
        if (clave.isEmpty()) return true;
        for (Ejercicio otro : delPerfil) {
            if (otro.id() < ejercicioId && clave.equals(ClaveEjercicio.de(otro.nombre()))) return false;
        }
        return true;
    }
}
