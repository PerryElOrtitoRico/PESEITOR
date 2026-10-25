package com.rutina.registro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EscalasTest {

    @Test
    void descansoDeValor() {
        assertEquals(EscalaDescanso.DESCANSADO_DISTRAIDO, EscalaDescanso.deValor(4));
        assertEquals("Bien descansado pero distraído", EscalaDescanso.deValor(4).texto());
    }

    @Test
    void valorInexistente() {
        assertThrows(IllegalArgumentException.class, () -> EscalaEsfuerzo.deValor(6));
        assertThrows(IllegalArgumentException.class, () -> EscalaDescanso.deValor(0));
    }

    @Test
    void descansoVaDeSeisAUno() {
        assertEquals(6, EscalaDescanso.values()[0].valor());
        assertEquals(1, EscalaDescanso.values()[5].valor());
    }

    @Test
    void finalYEsfuerzo() {
        assertEquals(EscalaFinal.CON_ENERGIA, EscalaFinal.deValor(5));
        assertEquals("Justo", EscalaEsfuerzo.deValor(3).texto());
        var e = assertThrows(IllegalArgumentException.class, () -> EscalaFinal.deValor(42));
        assertTrue(e.getMessage().contains("42"));
    }
}
