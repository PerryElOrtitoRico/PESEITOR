package com.rutina.service.calculo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpcionesRedondeoTest {

    @Test
    void indice() {
        assertEquals(0, OpcionesRedondeo.indiceDe(1.0));
        assertEquals(1, OpcionesRedondeo.indiceDe(2.5));
        assertEquals(2, OpcionesRedondeo.indiceDe(5.0));
        assertEquals(1, OpcionesRedondeo.indiceDe(3.0));
    }

    @Test
    void valor() {
        assertEquals(1.0, OpcionesRedondeo.valorDe(0));
        assertEquals(2.5, OpcionesRedondeo.valorDe(1));
        assertEquals(5.0, OpcionesRedondeo.valorDe(2));
    }
}
