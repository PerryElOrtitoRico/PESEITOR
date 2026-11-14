package com.rutina.registro;

import java.util.List;

/**
 * El plan que se exporta al celular (sección 5 de la especificación). Los nombres de los componentes son las claves del JSON.
 * {@code pliometria} es opcional: los planes viejos no la traen y quedan con la lista vacía.
 */
public record PlanCelular(String formato, int version, String perfil, long mesocicloId, String inicio,
                          int microciclos, int dias, List<SesionPlan> sesiones, List<PlanPlio> pliometria) {
    public static final String FORMATO = "rutineitor-plan";
    public static final int VERSION = 1;

    public PlanCelular {
        pliometria = pliometria == null ? List.of() : List.copyOf(pliometria);
    }

    /** Plan sin pliometría. */
    public PlanCelular(String formato, int version, String perfil, long mesocicloId, String inicio,
                       int microciclos, int dias, List<SesionPlan> sesiones) {
        this(formato, version, perfil, mesocicloId, inicio, microciclos, dias, sesiones, List.of());
    }

    /** Nunca null: Gson en Android arma el objeto sin pasar por el constructor, y un JSON viejo no trae el campo. */
    @Override
    public List<PlanPlio> pliometria() {
        return pliometria == null ? List.of() : pliometria;
    }
}
