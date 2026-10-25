package com.rutina.registro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
