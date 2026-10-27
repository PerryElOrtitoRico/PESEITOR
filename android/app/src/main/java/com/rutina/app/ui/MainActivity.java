package com.rutina.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.rutina.app.App;
import com.rutina.app.R;

import java.io.IOException;

/** Pantalla de inicio de RUTINEITOR: barra superior con el interruptor de modo oscuro (las pestañas llegan después). */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.barra));
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
