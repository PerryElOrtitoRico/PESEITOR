package com.rutina.registro;

/** Sensación de un ejercicio (1 = muy liviano, 5 = al límite). */
public enum EscalaEsfuerzo {
    MUY_LIVIANO(1, "Muy liviano"),
    LIVIANO(2, "Liviano"),
    JUSTO(3, "Justo"),
    PESADO(4, "Pesado"),
    AL_LIMITE(5, "Al límite");

    private final int valor;
    private final String texto;

    EscalaEsfuerzo(int valor, String texto) {
        this.valor = valor;
        this.texto = texto;
    }

    /** Valor numérico de la escala (el que va en el JSON). */
    public int valor() {
        return valor;
    }

    /** Texto que ve el usuario. */
    public String texto() {
        return texto;
    }

    /** Lanza IllegalArgumentException si no existe ese valor. */
    public static EscalaEsfuerzo deValor(int valor) {
        for (EscalaEsfuerzo e : values()) {
            if (e.valor == valor) return e;
        }
        throw new IllegalArgumentException("valor de EscalaEsfuerzo inexistente: " + valor);
    }
}
