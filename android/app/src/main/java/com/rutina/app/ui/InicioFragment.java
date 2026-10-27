package com.rutina.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rutina.app.App;
import com.rutina.app.R;
import com.rutina.app.logica.Fechas;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Pestaña Sesión: elegir plan, microciclo y día, abrir la sesión y ver las sesiones ya anotadas. */
public class InicioFragment extends Fragment {

    private static final String ESTADO_MICRO = "micro";
    private static final String ESTADO_DIA = "dia";

    private View conPlan;
    private TextView sinPlan;
    private Spinner elegirPlan;
    private Spinner elegirMicro;
    private Spinner elegirDia;
    private TableLayout tabla;
    private TextView tituloTabla;

    private List<PlanCelular> planes = new ArrayList<>();
    private List<Integer> micros = new ArrayList<>();
    private List<Integer> dias = new ArrayList<>();
    private int microSel = 1;
    private int diaSel = 1;
    /** true mientras se llenan los desplegables por código (para ignorar los eventos que eso dispara). */
    private boolean cargando;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor, @Nullable Bundle estado) {
        View v = inflater.inflate(R.layout.fragment_inicio, contenedor, false);
        conPlan = v.findViewById(R.id.con_plan);
        sinPlan = v.findViewById(R.id.sin_plan);
        elegirPlan = v.findViewById(R.id.elegir_plan);
        elegirMicro = v.findViewById(R.id.elegir_micro);
        elegirDia = v.findViewById(R.id.elegir_dia);
        tabla = v.findViewById(R.id.tabla_sesiones);
        tituloTabla = v.findViewById(R.id.titulo_tabla);
        if (estado != null) {
            microSel = estado.getInt(ESTADO_MICRO, 1);
            diaSel = estado.getInt(ESTADO_DIA, 1);
        }

        elegirPlan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int posicion, long id) {
                if (cargando || posicion >= planes.size()) return;
                PlanCelular plan = planes.get(posicion);
                Long activo = App.almacen().activo();
                if (activo != null && activo == plan.mesocicloId()) return;
                try {
                    App.almacen().activo(plan.mesocicloId());
                } catch (java.io.IOException e) {
                    // el plan se usa igual en esta pantalla
                }
                rearmar(plan);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        elegirMicro.setOnItemSelectedListener(new AlRecordar(true));
        elegirDia.setOnItemSelectedListener(new AlRecordar(false));
        v.findViewById(R.id.abrir_sesion).setOnClickListener(x -> abrirSesion());
        return v;
    }

    /** Recuerda el microciclo y el día elegidos. */
    private final class AlRecordar implements AdapterView.OnItemSelectedListener {
        private final boolean esMicro;

        AlRecordar(boolean esMicro) {
            this.esMicro = esMicro;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int posicion, long id) {
            if (cargando) return;
            if (esMicro && posicion < micros.size()) microSel = micros.get(posicion);
            if (!esMicro && posicion < dias.size()) diaSel = dias.get(posicion);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        recargar();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle salida) {
        super.onSaveInstanceState(salida);
        salida.putInt(ESTADO_MICRO, microSel);
        salida.putInt(ESTADO_DIA, diaSel);
    }

    /** Vuelve a leer los planes del almacén y rearma los desplegables y la tabla. */
    private void recargar() {
        planes = App.almacen().planes();
        boolean hay = !planes.isEmpty();
        conPlan.setVisibility(hay ? View.VISIBLE : View.GONE);
        sinPlan.setVisibility(hay ? View.GONE : View.VISIBLE);
        if (!hay) return;

        Long activo = App.almacen().activo();
        int indice = 0;
        List<String> nombres = new ArrayList<>();
        for (int i = 0; i < planes.size(); i++) {
            PlanCelular p = planes.get(i);
            nombres.add(p.perfil() + " · desde " + Fechas.larga(p.inicio()));
            if (activo != null && activo == p.mesocicloId()) indice = i;
        }
        PlanCelular plan = planes.get(indice);
        if (activo == null || activo != plan.mesocicloId()) {
            try {
                App.almacen().activo(plan.mesocicloId());
            } catch (java.io.IOException e) {
                // sin poder guardarlo, se usa igual
            }
        }
        cargando = true;
        elegirPlan.setAdapter(adaptador(nombres));
        elegirPlan.setSelection(indice);
        cargando = false;
        rearmar(plan);
    }

    /** Microciclos, días y tabla de sesiones anotadas del plan elegido. */
    private void rearmar(PlanCelular plan) {
        TreeSet<Integer> m = new TreeSet<>();
        TreeSet<Integer> d = new TreeSet<>();
        for (SesionPlan s : plan.sesiones()) {
            m.add(s.microciclo());
            d.add(s.dia());
        }
        micros = new ArrayList<>(m);
        dias = new ArrayList<>(d);
        List<String> textosMicro = new ArrayList<>();
        for (int x : micros) textosMicro.add(String.valueOf(x));
        List<String> textosDia = new ArrayList<>();
        for (int x : dias) textosDia.add(String.valueOf(x));

        cargando = true;
        elegirMicro.setAdapter(adaptador(textosMicro));
        elegirDia.setAdapter(adaptador(textosDia));
        if (micros.contains(microSel)) elegirMicro.setSelection(micros.indexOf(microSel));
        else if (!micros.isEmpty()) microSel = micros.get(0);
        if (dias.contains(diaSel)) elegirDia.setSelection(dias.indexOf(diaSel));
        else if (!dias.isEmpty()) diaSel = dias.get(0);
        cargando = false;

        dibujarTabla(plan);
    }

    private ArrayAdapter<String> adaptador(List<String> textos) {
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, textos);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    /** Tabla Micro | Día | Anotada (dd/MM) con las sesiones que ya tienen algo anotado. */
    private void dibujarTabla(PlanCelular plan) {
        tabla.removeAllViews();
        RegistroCelular registro = App.almacen().registro(plan.mesocicloId());
        List<SesionRegistro> sesiones = registro == null ? new ArrayList<>() : registro.sesiones();
        tituloTabla.setText("Sesiones anotadas: " + sesiones.size() + " de " + plan.sesiones().size());
        tabla.addView(fila(true, "Micro", "Día", "Anotada"));
        for (SesionRegistro s : sesiones) {
            tabla.addView(fila(false, String.valueOf(s.microciclo()), String.valueOf(s.dia()), Fechas.corta(s.fecha())));
        }
    }

    private TableRow fila(boolean encabezado, String... textos) {
        TableRow fila = new TableRow(requireContext());
        for (String t : textos) {
            TextView c = new TextView(requireContext());
            c.setText(t);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(8), dp(8), dp(8), dp(8));
            c.setTextColor(requireContext().getColor(R.color.texto));
            c.setBackgroundResource(encabezado ? R.drawable.celda_encabezado : R.drawable.celda);
            if (encabezado) c.setTypeface(c.getTypeface(), android.graphics.Typeface.BOLD);
            fila.addView(c, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        }
        return fila;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private void abrirSesion() {
        Long activo = App.almacen().activo();
        if (activo == null) return;
        Intent i = new Intent(requireContext(), SesionActivity.class);
        i.putExtra(SesionActivity.EXTRA_PLAN, (long) activo);
        i.putExtra(SesionActivity.EXTRA_MICRO, microSel);
        i.putExtra(SesionActivity.EXTRA_DIA, diaSel);
        startActivity(i);
    }
}
