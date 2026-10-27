package com.rutina.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.rutina.registro.JsonRegistro;
import com.rutina.registro.NotacionRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.SerieReal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Comprueba que la lógica compartida con la PC está disponible en la app. */
class CompartidoTest {

    @Test
    void leeElPlanDeEjemplo() throws IOException {
        String json = new String(Files.readAllBytes(
                Paths.get("../../docs/registro-sesiones/ejemplos/plan-ejemplo.json")), StandardCharsets.UTF_8);
        PlanCelular plan = JsonRegistro.planDeJson(json);
        assertEquals("rutineitor-plan", plan.formato());
        assertFalse(plan.sesiones().isEmpty());

        List<SerieReal> res = NotacionRegistro.parse("3-80.5",
                List.of(new SerieReal(3, 85), new SerieReal(3, 85)));
        assertEquals(List.of(new SerieReal(3, 80.0), new SerieReal(5, 85.0)), res);
    }
}
