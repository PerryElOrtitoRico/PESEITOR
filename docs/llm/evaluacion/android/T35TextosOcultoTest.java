package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.registro.SerieReal;

import java.util.List;

import org.junit.jupiter.api.Test;

class T35TextosOcultoTest {

    private static final List<SerieReal> TRES =
            List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 127.5));

    @Test
    void titulo() {
        assertEquals("Microciclo 1 · Día 2", TextosSesion.tituloSesion(1, 2));
        assertEquals("Microciclo 12 · Día 3", TextosSesion.tituloSesion(12, 3));
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
        assertEquals("0 / 11 reps", TextosSesion.repsTexto(new int[]{11, 0}));
        assertEquals("36 / 36 reps", TextosSesion.repsTexto(new int[]{36, 36}));
    }

    @Test
    void error() {
        assertNull(TextosSesion.errorCasilla(null, TRES));
        assertNull(TextosSesion.errorCasilla("", TRES));
        assertNull(TextosSesion.errorCasilla("   ", TRES));
        assertNull(TextosSesion.errorCasilla("3.3.3", TRES));
        assertNull(TextosSesion.errorCasilla("0", TRES));
        assertNull(TextosSesion.errorCasilla("3.3.3.3-50", TRES));
        assertEquals("serie inválida: \"x\" (reps de 1 a 99, kg con coma)", TextosSesion.errorCasilla("x", TRES));
        assertEquals("falta una serie entre puntos en \"3..3\"", TextosSesion.errorCasilla("3..3", TRES));
        assertEquals("el kg tiene que ser mayor que 0: \"3-0\"", TextosSesion.errorCasilla("3-0", TRES));
        assertEquals("indicá el peso de la serie \"3\"", TextosSesion.errorCasilla("3", List.of()));
        assertNotNull(TextosSesion.errorCasilla("100", TRES));
    }

    @Test
    void ojo() {
        assertNull(TextosSesion.ojoCasilla(null, TRES));
        assertNull(TextosSesion.ojoCasilla("", TRES));
        assertNull(TextosSesion.ojoCasilla("  ", TRES));
        assertNull(TextosSesion.ojoCasilla("3.3.3", TRES));
        assertNull(TextosSesion.ojoCasilla("3.3", TRES));
        assertNull(TextosSesion.ojoCasilla("0", TRES));
        assertNull(TextosSesion.ojoCasilla("x", TRES));
        assertEquals("Ojo: 4 series (planificadas: 3). Los decimales del peso van con coma.",
                TextosSesion.ojoCasilla("3.3.3.3-50", TRES));
        assertEquals("Ojo: 2 series (planificadas: 1). Los decimales del peso van con coma.",
                TextosSesion.ojoCasilla("5.5", List.of(new SerieReal(5, 60))));
    }
}
