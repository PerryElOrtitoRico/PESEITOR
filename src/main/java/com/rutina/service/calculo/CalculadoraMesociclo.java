package com.rutina.service.calculo;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.CeldaPlantilla;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.Plantilla;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import com.rutina.service.calculo.ResultadoMesociclo.Contador;
import com.rutina.service.calculo.ResultadoMesociclo.PuntoGrafico;
import com.rutina.service.calculo.ResultadoMesociclo.ResultadoBloque;
import com.rutina.service.calculo.ResultadoMesociclo.SemanaBloque;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Arma el mesociclo completo a partir de sus parámetros y su grilla (spec §6). */
public final class CalculadoraMesociclo {

    private static final Zona[] ZONAS = Zona.values();

    private CalculadoraMesociclo() {
    }

    public static ResultadoMesociclo calcular(Metodologia m, Mesociclo mes, Map<Long, Ejercicio> ejercicios)
            throws ParametrosInvalidosException {
        if (mes.variante() == null || mes.dias() == null) {
            throw new ParametrosInvalidosException("Elegí la variante y los días por microciclo para armar el mesociclo.");
        }
        int dias = mes.dias();
        int n = mes.microciclos();
        List<String> avisos = new ArrayList<>();

        // Bloques válidos, ordenados por día y posición
        List<Bloque> bloques = new ArrayList<>();
        List<Ejercicio> ejs = new ArrayList<>();
        List<Aproximacion> aprox = new ArrayList<>();
        for (Bloque b : mes.bloques().stream()
                .sorted(Comparator.comparingInt(Bloque::dia).thenComparingInt(Bloque::posicion)).toList()) {
            Ejercicio e = ejercicios.get(b.ejercicioId());
            if (e == null) {
                avisos.add("Hay un bloque con un ejercicio que ya no existe (día " + b.dia() + ").");
            } else if (b.dia() < 1 || b.dia() > dias) {
                avisos.add(e.nombre() + " está en el día " + b.dia() + ", pero el microciclo tiene " + dias + " días.");
            } else if (e.tipo() == Tipo.PLIOMETRIA) {
                avisos.add(e.nombre() + " es de pliometría: se programa en un plan de pliometría, no en la grilla.");
            } else {
                bloques.add(b);
                ejs.add(e);
                aprox.add(aproximacion(e, mes));
            }
        }

        List<List<SemanaBloque>> semanas = new ArrayList<>();
        for (int k = 0; k < bloques.size(); k++) semanas.add(new ArrayList<>());
        List<Contador> contadores = new ArrayList<>();
        List<PuntoGrafico> planificado = new ArrayList<>();
        List<PuntoGrafico> real = new ArrayList<>();

        for (Tipo tipo : List.of(Tipo.POTENCIA, Tipo.FUERZA)) {
            List<Integer> idxs = new ArrayList<>();
            for (int k = 0; k < bloques.size(); k++) {
                if (ejs.get(k).tipo() == tipo && aprox.get(k) == Aproximacion.COMPLEJA) idxs.add(k);
            }
            ParametrosTipo p = tipo == Tipo.POTENCIA ? mes.potencia() : mes.fuerza();
            if (p == null || p.volumen() <= 0) {
                for (int k : idxs) for (int i = 0; i < n; i++) semanas.get(k).add(vacia());
                continue;
            }
            List<PlanMicro> planes = DistribucionVolumen.porMicrociclo(m, p, n, mes.variante());
            avisarPctBolsa(tipo, bloques, idxs, avisos);
            calcularCompleja(m, tipo, dias, planes, bloques, ejs, idxs, semanas, contadores, planificado, real, avisos);
        }

        calcularPlantillas(m, n, bloques, ejs, aprox, semanas, avisos);

        List<ResultadoBloque> res = new ArrayList<>();
        for (int k = 0; k < bloques.size(); k++) {
            res.add(new ResultadoBloque(bloques.get(k), ejs.get(k), aprox.get(k), semanas.get(k)));
        }
        return new ResultadoMesociclo(res, contadores, planificado, real, avisos);
    }

    private static Aproximacion aproximacion(Ejercicio e, Mesociclo mes) {
        return switch (e.tipo()) {
            case POTENCIA -> Aproximacion.COMPLEJA;
            case FUERZA -> mes.aproximaciones().getOrDefault(e.id(), Aproximacion.COMPLEJA);
            default -> Aproximacion.BASICA;
        };
    }

    private static void calcularCompleja(Metodologia m, Tipo tipo, int dias, List<PlanMicro> planes,
                                         List<Bloque> bloques, List<Ejercicio> ejs, List<Integer> idxs,
                                         List<List<SemanaBloque>> semanas, List<Contador> contadores,
                                         List<PuntoGrafico> planificado, List<PuntoGrafico> real,
                                         List<String> avisos) {
        Set<Integer> diasConTipo = new TreeSet<>();
        for (int k : idxs) diasConTipo.add(bloques.get(k).dia());
        double[] pesos = DistribucionVolumen.pesosDias(m, dias, diasConTipo);
        int[] deltas = m.diaDeltaImr().getOrDefault(dias, new int[dias]);

        for (int i = 0; i < planes.size(); i++) {
            PlanMicro plan = planes.get(i);
            int[][] repsBloqueZona = new int[bloques.size()][ZONAS.length];
            int[] planDia = DistribucionZonas.repartirEnDias(plan.volumen(), pesos);
            int[] sinDia = new int[dias];

            // Cada día reparte su volumen en zonas con su propio IMR (IMR del microciclo + ΔIMR del día),
            // así el día de pico no es necesariamente el de más volumen
            for (int d = 0; d < dias; d++) {
                if (planDia[d] == 0) continue;
                int imrDia = DistribucionVolumen.imrEnTabla(m, plan.imr() + deltas[d]);
                if (imrDia != plan.imr() + deltas[d]) {
                    avisos.add(nombre(tipo) + ", microciclo " + (i + 1) + ", día " + (d + 1) + ": el IMR "
                            + (plan.imr() + deltas[d]) + " % queda fuera de la tabla, se usa " + imrDia + " %.");
                }
                int[] zonas = DistribucionZonas.repsPorZona(m, planDia[d], imrDia);
                for (int z = 0; z < ZONAS.length; z++) {
                    if (zonas[z] == 0) continue;
                    List<Participante> ps = new ArrayList<>();
                    for (int k : idxs) {
                        if (bloques.get(k).dia() == d + 1) {
                            ps.add(new Participante(k, bloques.get(k).pctBolsa(), ejs.get(k).techoPct()));
                        }
                    }
                    ResultadoReparto rr = RepartoEjercicios.repartir(zonas[z], ZONAS[z], ps);
                    sinDia[d] += rr.sinAsignar();
                    for (Map.Entry<Long, Integer> en : rr.porEjercicio().entrySet()) {
                        repsBloqueZona[en.getKey().intValue()][z] += en.getValue();
                    }
                }
            }

            // Series de cada bloque: el resto de una zona pasa a la zona siguiente del mismo día,
            // así el volumen del día no se dispara cuando cada zona tiene pocas repeticiones
            Map<Integer, Map<Zona, List<List<Integer>>>> series = new LinkedHashMap<>();
            for (int k : idxs) {
                Ejercicio e = ejs.get(k);
                int[] s = new int[ZONAS.length];
                for (int z = 0; z < ZONAS.length; z++) s[z] = e.repsPorSerie(ZONAS[z], m);
                SeriesZona sz = GeneradorSeries.generar(repsBloqueZona[k], s);
                Map<Zona, List<List<Integer>>> porZona = new EnumMap<>(Zona.class);
                for (int z = 0; z < ZONAS.length; z++) porZona.put(ZONAS[z], List.of(sz.porPaso().get(z)));
                series.put(k, porZona);
            }

            int totalReps = 0;
            double totalPct = 0;
            int[] asignadoDia = new int[dias];
            for (int k : idxs) {
                int vol = 0;
                for (Map.Entry<Zona, List<List<Integer>>> en : series.get(k).entrySet()) {
                    int reps = en.getValue().get(0).stream().mapToInt(Integer::intValue).sum();
                    vol += reps;
                    totalPct += reps * en.getKey().pct();
                }
                totalReps += vol;
                asignadoDia[bloques.get(k).dia() - 1] += vol;
                semanas.get(k).add(new SemanaBloque(vol, series.get(k), null));
            }

            if (diasConTipo.isEmpty()) {
                contadores.add(new Contador(tipo, i + 1, 0, plan.volumen(), 0, plan.volumen()));
            } else {
                for (int d : diasConTipo) {
                    contadores.add(new Contador(tipo, i + 1, d, planDia[d - 1], asignadoDia[d - 1], sinDia[d - 1]));
                }
            }
            planificado.add(new PuntoGrafico(tipo, i + 1, plan.volumen(), plan.imr()));
            real.add(new PuntoGrafico(tipo, i + 1, totalReps, totalReps > 0 ? totalPct / totalReps : 0));
        }
    }

    private static void calcularPlantillas(Metodologia m, int n, List<Bloque> bloques, List<Ejercicio> ejs,
                                           List<Aproximacion> aprox, List<List<SemanaBloque>> semanas,
                                           List<String> avisos) {
        Map<Long, List<Integer>> porEjercicio = new LinkedHashMap<>();
        for (int k = 0; k < bloques.size(); k++) {
            if (aprox.get(k) == Aproximacion.BASICA) {
                porEjercicio.computeIfAbsent(bloques.get(k).ejercicioId(), x -> new ArrayList<>()).add(k);
            }
        }
        for (List<Integer> apariciones : porEjercicio.values()) {
            Ejercicio e = ejs.get(apariciones.get(0));
            Tipo tipo = e.tipo() == Tipo.HIPERTROFIA ? Tipo.HIPERTROFIA : Tipo.FUERZA;
            Plantilla p;
            try {
                p = Plantillas.obtener(m, tipo, n, apariciones.size());
            } catch (IllegalArgumentException ex) {
                avisos.add(e.nombre() + ": " + ex.getMessage());
                for (int k : apariciones) for (int i = 0; i < n; i++) semanas.get(k).add(vacia());
                continue;
            }
            for (int a = 0; a < apariciones.size(); a++) {
                for (int w = 0; w < n; w++) {
                    CeldaPlantilla c = p.semanas().get(w).get(a);
                    semanas.get(apariciones.get(a)).add(new SemanaBloque(c.series() * c.reps(), Map.of(), c));
                }
            }
        }
    }

    private static void avisarPctBolsa(Tipo tipo, List<Bloque> bloques, List<Integer> idxs, List<String> avisos) {
        Map<Integer, Double> suma = new LinkedHashMap<>();
        for (int k : idxs) suma.merge(bloques.get(k).dia(), bloques.get(k).pctBolsa(), Double::sum);
        suma.forEach((dia, s) -> {
            if (Math.abs(s - 100) > 0.01) {
                avisos.add(nombre(tipo) + ", día " + dia + ": los % de la bolsa suman "
                        + Math.round(s) + " % (se reparte en proporción).");
            }
        });
    }

    private static SemanaBloque vacia() {
        Map<Zona, List<List<Integer>>> m = new EnumMap<>(Zona.class);
        for (Zona z : ZONAS) m.put(z, List.of(List.of()));
        return new SemanaBloque(0, m, null);
    }

    private static String nombre(Tipo t) {
        String s = t.name().toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
