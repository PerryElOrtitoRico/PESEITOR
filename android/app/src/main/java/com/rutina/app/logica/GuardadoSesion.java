package com.rutina.app.logica;

import com.rutina.registro.SesionRegistro;

/** Cuándo la pantalla de la sesión tiene que guardar lo que se ve. */
public final class GuardadoSesion {

    private GuardadoSesion() {
    }

    /**
     * @param guardada la sesión que hay en el registro, o null si todavía no hay
     * @param actual   lo que se ve en pantalla, ya pasado por {@link Sesiones#limpiarSesion}
     * @return false si no cambió nada respecto de lo guardado, o si no hay nada anotado y tampoco hay sesión guardada
     *         (abrir la pantalla no crea una sesión vacía)
     */
    public static boolean hayQueGuardar(SesionRegistro guardada, SesionRegistro actual) {
        if (guardada == null) return !vacia(actual);
        return !Sesiones.limpiarSesion(guardada).equals(actual);
    }

    /** Sin descanso, sin sensación final, sin ejercicios con algo anotado y sin duración. */
    public static boolean vacia(SesionRegistro s) {
        return s.descanso() == null && s.sensacionFinal() == null && s.ejercicios().isEmpty() && s.duracionMin() == null;
    }
}
