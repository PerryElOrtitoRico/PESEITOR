package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CronoTest {

    @Test
    void nuevoIniciarPausar() {
        assertEquals(new EstadoCrono(0, null), Crono.nuevo());
        EstadoCrono i = Crono.iniciar(Crono.nuevo(), 1_000);
        assertEquals(new EstadoCrono(0, 1_000L), i);
        assertEquals(i, Crono.iniciar(i, 5_000));
        EstadoCrono p = Crono.pausar(i, 4_000);
        assertEquals(new EstadoCrono(3_000, null), p);
        assertEquals(p, Crono.pausar(p, 9_000));
    }

    @Test
    void ms() {
        assertEquals(0L, Crono.ms(Crono.nuevo(), 99_999));
        assertEquals(63_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 4_000));
        assertEquals(3_660_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 3_601_000));
    }

    @Test
    void texto() {
        assertEquals("0:00:00", Crono.texto(0));
        assertEquals("0:01:15", Crono.texto(75_500));
        assertEquals("1:02:05", Crono.texto(3_725_000));
    }

    @Test
    void minutosDe() {
        assertEquals(1, Crono.minutosDe(29_999));
        assertEquals(2, Crono.minutosDe(90_000));
        assertEquals(75, Crono.minutosDe(4_500_000));
        assertNull(Crono.minutosDe(0));
    }

    @Test
    void duracionValida() {
        assertEquals(75, Crono.duracionValida("75"));
        assertEquals(5, Crono.duracionValida(" 5 "));
        assertNull(Crono.duracionValida("0"));
        assertNull(Crono.duracionValida("601"));
        assertNull(Crono.duracionValida("7,5"));
        assertNull(Crono.duracionValida(""));
    }
}
