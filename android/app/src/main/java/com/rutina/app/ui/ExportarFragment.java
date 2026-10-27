package com.rutina.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.rutina.app.App;
import com.rutina.app.R;
import com.rutina.app.logica.Sesiones;
import com.rutina.registro.JsonRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Pestaña Exportar: compartir el registro del plan activo o guardarlo en un archivo (Descargas). */
public class ExportarFragment extends Fragment {

    private TextView info;

    private final ActivityResultLauncher<String> crearDocumento =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::alElegirDestino);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor, @Nullable Bundle estado) {
        View v = inflater.inflate(R.layout.fragment_exportar, contenedor, false);
        info = v.findViewById(R.id.info_exportar);
        v.findViewById(R.id.compartir_registro).setOnClickListener(x -> compartir());
        v.findViewById(R.id.guardar_descargas).setOnClickListener(x -> guardarEnDescargas());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        PlanCelular plan = planActivo();
        if (plan == null) {
            info.setText("Cargá un plan en la pestaña Planes");
            return;
        }
        RegistroCelular r = App.almacen().registro(plan.mesocicloId());
        int n = r == null ? 0 : r.sesiones().size();
        info.setText("Plan: " + plan.perfil() + "\nSesiones anotadas: " + n + "\nArchivo: " + Sesiones.nombreArchivoRegistro(plan));
    }

    private PlanCelular planActivo() {
        Long id = App.almacen().activo();
        return id == null ? null : App.almacen().plan(id);
    }

    /** El registro del plan activo, o null (con un aviso) si no hay plan o no hay nada anotado. */
    private RegistroCelular registroParaExportar(PlanCelular plan) {
        if (plan == null) {
            Dialogos.aviso(requireContext(), "Exportar", "Cargá un plan en la pestaña Planes");
            return null;
        }
        RegistroCelular r = App.almacen().registro(plan.mesocicloId());
        if (r == null || r.sesiones().isEmpty()) {
            Dialogos.aviso(requireContext(), "Exportar", "Todavía no anotaste ninguna sesión");
            return null;
        }
        return r;
    }

    private void compartir() {
        PlanCelular plan = planActivo();
        RegistroCelular r = registroParaExportar(plan);
        if (r == null) return;
        String nombre = Sesiones.nombreArchivoRegistro(plan);
        try {
            File carpeta = new File(requireContext().getCacheDir(), "exportados");
            if (!carpeta.isDirectory() && !carpeta.mkdirs()) throw new IOException("No se pudo crear la carpeta temporal");
            File archivo = new File(carpeta, nombre);
            Files.write(archivo.toPath(), JsonRegistro.registroAJson(r).getBytes(StandardCharsets.UTF_8));
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".archivos", archivo);
            Intent enviar = new Intent(Intent.ACTION_SEND);
            enviar.setType("application/json");
            enviar.putExtra(Intent.EXTRA_STREAM, uri);
            enviar.putExtra(Intent.EXTRA_SUBJECT, nombre);
            enviar.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(enviar, "Compartir registro"));
        } catch (IOException e) {
            Dialogos.aviso(requireContext(), "No se pudo exportar", String.valueOf(e.getMessage()));
        }
    }

    private void guardarEnDescargas() {
        PlanCelular plan = planActivo();
        if (registroParaExportar(plan) == null) return;
        crearDocumento.launch(Sesiones.nombreArchivoRegistro(plan));
    }

    private void alElegirDestino(Uri uri) {
        if (uri == null) return;
        PlanCelular plan = planActivo();
        RegistroCelular r = plan == null ? null : App.almacen().registro(plan.mesocicloId());
        if (r == null) return;
        try {
            Archivos.escribirTexto(requireContext().getContentResolver(), uri, JsonRegistro.registroAJson(r));
            Toast.makeText(requireContext(), "Registro guardado", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Dialogos.aviso(requireContext(), "No se pudo guardar", String.valueOf(e.getMessage()));
        }
    }
}
