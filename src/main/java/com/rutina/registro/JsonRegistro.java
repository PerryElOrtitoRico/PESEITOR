package com.rutina.registro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lectura y escritura de los archivos que van y vienen del celular. */
public final class JsonRegistro {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private JsonRegistro() {
    }

    /** JSON con sangría; los null se escriben (por ejemplo "rm": null). */
    public static String planAJson(PlanCelular plan) {
        return GSON.toJson(plan);
    }

    /**
     * @throws IllegalArgumentException si no es un plan válido: formato distinto de "rutineitor-plan",
     *                                  version distinta de 1, o JSON mal formado (mensaje en español).
     */
    public static PlanCelular planDeJson(String json) {
        PlanCelular p;
        try {
            p = GSON.fromJson(json, PlanCelular.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("El archivo del plan no es un JSON válido: " + e.getMessage(), e);
        }
        if (p == null) throw new IllegalArgumentException("El archivo del plan está vacío");
        if (!PlanCelular.FORMATO.equals(p.formato())) {
            throw new IllegalArgumentException("No es un plan de RUTINEITOR (formato \"" + p.formato() + "\", se esperaba \""
                    + PlanCelular.FORMATO + "\")");
        }
        if (p.version() != PlanCelular.VERSION) {
            throw new IllegalArgumentException("Versión del plan no soportada: " + p.version() + " (se esperaba "
                    + PlanCelular.VERSION + ")");
        }
        if (p.sesiones() == null) throw new IllegalArgumentException("El plan no tiene la lista de sesiones");
        return p;
    }

    /** Escribe en UTF-8. */
    public static void escribirPlan(Path destino, PlanCelular plan) throws IOException {
        Files.writeString(destino, planAJson(plan), StandardCharsets.UTF_8);
    }

    /** @throws IllegalArgumentException si el archivo no es un registro válido (formato, versión, fechas, rangos, repetidos). */
    public static RegistroCelular registroDeJson(String json) {
        RegistroCelular r;
        try {
            r = GSON.fromJson(json, RegistroCelular.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("El archivo del registro no es un JSON válido: " + e.getMessage(), e);
        }
        if (r == null) throw new IllegalArgumentException("El archivo del registro está vacío");
        if (!RegistroCelular.FORMATO.equals(r.formato())) {
            throw new IllegalArgumentException("No es un registro de RUTINEITOR (formato \"" + r.formato() + "\", se esperaba \""
                    + RegistroCelular.FORMATO + "\")");
        }
        if (r.version() != RegistroCelular.VERSION) {
            throw new IllegalArgumentException("Versión del registro no soportada: " + r.version() + " (se esperaba "
                    + RegistroCelular.VERSION + ")");
        }
        List<SesionRegistro> sesiones = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (SesionRegistro s : r.sesiones() == null ? List.<SesionRegistro>of() : r.sesiones()) {
            if (s == null) throw new IllegalArgumentException("Hay una sesión vacía (null) en el registro");
            String donde = "sesión microciclo " + s.microciclo() + ", día " + s.dia();
            if (s.microciclo() < 1) throw new IllegalArgumentException(donde + ": el microciclo tiene que ser 1 o más");
            if (s.dia() < 1) throw new IllegalArgumentException(donde + ": el día tiene que ser 1 o más");
            if (s.fecha() == null) throw new IllegalArgumentException(donde + ": falta la fecha");
            try {
                LocalDate.parse(s.fecha());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(donde + ": fecha inválida \"" + s.fecha() + "\" (formato AAAA-MM-DD)");
            }
            enRango(s.descanso(), 1, 6, donde + ": descanso");
            enRango(s.sensacionFinal(), 1, 5, donde + ": sensación final");
            enRango(s.duracionMin(), 1, 600, donde + ": duración");
            if (!vistas.add(s.microciclo() + "-" + s.dia())) {
                throw new IllegalArgumentException(donde + ": está repetida en el registro");
            }
            List<EjercicioRegistro> ejercicios = new ArrayList<>();
            for (EjercicioRegistro e : s.ejercicios() == null ? List.<EjercicioRegistro>of() : s.ejercicios()) {
                if (e == null) throw new IllegalArgumentException(donde + ": hay un ejercicio vacío (null)");
                enRango(e.esfuerzo(), 1, 5, donde + ", ejercicio " + e.bloque() + ": esfuerzo");
                Map<String, String> filas = e.filas() == null ? Map.of() : e.filas();
                ejercicios.add(new EjercicioRegistro(e.bloque(), e.esfuerzo(), filas, e.nota()));
            }
            sesiones.add(new SesionRegistro(s.microciclo(), s.dia(), s.fecha(), s.descanso(), s.sensacionFinal(),
                    List.copyOf(ejercicios), s.duracionMin()));
        }
        return new RegistroCelular(r.formato(), r.version(), r.perfil(), r.mesocicloId(), List.copyOf(sesiones));
    }

    public static RegistroCelular leerRegistro(Path archivo) throws IOException {
        return registroDeJson(Files.readString(archivo, StandardCharsets.UTF_8));
    }

    /** JSON con sangría (la clave de la sensación final es "final"). */
    public static String registroAJson(RegistroCelular r) {
        return GSON.toJson(r);
    }

    private static void enRango(Integer valor, int min, int max, String que) {
        if (valor != null && (valor < min || valor > max)) {
            throw new IllegalArgumentException(que + " fuera de rango: " + valor + " (va de " + min + " a " + max + ")");
        }
    }
}
