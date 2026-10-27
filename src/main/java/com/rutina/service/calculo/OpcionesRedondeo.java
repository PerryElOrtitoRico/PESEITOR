package com.rutina.service.calculo;

/** Las opciones del combo "Redondeo de kg" de Plantillas: textos y múltiplos (kg) en el mismo orden. */
public final class OpcionesRedondeo {

    public static final String[] TEXTOS = {"1 kg", "2,5 kg", "5 kg"};
    public static final double[] VALORES = {1.0, 2.5, 5.0};

    private OpcionesRedondeo() {
    }

    /** Índice de la opción cuyo valor es igual a incrementoKg; si no coincide ninguna, 1 ("2,5 kg"). */
    public static int indiceDe(double incrementoKg) {
        for (int i = 0; i < VALORES.length; i++) {
            if (VALORES[i] == incrementoKg) return i;
        }
        return 1;
    }

    /** Valor (kg) de la opción con ese índice. */
    public static double valorDe(int indice) {
        return VALORES[indice];
    }
}
