package com.rutina.app.ui;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.rutina.app.App;
import com.rutina.app.R;
import com.rutina.app.logica.Crono;
import com.rutina.app.logica.DuracionSesion;
import com.rutina.app.logica.EstadoCrono;
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

    /** Cada cuántos segundos, mientras corre el cronómetro, se guarda la duración por si se cierra la app sin pausar. */
    private static final int SEGUNDOS_ENTRE_GUARDADOS = 30;
    /** Para pedir el permiso de notificaciones una sola vez por vez que se abre la app. */
    private static boolean permisoPedido;

    private EstadoCrono crono = Crono.nuevo();
    private TextView tiempo;
    private Button botonCrono;
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private int ticks;
    private final Runnable tic = new Runnable() {
        @Override
        public void run() {
            dibujarCrono();
            if (crono.desde() != null && ++ticks % SEGUNDOS_ENTRE_GUARDADOS == 0) guardar();
            reloj.postDelayed(this, 1000);
        }
    };
    private final ActivityResultLauncher<String> pedirPermiso =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido && crono.desde() != null) mostrarNotificacion();
            });

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
        tiempo = findViewById(R.id.tiempo);
        botonCrono = findViewById(R.id.crono_boton);
        botonCrono.setOnClickListener(v -> alternarCrono());
        findViewById(R.id.crono_editar).setOnClickListener(v -> editarDuracion());
        findViewById(R.id.borrar_sesion).setOnClickListener(v -> pedirBorrado());
        Escalas.DESCANSO.llenar(this, descanso);
        Escalas.FINAL.llenar(this, sensacionFinal);
        armar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (plan == null) return;
        // el tiempo sale de la hora de arranque guardada, así que sigue bien aunque la app se haya cerrado o reiniciado
        crono = DuracionSesion.estadoInicial(App.almacen().crono(plan.mesocicloId(), micro, dia), duracionGuardada());
        ticks = 0;
        dibujarCrono();
        if (crono.desde() != null) mostrarNotificacion();
        reloj.removeCallbacks(tic);
        reloj.postDelayed(tic, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        reloj.removeCallbacks(tic);
        if (plan != null) guardar();
    }

    // ---------- cronómetro ----------

    private Integer duracionGuardada() {
        SesionRegistro g = sesionGuardada();
        return g == null ? null : g.duracionMin();
    }

    private void dibujarCrono() {
        long ms = Crono.ms(crono, System.currentTimeMillis());
        tiempo.setText(Crono.texto(ms));
        botonCrono.setText(crono.desde() != null ? "Pausar" : (crono.acumuladoMs() > 0 ? "Reanudar" : "Iniciar"));
    }

    /** Guarda el estado del cronómetro en el almacén. */
    private void guardarCrono() {
        try {
            App.almacen().crono(plan.mesocicloId(), micro, dia, crono);
        } catch (IOException e) {
            Dialogos.aviso(this, "No se pudo guardar", String.valueOf(e.getMessage()));
        }
    }

    private void mostrarNotificacion() {
        NotificacionCrono.mostrar(this, plan.mesocicloId(), micro, dia, Crono.ms(crono, System.currentTimeMillis()));
    }

    private void quitarNotificacion() {
        NotificacionCrono.quitar(this, plan.mesocicloId(), micro, dia);
    }

    private void alternarCrono() {
        long ahora = System.currentTimeMillis();
        if (crono.desde() != null) {
            crono = Crono.pausar(crono, ahora);
            quitarNotificacion();
        } else {
            crono = Crono.iniciar(crono, ahora);
            if (NotificacionCrono.permitido(this)) {
                mostrarNotificacion();
            } else if (Build.VERSION.SDK_INT >= 33 && !permisoPedido) {
                // si lo niega, el cronómetro anda igual pero sin la notificación
                permisoPedido = true;
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        guardarCrono();
        dibujarCrono();
        guardar();
    }

    /** El botón seguro por si te olvidaste de pausar: se escribe la duración en minutos. */
    private void editarDuracion() {
        Integer actuales = Crono.minutosDe(Crono.ms(crono, System.currentTimeMillis()));
        EditText campo = new EditText(this);
        campo.setInputType(InputType.TYPE_CLASS_NUMBER);
        campo.setText(actuales == null ? "" : String.valueOf(actuales));
        campo.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Duración en minutos").setView(campo)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aceptar", (d, w) -> {
                    Integer n = Crono.duracionValida(campo.getText().toString());
                    if (n == null) {
                        Dialogos.aviso(this, "Duración", "Escribí los minutos, de 1 a 600");
                        return;
                    }
                    crono = new EstadoCrono(n * 60_000L, null);
                    quitarNotificacion();
                    guardarCrono();
                    dibujarCrono();
                    guardar();
                }).show();
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

    /** La sesión tal como está en pantalla (la fecha es la guardada, o la de hoy si es la primera vez; la duración sale del cronómetro). */
    private SesionRegistro sesionEnPantalla(SesionRegistro guardada) {
        List<EjercicioRegistro> ejercicios = new ArrayList<>();
        for (VistaEjercicio v : vistas) ejercicios.add(v.leer());
        String fecha = guardada != null && guardada.fecha() != null ? guardada.fecha() : Fechas.hoy();
        Integer duracion = DuracionSesion.paraGuardar(crono, System.currentTimeMillis(), guardada == null ? null : guardada.duracionMin());
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
            crono = Crono.nuevo();
            App.almacen().crono(plan.mesocicloId(), micro, dia, crono);
            quitarNotificacion();
        } catch (IOException e) {
            Dialogos.aviso(this, "No se pudo borrar", String.valueOf(e.getMessage()));
            return;
        }
        dibujarCrono();
        armar();
    }
}
