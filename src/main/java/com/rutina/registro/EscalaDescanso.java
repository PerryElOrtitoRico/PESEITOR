package com.rutina.registro;

/** Cómo llegó descansado a la sesión (6 = mejor, 1 = peor). */
public enum EscalaDescanso {
    DESCANSADO_CON_GANAS(6, "Bien descansado y con ganas de entrenar"),
    DESCANSADO(5, "Bien descansado"),
    DESCANSADO_DISTRAIDO(4, "Bien descansado pero distraído"),
    MAL_DESCANSADO_CON_GANAS(3, "Mal descansado pero con ganas de entrenar"),
    MAL_DESCANSADO(2, "Mal descansado"),
    MAL_DESCANSADO_SIN_GANAS(1, "Mal descansado y sin ganas de entrenar");

    private final int valor;
    private final String texto;

    EscalaDescanso(int valor, String texto) {
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
    public static EscalaDescanso deValor(int valor) {
        for (EscalaDescanso e : values()) {
            if (e.valor == valor) return e;
        }
        throw new IllegalArgumentException("valor de EscalaDescanso inexistente: " + valor);
    }
}
