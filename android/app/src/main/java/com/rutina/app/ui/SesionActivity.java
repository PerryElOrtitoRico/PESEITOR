package com.rutina.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.rutina.app.App;
import com.rutina.app.R;
import com.rutina.app.logica.Fechas;
import com.rutina.app.logica.GuardadoSesion;
import com.rutina.app.logica.Sesiones;
import com.rutina.app.logica.TextosSesion;
import com.rutina.registro.EjercicioPlan;
import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de una sesión (plan + microciclo + día). Todo lo que se escribe se guarda en el momento en el almacén; la
 * pantalla se arma siempre desde el almacén, así girar el celular o que Android cierre la app no pierde nada.
 */
public class SesionActivity extends AppCompatActivity {

    public static final String EXTRA_PLAN = "plan";
    public static final String EXTRA_MICRO = "micro";
    public static final String EXTRA_DIA = "dia";

    private PlanCelular plan;
    private int micro;
    private int dia;
    private Toolbar barra;
    private Spinner descanso;
    private Spinner sensacionFinal;
    private LinearLayout contenedor;
    private final List<VistaEjercicio> vistas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        plan = App.almacen().plan(getIntent().getLongExtra(EXTRA_PLAN, -1));
        micro = getIntent().getIntExtra(EXTRA_MICRO, 1);
        dia = getIntent().getIntExtra(EXTRA_DIA, 1);
        if (plan == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_sesion);
        barra = findViewById(R.id.barra_sesion);
        barra.setNavigationOnClickListener(v -> finish());
        barra.setTitle(TextosSesion.tituloSesion(micro, dia));
        descanso = findViewById(R.id.descanso);
        sensacionFinal = findViewById(R.id.sensacion_final);
        contenedor = findViewById(R.id.ejercicios);
        findViewById(R.id.borrar_sesion).setOnClickListener(v -> pedirBorrado());
        Escalas.DESCANSO.llenar(this, descanso);
        Escalas.FINAL.llenar(this, sensacionFinal);
        armar();
    }

    /** Arma la pantalla con lo que hay guardado en el almacén. */
    private void armar() {
        SesionRegistro guardada = sesionGuardada();
        barra.setSubtitle(TextosSesion.lineaFecha(guardada == null ? null : guardada.fecha()));
        Escalas.DESCANSO.elegir(descanso, guardada == null ? null : guardada.descanso());
        Escalas.FINAL.elegir(sensacionFinal, guardada == null ? null : guardada.sensacionFinal());
        AdapterView.OnItemSelectedListener alElegir = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int posicion, long id) {
                guardar();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        descanso.setOnItemSelectedListener(alElegir);
        sensacionFinal.setOnItemSelectedListener(alElegir);

        contenedor.removeAllViews();
        vistas.clear();
        SesionPlan sesionPlan = Sesiones.sesionDelPlan(plan, micro, dia);
        List<EjercicioPlan> ejercicios = sesionPlan == null ? new ArrayList<>() : sesionPlan.ejercicios();
        if (ejercicios.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Este día no tiene ejercicios");
            vacio.setTextColor(getColor(R.color.texto));
            vacio.setTextSize(16);
            contenedor.addView(vacio);
        }
        for (EjercicioPlan ej : ejercicios) {
            EjercicioRegistro anotado = null;
            if (guardada != null) {
                for (EjercicioRegistro e : guardada.ejercicios()) {
                    if (e.bloque().equals(ej.bloque())) anotado = e;
                }
            }
            VistaEjercicio v = new VistaEjercicio(this, contenedor, ej, anotado, this::guardar);
            vistas.add(v);
            contenedor.addView(v.vista());
        }
    }

    private SesionRegistro sesionGuardada() {
        RegistroCelular r = App.almacen().registro(plan.mesocicloId());
        return r == null ? null : Sesiones.sesionDelRegistro(r, micro, dia);
    }

    /** La sesión tal como está en pantalla (la fecha y la duración son las guardadas; la fecha de hoy si es la primera vez). */
    private SesionRegistro sesionEnPantalla(SesionRegistro guardada) {
        List<EjercicioRegistro> ejercicios = new ArrayList<>();
        for (VistaEjercicio v : vistas) ejercicios.add(v.leer());
        String fecha = guardada != null && guardada.fecha() != null ? guardada.fecha() : Fechas.hoy();
        Integer duracion = guardada == null ? null : guardada.duracionMin();
        return new SesionRegistro(micro, dia, fecha, Escalas.DESCANSO.valor(descanso), Escalas.FINAL.valor(sensacionFinal),
                ejercicios, duracion);
    }

    /**
     * Guarda lo que hay en pantalla (se llama en cada cambio). No guarda si no cambió nada respecto de lo guardado, ni crea
     * una sesión vacía por solo abrir la pantalla.
     */
    private void guardar() {
        if (plan == null) return;
        SesionRegistro guardada = sesionGuardada();
        SesionRegistro actual = Sesiones.limpiarSesion(sesionEnPantalla(guardada));
        if (!GuardadoSesion.hayQueGuardar(guardada, actual)) return;
        try {
            RegistroCelular nuevo = Sesiones.guardarSesion(App.almacen().registro(plan.mesocicloId()), actual);
            App.almacen().guardarRegistro(nuevo);
            barra.setSubtitle(TextosSesion.lineaFecha(actual.fecha()));
        } catch (IOException e) {
            Dialogos.aviso(this, "No se pudo guardar", String.valueOf(e.getMessage()));
        }
    }

    /** Misma confirmación que la app web. */
    private void pedirBorrado() {
        if (sesionGuardada() == null) {
            Dialogos.aviso(this, "Borrar", "Esta sesión no tiene nada anotado.");
            return;
        }
        Dialogos.confirmar(this, "¿Borrar lo anotado en el microciclo " + micro + ", día " + dia + "?", "Borrar", this::borrar);
    }

    private void borrar() {
        RegistroCelular r = App.almacen().registro(plan.mesocicloId());
        List<SesionRegistro> resto = new ArrayList<>();
        for (SesionRegistro s : r.sesiones()) {
            if (s.microciclo() != micro || s.dia() != dia) resto.add(s);
        }
        try {
            App.almacen().guardarRegistro(new RegistroCelular(r.formato(), r.version(), r.perfil(), r.mesocicloId(), resto));
        } catch (IOException e) {
            Dialogos.aviso(this, "No se pudo borrar", String.valueOf(e.getMessage()));
            return;
        }
        armar();
    }
}
