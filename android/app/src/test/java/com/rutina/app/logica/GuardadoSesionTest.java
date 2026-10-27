package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.SesionRegistro;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GuardadoSesionTest {

    private static SesionRegistro sesion(Integer descanso, List<EjercicioRegistro> ejercicios, Integer duracion) {
        return new SesionRegistro(1, 1, "2026-10-27", descanso, null, ejercicios, duracion);
    }

    @Test
    void abrirSinAnotarNoCreaUnaSesionVacia() {
        assertFalse(GuardadoSesion.hayQueGuardar(null, Sesiones.limpiarSesion(sesion(null, List.of(
                new EjercicioRegistro("1-1", null, Map.of("80-85", "  "), "  ")), null))));
    }

    @Test
    void laPrimeraAnotacionSeGuarda() {
        assertTrue(GuardadoSesion.hayQueGuardar(null, Sesiones.limpiarSesion(sesion(5, List.of(), null))));
        assertTrue(GuardadoSesion.hayQueGuardar(null, Sesiones.limpiarSesion(sesion(null, List.of(
                new EjercicioRegistro("1-1", null, Map.of("80-85", "3.2"), null)), null))));
        assertTrue(GuardadoSesion.hayQueGuardar(null, Sesiones.limpiarSesion(sesion(null, List.of(), 45))));
    }

    @Test
    void sinCambiosNoSeVuelveAGuardar() {
        SesionRegistro guardada = Sesiones.limpiarSesion(sesion(5, List.of(
                new EjercicioRegistro("1-1", 3, Map.of("80-85", "3.2"), "nota")), 30));
        assertFalse(GuardadoSesion.hayQueGuardar(guardada, guardada));
        assertFalse(GuardadoSesion.hayQueGuardar(guardada, Sesiones.limpiarSesion(sesion(5, List.of(
                new EjercicioRegistro("1-1", 3, Map.of("80-85", " 3.2 "), " nota ")), 30))));
    }

    @Test
    void unCambioSeGuarda() {
        SesionRegistro guardada = Sesiones.limpiarSesion(sesion(5, List.of(), null));
        assertTrue(GuardadoSesion.hayQueGuardar(guardada, Sesiones.limpiarSesion(sesion(4, List.of(), null))));
    }

    @Test
    void borrarTodoLoAnotadoDeUnaSesionGuardadaSeGuarda() {
        SesionRegistro guardada = Sesiones.limpiarSesion(sesion(5, List.of(), null));
        assertTrue(GuardadoSesion.hayQueGuardar(guardada, Sesiones.limpiarSesion(sesion(null, List.of(), null))));
    }
}
