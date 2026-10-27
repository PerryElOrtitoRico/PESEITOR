package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;
import com.rutina.registro.EjercicioPlan;
import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.FilaPlan;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SerieReal;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SesionesTest {

    private static final List<SerieReal> SERIES =
            List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 127.5));

    private static PlanCelular plan() {
        EjercicioPlan sentadilla = new EjercicioPlan("1-1", "Sentadilla", Tipo.FUERZA, Aproximacion.COMPLEJA, 150.0,
                List.of(new FilaPlan("80-85", 80, 85, SERIES)));
        return new PlanCelular("rutineitor-plan", 1, "Franco Pérez", 3L, "2026-10-26", 2, 2, List.of(
                new SesionPlan(1, 1, List.of(sentadilla)),
                new SesionPlan(1, 2, List.of()),
                new SesionPlan(2, 1, List.of()),
                new SesionPlan(2, 2, List.of())));
    }

    @Test
    void sesionDelPlan() {
        assertEquals("1-1", Sesiones.sesionDelPlan(plan(), 1, 1).ejercicios().get(0).bloque());
        assertEquals(List.of(), Sesiones.sesionDelPlan(plan(), 2, 2).ejercicios());
        assertNull(Sesiones.sesionDelPlan(plan(), 3, 1));
    }

    @Test
    void sesionDelRegistro() {
        SesionRegistro a = new SesionRegistro(1, 1, "2026-10-27", 5, 3, List.of(), null);
        RegistroCelular r = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of(a));
        assertEquals(a, Sesiones.sesionDelRegistro(r, 1, 1));
        assertNull(Sesiones.sesionDelRegistro(r, 1, 2));
    }

    @Test
    void registroVacio() {
        assertEquals(new RegistroCelular("rutineitor-registro", 1, "Franco Pérez", 3L, List.of()), Sesiones.registroVacio(plan()));
    }

    @Test
    void pista() {
        assertEquals("3.3.3 · 120/127,5", Sesiones.pista(SERIES));
        assertEquals("12.12", Sesiones.pista(List.of(new SerieReal(12, 0), new SerieReal(12, 0))));
        assertEquals("4.4 · 105", Sesiones.pista(List.of(new SerieReal(4, 105), new SerieReal(4, 105))));
        assertEquals("", Sesiones.pista(List.of()));
    }

    @Test
    void nombreArchivoRegistro() {
        assertEquals("registro-Franco_Pérez-2026-10-26.json", Sesiones.nombreArchivoRegistro(plan()));
    }

    @Test
    void limpiaSesion() {
        SesionRegistro s = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-1", null, Map.of("80-85", " 3.3 "), null), new EjercicioRegistro("1-2", null, Map.of("4x12", ""), null), new EjercicioRegistro("1-3", 2, Map.of(), "  subir peso  ")), null);
        SesionRegistro l = Sesiones.limpiarSesion(s);
        assertEquals(2, l.ejercicios().size());
        assertEquals(Map.of("80-85", "3.3"), l.ejercicios().get(0).filas());
        assertEquals("1-3", l.ejercicios().get(1).bloque());
        assertEquals("subir peso", l.ejercicios().get(1).nota());
        assertEquals(3, s.ejercicios().size());
    }

    @Test
    void guardaSesion() {
        RegistroCelular r0 = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of());
        SesionRegistro s22 = new SesionRegistro(2, 2, "2026-11-05", 4, null, List.of(), null);
        SesionRegistro s11 = new SesionRegistro(1, 1, "2026-10-27", 5, 3, List.of(), null);
        RegistroCelular r2 = Sesiones.guardarSesion(Sesiones.guardarSesion(r0, s22), s11);
        assertEquals(List.of(), r0.sesiones());
        assertEquals(1, r2.sesiones().get(0).microciclo());
        assertEquals(2, r2.sesiones().get(1).microciclo());
        RegistroCelular r3 = Sesiones.guardarSesion(r2, new SesionRegistro(1, 1, "2026-10-27", 1, 3, List.of(), null));
        assertEquals(2, r3.sesiones().size());
        assertEquals(1, Sesiones.sesionDelRegistro(r3, 1, 1).descanso());
    }
}
