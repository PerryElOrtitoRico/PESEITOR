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
import com.rutina.registro.DiaPlioCelular;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.PlanPlio;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionPlioRegistro;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Pestaña Pliometría: elegir plan de pliometría, semana y día del plan activo, abrir la sesión y ver las anotadas. */
public class PlioFragment extends Fragment {

    private View conPlio;
    private TextView sinPlio;
    private Spinner elegirPlio;
    private Spinner elegirSemana;
    private Spinner elegirDia;
    private TableLayout tabla;
    private TextView tituloTabla;

    private PlanCelular plan;
    private List<Integer> semanas = new ArrayList<>();
    private List<Integer> dias = new ArrayList<>();
    private int indicePlio;
    private int semanaSel = 1;
    private int diaSel = 1;
    /** true mientras se llenan los desplegables por código (para ignorar los eventos que eso dispara). */
    private boolean cargando;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor, @Nullable Bundle estado) {
        View v = inflater.inflate(R.layout.fragment_plio, contenedor, false);
        conPlio = v.findViewById(R.id.con_plio);
        sinPlio = v.findViewById(R.id.sin_plio);
        elegirPlio = v.findViewById(R.id.elegir_plio);
        elegirSemana = v.findViewById(R.id.elegir_semana);
        elegirDia = v.findViewById(R.id.elegir_dia_plio);
        tabla = v.findViewById(R.id.tabla_plio);
        tituloTabla = v.findViewById(R.id.titulo_tabla_plio);

        elegirPlio.setOnItemSelectedListener(new Al(0));
        elegirSemana.setOnItemSelectedListener(new Al(1));
        elegirDia.setOnItemSelectedListener(new Al(2));
        v.findViewById(R.id.abrir_plio).setOnClickListener(x -> abrir());
        return v;
    }

    /** Recuerda lo elegido; cambiar de plan de pliometría rearma el resto. */
    private final class Al implements AdapterView.OnItemSelectedListener {
        private final int cual;

        Al(int cual) {
            this.cual = cual;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int posicion, long id) {
            if (cargando) return;
            if (cual == 0 && posicion < plan.pliometria().size()) {
                indicePlio = posicion;
                rearmar();
            } else if (cual == 1 && posicion < semanas.size()) {
                semanaSel = semanas.get(posicion);
            } else if (cual == 2 && posicion < dias.size()) {
                diaSel = dias.get(posicion);
            }
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

    private void recargar() {
        Long activo = App.almacen().activo();
        plan = activo == null ? null : App.almacen().plan(activo);
        if (plan == null || plan.pliometria().isEmpty()) {
            conPlio.setVisibility(View.GONE);
            sinPlio.setVisibility(View.VISIBLE);
            sinPlio.setText(plan == null ? "Cargá un plan en la pestaña Planes"
                    : "Este plan no trae pliometría. En PESEITOR, planificala en Macrociclo y volvé a exportar el plan al celular.");
            return;
        }
        conPlio.setVisibility(View.VISIBLE);
        sinPlio.setVisibility(View.GONE);
        if (indicePlio >= plan.pliometria().size()) indicePlio = 0;
        List<String> nombres = new ArrayList<>();
        for (PlanPlio p : plan.pliometria()) nombres.add("Nivel " + p.nivel() + " · desde " + Fechas.larga(p.inicio()));
        cargando = true;
        elegirPlio.setAdapter(adaptador(nombres));
        elegirPlio.setSelection(indicePlio);
        cargando = false;
        rearmar();
    }

    private void rearmar() {
        PlanPlio plio = plan.pliometria().get(indicePlio);
        TreeSet<Integer> d = new TreeSet<>();
        for (List<DiaPlioCelular> semana : plio.semanas()) for (DiaPlioCelular x : semana) d.add(x.dia());
        semanas = new ArrayList<>();
        for (int i = 1; i <= plio.semanas().size(); i++) semanas.add(i);
        dias = new ArrayList<>(d);
        cargando = true;
        elegirSemana.setAdapter(adaptador(textos(semanas)));
        elegirDia.setAdapter(adaptador(textos(dias)));
        if (semanas.contains(semanaSel)) elegirSemana.setSelection(semanas.indexOf(semanaSel));
        else if (!semanas.isEmpty()) semanaSel = semanas.get(0);
        if (dias.contains(diaSel)) elegirDia.setSelection(dias.indexOf(diaSel));
        else if (!dias.isEmpty()) diaSel = dias.get(0);
        cargando = false;
        dibujarTabla(plio);
    }

    private static List<String> textos(List<Integer> numeros) {
        List<String> t = new ArrayList<>();
        for (int x : numeros) t.add(String.valueOf(x));
        return t;
    }

    private ArrayAdapter<String> adaptador(List<String> textos) {
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, textos);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    /** Tabla Semana | Día | Fecha con las sesiones de este plan de pliometría que ya tienen algo anotado. */
    private void dibujarTabla(PlanPlio plio) {
        tabla.removeAllViews();
        RegistroCelular registro = App.almacen().registro(plan.mesocicloId());
        List<SesionPlioRegistro> hechas = new ArrayList<>();
        if (registro != null) for (SesionPlioRegistro s : registro.pliometria()) if (s.inicio().equals(plio.inicio())) hechas.add(s);
        int total = 0;
        for (List<DiaPlioCelular> semana : plio.semanas()) total += semana.size();
        tituloTabla.setText("Sesiones anotadas: " + hechas.size() + " de " + total);
        tabla.addView(fila(true, "Semana", "Día", "Anotada"));
        for (SesionPlioRegistro s : hechas) {
            tabla.addView(fila(false, String.valueOf(s.semana()), String.valueOf(s.dia()), Fechas.corta(s.fecha())));
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

    private void abrir() {
        if (plan == null || plan.pliometria().isEmpty()) return;
        Intent i = new Intent(requireContext(), PlioActivity.class);
        i.putExtra(PlioActivity.EXTRA_PLAN, plan.mesocicloId());
        i.putExtra(PlioActivity.EXTRA_INICIO, plan.pliometria().get(indicePlio).inicio());
        i.putExtra(PlioActivity.EXTRA_SEMANA, semanaSel);
        i.putExtra(PlioActivity.EXTRA_DIA, diaSel);
        startActivity(i);
    }
}
