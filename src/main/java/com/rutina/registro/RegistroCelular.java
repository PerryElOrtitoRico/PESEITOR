package com.rutina.registro;

import java.util.List;

/** El registro que exporta el celular (sección 6 de la especificación). */
public record RegistroCelular(String formato, int version, String perfil, long mesocicloId,
                              List<SesionRegistro> sesiones) {
    public static final String FORMATO = "rutineitor-registro";
    public static final int VERSION = 1;
}
