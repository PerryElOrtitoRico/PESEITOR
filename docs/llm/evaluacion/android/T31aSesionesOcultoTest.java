package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;
import com.rutina.registro.EjercicioPlan;
import com.rutina.registro.FilaPlan;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SerieReal;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;

import java.util.List;

import org.junit.jupiter.api.Test;

class T31aSesionesOcultoTest {

    private static final List<SerieReal> S = List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 127.5));

    private static PlanCelular plan(String perfil) {
        EjercicioPlan sentadilla = new EjercicioPlan("1-1", "Sentadilla", Tipo.FUERZA, Aproximacion.COMPLEJA, 150.0,
                List.of(new FilaPlan("80-85", 80, 85, S)));
        return new PlanCelular("rutineitor-plan", 1, perfil, 3L, "2026-10-26", 2, 2, List.of(
                new SesionPlan(1, 1, List.of(sentadilla)),
                new SesionPlan(1, 2, List.of()),
                new SesionPlan(2, 1, List.of()),
                new SesionPlan(2, 2, List.of())));
    }

    @Test
    void sesionDelPlan() {
        PlanCelular plan = plan("Franco Pérez");
        assertEquals("1-1", Sesiones.sesionDelPlan(plan, 1, 1).ejercicios().get(0).bloque());
        assertEquals(List.of(), Sesiones.sesionDelPlan(plan, 2, 2).ejercicios());
        assertNull(Sesiones.sesionDelPlan(plan, 3, 1));
        assertNull(Sesiones.sesionDelPlan(plan, 1, 3));
    }

    @Test
    void sesionDelRegistro() {
        SesionRegistro a = new SesionRegistro(1, 1, "2026-10-27", 5, 3, List.of(), null);
        SesionRegistro b = new SesionRegistro(2, 1, "2026-11-02", null, null, List.of(), 75);
        RegistroCelular r = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of(a, b));
        assertEquals(a, Sesiones.sesionDelRegistro(r, 1, 1));
        assertEquals(b, Sesiones.sesionDelRegistro(r, 2, 1));
        assertNull(Sesiones.sesionDelRegistro(r, 1, 2));
        assertNull(Sesiones.sesionDelRegistro(r, 2, 2));
    }

    @Test
    void registroVacio() {
        assertEquals(new RegistroCelular("rutineitor-registro", 1, "Franco Pérez", 3L, List.of()),
                Sesiones.registroVacio(plan("Franco Pérez")));
    }

    @Test
    void pista() {
        assertEquals("3.3.3 · 120/127,5", Sesiones.pista(S));
        assertEquals("12.12", Sesiones.pista(List.of(new SerieReal(12, 0), new SerieReal(12, 0))));
        assertEquals("4.4 · 105", Sesiones.pista(List.of(new SerieReal(4, 105), new SerieReal(4, 105))));
        assertEquals("", Sesiones.pista(List.of()));
        assertEquals("5.5.5 · 60", Sesiones.pista(List.of(new SerieReal(5, 60), new SerieReal(5, 0), new SerieReal(5, 60))));
        assertEquals("8 · 22,5", Sesiones.pista(List.of(new SerieReal(8, 22.5))));
    }

    @Test
    void nombreArchivoRegistro() {
        assertEquals("registro-Franco_Pérez-2026-10-26.json", Sesiones.nombreArchivoRegistro(plan("Franco Pérez")));
        assertEquals("registro-Ana-2026-10-26.json", Sesiones.nombreArchivoRegistro(plan("Ana")));
    }
}
