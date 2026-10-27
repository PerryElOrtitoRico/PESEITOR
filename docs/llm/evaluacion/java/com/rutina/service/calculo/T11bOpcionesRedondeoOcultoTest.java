package com.rutina.service.calculo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class T11bOpcionesRedondeoOcultoTest {

    @Test
    void textosYValores() {
        assertArrayEquals(new String[]{"1 kg", "2,5 kg", "5 kg"}, OpcionesRedondeo.TEXTOS);
        assertArrayEquals(new double[]{1.0, 2.5, 5.0}, OpcionesRedondeo.VALORES);
    }

    @Test
    void indiceDeValorConocido() {
        assertEquals(0, OpcionesRedondeo.indiceDe(1.0));
        assertEquals(1, OpcionesRedondeo.indiceDe(2.5));
        assertEquals(2, OpcionesRedondeo.indiceDe(5.0));
    }

    @Test
    void indiceDeValorDesconocidoEsDosYMedio() {
        assertEquals(1, OpcionesRedondeo.indiceDe(3.0));
        assertEquals(1, OpcionesRedondeo.indiceDe(0.0));
        assertEquals(1, OpcionesRedondeo.indiceDe(-5.0));
    }

    @Test
    void valorDeIndice() {
        assertEquals(1.0, OpcionesRedondeo.valorDe(0));
        assertEquals(2.5, OpcionesRedondeo.valorDe(1));
        assertEquals(5.0, OpcionesRedondeo.valorDe(2));
    }

    @Test
    void idaYVuelta() {
        for (int i = 0; i < OpcionesRedondeo.VALORES.length; i++) {
            assertEquals(i, OpcionesRedondeo.indiceDe(OpcionesRedondeo.valorDe(i)));
        }
    }
}
