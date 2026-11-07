package com.rutina.registro;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** El registro que exporta el celular (sección 6 de la especificación). */
public record RegistroCelular(String formato, int version, String perfil, long mesocicloId,
                              List<SesionRegistro> sesiones) {
    public static final String FORMATO = "rutineitor-registro";
    public static final int VERSION = 1;

    /** Sesiones de este registro (por microciclo y día) que no están en {@code nuevo}: las que se pierden al reemplazarlo. */
    public int sesionesQueFaltanEn(RegistroCelular nuevo) {
        Set<List<Integer>> hay = new HashSet<>();
        for (SesionRegistro s : nuevo.sesiones()) hay.add(List.of(s.microciclo(), s.dia()));
        int faltan = 0;
        for (SesionRegistro s : sesiones) if (!hay.contains(List.of(s.microciclo(), s.dia()))) faltan++;
        return faltan;
    }
}
