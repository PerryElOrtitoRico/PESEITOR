package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.registro.DiaPlioCelular;
import com.rutina.registro.EjercicioPlioRegistro;
import com.rutina.registro.PlanPlio;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionPlioRegistro;
import com.rutina.registro.SesionRegistro;

import java.util.List;

import org.junit.jupiter.api.Test;

class SesionesPlioTest {

    private static final PlanPlio PLAN = new PlanPlio(1, "2026-11-02", List.of(
            List.of(new DiaPlioCelular(1, 120, 2, 10, List.of("A", "B")), new DiaPlioCelular(2, 60, 1, 10, List.of("C"))),
            List.of(new DiaPlioCelular(1, 180, 3, 10, List.of("A", "B")))));

    private static SesionPlioRegistro ses(int semana, int dia, Integer min, String... notas) {
        EjercicioPlioRegistro[] e = new EjercicioPlioRegistro[notas.length];
        for (int i = 0; i < notas.length; i++) e[i] = new EjercicioPlioRegistro(i + 1, notas[i]);
        return new SesionPlioRegistro("2026-11-02", semana, dia, "2026-11-12", min, List.of(e));
    }

    private static RegistroCelular vacio() {
        return new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of());
    }

    @Test
    void buscaElDiaDelPlan() {
        assertEquals(3, SesionesPlio.dia(PLAN, 2, 1).series());
        assertNull(SesionesPlio.dia(PLAN, 2, 2));
        assertNull(SesionesPlio.dia(PLAN, 5, 1));
    }

    @Test
    void limpiaNotasEnBlancoYRecortaLasDemas() {
        SesionPlioRegistro l = SesionesPlio.limpiar(ses(1, 1, 20, " ", "  bien  ", null));
        assertEquals(List.of(new EjercicioPlioRegistro(2, "bien")), l.ejercicios());
    }

    @Test
    void noGuardaUnaSesionVaciaPeroSiLaConDuracion() {
        assertFalse(SesionesPlio.hayQueGuardar(null, SesionesPlio.limpiar(ses(1, 1, null, " "))));
        assertTrue(SesionesPlio.hayQueGuardar(null, SesionesPlio.limpiar(ses(1, 1, 5))));
        SesionPlioRegistro g = SesionesPlio.limpiar(ses(1, 1, 5, "x"));
        assertFalse(SesionesPlio.hayQueGuardar(g, g));
        assertTrue(SesionesPlio.hayQueGuardar(g, SesionesPlio.limpiar(ses(1, 1, 6, "x"))));
    }

    @Test
    void guardarReemplazaOrdenaYNoTocaLasPesas() {
        SesionRegistro pesas = new SesionRegistro(1, 1, "2026-11-10", null, null, List.of(), null);
        RegistroCelular r = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of(pesas));
        r = SesionesPlio.guardar(r, ses(2, 1, 5, "a"));
        r = SesionesPlio.guardar(r, ses(1, 2, 5, "b"));
        r = SesionesPlio.guardar(r, ses(2, 1, 9, "c"));
        assertEquals(2, r.pliometria().size());
        assertEquals(1, r.pliometria().get(0).semana());
        assertEquals("c", SesionesPlio.nota(SesionesPlio.sesion(r, "2026-11-02", 2, 1), 1));
        assertEquals(List.of(pesas), r.sesiones());
    }

    @Test
    void guardarPesasConservaLaPliometria() {
        RegistroCelular r = SesionesPlio.guardar(vacio(), ses(1, 1, 5, "a"));
        SesionRegistro pesas = new SesionRegistro(1, 1, "2026-11-10", 3, null, List.of(), null);
        assertEquals(1, Sesiones.guardarSesion(r, pesas).pliometria().size());
    }

    @Test
    void borrarQuitaSoloEsaSesion() {
        RegistroCelular r = SesionesPlio.guardar(SesionesPlio.guardar(vacio(), ses(1, 1, 5, "a")), ses(1, 2, 5, "b"));
        RegistroCelular sin = SesionesPlio.borrar(r, "2026-11-02", 1, 1);
        assertNull(SesionesPlio.sesion(sin, "2026-11-02", 1, 1));
        assertNotNull(SesionesPlio.sesion(sin, "2026-11-02", 1, 2));
    }

    @Test
    void textos() {
        assertEquals("2 series × 10 saltos", SesionesPlio.seriesTexto(PLAN.semanas().get(0).get(0)));
        assertEquals("1 serie × 10 saltos", SesionesPlio.seriesTexto(PLAN.semanas().get(0).get(1)));
        assertEquals("Semana 2 · Día 1", SesionesPlio.titulo(2, 1));
        assertTrue(SesionesPlio.claveCrono(3, "2026-11-02", 1, 2).startsWith("3-"));
    }
}
