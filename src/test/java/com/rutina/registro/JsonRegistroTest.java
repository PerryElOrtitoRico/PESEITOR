package com.rutina.registro;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonRegistroTest {

    private static String con(String sesiones) {
        return "{\"formato\":\"rutineitor-registro\",\"version\":1,\"perfil\":\"F\",\"mesocicloId\":3,\"sesiones\":" + sesiones + "}";
    }

    @Test
    void leeElEjemplo() throws Exception {
        var r = JsonRegistro.leerRegistro(Path.of("docs/registro-sesiones/ejemplos/registro-ejemplo.json"));
        assertEquals(2, r.sesiones().size());
        var s1 = r.sesiones().get(0);
        assertEquals(5, s1.descanso());
        assertEquals(4, s1.sensacionFinal());
        var ej = s1.ejercicios().stream().filter(e -> e.bloque().equals("1-1")).findFirst().orElseThrow();
        assertEquals(4, ej.esfuerzo());
        assertEquals("3.3-115", ej.filas().get("80-85"));
        assertNull(r.sesiones().get(1).sensacionFinal());
        assertTrue(r.sesiones().get(1).ejercicios().isEmpty());
    }

    @Test
    void idaYVuelta() {
        var r = new RegistroCelular(RegistroCelular.FORMATO, 1, "Franco", 3, List.of(
                new SesionRegistro(1, 1, "2026-10-27", 5, 4, List.of(new EjercicioRegistro("1-1", 3, Map.of("80-85", "3.3"), null)), null),
                new SesionRegistro(1, 2, "2026-10-29", null, null, List.of(), null)));
        String json = JsonRegistro.registroAJson(r);
        assertTrue(json.contains("\"final\""));
        assertFalse(json.contains("sensacionFinal"));
        assertEquals(r, JsonRegistro.registroDeJson(json));
    }

    @Test
    void errores() {
        String[] malos = {
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"descanso\":7}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"ejercicios\":[{\"bloque\":\"1-1\",\"esfuerzo\":0}]}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"27/10/2026\"}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\"},{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-28\"}]"),
                "{\"formato\":\"rutineitor-plan\",\"version\":1,\"sesiones\":[]}",
                "{ roto",
        };
        for (String m : malos) {
            var e = assertThrows(IllegalArgumentException.class, () -> JsonRegistro.registroDeJson(m), m);
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void ejerciciosNullQuedaVacio() {
        var r = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"ejercicios\":null}]"));
        assertNotNull(r.sesiones().get(0).ejercicios());
        assertTrue(r.sesiones().get(0).ejercicios().isEmpty());
    }
}
