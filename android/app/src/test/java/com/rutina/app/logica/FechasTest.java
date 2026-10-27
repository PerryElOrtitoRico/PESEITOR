package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FechasTest {

    @Test
    void formatosDeFecha() {
        assertEquals("27/10/2026", Fechas.larga("2026-10-27"));
        assertEquals("27/10", Fechas.corta("2026-10-27"));
        assertEquals("", Fechas.larga(null));
        assertEquals("", Fechas.corta(null));
        assertEquals("rara", Fechas.larga("rara"));
        assertEquals("rara", Fechas.corta("rara"));
    }

    @Test
    void hoyTieneFormatoIso() {
        assertTrue(Fechas.hoy().matches("\\d{4}-\\d{2}-\\d{2}"));
    }
}
