package com.rutina.app.logica;

import com.rutina.registro.DiaPlioCelular;
import com.rutina.registro.EjercicioPlioRegistro;
import com.rutina.registro.PlanPlio;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionPlioRegistro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lógica pura de las sesiones de pliometría del celular. Nada modifica sus argumentos. */
public final class SesionesPlio {

    private SesionesPlio() {
    }

    /** El día (semana y día desde 1) del plan de pliometría, o null. */
    public static DiaPlioCelular dia(PlanPlio plan, int semana, int dia) {
        if (semana < 1 || semana > plan.semanas().size()) return null;
        for (DiaPlioCelular d : plan.semanas().get(semana - 1)) if (d.dia() == dia) return d;
        return null;
    }

    /** La sesión anotada de ese plan (por su inicio), semana y día, o null. */
    public static SesionPlioRegistro sesion(RegistroCelular r, String inicio, int semana, int dia) {
        for (SesionPlioRegistro s : r.pliometria()) {
            if (s.inicio().equals(inicio) && s.semana() == semana && s.dia() == dia) return s;
        }
        return null;
    }

    /** La nota del ejercicio número {@code numero} (desde 1), o null. */
    public static String nota(SesionPlioRegistro s, int numero) {
        if (s == null) return null;
        for (EjercicioPlioRegistro e : s.ejercicios()) if (e.numero() == numero) return e.nota();
        return null;
    }

    /** Copia sin ruido: notas en blanco fuera y recortadas, ordenadas por número. */
    public static SesionPlioRegistro limpiar(SesionPlioRegistro s) {
        List<EjercicioPlioRegistro> limpios = new ArrayList<>();
        for (EjercicioPlioRegistro e : s.ejercicios()) {
            if (e.nota() == null || e.nota().isBlank()) continue;
            limpios.add(new EjercicioPlioRegistro(e.numero(), e.nota().strip()));
        }
        limpios.sort(Comparator.comparingInt(EjercicioPlioRegistro::numero));
        return new SesionPlioRegistro(s.inicio(), s.semana(), s.dia(), s.fecha(), s.duracionMin(), limpios);
    }

    /** Sin notas y sin duración: abrir la pantalla no crea una sesión vacía. */
    public static boolean vacia(SesionPlioRegistro s) {
        return s.ejercicios().isEmpty() && s.duracionMin() == null;
    }

    /**
     * @param guardada la sesión del registro, o null; @param actual lo que se ve, ya pasado por {@link #limpiar}
     * @return false si no cambió nada, o si no hay nada anotado y tampoco hay sesión guardada
     */
    public static boolean hayQueGuardar(SesionPlioRegistro guardada, SesionPlioRegistro actual) {
        if (guardada == null) return !vacia(actual);
        return !limpiar(guardada).equals(actual);
    }

    /** Registro nuevo con la sesión (limpia) agregada o reemplazada; las pesas quedan como estaban. */
    public static RegistroCelular guardar(RegistroCelular r, SesionPlioRegistro s) {
        SesionPlioRegistro limpia = limpiar(s);
        List<SesionPlioRegistro> lista = new ArrayList<>();
        for (SesionPlioRegistro o : r.pliometria()) if (!o.clave().equals(limpia.clave())) lista.add(o);
        lista.add(limpia);
        lista.sort(Comparator.comparing(SesionPlioRegistro::inicio).thenComparingInt(SesionPlioRegistro::semana)
                .thenComparingInt(SesionPlioRegistro::dia));
        return r.conPliometria(lista);
    }

    /** Registro nuevo sin esa sesión; las pesas quedan como estaban. */
    public static RegistroCelular borrar(RegistroCelular r, String inicio, int semana, int dia) {
        List<SesionPlioRegistro> resto = new ArrayList<>();
        for (SesionPlioRegistro o : r.pliometria()) {
            if (!(o.inicio().equals(inicio) && o.semana() == semana && o.dia() == dia)) resto.add(o);
        }
        return r.conPliometria(resto);
    }

    /** "3 series × 10 saltos". */
    public static String seriesTexto(DiaPlioCelular d) {
        return d.series() + (d.series() == 1 ? " serie" : " series") + " × " + d.saltosPorSerie() + " saltos";
    }

    /** "Semana 2 · Día 1". */
    public static String titulo(int semana, int dia) {
        return "Semana " + semana + " · Día " + dia;
    }

    /** Clave del cronómetro en las preferencias; empieza con "<id>-" como las de pesas, así borrar el plan las limpia. */
    public static String claveCrono(long mesocicloId, String inicio, int semana, int dia) {
        return mesocicloId + "-plio-" + inicio + "-" + semana + "-" + dia;
    }
}
