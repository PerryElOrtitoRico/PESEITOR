package com.rutina.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.rutina.app.App;
import com.rutina.app.R;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;

import java.io.IOException;

/**
 * Inicio de RUTINEITOR: pestañas Sesión · Planes · Exportar. También recibe planes y registros que llegan por el menú
 * Compartir o "Abrir con" de Android, sin mirar el nombre ni el tipo del archivo (decide el campo "formato").
 */
public class MainActivity extends AppCompatActivity {

    private static final String ESTADO_PESTANA = "pestana";

    private TabLayout pestanas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.barra));

        pestanas = findViewById(R.id.pestanas);
        pestanas.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mostrar(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        pestanas.addTab(pestanas.newTab().setText("Sesión"));
        pestanas.addTab(pestanas.newTab().setText("Planes"));
        pestanas.addTab(pestanas.newTab().setText("Exportar"));

        if (savedInstanceState != null) {
            TabLayout.Tab tab = pestanas.getTabAt(savedInstanceState.getInt(ESTADO_PESTANA, 0));
            if (tab != null) tab.select();
        } else {
            manejarIntent(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_PESTANA, pestanas.getSelectedTabPosition());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        manejarIntent(intent);
    }

    private void mostrar(int posicion) {
        Fragment f;
        switch (posicion) {
            case 1:
                f = new PlanesFragment();
                break;
            case 2:
                f = new ExportarFragment();
                break;
            default:
                f = new InicioFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.contenido, f).commit();
    }

    /** Muestra la pestaña Sesión (y la rearma si ya estaba ahí). */
    void irASesion() {
        if (pestanas.getSelectedTabPosition() == 0) {
            mostrar(0);
        } else {
            TabLayout.Tab tab = pestanas.getTabAt(0);
            if (tab != null) tab.select();
        }
    }

    /** Un archivo recibido por Compartir (ACTION_SEND) o "Abrir con" (ACTION_VIEW). */
    @SuppressWarnings("deprecation")
    private void manejarIntent(Intent intent) {
        if (intent == null) return;
        String accion = intent.getAction();
        String texto = null;
        try {
            if (Intent.ACTION_SEND.equals(accion)) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                texto = uri != null ? Archivos.leerTexto(getContentResolver(), uri) : intent.getStringExtra(Intent.EXTRA_TEXT);
            } else if (Intent.ACTION_VIEW.equals(accion) && intent.getData() != null) {
                texto = Archivos.leerTexto(getContentResolver(), intent.getData());
            }
        } catch (IOException | SecurityException e) {
            Dialogos.aviso(this, "No se pudo leer el archivo", String.valueOf(e.getMessage()));
            return;
        }
        if (texto != null) cargarTexto(texto);
    }

    /** Carga un plan o un registro (según su campo "formato"); lo deja activo y muestra la pestaña Sesión. */
    void cargarTexto(String texto) {
        try {
            Object cargado = App.almacen().cargarDesdeTexto(texto);
            if (cargado instanceof PlanCelular) {
                PlanCelular plan = (PlanCelular) cargado;
                App.almacen().activo(plan.mesocicloId());
                Toast.makeText(this, "Plan cargado: " + plan.perfil(), Toast.LENGTH_SHORT).show();
            } else {
                RegistroCelular registro = (RegistroCelular) cargado;
                App.almacen().activo(registro.mesocicloId());
                Toast.makeText(this, "Registro cargado: " + registro.sesiones().size() + " sesiones", Toast.LENGTH_SHORT).show();
            }
            irASesion();
        } catch (IllegalArgumentException e) {
            Dialogos.aviso(this, "No se pudo cargar", String.valueOf(e.getMessage()));
        } catch (IOException e) {
            Dialogos.aviso(this, "No se pudo guardar", String.valueOf(e.getMessage()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.principal, menu);
        menu.findItem(R.id.menu_oscuro).setChecked(App.almacen().oscuro());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_oscuro) {
            boolean oscuro = !item.isChecked();
            item.setChecked(oscuro);
            try {
                App.almacen().oscuro(oscuro);
            } catch (IOException e) {
                // si no se puede guardar, el modo igual se aplica en esta sesión
            }
            App.aplicarTema(oscuro);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
