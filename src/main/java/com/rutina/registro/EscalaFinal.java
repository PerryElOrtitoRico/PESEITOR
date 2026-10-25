package com.rutina.registro;

/** Sensación al terminar la sesión (1 = destruido, 5 = con energía). */
public enum EscalaFinal {
    DESTRUIDO(1, "Destruido"),
    CANSADO(2, "Cansado"),
    NORMAL(3, "Normal"),
    BIEN(4, "Bien"),
    CON_ENERGIA(5, "Con energía");

    private final int valor;
    private final String texto;

    EscalaFinal(int valor, String texto) {
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
    public static EscalaFinal deValor(int valor) {
        for (EscalaFinal e : values()) {
            if (e.valor == valor) return e;
        }
        throw new IllegalArgumentException("valor de EscalaFinal inexistente: " + valor);
    }
}
