package com.rutina.registro;

import com.rutina.model.Aproximacion;
import com.rutina.model.CeldaPlantilla;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.Zona;
import com.rutina.service.calculo.Redondeo;
import com.rutina.service.calculo.ResultadoMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo.ResultadoBloque;
import com.rutina.service.calculo.ResultadoMesociclo.SemanaBloque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Arma el {@link PlanCelular} a partir de un mesociclo ya calculado (sección 4 de la especificación). */
public final class ArmadorPlan {

    /** Grupos de zonas de la aproximación compleja, en orden: etiqueta y zonas. */
    private record Grupo(String etiqueta, List<Zona> zonas) {
    }

    private static final List<Grupo> GRUPOS = List.of(
            new Grupo("55", List.of(Zona.Z55)),
            new Grupo("60", List.of(Zona.Z60)),
            new Grupo("65", List.of(Zona.Z65)),
            new Grupo("70", List.of(Zona.Z70)),
            new Grupo("75", List.of(Zona.Z75)),
            new Grupo("80-85", List.of(Zona.Z80, Zona.Z85)),
            new Grupo("90-95", List.of(Zona.Z90, Zona.Z95)),
            new Grupo("100", List.of(Zona.Z100)));

    private ArmadorPlan() {
    }

    /** mes.dias() no es null (el mesociclo ya se calculó). */
    public static PlanCelular armar(String perfil, Mesociclo mes, ResultadoMesociclo res, Metodologia m) {
        int dias = mes.dias();
        List<ResultadoBloque> bloques = res.bloques().stream()
                .sorted(Comparator.comparingInt((ResultadoBloque b) -> b.bloque().dia())
                        .thenComparingInt(b -> b.bloque().posicion()))
                .toList();
        List<SesionPlan> sesiones = new ArrayList<>();
        for (int micro = 1; micro <= mes.microciclos(); micro++) {
            for (int dia = 1; dia <= dias; dia++) {
                List<EjercicioPlan> ejercicios = new ArrayList<>();
                for (ResultadoBloque rb : bloques) {
                    if (rb.bloque().dia() != dia || micro > rb.semanas().size()) continue;
                    ejercicios.add(ejercicio(mes, rb, rb.semanas().get(micro - 1), m.incrementoKg()));
                }
                sesiones.add(new SesionPlan(micro, dia, List.copyOf(ejercicios)));
            }
        }
        return new PlanCelular(PlanCelular.FORMATO, PlanCelular.VERSION, perfil, mes.id(), mes.inicio().toString(),
                mes.microciclos(), dias, List.copyOf(sesiones));
    }

    private static EjercicioPlan ejercicio(Mesociclo mes, ResultadoBloque rb, SemanaBloque semana, double inc) {
        var ej = rb.ejercicio();
        Double rm = mes.rmUsados().getOrDefault(ej.id(), ej.rm());
        List<FilaPlan> filas = rb.aprox() == Aproximacion.COMPLEJA || semana.celda() == null
                ? filasComplejas(semana, rm, inc)
                : List.of(filaBasica(semana.celda(), rm, inc));
        return new EjercicioPlan(rb.bloque().dia() + "-" + rb.bloque().posicion(), ej.nombre(), ej.tipo(), rb.aprox(), rm, filas);
    }

    private static List<FilaPlan> filasComplejas(SemanaBloque semana, Double rm, double inc) {
        List<FilaPlan> filas = new ArrayList<>();
        for (Grupo g : GRUPOS) {
            List<SerieReal> series = new ArrayList<>();
            for (Zona z : g.zonas()) {
                double kg = rm == null ? 0 : Redondeo.kg(rm, z.pct(), inc);
                List<List<Integer>> listas = semana.seriesPorZona().getOrDefault(z, List.of());
                for (List<Integer> lista : listas) {
                    for (int reps : lista) series.add(new SerieReal(reps, kg));
                }
            }
            if (!series.isEmpty()) {
                int min = g.zonas().get(0).pct();
                int max = g.zonas().get(g.zonas().size() - 1).pct();
                filas.add(new FilaPlan(g.etiqueta(), min, max, List.copyOf(series)));
            }
        }
        return List.copyOf(filas);
    }

    private static FilaPlan filaBasica(CeldaPlantilla celda, Double rm, double inc) {
        double kg = rm != null && celda.pctMin() != null
                ? Redondeo.kg(rm, (celda.pctMin() + celda.pctMax()) / 2.0, inc)
                : 0;
        return new FilaPlan(celda.texto(), celda.pctMin(), celda.pctMax(),
                Collections.nCopies(celda.series(), new SerieReal(celda.reps(), kg)));
    }
}
