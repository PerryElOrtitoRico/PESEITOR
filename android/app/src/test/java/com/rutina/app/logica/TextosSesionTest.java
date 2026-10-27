package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.registro.SerieReal;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextosSesionTest {

    private static final List<SerieReal> TRES =
            List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 127.5));

    @Test
    void titulo() {
        assertEquals("Microciclo 1 · Día 2", TextosSesion.tituloSesion(1, 2));
    }

    @Test
    void fecha() {
        assertEquals("Sin anotar todavía", TextosSesion.lineaFecha(null));
        assertEquals("Anotada el 27/10/2026", TextosSesion.lineaFecha("2026-10-27"));
    }

    @Test
    void reps() {
        assertEquals("10 / 11 reps", TextosSesion.repsTexto(new int[]{11, 10}));
        assertEquals("— / 11 reps", TextosSesion.repsTexto(new int[]{11, -1}));
    }

    @Test
    void error() {
        assertNull(TextosSesion.errorCasilla("", TRES));
        assertNull(TextosSesion.errorCasilla("3.3.3", TRES));
        assertEquals("serie inválida: \"x\" (reps de 1 a 99, kg con coma)", TextosSesion.errorCasilla("x", TRES));
        assertEquals("falta una serie entre puntos en \"3..3\"", TextosSesion.errorCasilla("3..3", TRES));
    }

    @Test
    void ojo() {
        assertNull(TextosSesion.ojoCasilla("3.3.3", TRES));
        assertNull(TextosSesion.ojoCasilla("x", TRES));
        assertEquals("Ojo: 4 series (planificadas: 3). Los decimales del peso van con coma.", TextosSesion.ojoCasilla("3.3.3.3-50", TRES));
    }
}
