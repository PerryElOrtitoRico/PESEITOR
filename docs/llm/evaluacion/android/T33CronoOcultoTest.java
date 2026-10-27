package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class T33CronoOcultoTest {

    @Test
    void nuevoIniciarPausar() {
        EstadoCrono n = Crono.nuevo();
        assertEquals(new EstadoCrono(0, null), n);
        EstadoCrono i = Crono.iniciar(n, 1_000);
        assertEquals(new EstadoCrono(0, 1_000L), i);
        assertEquals(i, Crono.iniciar(i, 5_000));
        EstadoCrono p = Crono.pausar(i, 4_000);
        assertEquals(new EstadoCrono(3_000, null), p);
        assertEquals(p, Crono.pausar(p, 9_000));
        EstadoCrono j = Crono.iniciar(p, 10_000);
        assertEquals(new EstadoCrono(3_000, 10_000L), j);
        assertEquals(new EstadoCrono(5_500, null), Crono.pausar(j, 12_500));
        assertEquals(new EstadoCrono(0, null), n);
    }

    @Test
    void ms() {
        assertEquals(0L, Crono.ms(Crono.nuevo(), 99_999));
        assertEquals(60_000L, Crono.ms(new EstadoCrono(60_000, null), 99_999));
        assertEquals(63_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 4_000));
        assertEquals(3_660_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 3_601_000));
    }

    @Test
    void texto() {
        assertEquals("0:00:00", Crono.texto(0));
        assertEquals("0:01:15", Crono.texto(75_500));
        assertEquals("1:02:05", Crono.texto(3_725_000));
        assertEquals("0:00:59", Crono.texto(59_999));
        assertEquals("10:00:00", Crono.texto(36_000_000));
    }

    @Test
    void minutosDe() {
        assertEquals(1, Crono.minutosDe(29_999));
        assertEquals(2, Crono.minutosDe(90_000));
        assertEquals(75, Crono.minutosDe(4_500_000));
        assertNull(Crono.minutosDe(0));
        assertNull(Crono.minutosDe(-5));
        assertEquals(1, Crono.minutosDe(1));
        assertEquals(1, Crono.minutosDe(60_000));
    }

    @Test
    void duracionValida() {
        assertEquals(75, Crono.duracionValida("75"));
        assertEquals(5, Crono.duracionValida(" 5 "));
        assertEquals(1, Crono.duracionValida("1"));
        assertEquals(600, Crono.duracionValida("600"));
        assertEquals(75, Crono.duracionValida("0075"));
        assertNull(Crono.duracionValida("0"));
        assertNull(Crono.duracionValida("601"));
        assertNull(Crono.duracionValida("7,5"));
        assertNull(Crono.duracionValida("7.5"));
        assertNull(Crono.duracionValida(""));
        assertNull(Crono.duracionValida("   "));
        assertNull(Crono.duracionValida(null));
        assertNull(Crono.duracionValida("-5"));
        assertNull(Crono.duracionValida("abc"));
        assertNull(Crono.duracionValida("99999999999999999999999"));
    }
}
