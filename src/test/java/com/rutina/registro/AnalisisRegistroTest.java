package com.rutina.registro;

import com.rutina.model.Tipo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalisisRegistroTest {

    private static final double EPS = 1e-9;
    private static PlanCelular plan;
    private static List<ResumenSesion> res;

    @BeforeAll
    static void leer() throws Exception {
        plan = JsonRegistro.planDeJson(Files.readString(Path.of("docs/registro-sesiones/ejemplos/plan-ejemplo.json")));
        res = AnalisisRegistro.analizar(plan,
                JsonRegistro.leerRegistro(Path.of("docs/registro-sesiones/ejemplos/registro-ejemplo.json")));
    }

    private static ComparacionTipo tipo(ResumenSesion r, Tipo t) {
        return r.porTipo().stream().filter(c -> c.tipo() == t).findFirst().orElseThrow();
    }

    @Test
    void sesion11() {
        var s = res.get(0);
        assertEquals("2026-10-27", s.fecha());
        assertEquals(5, s.descanso());
        assertEquals(4, s.sensacionFinal());
        assertEquals(4.0, s.esfuerzoPromedio());
        var f = tipo(s, Tipo.FUERZA);
        assertEquals(17, f.volumenPlan());
        assertEquals(14, f.volumenReal());
        assertEquals(1295.0 / 17, f.imrPlan(), EPS);
        assertEquals(1030.0 / 14, f.imrReal(), EPS);
        var h = tipo(s, Tipo.HIPERTROFIA);
        assertEquals(48, h.volumenPlan());
        assertEquals(48, h.volumenReal());
        assertNull(h.imrPlan());
        assertNull(h.imrReal());
    }

    @Test
    void sesion12() {
        var s = res.get(1);
        assertEquals("2026-10-29", s.fecha());
        assertEquals(2, s.descanso());
        assertNull(s.sensacionFinal());
        assertNull(s.esfuerzoPromedio());
        var f = tipo(s, Tipo.FUERZA);
        assertEquals(24, f.volumenPlan());
        assertEquals(24, f.volumenReal());
        assertEquals(90.0 / 130 * 100, f.imrPlan(), EPS);
        assertEquals(90.0 / 130 * 100, f.imrReal(), EPS);
    }

    @Test
    void otroMesociclo() {
        var otro = new RegistroCelular(RegistroCelular.FORMATO, 1, "F", 99, List.of());
        assertThrows(IllegalArgumentException.class, () -> AnalisisRegistro.analizar(plan, otro));
    }

    @Test
    void notacionInvalidaDiceDonde() {
        var r = new RegistroCelular(RegistroCelular.FORMATO, 1, "F", 3, List.of(new SesionRegistro(1, 1, "2026-10-27", null, null,
                List.of(new EjercicioRegistro("1-1", null, Map.of("80-85", "3..3"), null)), null)));
        var e = assertThrows(IllegalArgumentException.class, () -> AnalisisRegistro.analizar(plan, r));
        assertTrue(e.getMessage().contains("1-1"), e.getMessage());
        assertTrue(e.getMessage().contains("80-85"), e.getMessage());
    }
}
