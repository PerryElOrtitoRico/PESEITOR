package com.rutina.app.ui;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/** Diálogos simples de la app. */
final class Dialogos {

    private Dialogos() {
    }

    /** Un aviso con un solo botón. */
    static void aviso(Context ctx, String titulo, String mensaje) {
        new AlertDialog.Builder(ctx).setTitle(titulo).setMessage(mensaje).setPositiveButton("Aceptar", null).show();
    }

    /** Pregunta con Cancelar y un botón de confirmar; {@code si} corre solo si se confirma. */
    static void confirmar(Context ctx, String mensaje, String botonSi, Runnable si) {
        new AlertDialog.Builder(ctx).setMessage(mensaje).setNegativeButton("Cancelar", null)
                .setPositiveButton(botonSi, (d, w) -> si.run()).show();
    }
}
