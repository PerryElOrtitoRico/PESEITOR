package com.rutina.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.rutina.app.datos.Almacen;

/** Aplicación: tiene el único {@link Almacen} y aplica el tema claro/oscuro al iniciar. */
public class App extends Application {

    private static Almacen almacen;

    @Override
    public void onCreate() {
        super.onCreate();
        almacen = new Almacen(getFilesDir().toPath());
        aplicarTema(almacen.oscuro());
    }

    /** El almacén de planes, registros y preferencias (en el almacenamiento interno de la app). */
    public static Almacen almacen() {
        return almacen;
    }

    /** Pone el modo oscuro o el claro en toda la app (las pantallas abiertas se recrean solas). */
    public static void aplicarTema(boolean oscuro) {
        AppCompatDelegate.setDefaultNightMode(oscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
