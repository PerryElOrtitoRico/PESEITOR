package com.rutina.service.calculo;

import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DistribucionVolumenTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    @Test
    void fuerza5A() throws Exception {
        assertEquals(List.of(new PlanMicro(74, 76), new PlanMicro(80, 75), new PlanMicro(91, 74),
                        new PlanMicro(63, 78), new PlanMicro(42, 79)),
                DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(350, 76), 5, 'A'));
    }

    @Test
    void imrFueraDeTabla() {
        var e = assertThrows(ParametrosInvalidosException.class,
                () -> DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(350, 79), 5, 'A'));
        assertTrue(e.getMessage().contains("82"), e.getMessage());
        assertTrue(e.getMessage().contains("microciclo 5"), e.getMessage());
    }

    @Test
    void varianteInexistente() {
        var e = assertThrows(ParametrosInvalidosException.class,
                () -> DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(300, 72), 5, 'C'));
        assertTrue(e.getMessage().contains("5C"), e.getMessage());
    }

    @Test
    void pesosDiasReescalados() {
        assertArrayEquals(new double[]{0.35 / 0.55, 0, 0.20 / 0.55},
                DistribucionVolumen.pesosDias(M, 3, Set.of(1, 3)), 1e-9);
    }

    @Test
    void unSoloDiaSeLlevaTodo() {
        assertArrayEquals(new double[]{0, 0, 1, 0}, DistribucionVolumen.pesosDias(M, 4, Set.of(3)), 1e-9);
    }

    @Test
    void sinDiasTodoCero() {
        assertArrayEquals(new double[]{0, 0, 0}, DistribucionVolumen.pesosDias(M, 3, Set.of()), 1e-9);
    }

    @Test
    void desvioImrDias() {
        double[] pct = {0.35, 0.45, 0.20};
        assertEquals(0, DistribucionVolumen.desvioImrDias(pct, new int[]{-1, -1, 4}), 1e-9);
        assertEquals(-2.5, DistribucionVolumen.desvioImrDias(pct, new int[]{-3, -5, 4}), 1e-9);
    }

    @Test
    void imrDiasRecortaYCompensa() {
        var m = com.rutina.service.datos.DatosIniciales.metodologia();
        double[] w = {0.35, 0.45, 0.20};
        assertArrayEquals(new int[]{71, 71, 76}, DistribucionVolumen.imrDias(m, 72, w, new int[]{-1, -1, 4}));
        // 77 + 4 = 81: el día 3 queda en 80 y el promedio se mantiene en 77 lo más cerca posible
        int[] r = DistribucionVolumen.imrDias(m, 77, w, new int[]{-1, -1, 4});
        assertEquals(80, r[2]);
        double prom = w[0] * r[0] + w[1] * r[1] + w[2] * r[2];
        assertEquals(77, prom, 0.2);
        assertArrayEquals(new int[]{77, 76, 80}, r);
        // por abajo: 61 - 3 = 58 queda en 60 y los otros bajan
        int[] b = DistribucionVolumen.imrDias(m, 61, w, new int[]{-3, 2, 1});
        assertEquals(60, b[0]);
        assertEquals(61 + (0.35 * -3 + 0.45 * 2 + 0.20 * 1), w[0] * b[0] + w[1] * b[1] + w[2] * b[2], 0.25);
    }
}
