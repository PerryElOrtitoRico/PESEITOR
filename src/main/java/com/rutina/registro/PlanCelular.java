package com.rutina.registro;

import java.util.List;

/** El plan que se exporta al celular (sección 5 de la especificación). Los nombres de los componentes son las claves del JSON. */
public record PlanCelular(String formato, int version, String perfil, long mesocicloId, String inicio,
                          int microciclos, int dias, List<SesionPlan> sesiones) {
    public static final String FORMATO = "rutineitor-plan";
    public static final int VERSION = 1;
}
