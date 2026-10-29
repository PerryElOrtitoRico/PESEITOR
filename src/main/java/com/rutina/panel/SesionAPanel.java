package com.rutina.panel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rutina.registro.EjercicioPlan;
import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.EscalaEsfuerzo;
import com.rutina.registro.FilaPlan;
import com.rutina.registro.NotacionRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.SerieReal;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;
import panel.core.Registro;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Convierte una sesión anotada en el celular en un registro del panel. No lanza nunca. */
public final class SesionAPanel {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private SesionAPanel() {
    }

    public record Datos(long perfilId, long mesocicloId, int macro, int meso, String dispositivo) {
    }

    /** Exactamente uno de los dos es no nulo. */
    public record Resultado(Registro registro, String omitida) {
    }

    // Campos en el orden del detalleJson; Gson omite los nulos.
    private record EjercicioDetalle(String nombre, String planificado, String hecho, Integer esfuerzo,
                                    String esfuerzoTexto, String aviso) {
    }

    private record Detalle(String dia, List<EjercicioDetalle> ejercicios) {
    }

    private record Hecho(List<SerieReal> series, String texto, boolean ilegible) {
    }

    public static Resultado convertir(Datos datos, PlanCelular plan, SesionRegistro sesion, long ahoraMs) {
        try {
            return convertirSeguro(datos, plan, sesion, ahoraMs);
        } catch (RuntimeException e) {
            return omitida("error inesperado");
        }
    }

    private static Resultado convertirSeguro(Datos datos, PlanCelular plan, SesionRegistro sesion, long ahoraMs) {
        SesionPlan enPlan = plan.sesiones().stream()
                .filter(s -> s.microciclo() == sesion.microciclo() && s.dia() == sesion.dia())
                .findFirst().orElse(null);
        if (enPlan == null) return omitida("fuera del plan");
        Integer minutos = sesion.duracionMin();
        if (minutos == null || minutos < 1) return omitida("sin duración");
        if (minutos > 600) return omitida("duración fuera de rango");
        if (!fechaIso(sesion.fecha())) return omitida("fecha inválida");

        String dia = "Macro " + datos.macro() + " · Meso " + datos.meso() + " · Micro " + sesion.microciclo()
                + " · Día " + sesion.dia();
        Integer esfuerzo = valido(sesion.sensacionFinal()) ? sesion.sensacionFinal() : null;
        Registro registro = new Registro("esfuerzo", "rutineitor",
                "ses:" + datos.perfilId() + ":" + datos.mesocicloId() + ":" + sesion.microciclo() + ":" + sesion.dia(),
                "gimnasio", sesion.fecha(), minutos, dia, detalle(dia, enPlan, sesion), esfuerzo, null, null,
                datos.dispositivo(), ahoraMs, false);
        List<String> errores = registro.errores();
        if (!errores.isEmpty()) return omitida(String.join("; ", errores));
        return new Resultado(registro, null);
    }

    private static Resultado omitida(String motivo) {
        return new Resultado(null, motivo);
    }

    private static boolean fechaIso(String fecha) {
        if (fecha == null) return false;
        try {
            return LocalDate.parse(fecha).toString().equals(fecha);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean valido(Integer esfuerzo) {
        return esfuerzo != null && esfuerzo >= 1 && esfuerzo <= 5;
    }

    private static String detalle(String dia, SesionPlan enPlan, SesionRegistro sesion) {
        List<EjercicioRegistro> anotados = sesion.ejercicios() == null ? List.of() : sesion.ejercicios();
        List<EjercicioDetalle> ejercicios = new ArrayList<>();
        for (EjercicioPlan ej : enPlan.ejercicios()) {
            EjercicioRegistro anotado = anotados.stream()
                    .filter(a -> ej.bloque().equals(a.bloque())).findFirst().orElse(null);
            ejercicios.add(ejercicio(ej, anotado));
        }
        return GSON.toJson(new Detalle(dia, ejercicios));
    }

    private static EjercicioDetalle ejercicio(EjercicioPlan ej, EjercicioRegistro anotado) {
        Map<String, String> casillas = anotado == null ? null : anotado.filas();
        List<SerieReal> planificadas = new ArrayList<>();
        List<SerieReal> hechas = new ArrayList<>();
        List<String> partes = new ArrayList<>();
        boolean ilegible = false;
        for (FilaPlan fila : ej.filas()) {
            planificadas.addAll(fila.series());
            String texto = casillas == null ? null : casillas.get(fila.etiqueta());
            if (texto == null || texto.isBlank()) {
                hechas.addAll(fila.series());
                partes.add(FormatoSeries.texto(fila.series()));
                continue;
            }
            try {
                List<SerieReal> parseadas = NotacionRegistro.parse(texto, fila.series());
                hechas.addAll(parseadas);
                partes.add(FormatoSeries.texto(parseadas));
            } catch (IllegalArgumentException e) {
                ilegible = true;
                partes.add(texto.trim());
            }
        }
        String hecho = ilegible ? String.join(" · ", partes) : FormatoSeries.texto(hechas);
        String aviso = ilegible ? "casilla ilegible" : aviso(planificadas, hechas);
        Integer esfuerzo = null;
        String esfuerzoTexto = null;
        if (anotado != null && valido(anotado.esfuerzo())) {
            esfuerzo = anotado.esfuerzo();
            esfuerzoTexto = EscalaEsfuerzo.deValor(esfuerzo).texto();
        }
        return new EjercicioDetalle(ej.nombre(), FormatoSeries.texto(planificadas), hecho, esfuerzo, esfuerzoTexto,
                aviso);
    }

    private static String aviso(List<SerieReal> planificadas, List<SerieReal> hechas) {
        if (hechas.size() < planificadas.size()) return "bajó series";
        if (hechas.size() > planificadas.size()) return null;
        if (reps(hechas) < reps(planificadas)) return "bajó reps";
        if (reps(hechas) > reps(planificadas)) return null;
        double kgPlan = maxKg(planificadas);
        if (kgPlan > 0 && maxKg(hechas) < kgPlan) return "bajó peso";
        return null;
    }

    private static int reps(List<SerieReal> series) {
        return series.stream().mapToInt(SerieReal::reps).sum();
    }

    private static double maxKg(List<SerieReal> series) {
        return series.stream().mapToDouble(SerieReal::kg).max().orElse(0);
    }
}
