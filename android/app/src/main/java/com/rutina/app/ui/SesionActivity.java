package com.rutina.app.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Pantalla de la sesión (por ahora solo el título; la arma la Tarea 6). */
public class SesionActivity extends AppCompatActivity {

    public static final String EXTRA_PLAN = "plan";
    public static final String EXTRA_MICRO = "micro";
    public static final String EXTRA_DIA = "dia";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setPadding(32, 32, 32, 32);
        t.setText("Microciclo " + getIntent().getIntExtra(EXTRA_MICRO, 0) + " · Día " + getIntent().getIntExtra(EXTRA_DIA, 0));
        setContentView(t);
    }
}
