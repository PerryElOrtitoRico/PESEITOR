package com.rutina.app.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.rutina.app.App;
import com.rutina.app.R;
import com.rutina.app.logica.Fechas;
import com.rutina.registro.PlanCelular;

import java.io.IOException;
import java.util.List;

/** Pestaña Planes: cargar un archivo (plan o registro), elegir el plan activo y borrar planes. */
public class PlanesFragment extends Fragment {

    private LinearLayout lista;
    private TextView vacio;

    private final ActivityResultLauncher<String[]> selector =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::alElegirArchivo);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor, @Nullable Bundle estado) {
        View v = inflater.inflate(R.layout.fragment_planes, contenedor, false);
        lista = v.findViewById(R.id.lista_planes);
        vacio = v.findViewById(R.id.sin_planes);
        v.findViewById(R.id.cargar_archivo).setOnClickListener(x -> selector.launch(new String[]{"*/*"}));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        dibujar();
    }

    private void alElegirArchivo(Uri uri) {
        if (uri == null) return;
        try {
            String texto = Archivos.leerTexto(requireContext().getContentResolver(), uri);
            ((MainActivity) requireActivity()).cargarTexto(texto);
        } catch (IOException | SecurityException e) {
            Dialogos.aviso(requireContext(), "No se pudo leer el archivo", String.valueOf(e.getMessage()));
        }
    }

    private void dibujar() {
        lista.removeAllViews();
        List<PlanCelular> planes = App.almacen().planes();
        vacio.setVisibility(planes.isEmpty() ? View.VISIBLE : View.GONE);
        Long activo = App.almacen().activo();
        for (PlanCelular plan : planes) {
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setBackgroundResource(R.drawable.recuadro);
            fila.setPadding(dp(12), dp(8), dp(8), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);

            boolean esActivo = activo != null && activo == plan.mesocicloId();
            TextView nombre = new TextView(requireContext());
            nombre.setText(plan.perfil() + " · desde " + Fechas.larga(plan.inicio()) + (esActivo ? "  ✓" : ""));
            nombre.setTextColor(requireContext().getColor(R.color.texto));
            nombre.setTextSize(16);
            if (esActivo) nombre.setTypeface(nombre.getTypeface(), android.graphics.Typeface.BOLD);
            nombre.setOnClickListener(x -> activar(plan));
            fila.addView(nombre, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            MaterialButton borrar = new MaterialButton(requireContext());
            borrar.setText("Borrar plan");
            borrar.setOnClickListener(x -> pedirBorrado(plan));
            fila.addView(borrar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            lista.addView(fila, lp);
        }
    }

    private void activar(PlanCelular plan) {
        try {
            App.almacen().activo(plan.mesocicloId());
        } catch (IOException e) {
            Dialogos.aviso(requireContext(), "No se pudo guardar", String.valueOf(e.getMessage()));
        }
        dibujar();
    }

    /** Mismo texto de confirmación que la app web. */
    private void pedirBorrado(PlanCelular plan) {
        String mensaje = "¿Borrar el plan de " + plan.perfil() + " desde " + Fechas.larga(plan.inicio())
                + " y todo lo anotado en él? Exportá el registro antes si lo necesitás.";
        Dialogos.confirmar(requireContext(), mensaje, "Borrar", () -> {
            try {
                App.almacen().borrarPlan(plan.mesocicloId());
            } catch (IOException e) {
                Dialogos.aviso(requireContext(), "No se pudo borrar", String.valueOf(e.getMessage()));
            }
            dibujar();
        });
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
