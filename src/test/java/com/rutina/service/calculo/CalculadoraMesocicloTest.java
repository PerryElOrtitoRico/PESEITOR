package com.rutina.service.calculo;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import com.rutina.service.calculo.ResultadoMesociclo.Contador;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraMesocicloTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    private static Ejercicio fuerza(long id, Double rm, int techo) {
        return new Ejercicio(id, 1, "Sentadilla " + id, Tipo.FUERZA, rm, null, techo, Map.of(), null, 0, true);
    }

    /** 2A, 3 días, fuerza 200 @ 70; un bloque complejo el día 1. */
    private static Mesociclo mesociclo(ParametrosTipo potencia, Character variante, List<Bloque> bloques) {
        return new Mesociclo(0, 1, LocalDate.of(2026, 11, 2), 2, potencia, new ParametrosTipo(200, 70),
                variante, 3, Map.of(1L, Aproximacion.COMPLEJA, 2L, Aproximacion.COMPLEJA), bloques, "", "", Map.of());
    }

    private static ResultadoMesociclo calc() throws ParametrosInvalidosException {
        return CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                List.of(new Bloque(1, 1, 1L, 100))), Map.of(1L, fuerza(1, 100.0, 100)));
    }

    @Test
    void microciclo1() throws Exception {
        var r = calc();
        var sem = r.bloques().get(0).semanas().get(0);
        assertEquals(List.of(List.of(3, 3, 3, 3, 3, 3)), sem.seriesPorZona().get(Zona.Z80));
        assertEquals(List.of(List.of(4, 4)), sem.seriesPorZona().get(Zona.Z55));
        assertEquals(80, sem.volumen());
        assertEquals(80, r.planificado().stream().filter(p -> p.tipo() == Tipo.FUERZA && p.microciclo() == 1)
                .findFirst().get().volumen());
        assertEquals(80, r.real().stream().filter(p -> p.tipo() == Tipo.FUERZA && p.microciclo() == 1)
                .findFirst().get().volumen());
    }

    @Test
    void contadorDiaUnico() throws Exception {
        var c = calc().contadores().stream()
                .filter(x -> x.tipo() == Tipo.FUERZA && x.microciclo() == 1 && x.dia() == 1).findFirst().get();
        assertEquals(80, c.planificado());
        assertEquals(80, c.asignado());
        assertEquals(0, c.sinAsignar());
    }

    @Test
    void tipoSinEjercicios() throws Exception {
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(100, 72), 'A',
                List.of(new Bloque(1, 1, 1L, 100))), Map.of(1L, fuerza(1, 100.0, 100)));
        assertEquals(40, r.contadores().stream().filter(x -> x.tipo() == Tipo.POTENCIA && x.microciclo() == 1)
                .mapToInt(Contador::sinAsignar).sum());
    }

    @Test
    void faltaVariante() {
        assertThrows(ParametrosInvalidosException.class, () -> CalculadoraMesociclo.calcular(M,
                mesociclo(new ParametrosTipo(0, 72), null, List.of()), Map.of()));
    }

    @Test
    void ejercicioSinRmNoRompe() throws Exception {
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                List.of(new Bloque(1, 1, 1L, 100))), Map.of(1L, fuerza(1, null, 100)));
        assertEquals(2, r.bloques().get(0).semanas().size());
    }

    @Test
    void avisoPctBolsa() throws Exception {
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                        List.of(new Bloque(1, 1, 1L, 60), new Bloque(1, 2, 2L, 30))),
                Map.of(1L, fuerza(1, 100.0, 100), 2L, fuerza(2, 100.0, 100)));
        assertFalse(r.avisos().isEmpty());
    }

    @Test
    void techoMandaLasZonasAltasAlOtro() throws Exception {
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                        List.of(new Bloque(1, 1, 1L, 50), new Bloque(1, 2, 2L, 50))),
                Map.of(1L, fuerza(1, 100.0, 85), 2L, fuerza(2, 100.0, 100)));
        var conTecho = r.bloques().get(0).semanas().get(0).seriesPorZona();
        var sinTecho = r.bloques().get(1).semanas().get(0).seriesPorZona();
        assertTrue(conTecho.get(Zona.Z90).get(0).isEmpty());
        // las 5 reps de 90 % van todas al que no tiene techo (± el arrastre de la zona anterior)
        int reps90 = sinTecho.get(Zona.Z90).get(0).stream().mapToInt(Integer::intValue).sum();
        assertTrue(reps90 >= 3 && reps90 <= 7, "reps al 90 %: " + reps90);
        assertTrue(sinTecho.get(Zona.Z90).get(0).stream().allMatch(s -> s == 2));
    }

    @Test
    void volumenDelDiaNoSeDisparaConNumerosChicos() throws Exception {
        // potencia 250 @ 72, 5A: dos ejercicios 50/50 solo el día 2 y otro los días 1 y 3 (caso real del usuario)
        var pot = new java.util.HashMap<Long, Ejercicio>();
        for (long i = 1; i <= 3; i++) {
            pot.put(i, new Ejercicio(i, 1, "Pot " + i, Tipo.POTENCIA, 100.0, null, 100, Map.of(), null, 0, true));
        }
        var mes = new Mesociclo(0, 1, LocalDate.of(2026, 10, 26), 5, new ParametrosTipo(250, 72), new ParametrosTipo(0, 70),
                'A', 3, Map.of(), List.of(new Bloque(1, 1, 1L, 100), new Bloque(2, 1, 2L, 50), new Bloque(2, 2, 3L, 50),
                new Bloque(3, 1, 1L, 100)), "", "", Map.of());
        for (Contador c : CalculadoraMesociclo.calcular(M, mes, pot).contadores()) {
            assertTrue(Math.abs(c.asignado() - c.planificado()) <= 4, c.toString());
        }
    }

    @Test
    void nuncaHaySeriesSobreElTechoNiFueraDeLasZonasDeLaSemana() throws Exception {
        // barrido como el del revisor: techo 80 % compartiendo la bolsa con otro sin techo
        var ejercicios = Map.of(1L, fuerza(1, 100.0, 80), 2L, fuerza(2, 100.0, 100));
        for (int vol = 30; vol <= 400; vol += 20) {
            for (int imr = 62; imr <= 75; imr++) {
                for (int dias = 3; dias <= 5; dias++) {
                    var mes = new Mesociclo(0, 1, LocalDate.of(2026, 11, 2), 4, new ParametrosTipo(0, 72),
                            new ParametrosTipo(vol, imr), 'C', dias, Map.of(),
                            List.of(new Bloque(1, 1, 1L, 50), new Bloque(1, 2, 2L, 50)), "", "", Map.of());
                    var r = CalculadoraMesociclo.calcular(M, mes, ejercicios);
                    var planes = DistribucionVolumen.porMicrociclo(M, mes.fuerza(), 4, 'C');
                    for (var rb : r.bloques()) {
                        for (int w = 0; w < 4; w++) {
                            int[] planZonas = DistribucionZonas.repsPorZona(M, planes.get(w).volumen(), planes.get(w).imr());
                            for (Zona z : Zona.values()) {
                                boolean hay = !rb.semanas().get(w).seriesPorZona().get(z).get(0).isEmpty();
                                String caso = "vol=" + vol + " imr=" + imr + " dias=" + dias + " sem=" + (w + 1) + " " + z
                                        + " " + rb.ejercicio().nombre();
                                if (hay) assertTrue(z.pct() <= rb.ejercicio().techoPct(), "sobre el techo: " + caso);
                                if (hay) assertTrue(planZonas[z.ordinal()] > 0, "zona no planificada: " + caso);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void hipertrofiaUsaPlantilla() throws Exception {
        var hip = new Ejercicio(3, 1, "Biceps", Tipo.HIPERTROFIA, null, null, 100, Map.of(), null, 0, true);
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                List.of(new Bloque(2, 1, 3L, 100))), Map.of(3L, hip));
        assertEquals("4x12", r.bloques().get(0).semanas().get(0).celda().texto());
        assertEquals("4x10", r.bloques().get(0).semanas().get(1).celda().texto());
    }

    private static double imrReal(ResultadoMesociclo.SemanaBloque sem) {
        double reps = 0, pct = 0;
        for (var en : sem.seriesPorZona().entrySet()) {
            int r = en.getValue().get(0).stream().mapToInt(Integer::intValue).sum();
            reps += r;
            pct += r * en.getKey().pct();
        }
        return pct / reps;
    }

    @Test
    void elDiaDePicoLlevaMasIntensidadAunqueTengaMenosVolumen() throws Exception {
        var deltas = new java.util.LinkedHashMap<>(M.diaDeltaImr());
        deltas.put(3, new int[]{0, -4, 5}); // día 2: 45 % del volumen; día 3: 20 % pero pico
        var m = new Metodologia(M.variantes(), M.diaPct(), deltas, M.ref(), M.repsPorSerie(), M.plantillas(),
                M.volPlio(), M.incrementoKg());
        var r = CalculadoraMesociclo.calcular(m, mesociclo(new ParametrosTipo(0, 72), 'A',
                        List.of(new Bloque(2, 1, 1L, 100), new Bloque(3, 1, 2L, 100))),
                Map.of(1L, fuerza(1, 100.0, 100), 2L, fuerza(2, 100.0, 100)));
        var dia2 = r.bloques().get(0).semanas().get(0);
        var dia3 = r.bloques().get(1).semanas().get(0);
        assertTrue(dia2.volumen() > dia3.volumen());
        assertTrue(imrReal(dia3) > imrReal(dia2) + 5, imrReal(dia2) + " vs " + imrReal(dia3));
    }
}
