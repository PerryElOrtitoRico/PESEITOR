package com.rutina.registro;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import com.rutina.service.calculo.CalculadoraMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArmadorPlanTest {

    private static final Metodologia M = DatosIniciales.metodologia();
    private static ResultadoMesociclo res;
    private static PlanCelular plan;

    @BeforeAll
    static void armar() throws Exception {
        var sentadilla = new Ejercicio(1, 1, "Sentadilla", Tipo.FUERZA, 150.0, null, 100, Map.of(), null, 0, true);
        var biceps = new Ejercicio(2, 1, "Biceps", Tipo.HIPERTROFIA, null, null, 100, Map.of(), null, 0, true);
        var mes = new Mesociclo(7, 1, LocalDate.of(2026, 10, 26), 2, new ParametrosTipo(0, 72), new ParametrosTipo(200, 70),
                'A', 3, Map.of(1L, Aproximacion.COMPLEJA), List.of(new Bloque(1, 1, 1L, 100), new Bloque(2, 1, 2L, 100)),
                "", "", Map.of());
        res = CalculadoraMesociclo.calcular(M, mes, Map.of(1L, sentadilla, 2L, biceps));
        plan = ArmadorPlan.armar("Franco", mes, res, M);
    }

    @Test
    void seisSesionesYLaTerceraVacia() {
        assertEquals(6, plan.sesiones().size());
        var tercera = plan.sesiones().get(2);
        assertEquals(1, tercera.microciclo());
        assertEquals(3, tercera.dia());
        assertTrue(tercera.ejercicios().isEmpty());
        assertEquals("2026-10-26", plan.inicio());
        assertEquals(PlanCelular.FORMATO, plan.formato());
    }

    @Test
    void ejercicioComplejo() {
        var ej = plan.sesiones().get(0).ejercicios().get(0);
        assertEquals("1-1", ej.bloque());
        assertEquals(Aproximacion.COMPLEJA, ej.modo());
        assertEquals(150.0, ej.rm());
    }

    @Test
    void fila80_85ConcatenaZonasConSuKg() {
        var semana = res.bloques().get(0).semanas().get(0);
        List<SerieReal> esperadas = new ArrayList<>();
        for (int reps : semana.seriesPorZona().get(Zona.Z80).get(0)) esperadas.add(new SerieReal(reps, 120.0));
        for (int reps : semana.seriesPorZona().get(Zona.Z85).get(0)) esperadas.add(new SerieReal(reps, 127.5));
        var fila = plan.sesiones().get(0).ejercicios().get(0).filas().stream()
                .filter(f -> f.etiqueta().equals("80-85")).findFirst().orElseThrow();
        assertFalse(esperadas.isEmpty());
        assertEquals(esperadas, fila.series());
        assertEquals(80, fila.pctMin());
        assertEquals(85, fila.pctMax());
    }

    @Test
    void sinFilasVacias() {
        plan.sesiones().forEach(s -> s.ejercicios().forEach(e -> e.filas().forEach(f -> assertFalse(f.series().isEmpty()))));
    }

    @Test
    void bicepsBasico() {
        var s = plan.sesiones().get(4);
        assertEquals(2, s.microciclo());
        assertEquals(2, s.dia());
        var biceps = s.ejercicios().get(0);
        assertEquals(1, biceps.filas().size());
        var fila = biceps.filas().get(0);
        assertEquals("4x10", fila.etiqueta());
        assertEquals(List.of(new SerieReal(10, 0.0), new SerieReal(10, 0.0), new SerieReal(10, 0.0), new SerieReal(10, 0.0)),
                fila.series());
    }
}
