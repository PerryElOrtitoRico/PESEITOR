package com.rutina.registro;

import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonPlanTest {

    private static PlanCelular chico() {
        var fila = new FilaPlan("80-85", 80, 85, List.of(new SerieReal(3, 120), new SerieReal(3, 127.5)));
        var ej = new EjercicioPlan("1-1", "Sentadilla", Tipo.FUERZA, Aproximacion.COMPLEJA, null, List.of(fila));
        return new PlanCelular(PlanCelular.FORMATO, 1, "Franco", 3, "2026-10-26", 1, 1,
                List.of(new SesionPlan(1, 1, List.of(ej))));
    }

    @Test
    void idaYVuelta() {
        assertEquals(chico(), JsonRegistro.planDeJson(JsonRegistro.planAJson(chico())));
    }

    @Test
    void clavesDeLaEspecificacion() {
        String json = JsonRegistro.planAJson(chico());
        assertTrue(json.contains("\"formato\": \"rutineitor-plan\""), json);
        assertTrue(json.contains("\"mesocicloId\""));
        assertTrue(json.contains("\"etiqueta\""));
        assertTrue(json.contains("\"rm\": null"));
        assertTrue(json.contains("\"tipo\": \"FUERZA\""));
    }

    @Test
    void leeElEjemplo() throws Exception {
        var p = JsonRegistro.planDeJson(Files.readString(Path.of("docs/registro-sesiones/ejemplos/plan-ejemplo.json")));
        assertEquals(4, p.sesiones().size());
        var fila = p.sesiones().get(0).ejercicios().get(0).filas().stream()
                .filter(f -> f.etiqueta().equals("80-85")).findFirst().orElseThrow();
        assertEquals(3, fila.series().size());
        assertEquals(127.5, fila.series().get(2).kg());
    }

    @Test
    void escribeArchivo(@TempDir Path d) throws Exception {
        var f = d.resolve("plan.json");
        JsonRegistro.escribirPlan(f, chico());
        assertEquals(chico(), JsonRegistro.planDeJson(Files.readString(f)));
    }

    @Test
    void errores() {
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.planDeJson("{\"formato\":\"otro\",\"version\":1}"));
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.planDeJson("{"));
        String v2 = JsonRegistro.planAJson(chico()).replace("\"version\": 1", "\"version\": 2");
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.planDeJson(v2));
    }
}
