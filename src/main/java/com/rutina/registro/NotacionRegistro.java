package com.rutina.registro;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase utilitaria para parsear la notación de registro de sesiones.
 */
public final class NotacionRegistro {

    private NotacionRegistro() { }

    /**
     * @param texto        lo que escribió el usuario; no puede ser vacío (vacío = "como lo planificado", lo resuelve quien llama)
     * @param planificadas series planificadas de esa fila, en orden (puede ser una lista vacía)
     * @return las series realizadas; lista vacía si el texto es "0"
     * @throws IllegalArgumentException si el texto no cumple la gramática; el mensaje dice qué está mal
     */
    public static List<SerieReal> parse(String texto, List<SerieReal> planificadas) {
        if (texto == null || texto.isEmpty()) {
            throw new IllegalArgumentException("El texto no puede ser nulo ni vacío.");
        }

        // Paso 1: Normalizar espacios
        String normalizado = texto.replaceAll("\\s+", "");

        // Paso 2: Validar caso especial "0"
        if (normalizado.equals("0")) {
            return List.of();
        }

        // Paso 3: Separar por puntos
        String[] partes = normalizado.split("\\.");

        // Paso 4: Procesar cada parte
        List<SerieReal> resultado = new ArrayList<>();
        int indicePlanificado = 0;

        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i];

            // Validar que la parte no esté vacía (ej: "3..3", ".3", "3.")
            if (parte.isEmpty()) {
                throw new IllegalArgumentException("Formato inválido: series separadas por puntos consecutivos o punto inicial/final.");
            }

            // Separar reps y kg si existe
            String repsStr;
            String kgStr = null;

            int dashIndex = parte.indexOf('-');
            if (dashIndex != -1) {
                repsStr = parte.substring(0, dashIndex);
                kgStr = parte.substring(dashIndex + 1);
            } else {
                repsStr = parte;
            }

            // Validar reps: solo dígitos y entre 1 y 99
            if (!repsStr.matches("\\d+")) {
                throw new IllegalArgumentException("Formato inválido en la serie " + (i + 1) + ": repeticiones deben ser un número entero.");
            }

            int reps = Integer.parseInt(repsStr);
            if (reps < 1 || reps > 99) {
                throw new IllegalArgumentException("Formato inválido en la serie " + (i + 1) + ": repeticiones deben estar entre 1 y 99.");
            }

            // Validar kg si fue proporcionado
            if (kgStr != null) {
                if (kgStr.isEmpty()) {
                    throw new IllegalArgumentException("Formato inválido en la serie " + (i + 1) + ": peso no puede estar vacío después del guion.");
                }

                // Validar que el peso tenga formato numérico correcto (dígitos y opcionalmente coma para decimales)
                if (!kgStr.matches("\\d+(,\\d+)?")) {
                    throw new IllegalArgumentException("Formato inválido en la serie " + (i + 1) + ": el peso debe ser un número con decimales usando coma.");
                }

                // Convertir de coma a punto para parsear como double
                String kgConPunto = kgStr.replace(',', '.');
                double kg = Double.parseDouble(kgConPunto);

                if (kg <= 0) {
                    throw new IllegalArgumentException("Formato inválido en la serie " + (i + 1) + ": el peso debe ser mayor a 0.");
                }
            } else {
                // No hay peso especificado, buscarlo en las planificadas
                if (indicePlanificado >= planificadas.size()) {
                    // Usar el de la última planificada si existen
                    SerieReal ultima = planificadas.get(planificadas.size() - 1);
                    resultado.add(new SerieReal(reps, ultima.kg()));
                } else {
                    // Usar el de la posición actual
                    SerieReal planificada = planificadas.get(indicePlanificado);
                    resultado.add(new SerieReal(reps, planificada.kg()));
                    indicePlanificado++;
                }
            }

            // Validar que no se acaben las planificadas antes de tiempo si el usuario no especificó peso
            // Nota: La lógica anterior ya maneja esto, pero aseguramos que si hay más series que planificadas y ninguna tiene peso,
            // usamos la última. Si todas tienen peso explícito, no importa.
        }

        return resultado;
    }

    /** true si parse no lanzaría excepción. */
    public static boolean esValida(String texto, List<SerieReal> planificadas) {
        try {
            parse(texto, planificadas);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
