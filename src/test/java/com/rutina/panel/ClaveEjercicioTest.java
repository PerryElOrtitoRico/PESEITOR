package com.rutina.panel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaveEjercicioTest {

    @Test
    void claveSinTildesNiEspacios() {
        assertEquals("press-banca", ClaveEjercicio.de("Press banca"));
        assertEquals("sentadilla-trasera-barra", ClaveEjercicio.de("Sentadilla  trasera (barra)"));
        assertEquals("dominadas", ClaveEjercicio.de("Dominadas"));
        assertEquals("peso-muerto-rumano", ClaveEjercicio.de("  Péso Múerto Rumáno! "));
    }

    @Test
    void claveDeSoloSignosEsVacia() {
        assertEquals("", ClaveEjercicio.de("(*) -- !"));
        assertEquals("", ClaveEjercicio.de(""));
        assertEquals("", ClaveEjercicio.de(null));
    }
}
