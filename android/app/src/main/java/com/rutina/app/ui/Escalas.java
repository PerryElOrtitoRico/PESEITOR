package com.rutina.app.ui;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.rutina.registro.EscalaDescanso;
import com.rutina.registro.EscalaEsfuerzo;
import com.rutina.registro.EscalaFinal;

import java.util.ArrayList;
import java.util.List;

/** Las tres escalas del registro como desplegables: la primera opción "—" es "sin anotar". */
final class Escalas {

    static final Escalas DESCANSO = new Escalas(valoresDescanso(), textosDescanso());
    static final Escalas ESFUERZO = new Escalas(valoresEsfuerzo(), textosEsfuerzo());
    static final Escalas FINAL = new Escalas(valoresFinal(), textosFinal());

    private final int[] valores;
    private final String[] textos;

    private Escalas(int[] valores, String[] textos) {
        this.valores = valores;
        this.textos = textos;
    }

    /** Pone las opciones ("—" y "valor · texto") en el desplegable. */
    void llenar(Context ctx, Spinner spinner) {
        List<String> opciones = new ArrayList<>();
        opciones.add("—");
        for (int i = 0; i < valores.length; i++) opciones.add(valores[i] + " · " + textos[i]);
        ArrayAdapter<String> a = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, opciones);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
    }

    /** Elige en el desplegable el valor guardado (null = "—"). */
    void elegir(Spinner spinner, Integer valor) {
        int posicion = 0;
        if (valor != null) {
            for (int i = 0; i < valores.length; i++) {
                if (valores[i] == valor) posicion = i + 1;
            }
        }
        spinner.setSelection(posicion);
    }

    /** El valor de la opción elegida; null si es "—". */
    Integer valor(Spinner spinner) {
        int p = spinner.getSelectedItemPosition();
        return p <= 0 || p > valores.length ? null : valores[p - 1];
    }

    private static int[] valoresDescanso() {
        EscalaDescanso[] v = EscalaDescanso.values();
        int[] r = new int[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i].valor();
        return r;
    }

    private static String[] textosDescanso() {
        EscalaDescanso[] v = EscalaDescanso.values();
        String[] r = new String[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i].texto();
        return r;
    }

    private static int[] valoresEsfuerzo() {
        EscalaEsfuerzo[] v = EscalaEsfuerzo.values();
        int[] r = new int[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i].valor();
        return r;
    }

    private static String[] textosEsfuerzo() {
        EscalaEsfuerzo[] v = EscalaEsfuerzo.values();
        String[] r = new String[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i].texto();
        return r;
    }

    private static int[] valoresFinal() {
        EscalaFinal[] v = EscalaFinal.values();
        int[] r = new int[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i].valor();
        return r;
    }

    private static String[] textosFinal() {
        EscalaFinal[] v = EscalaFinal.values();
        String[] r = new String[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i].texto();
        return r;
    }
}
