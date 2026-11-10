package com.rutina.service;

import com.rutina.model.Mesociclo;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Nombre legible del mesociclo y nombre de archivo estándar para el PDF y el plan del celular. */
public final class NombreMeso {

    private NombreMeso() {
    }

    /** "4s3d", o "4s" mientras no se eligieron los días. */
    public static String duracion(Mesociclo m) {
        return m.microciclos() + "s" + (m.dias() == null ? "" : m.dias() + "d");
    }

    /** "Sentadilla (4s3d)", o null si el mesociclo no tiene objetivo. */
    public static String etiqueta(Mesociclo m) {
        return vacio(m.objetivo()) ? null : m.objetivo().trim() + " (" + duracion(m) + ")";
    }

    /** "AAAA-MM-DD_Macro_Objetivo_4s3d.ext"; omite lo que está vacío. Sin espacios, acentos ni símbolos. */
    public static String archivo(String prefijo, Mesociclo m, String macroObjetivo, String extension) {
        String partes = Arrays.asList(prefijo, m.inicio().toString(), macroObjetivo, m.objetivo(), duracion(m)).stream()
                .map(NombreMeso::limpio).filter(x -> !x.isEmpty()).collect(Collectors.joining("_"));
        return partes + "." + extension;
    }

    /** Cada palabra con mayúscula inicial y pegadas ("press banca" → "PressBanca"). */
    static String limpio(String s) {
        if (s == null) return "";
        String sin = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Arrays.stream(sin.split("[^A-Za-z0-9+-]+")).filter(x -> !x.isEmpty())
                .map(x -> Character.toUpperCase(x.charAt(0)) + x.substring(1)).collect(Collectors.joining());
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
