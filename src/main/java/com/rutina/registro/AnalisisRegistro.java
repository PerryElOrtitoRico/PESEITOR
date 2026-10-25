package com.rutina.registro;

import com.rutina.model.Tipo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Compara lo planificado (plan del celular) con lo realizado (registro), sesión por sesión (sección 7 de la especificación). */
public final class AnalisisRegistro {

    private AnalisisRegistro() {
    }

    /** Acumula volumen y Σ(reps × intensidad) de un tipo. */
    private static final class Acumulador {
        int volumen;
        int repsConIntensidad;
        double sumaIntensidad;

        void sumar(SerieReal s, Double rm, FilaPlan fila) {
            volumen += s.reps();
            Double intensidad = intensidad(s, rm, fila);
            if (intensidad != null) {
                repsConIntensidad += s.reps();
                sumaIntensidad += s.reps() * intensidad;
            }
        }

        Double imr() {
            return repsConIntensidad == 0 ? null : sumaIntensidad / repsConIntensidad;
        }
    }

    /**
     * Un ResumenSesion por cada sesión del registro que exista en el plan, ordenados por (microciclo, día).
     *
     * @throws IllegalArgumentException si plan y registro son de distinto mesocicloId, o si una casilla tiene
     *                                  una notación inválida (el mensaje dice microciclo, día, bloque y etiqueta).
     */
    public static List<ResumenSesion> analizar(PlanCelular plan, RegistroCelular registro) {
        if (plan.mesocicloId() != registro.mesocicloId()) {
            throw new IllegalArgumentException("El registro es del mesociclo " + registro.mesocicloId()
                    + " y el plan del mesociclo " + plan.mesocicloId());
        }
        List<ResumenSesion> res = new ArrayList<>();
        for (SesionRegistro sr : registro.sesiones()) {
            SesionPlan sp = plan.sesiones().stream()
                    .filter(s -> s.microciclo() == sr.microciclo() && s.dia() == sr.dia())
                    .findFirst().orElse(null);
            if (sp != null) res.add(resumir(sp, sr));
        }
        res.sort(Comparator.comparingInt(ResumenSesion::microciclo).thenComparingInt(ResumenSesion::dia));
        return List.copyOf(res);
    }

    private static ResumenSesion resumir(SesionPlan sp, SesionRegistro sr) {
        List<EjercicioRegistro> anotados = sr.ejercicios() == null ? List.of() : sr.ejercicios();
        Map<Tipo, Acumulador> plan = new EnumMap<>(Tipo.class);
        Map<Tipo, Acumulador> real = new EnumMap<>(Tipo.class);
        for (EjercicioPlan ej : sp.ejercicios()) {
            Map<String, String> casillas = anotados.stream()
                    .filter(e -> ej.bloque().equals(e.bloque()))
                    .findFirst()
                    .map(EjercicioRegistro::filas)
                    .orElse(null);
            Acumulador ap = plan.computeIfAbsent(ej.tipo(), t -> new Acumulador());
            Acumulador ar = real.computeIfAbsent(ej.tipo(), t -> new Acumulador());
            for (FilaPlan fila : ej.filas()) {
                for (SerieReal s : fila.series()) ap.sumar(s, ej.rm(), fila);
                String texto = casillas == null ? null : casillas.get(fila.etiqueta());
                List<SerieReal> hechas;
                if (texto == null || texto.isBlank()) {
                    hechas = fila.series();
                } else {
                    try {
                        hechas = NotacionRegistro.parse(texto, fila.series());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Microciclo " + sp.microciclo() + ", día " + sp.dia()
                                + ", ejercicio " + ej.bloque() + " (" + ej.nombre() + "), fila " + fila.etiqueta()
                                + ": " + e.getMessage(), e);
                    }
                }
                for (SerieReal s : hechas) ar.sumar(s, ej.rm(), fila);
            }
        }
        List<ComparacionTipo> porTipo = new ArrayList<>();
        for (var entrada : plan.entrySet()) {   // EnumMap: en el orden del enum
            Acumulador r = real.get(entrada.getKey());
            porTipo.add(new ComparacionTipo(entrada.getKey(), entrada.getValue().volumen, r.volumen,
                    entrada.getValue().imr(), r.imr()));
        }
        Double esfuerzo = anotados.stream()
                .map(EjercicioRegistro::esfuerzo)
                .filter(e -> e != null)
                .mapToInt(Integer::intValue)
                .average()
                .stream().boxed().findFirst().orElse(null);
        return new ResumenSesion(sr.microciclo(), sr.dia(), sr.fecha(), sr.descanso(), sr.sensacionFinal(),
                esfuerzo, List.copyOf(porTipo));
    }

    /** kg / rm × 100 si hay rm y kg; si no, el medio del porcentaje de la fila; null si tampoco hay. */
    private static Double intensidad(SerieReal s, Double rm, FilaPlan fila) {
        if (rm != null && rm > 0 && s.kg() > 0) return s.kg() / rm * 100;
        if (fila.pctMin() != null && fila.pctMax() != null) return (fila.pctMin() + fila.pctMax()) / 2.0;
        return null;
    }
}
