package com.rutina.panel;

import com.rutina.registro.JsonRegistro;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Los archivos de docs/ejemplos se pueden leer con los lectores de verdad y son coherentes entre sí. */
class EjemplosTest {

    static final Path PLAN = Path.of("docs/ejemplos/plan-ejemplo.json");
    static final Path REGISTRO = Path.of("docs/ejemplos/registro-ejemplo.json");

    @Test
    void elPlanYElRegistroDeEjemploSeLeen() throws Exception {
        assertTrue(Files.exists(PLAN), "falta " + PLAN);
        assertTrue(Files.exists(REGISTRO), "falta " + REGISTRO);
        var plan = JsonRegistro.planDeJson(Files.readString(PLAN));
        var registro = JsonRegistro.leerRegistro(REGISTRO);
        assertEquals("Ejemplo", plan.perfil());
        assertEquals("Ejemplo", registro.perfil());
        assertEquals(plan.mesocicloId(), registro.mesocicloId());
        assertEquals(2, plan.microciclos());
        assertTrue(registro.sesiones().size() >= 4);
    }

    @Test
    void elRegistroTieneLosCasosQuePideLaPrueba() throws Exception {
        var plan = JsonRegistro.planDeJson(Files.readString(PLAN));
        var registro = JsonRegistro.leerRegistro(REGISTRO);
        long conDuracion = registro.sesiones().stream().filter(s -> s.duracionMin() != null).count();
        long sinDuracion = registro.sesiones().stream().filter(s -> s.duracionMin() == null).count();
        long fueraDelPlan = registro.sesiones().stream().filter(s -> plan.sesiones().stream()
                .noneMatch(p -> p.microciclo() == s.microciclo() && p.dia() == s.dia())).count();
        assertTrue(conDuracion >= 3);
        assertTrue(sinDuracion >= 1);
        assertEquals(1, fueraDelPlan);
        assertTrue(registro.sesiones().stream().anyMatch(s -> s.sensacionFinal() != null));
        assertTrue(registro.sesiones().stream().anyMatch(s -> s.sensacionFinal() == null));
        assertTrue(registro.sesiones().stream().flatMap(s -> s.ejercicios().stream())
                .anyMatch(e -> e.esfuerzo() != null));
    }
}
