package com.rutina.registro;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class T11aEscalaEsfuerzoOcultoTest {

    @Test
    void ordenYTextos() {
        assertArrayEquals(new EscalaEsfuerzo[]{EscalaEsfuerzo.MUY_LIVIANO, EscalaEsfuerzo.LIVIANO, EscalaEsfuerzo.JUSTO,
                EscalaEsfuerzo.AL_LIMITE, EscalaEsfuerzo.PESADO}, EscalaEsfuerzo.values());
        assertEquals(4, EscalaEsfuerzo.AL_LIMITE.valor());
        assertEquals("Al límite", EscalaEsfuerzo.AL_LIMITE.texto());
        assertEquals(5, EscalaEsfuerzo.PESADO.valor());
        assertEquals("Pesado", EscalaEsfuerzo.PESADO.texto());
    }

    @Test
    void deValor() {
        assertEquals(EscalaEsfuerzo.AL_LIMITE, EscalaEsfuerzo.deValor(4));
        assertEquals(EscalaEsfuerzo.PESADO, EscalaEsfuerzo.deValor(5));
        assertEquals("Justo", EscalaEsfuerzo.deValor(3).texto());
        assertThrows(IllegalArgumentException.class, () -> EscalaEsfuerzo.deValor(6));
    }

    @Test
    void javadoc() throws Exception {
        String fuente = Files.readString(Path.of("src/main/java/com/rutina/registro/EscalaEsfuerzo.java"));
        assertTrue(fuente.contains("1 = muy liviano, 5 = pesado (no salió)"));
        assertFalse(fuente.contains("5 = al límite"));
    }
}
