package com.rutina.registro;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Test visible de T02: los casos de la tabla de la tarea. */
class NotacionRegistroTest {

    private static final List<SerieReal> PLAN =
            List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 120));
    private static final List<SerieReal> SIN_PLAN = List.of();

    private static SerieReal s(int reps, double kg) {
        return new SerieReal(reps, kg);
    }

    @Test
    void ejemplosDeLaTabla() {
        assertEquals(List.of(s(3, 120), s(3, 85), s(3, 80)), NotacionRegistro.parse("3.3-85.3-80", PLAN));
        assertEquals(List.of(s(3, 120), s(3, 120)), NotacionRegistro.parse("3.3", PLAN));
        assertEquals(List.of(s(2, 120), s(3, 120)), NotacionRegistro.parse("2.3", PLAN));
        assertEquals(List.of(s(3, 120), s(3, 120), s(3, 120), s(3, 120)), NotacionRegistro.parse("3.3.3.3", PLAN));
        assertEquals(List.of(s(3, 82.5)), NotacionRegistro.parse("3-82,5", PLAN));
        assertEquals(List.of(s(3, 120), s(3, 85)), NotacionRegistro.parse(" 3 . 3 - 85 ", PLAN));
        assertEquals(List.of(), NotacionRegistro.parse("0", PLAN));
        assertEquals(List.of(s(3, 90)), NotacionRegistro.parse("3-90", SIN_PLAN));
        assertEquals(List.of(s(3, 80), s(5, 120)), NotacionRegistro.parse("3-80.5", PLAN));
    }

    @Test
    void sinPlanificadasPideElPeso() {
        var e = assertThrows(IllegalArgumentException.class, () -> NotacionRegistro.parse("3", SIN_PLAN));
        assertTrue(e.getMessage().contains("indicá el peso"));
    }

    @Test
    void invalidos() {
        for (String t : new String[]{"", "3..3", "3-", "-80", "0.3", "100", "3-0", "a", "3."}) {
            assertThrows(IllegalArgumentException.class, () -> NotacionRegistro.parse(t, PLAN), "debería fallar: " + t);
        }
        assertThrows(IllegalArgumentException.class, () -> NotacionRegistro.parse(null, PLAN));
    }

    @Test
    void esValida() {
        assertTrue(NotacionRegistro.esValida("3.3", PLAN));
        assertFalse(NotacionRegistro.esValida("3..3", PLAN));
    }
}
