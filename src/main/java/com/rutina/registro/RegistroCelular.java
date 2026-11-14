package com.rutina.registro;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * El registro que exporta el celular (sección 6 de la especificación). {@code pliometria} es opcional: los registros
 * viejos no la traen y quedan con la lista vacía.
 */
public record RegistroCelular(String formato, int version, String perfil, long mesocicloId,
                              List<SesionRegistro> sesiones, List<SesionPlioRegistro> pliometria) {
    public static final String FORMATO = "rutineitor-registro";
    public static final int VERSION = 1;

    public RegistroCelular {
        pliometria = pliometria == null ? List.of() : List.copyOf(pliometria);
    }

    /** Registro sin pliometría. */
    public RegistroCelular(String formato, int version, String perfil, long mesocicloId, List<SesionRegistro> sesiones) {
        this(formato, version, perfil, mesocicloId, sesiones, List.of());
    }

    /** Copia con otras sesiones de pesas; conserva la pliometría. */
    public RegistroCelular conSesiones(List<SesionRegistro> nuevas) {
        return new RegistroCelular(formato, version, perfil, mesocicloId, nuevas, pliometria);
    }

    /** Copia con otras sesiones de pliometría; conserva las de pesas. */
    public RegistroCelular conPliometria(List<SesionPlioRegistro> nuevas) {
        return new RegistroCelular(formato, version, perfil, mesocicloId, sesiones, nuevas);
    }

    /**
     * Sesiones de este registro (pesas por microciclo y día, pliometría por plan, semana y día) que no están en
     * {@code nuevo}: las que se pierden al reemplazarlo.
     */
    public int sesionesQueFaltanEn(RegistroCelular nuevo) {
        Set<List<Integer>> hay = new HashSet<>();
        for (SesionRegistro s : nuevo.sesiones()) hay.add(List.of(s.microciclo(), s.dia()));
        Set<String> hayPlio = new HashSet<>();
        for (SesionPlioRegistro s : nuevo.pliometria()) hayPlio.add(s.clave());
        int faltan = 0;
        for (SesionRegistro s : sesiones) if (!hay.contains(List.of(s.microciclo(), s.dia()))) faltan++;
        for (SesionPlioRegistro s : pliometria) if (!hayPlio.contains(s.clave())) faltan++;
        return faltan;
    }

    /** Nunca null: Gson en Android arma el objeto sin pasar por el constructor, y un JSON viejo no trae el campo. */
    @Override
    public List<SesionPlioRegistro> pliometria() {
        return pliometria == null ? List.of() : pliometria;
    }
}
