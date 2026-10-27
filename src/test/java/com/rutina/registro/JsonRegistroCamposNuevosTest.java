package com.rutina.registro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonRegistroCamposNuevosTest {

    private static String con(String sesiones) {
        return "{\"formato\":\"rutineitor-registro\",\"version\":1,\"perfil\":\"F\",\"mesocicloId\":3,\"sesiones\":" + sesiones + "}";
    }

    @Test
    void limpiaNota() {
        assertNull(JsonRegistro.limpiarNota(null, "x"));
        assertNull(JsonRegistro.limpiarNota("   ", "x"));
        assertEquals("hola", JsonRegistro.limpiarNota("  hola  ", "x"));
        assertEquals(500, JsonRegistro.limpiarNota("a".repeat(500), "x").length());
        var e = assertThrows(IllegalArgumentException.class, () -> JsonRegistro.limpiarNota("a".repeat(501), "sesión 1"));
        assertEquals("sesión 1: la nota tiene más de 500 caracteres", e.getMessage());
    }

    @Test
    void leeDuracionYNota() {
        var r = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":75,\"ejercicios\":[{\"bloque\":\"1-1\",\"nota\":\"  subí 2,5 kg  \"}]}]"));
        assertEquals(75, r.sesiones().get(0).duracionMin());
        assertEquals("subí 2,5 kg", r.sesiones().get(0).ejercicios().get(0).nota());
        var vieja = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"ejercicios\":[{\"bloque\":\"1-1\"}]}]"));
        assertNull(vieja.sesiones().get(0).duracionMin());
        assertNull(vieja.sesiones().get(0).ejercicios().get(0).nota());
    }

    @Test
    void duracionFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":0}]")));
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":601}]")));
    }
}
