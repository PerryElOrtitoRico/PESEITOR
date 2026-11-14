package com.rutina.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.rutina.app.App;
import com.rutina.app.R;
import com.rutina.app.logica.Crono;
import com.rutina.app.logica.DuracionSesion;
import com.rutina.app.logica.EstadoCrono;
import com.rutina.app.logica.Fechas;
import com.rutina.app.logica.SesionesPlio;
import com.rutina.app.logica.TextosSesion;
import com.rutina.registro.DiaPlioCelular;
import com.rutina.registro.EjercicioPlioRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.PlanPlio;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionPlioRegistro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Una sesión de pliometría (plan de pesas + plan de pliometría + semana + día): ejercicios con sus series × saltos, una
 * nota por ejercicio y cronómetro. Es independiente de las pesas: la fecha es el día en que se aprieta "Iniciar".
 * Todo se guarda en el momento en el almacén.
 */
public class PlioActivity extends AppCompatActivity {

    public static final String EXTRA_PLAN = "plan";
    public static final String EXTRA_INICIO = "inicio";
    public static final String EXTRA_SEMANA = "semana";
    public static final String EXTRA_DIA = "dia";

    private static final int SEGUNDOS_ENTRE_GUARDADOS = 30;

    private PlanCelular plan;
    private PlanPlio plio;
    private DiaPlioCelular diaPlio;
    private int semana;
    private int dia;
    private String claveCrono;
    private Toolbar barra;
    private LinearLayout contenedor;
    private final List<EditText> notas = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        plan = App.almacen().plan(getIntent().getLongExtra(EXTRA_PLAN, -1));
        String inicio = getIntent().getStringExtra(EXTRA_INICIO);
        semana = getIntent().getIntExtra(EXTRA_SEMANA, 1);
        dia = getIntent().getIntExtra(EXTRA_DIA, 1);
        if (plan != null && inicio != null) {
            for (PlanPlio p : plan.pliometria()) if (p.inicio().equals(inicio)) plio = p;
        }
        diaPlio = plio == null ? null : SesionesPlio.dia(plio, semana, dia);
        if (diaPlio == null) {
            finish();
            return;
        }
        claveCrono = SesionesPlio.claveCrono(plan.mesocicloId(), plio.inicio(), semana, dia);
        setContentView(R.layout.activity_plio);
        barra = findViewById(R.id.barra_plio);
        barra.setNavigationOnClickListener(v -> finish());
        barra.setTitle("Pliometría · " + SesionesPlio.titulo(semana, dia));
        contenedor = findViewById(R.id.ejercicios);
        tiempo = findViewById(R.id.tiempo);
        botonCrono = findViewById(R.id.crono_boton);
        botonCrono.setOnClickListener(v -> alternarCrono());
        findViewById(R.id.crono_editar).setOnClickListener(v -> editarDuracion());
        findViewById(R.id.borrar_sesion).setOnClickListener(v -> pedirBorrado());
        armar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (diaPlio == null) return;
        // el tiempo sale de la hora de arranque guardada, así que sigue bien aunque la app se haya cerrado
        crono = DuracionSesion.estadoInicial(App.almacen().crono(claveCrono), duracionGuardada());
        ticks = 0;
        dibujarCrono();
        reloj.removeCallbacks(tic);
        reloj.postDelayed(tic, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        reloj.removeCallbacks(tic);
        if (diaPlio != null) guardar();
    }

    // ---------- cronómetro ----------

    private Integer duracionGuardada() {
        SesionPlioRegistro g = sesionGuardada();
        return g == null ? null : g.duracionMin();
    }

    private void dibujarCrono() {
        tiempo.setText(Crono.texto(Crono.ms(crono, System.currentTimeMillis())));
        botonCrono.setText(crono.desde() != null ? "Pausar" : (crono.acumuladoMs() > 0 ? "Reanudar" : "Iniciar"));
    }

    private void guardarCrono() {
        try {
            App.almacen().crono(claveCrono, crono);
        } catch (IOException e) {
            Dialogos.aviso(this, "No se pudo guardar", String.valueOf(e.getMessage()));
        }
    }

    private void alternarCrono() {
        long ahora = System.currentTimeMillis();
        crono = crono.desde() != null ? Crono.pausar(crono, ahora) : Crono.iniciar(crono, ahora);
        guardarCrono();
        dibujarCrono();
        guardar();
    }

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
                    guardarCrono();
                    dibujarCrono();
                    guardar();
                }).show();
    }

    // ---------- pantalla ----------

    private void armar() {
        SesionPlioRegistro guardada = sesionGuardada();
        barra.setSubtitle(TextosSesion.lineaFecha(guardada == null ? null : guardada.fecha()));
        contenedor.removeAllViews();
        notas.clear();

        TextView resumen = new TextView(this);
        resumen.setText(SesionesPlio.seriesTexto(diaPlio) + " cada ejercicio · " + diaPlio.contactos() + " contactos");
        resumen.setTextColor(getColor(R.color.secundario));
        resumen.setTextSize(14);
        contenedor.addView(resumen);

        if (diaPlio.ejercicios().isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Este día no tiene ejercicios: cargalos en PESEITOR (Ejercicios → Pliometría) y volvé a exportar el plan.");
            vacio.setTextColor(getColor(R.color.texto));
            vacio.setTextSize(16);
            contenedor.addView(vacio);
        }
        for (int i = 0; i < diaPlio.ejercicios().size(); i++) {
            int numero = i + 1;
            LinearLayout caja = new LinearLayout(this);
            caja.setOrientation(LinearLayout.VERTICAL);
            caja.setBackgroundResource(R.drawable.recuadro);
            caja.setPadding(dp(10), dp(10), dp(10), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(12);

            TextView nombre = new TextView(this);
            nombre.setText(numero + ". " + diaPlio.ejercicios().get(i));
            nombre.setTextColor(getColor(R.color.texto));
            nombre.setTextSize(16);
            nombre.setTypeface(nombre.getTypeface(), android.graphics.Typeface.BOLD);
            caja.addView(nombre);

            TextView series = new TextView(this);
            series.setText(SesionesPlio.seriesTexto(diaPlio));
            series.setTextColor(getColor(R.color.secundario));
            series.setTextSize(14);
            caja.addView(series);

            EditText nota = new EditText(this);
            nota.setHint("Notas");
            nota.setGravity(Gravity.TOP | Gravity.START);
            nota.setMinLines(2);
            nota.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            nota.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
            nota.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
            String guardadaNota = SesionesPlio.nota(guardada, numero);
            if (guardadaNota != null) nota.setText(guardadaNota);
            // el oyente va después de llenar: llenar la vista no cuenta como un cambio del usuario
            nota.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    guardar();
                }
            });
            caja.addView(nota, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            notas.add(nota);
            contenedor.addView(caja, lp);
        }
    }

    private RegistroCelular registro() {
        return App.almacen().registro(plan.mesocicloId());
    }

    private SesionPlioRegistro sesionGuardada() {
        RegistroCelular r = registro();
        return r == null ? null : SesionesPlio.sesion(r, plio.inicio(), semana, dia);
    }

    /** La sesión tal como está en pantalla (la fecha es la guardada, o la de hoy la primera vez). */
    private SesionPlioRegistro sesionEnPantalla(SesionPlioRegistro guardada) {
        List<EjercicioPlioRegistro> ejercicios = new ArrayList<>();
        for (int i = 0; i < notas.size(); i++) ejercicios.add(new EjercicioPlioRegistro(i + 1, notas.get(i).getText().toString()));
        String fecha = guardada != null && guardada.fecha() != null ? guardada.fecha() : Fechas.hoy();
        Integer duracion = DuracionSesion.paraGuardar(crono, System.currentTimeMillis(), guardada == null ? null : guardada.duracionMin());
        return new SesionPlioRegistro(plio.inicio(), semana, dia, fecha, duracion, ejercicios);
    }

    /** Guarda lo que hay en pantalla (en cada cambio); no guarda si no cambió nada ni crea una sesión vacía. */
    private void guardar() {
        if (diaPlio == null) return;
        SesionPlioRegistro guardada = sesionGuardada();
        SesionPlioRegistro actual = SesionesPlio.limpiar(sesionEnPantalla(guardada));
        if (!SesionesPlio.hayQueGuardar(guardada, actual)) return;
        try {
            App.almacen().guardarRegistro(SesionesPlio.guardar(registro(), actual));
            barra.setSubtitle(TextosSesion.lineaFecha(actual.fecha()));
        } catch (IOException e) {
            Dialogos.aviso(this, "No se pudo guardar", String.valueOf(e.getMessage()));
        }
    }

    private void pedirBorrado() {
        if (sesionGuardada() == null) {
            Dialogos.aviso(this, "Borrar", "Esta sesión no tiene nada anotado.");
            return;
        }
        Dialogos.confirmar(this, "¿Borrar lo anotado en la pliometría de la " + SesionesPlio.titulo(semana, dia).toLowerCase() + "?",
                "Borrar", this::borrar);
    }

    private void borrar() {
        try {
            App.almacen().guardarRegistro(SesionesPlio.borrar(registro(), plio.inicio(), semana, dia));
            crono = Crono.nuevo();
            App.almacen().crono(claveCrono, crono);
        } catch (IOException e) {
            Dialogos.aviso(this, "No se pudo borrar", String.valueOf(e.getMessage()));
            return;
        }
        dibujarCrono();
        armar();
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
