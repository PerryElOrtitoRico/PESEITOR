package com.rutina.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Mesociclo planificado en el macrociclo y, una vez armado, su grilla.
 *
 * @param variante       A/B/C; null hasta que el usuario la elige al armar
 * @param dias           días por microciclo (3–5); null hasta que se elige
 * @param aproximaciones aproximación de cada ejercicio de fuerza (por id de ejercicio)
 * @param rmUsados       1RM con los que se exportó por última vez (vacío si nunca)
 * @param objetivo       ejercicio o meta del mesociclo (ej. "Sentadilla"); vacío si no se puso
 * @param macroId        macrociclo al que pertenece; null si no tiene
 */
public record Mesociclo(long id, long perfilId, LocalDate inicio, int microciclos,
                        ParametrosTipo potencia, ParametrosTipo fuerza,
                        Character variante, Integer dias,
                        Map<Long, Aproximacion> aproximaciones, List<Bloque> bloques,
                        String calentamiento, String auxiliares, Map<Long, Double> rmUsados,
                        String objetivo, Long macroId) {

    /** Sin objetivo ni macrociclo (datos nuevos y tests); al copiar un mesociclo existente usar el canónico. */
    public Mesociclo(long id, long perfilId, LocalDate inicio, int microciclos, ParametrosTipo potencia,
                     ParametrosTipo fuerza, Character variante, Integer dias, Map<Long, Aproximacion> aproximaciones,
                     List<Bloque> bloques, String calentamiento, String auxiliares, Map<Long, Double> rmUsados) {
        this(id, perfilId, inicio, microciclos, potencia, fuerza, variante, dias, aproximaciones, bloques,
                calentamiento, auxiliares, rmUsados, "", null);
    }

    /** Lunes siguiente al último microciclo. */
    public LocalDate fin() {
        return inicio.plusWeeks(microciclos);
    }
}
