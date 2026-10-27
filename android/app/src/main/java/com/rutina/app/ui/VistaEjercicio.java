package com.rutina.app.ui;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.rutina.app.R;
import com.rutina.app.logica.Sesiones;
import com.rutina.app.logica.TextosSesion;
import com.rutina.app.logica.Totales;
import com.rutina.registro.EjercicioPlan;
import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.FilaPlan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El recuadro de un ejercicio en la pantalla de la sesión: encabezado con el total de reps, tabla con grilla
 * Zona | Planilla | Entrenado, sensación y notas. Cada cambio avisa a {@code alCambiar}.
 */
final class VistaEjercicio {

    /** Una fila de la tabla: la fila del plan, su casilla y el mensaje de abajo. */
    private static final class Casilla {
        final FilaPlan fila;
        final EditText campo;
        final TextView mensaje;

        Casilla(FilaPlan fila, EditText campo, TextView mensaje) {
            this.fila = fila;
            this.campo = campo;
            this.mensaje = mensaje;
        }
    }

    private final Context ctx;
    private final EjercicioPlan plan;
    private final Runnable alCambiar;
    private final View raiz;
    private final TextView reps;
    private final Spinner sensacion;
    private final EditText nota;
    private final List<Casilla> casillas = new ArrayList<>();

    VistaEjercicio(Context ctx, ViewGroup padre, EjercicioPlan plan, EjercicioRegistro anotado, Runnable alCambiar) {
        this.ctx = ctx;
        this.plan = plan;
        this.alCambiar = alCambiar;
        raiz = LayoutInflater.from(ctx).inflate(R.layout.vista_ejercicio, padre, false);
        ((TextView) raiz.findViewById(R.id.nombre)).setText(plan.nombre());
        reps = raiz.findViewById(R.id.reps);
        sensacion = raiz.findViewById(R.id.sensacion);
        nota = raiz.findViewById(R.id.nota);
        TableLayout tabla = raiz.findViewById(R.id.tabla);

        tabla.addView(filaTabla(true, "Zona", "Planilla"));
        for (FilaPlan fila : plan.filas()) {
            String guardado = anotado != null && anotado.filas() != null ? anotado.filas().get(fila.etiqueta()) : null;
            tabla.addView(filaDeEjercicio(fila, guardado));
        }

        Escalas.ESFUERZO.llenar(ctx, sensacion);
        Escalas.ESFUERZO.elegir(sensacion, anotado == null ? null : anotado.esfuerzo());
        if (anotado != null && anotado.nota() != null) nota.setText(anotado.nota());
        nota.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});

        for (Casilla c : casillas) actualizarMensaje(c);
        actualizarTotal();

        // los oyentes van al final: llenar la vista no cuenta como un cambio del usuario
        for (Casilla c : casillas) {
            c.campo.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int d) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int d) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    actualizarMensaje(c);
                    actualizarTotal();
                    VistaEjercicio.this.alCambiar.run();
                }
            });
        }
        nota.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int d) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                VistaEjercicio.this.alCambiar.run();
            }
        });
        sensacion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int posicion, long id) {
                VistaEjercicio.this.alCambiar.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    View vista() {
        return raiz;
    }

    /** Lo que hay anotado ahora en este ejercicio (sin limpiar: las casillas en blanco las saca Sesiones.limpiarSesion). */
    EjercicioRegistro leer() {
        Map<String, String> filas = new LinkedHashMap<>();
        for (Casilla c : casillas) filas.put(c.fila.etiqueta(), c.campo.getText().toString());
        String texto = nota.getText().toString();
        return new EjercicioRegistro(plan.bloque(), Escalas.ESFUERZO.valor(sensacion), filas, texto.isEmpty() ? null : texto);
    }

    private void actualizarTotal() {
        Map<String, String> textos = new LinkedHashMap<>();
        for (Casilla c : casillas) textos.put(c.fila.etiqueta(), c.campo.getText().toString());
        reps.setText(TextosSesion.repsTexto(Totales.totalesEjercicio(plan.filas(), textos)));
    }

    /** Borde rojo y mensaje si la notación es inválida; aviso gris si tiene más series que las planificadas. */
    private void actualizarMensaje(Casilla c) {
        String texto = c.campo.getText().toString();
        String error = TextosSesion.errorCasilla(texto, c.fila.series());
        String ojo = error == null ? TextosSesion.ojoCasilla(texto, c.fila.series()) : null;
        c.campo.setBackgroundResource(error == null ? R.drawable.fondo_campo : R.drawable.fondo_campo_error);
        String mensaje = error != null ? error : ojo;
        c.mensaje.setVisibility(mensaje == null ? View.GONE : View.VISIBLE);
        c.mensaje.setText(mensaje == null ? "" : mensaje);
        c.mensaje.setTextColor(ctx.getColor(error != null ? R.color.error : R.color.secundario));
    }

    private TableRow filaDeEjercicio(FilaPlan fila, String guardado) {
        String pista = Sesiones.pista(fila.series());
        TableRow tr = new TableRow(ctx);
        tr.addView(celda(fila.etiqueta(), false, true), new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT));
        tr.addView(celda(pista, false, false), new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT));

        LinearLayout caja = new LinearLayout(ctx);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setBackgroundResource(R.drawable.celda);
        caja.setPadding(dp(6), dp(6), dp(7), dp(7));
        EditText campo = new EditText(ctx);
        campo.setHint(pista);
        campo.setSingleLine(true);
        // teclado numérico que deja escribir dígitos, punto, coma, guion y espacio (la notación 3.3-85,5)
        campo.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,- "));
        campo.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        campo.setContentDescription(plan.nombre() + " " + fila.etiqueta());
        if (guardado != null) campo.setText(guardado);
        TextView mensaje = new TextView(ctx);
        mensaje.setTextSize(12);
        mensaje.setVisibility(View.GONE);
        caja.addView(campo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        caja.addView(mensaje, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tr.addView(caja, new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f));
        casillas.add(new Casilla(fila, campo, mensaje));
        return tr;
    }

    private TableRow filaTabla(boolean encabezado, String zona, String planilla) {
        TableRow tr = new TableRow(ctx);
        tr.addView(celda(zona, encabezado, true), new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT));
        tr.addView(celda(planilla, encabezado, false), new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT));
        tr.addView(celda("Entrenado", encabezado, false), new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f));
        return tr;
    }

    private TextView celda(String texto, boolean encabezado, boolean negrita) {
        TextView t = new TextView(ctx);
        t.setText(texto);
        t.setTextColor(ctx.getColor(R.color.texto));
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(8), dp(8), dp(9), dp(9));
        t.setBackgroundResource(encabezado ? R.drawable.celda_encabezado : R.drawable.celda);
        if (encabezado || negrita) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private int dp(int valor) {
        return Math.round(valor * ctx.getResources().getDisplayMetrics().density);
    }
}
