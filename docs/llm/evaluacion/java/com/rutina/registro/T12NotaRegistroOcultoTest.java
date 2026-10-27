package com.rutina.registro;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class T12NotaRegistroOcultoTest {

    private static String con(String sesiones) {
        return "{\"formato\":\"rutineitor-registro\",\"version\":1,\"perfil\":\"F\",\"mesocicloId\":3,\"sesiones\":" + sesiones + "}";
    }

    private static String sesionConNota(String notaJson) {
        return con("[{\"microciclo\":1,\"dia\":2,\"fecha\":\"2026-10-27\",\"ejercicios\":[{\"bloque\":\"2-3\",\"nota\":" + notaJson + "}]}]");
    }

    @Test
    void notaNullOEnBlancoQuedaNull() {
        assertNull(JsonRegistro.registroDeJson(sesionConNota("null")).sesiones().get(0).ejercicios().get(0).nota());
        assertNull(JsonRegistro.registroDeJson(sesionConNota("\"   \"")).sesiones().get(0).ejercicios().get(0).nota());
        assertNull(JsonRegistro.registroDeJson(sesionConNota("\"\"")).sesiones().get(0).ejercicios().get(0).nota());
        assertNull(JsonRegistro.registroDeJson(sesionConNota("\" \\n \"")).sesiones().get(0).ejercicios().get(0).nota());
    }

    @Test
    void notaSeRecorta() {
        var n = JsonRegistro.registroDeJson(sesionConNota("\"  dolor leve\\nen el hombro \"")).sesiones().get(0).ejercicios().get(0).nota();
        assertEquals("dolor leve\nen el hombro", n);
    }

    @Test
    void notaDe500VaYDe501No() {
        String ok = "a".repeat(500);
        assertEquals(ok, JsonRegistro.registroDeJson(sesionConNota("\"" + ok + "\"")).sesiones().get(0).ejercicios().get(0).nota());
        String conEspacios = "  " + ok + "  ";
        assertEquals(ok, JsonRegistro.registroDeJson(sesionConNota("\"" + conEspacios + "\"")).sesiones().get(0).ejercicios().get(0).nota());
        var e = assertThrows(IllegalArgumentException.class,
                () -> JsonRegistro.registroDeJson(sesionConNota("\"" + "a".repeat(501) + "\"")));
        assertEquals("sesión microciclo 1, día 2, ejercicio 2-3: la nota tiene más de 500 caracteres", e.getMessage());
    }

    @Test
    void duracionEnRango() {
        for (int d : new int[]{1, 75, 600}) {
            var r = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":" + d + "}]"));
            assertEquals(d, r.sesiones().get(0).duracionMin());
        }
        for (int d : new int[]{0, -3, 601}) {
            var e = assertThrows(IllegalArgumentException.class,
                    () -> JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":" + d + "}]")));
            assertTrue(e.getMessage().contains("duración"), e.getMessage());
        }
        var sin = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":null}]"));
        assertNull(sin.sesiones().get(0).duracionMin());
    }

    @Test
    void registroViejoSeLeeIgual() {
        var r = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"descanso\":5,\"final\":4,"
                + "\"ejercicios\":[{\"bloque\":\"1-1\",\"esfuerzo\":3,\"filas\":{\"80-85\":\"3.3\"}}]}]"));
        var s = r.sesiones().get(0);
        assertNull(s.duracionMin());
        assertEquals(5, s.descanso());
        assertEquals(4, s.sensacionFinal());
        assertNull(s.ejercicios().get(0).nota());
        assertEquals(Map.of("80-85", "3.3"), s.ejercicios().get(0).filas());
    }

    @Test
    void idaYVueltaConCamposNuevos() {
        var r = new RegistroCelular(RegistroCelular.FORMATO, 1, "F", 3, List.of(
                new SesionRegistro(1, 1, "2026-10-27", 5, 4,
                        List.of(new EjercicioRegistro("1-1", 3, Map.of("80-85", "3.3"), "buena técnica")), 75),
                new SesionRegistro(1, 2, "2026-10-29", null, null, List.of(), null)));
        String json = JsonRegistro.registroAJson(r);
        assertTrue(json.contains("\"duracionMin\": 75"));
        assertTrue(json.contains("\"nota\": \"buena técnica\""));
        assertEquals(r, JsonRegistro.registroDeJson(json));
    }

    @Test
    void conservaDuracionYNotaAlArmar() {
        var r = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":60,"
                + "\"ejercicios\":[{\"bloque\":\"1-1\",\"nota\":\"x\"},{\"bloque\":\"1-2\"}]}]"));
        assertEquals(60, r.sesiones().get(0).duracionMin());
        assertEquals("x", r.sesiones().get(0).ejercicios().get(0).nota());
        assertNull(r.sesiones().get(0).ejercicios().get(1).nota());
    }
}
