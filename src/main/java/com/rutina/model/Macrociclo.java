package com.rutina.model;

/** Grupo de mesociclos con un mismo objetivo (ej. "Fuerza"). */
public record Macrociclo(long id, long perfilId, String nombre, String objetivo) {
}
