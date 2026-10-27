package com.rutina.registro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EscalaEsfuerzoOrdenTest {

    @Test
    void alLimiteYPesadoIntercambiados() {
        assertEquals(EscalaEsfuerzo.AL_LIMITE, EscalaEsfuerzo.deValor(4));
        assertEquals("Al límite", EscalaEsfuerzo.deValor(4).texto());
        assertEquals("Pesado", EscalaEsfuerzo.deValor(5).texto());
        assertEquals(EscalaEsfuerzo.PESADO, EscalaEsfuerzo.values()[4]);
    }
}
