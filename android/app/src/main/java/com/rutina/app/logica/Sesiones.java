package com.rutina.app.logica;

import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SerieReal;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lógica pura de la sesión del celular (traducción de celular/logica.js). Nada modifica sus argumentos. */
public final class Sesiones {

    private Sesiones() {
    }

    /** La sesión del plan con ese microciclo y día, o null. */
    public static SesionPlan sesionDelPlan(PlanCelular plan, int micro, int dia) {
        for (SesionPlan s : plan.sesiones()) {
            if (s.microciclo() == micro && s.dia() == dia) {
                return s;
            }
        }
        return null;
    }

    /** La sesión del registro con ese microciclo y día, o null. */
    public static SesionRegistro sesionDelRegistro(RegistroCelular registro, int micro, int dia) {
        for (SesionRegistro s : registro.sesiones()) {
            if (s.microciclo() == micro && s.dia() == dia) {
                return s;
            }
        }
        return null;
    }

    /** Registro sin sesiones para ese plan (mismo perfil y mesocicloId). */
    public static RegistroCelular registroVacio(PlanCelular plan) {
        return new RegistroCelular(RegistroCelular.FORMATO, RegistroCelular.VERSION, plan.perfil(), plan.mesocicloId(), new ArrayList<>());
    }

    /** Copia de la sesión sin ruido: filas en blanco fuera, textos recortados, nota recortada (en blanco → null);
     *  un ejercicio se conserva si tiene filas, esfuerzo o nota. */
    public static SesionRegistro limpiarSesion(SesionRegistro s) {
        List<EjercicioRegistro> limpios = new ArrayList<>();
        for (EjercicioRegistro e : s.ejercicios()) {
            Map<String, String> filas = new LinkedHashMap<>();
            for (Map.Entry<String, String> f : e.filas().entrySet()) {
                if (f.getValue() != null && !f.getValue().isBlank()) {
                    filas.put(f.getKey(), f.getValue().strip());
                }
            }
            String nota = (e.nota() == null || e.nota().isBlank()) ? null : e.nota().strip();
            if (filas.isEmpty() && e.esfuerzo() == null && nota == null) {
                continue;
            }
            limpios.add(new EjercicioRegistro(e.bloque(), e.esfuerzo(), filas, nota));
        }
        return new SesionRegistro(s.microciclo(), s.dia(), s.fecha(), s.descanso(), s.sensacionFinal(), limpios, s.duracionMin());
    }

    /** Registro nuevo con la sesión (limpia) agregada o reemplazada, ordenadas por microciclo y día. */
    public static RegistroCelular guardarSesion(RegistroCelular r, SesionRegistro s) {
        SesionRegistro limpia = limpiarSesion(s);
        List<SesionRegistro> sesiones = new ArrayList<>();
        for (SesionRegistro o : r.sesiones()) {
            if (o.microciclo() != limpia.microciclo() || o.dia() != limpia.dia()) {
                sesiones.add(o);
            }
        }
        sesiones.add(limpia);
        sesiones.sort(Comparator.comparingInt(SesionRegistro::microciclo).thenComparingInt(SesionRegistro::dia));
        return new RegistroCelular(r.formato(), r.version(), r.perfil(), r.mesocicloId(), sesiones);
    }

    /** Pista gris de la casilla: "3.3.3 · 120/127,5", "12.12" o "" (lista vacía). */
    public static String pista(List<SerieReal> series) {
        if (series.isEmpty()) {
            return "";
        }
        StringBuilder reps = new StringBuilder();
        for (SerieReal s : series) {
            if (reps.length() > 0) {
                reps.append(".");
            }
            reps.append(s.reps());
        }
        List<String> kgs = new ArrayList<>();
        for (SerieReal s : series) {
            if (s.kg() > 0) {
                String t = textoKg(s.kg());
                if (!kgs.contains(t)) {
                    kgs.add(t);
                }
            }
        }
        if (kgs.isEmpty()) {
            return reps.toString();
        }
        return reps.toString() + " · " + String.join("/", kgs);
    }

    /** Un peso sin ".0" final y con coma decimal: 120.0 → "120", 127.5 → "127,5". */
    private static String textoKg(double kg) {
        String t = String.valueOf(kg);
        if (t.endsWith(".0")) {
            t = t.substring(0, t.length() - 2);
        }
        return t.replace('.', ',');
    }

    /** "registro-<perfil con _>-<inicio>.json". */
    public static String nombreArchivoRegistro(PlanCelular plan) {
        return "registro-" + plan.perfil().replaceAll("\\s", "_") + "-" + plan.inicio() + ".json";
    }
}
