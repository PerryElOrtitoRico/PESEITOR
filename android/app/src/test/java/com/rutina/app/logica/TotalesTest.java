package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.registro.FilaPlan;
import com.rutina.registro.SerieReal;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TotalesTest {

    private static final List<FilaPlan> FILAS = List.of(
            new FilaPlan("A", null, null, List.of(new SerieReal(3, 80), new SerieReal(3, 80))),
            new FilaPlan("B", null, null, List.of(new SerieReal(5, 60))));

    @Test
    void totalReps() {
        assertEquals(0, Totales.totalReps(List.of()));
        assertEquals(11, Totales.totalReps(List.of(new SerieReal(3, 80), new SerieReal(3, 80), new SerieReal(5, 0))));
    }

    @Test
    void totalesEjercicio() {
        assertArrayEquals(new int[]{11, 11}, Totales.totalesEjercicio(FILAS, Map.of()));
        assertArrayEquals(new int[]{11, 10}, Totales.totalesEjercicio(FILAS, Map.of("A", "3.2")));
        assertArrayEquals(new int[]{11, 6}, Totales.totalesEjercicio(FILAS, Map.of("B", "0")));
        assertArrayEquals(new int[]{11, -1}, Totales.totalesEjercicio(FILAS, Map.of("A", "x")));
    }
}
