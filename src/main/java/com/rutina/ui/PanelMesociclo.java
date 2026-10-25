package com.rutina.ui;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import com.rutina.pdf.ExportadorPdf;
import com.rutina.registro.AnalisisRegistro;
import com.rutina.registro.ArmadorPlan;
import com.rutina.registro.ComparacionTipo;
import com.rutina.registro.EscalaDescanso;
import com.rutina.registro.EscalaFinal;
import com.rutina.registro.JsonRegistro;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.ResumenSesion;
import com.rutina.service.Armado;
import com.rutina.service.calculo.CalculadoraMesociclo;
import com.rutina.service.calculo.GeneradorSeries;
import com.rutina.service.calculo.ParametrosInvalidosException;
import com.rutina.service.calculo.Redondeo;
import com.rutina.service.calculo.ResultadoMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo.Contador;
import com.rutina.service.calculo.ResultadoMesociclo.ResultadoBloque;
import com.rutina.service.calculo.ResultadoMesociclo.SemanaBloque;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Pestaña Mesociclo: grilla de días y ejercicios, series calculadas, contadores, gráfico, registro y exportación. */
final class PanelMesociclo extends JPanel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Contexto ctx;
    private final JComboBox<String> selector = new JComboBox<>();
    private final JComboBox<String> variante = new JComboBox<>();
    private final JComboBox<String> dias = new JComboBox<>(new String[]{"—", "3", "4", "5"});
    private final JPanel grilla = new JPanel();
    private final DefaultTableModel contadores = new DefaultTableModel(
            new Object[]{"Tipo", "Micro", "Día", "Teoría", "Planilla", "Diferencia", "Sin asignar"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JPanel grafico = new JPanel(new BorderLayout());
    /** Planilla (lo planificado) contra Entrenado (lo anotado en el celular), una fila por sesión y tipo. */
    private final DefaultTableModel registro = new DefaultTableModel(
            new Object[]{"Micro", "Día", "Fecha", "Descanso", "Final", "Esfuerzo", "Tipo", "Vol planilla",
                    "Vol entrenado", "IMR planilla", "IMR entrenado"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JPanel pliometria = new JPanel();
    private final JTextArea calentamiento = new JTextArea(3, 30);
    private final JTextArea auxiliares = new JTextArea(3, 30);
    private final JLabel avisos = new JLabel(" ");

    private List<Mesociclo> lista = List.of();
    private Mesociclo actual;
    private Map<Long, Ejercicio> ejs = Map.of();
    private ResultadoMesociclo resultado;
    private boolean cargando;

    PanelMesociclo(Contexto ctx) {
        super(new BorderLayout());
        this.ctx = ctx;

        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        arriba.add(new JLabel("Mesociclo:"));
        arriba.add(selector);
        arriba.add(new JLabel("  Variante:"));
        arriba.add(variante);
        arriba.add(new JLabel("  Días por microciclo:"));
        arriba.add(dias);
        JButton exportar = new JButton("Exportar PDF");
        exportar.addActionListener(e -> exportar());
        JButton recalcular = new JButton("Recalcular con 1RM actuales");
        recalcular.addActionListener(e -> recalcularRm());
        JButton alCelular = new JButton("Plan al celular");
        alCelular.setToolTipText("Guarda el plan en un archivo .json para cargarlo en la app del celular");
        alCelular.addActionListener(e -> planAlCelular());
        JButton importar = new JButton("Importar registro");
        importar.setToolTipText("Lee el registro exportado desde el celular y lo compara con la planilla");
        importar.addActionListener(e -> importarRegistro());
        arriba.add(exportar);
        arriba.add(alCelular);
        arriba.add(importar);
        arriba.add(recalcular);
        add(arriba, BorderLayout.NORTH);

        selector.addActionListener(e -> {
            if (!cargando) elegir(selector.getSelectedIndex());
        });
        variante.addActionListener(e -> {
            if (!cargando) cambiarVariante();
        });
        dias.addActionListener(e -> {
            if (!cargando) cambiarDias();
        });

        JTabbedPane derecha = new JTabbedPane();
        derecha.addTab("Contadores", new JScrollPane(new JTable(contadores)));
        derecha.addTab("Gráfico", grafico);
        JTable tablaRegistro = new JTable(registro);
        tablaRegistro.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaRegistro.getColumnModel().getColumn(3).setPreferredWidth(220);
        tablaRegistro.getColumnModel().getColumn(4).setPreferredWidth(110);
        tablaRegistro.getColumnModel().getColumn(2).setPreferredWidth(90);
        derecha.addTab("Registro", new JScrollPane(tablaRegistro));
        pliometria.setLayout(new BoxLayout(pliometria, BoxLayout.Y_AXIS));
        JPanel envolturaPlio = new JPanel(new BorderLayout());
        envolturaPlio.add(pliometria, BorderLayout.NORTH);
        derecha.addTab("Pliometría", new JScrollPane(envolturaPlio));
        JPanel textos = new JPanel(new GridLayout(2, 1, 4, 4));
        textos.add(conTitulo(calentamiento, "Calentamiento"));
        textos.add(conTitulo(auxiliares, "Auxiliares"));
        derecha.addTab("Calentamiento y auxiliares", textos);
        FocusAdapter guardarTextos = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // solo si cambió, y no por pérdidas temporales de foco (otra ventana, un diálogo) ni al cerrar
                if (actual == null || e.isTemporary() || !isShowing()) return;
                if (calentamiento.getText().equals(actual.calentamiento()) && auxiliares.getText().equals(actual.auxiliares())) return;
                guardar(con(actual.bloques(), actual.aproximaciones()));
            }
        };
        calentamiento.addFocusListener(guardarTextos);
        auxiliares.addFocusListener(guardarTextos);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(grilla), derecha);
        split.setResizeWeight(0.7);
        add(split, BorderLayout.CENTER);
        avisos.setForeground(new Color(170, 60, 0));
        add(avisos, BorderLayout.SOUTH);

        ctx.alCambiar(this::recargar);
        recargar();
    }

    private static JScrollPane conTitulo(JTextArea t, String titulo) {
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createTitledBorder(titulo));
        return sp;
    }

    /** Muestra un mesociclo por id (desde el Macrociclo). */
    void mostrar(long id) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).id() == id) {
                selector.setSelectedIndex(i);
                return;
            }
        }
    }

    private void recargar() {
        try {
            lista = ctx.mesociclos.listar(ctx.perfil.id());
            ejs = ctx.mapaEjercicios();
        } catch (Exception e) {
            Contexto.error(this, e);
            return;
        }
        long idActual = actual == null ? -1 : actual.id();
        cargando = true;
        selector.removeAllItems();
        int sel = lista.isEmpty() ? -1 : 0;
        for (int i = 0; i < lista.size(); i++) {
            Mesociclo m = lista.get(i);
            selector.addItem("Desde " + m.inicio().format(FECHA) + " · " + m.microciclos() + " microciclos");
            if (m.id() == idActual) sel = i;
        }
        cargando = false;
        if (sel >= 0) selector.setSelectedIndex(sel);
        elegir(sel);
    }

    private void elegir(int i) {
        actual = i < 0 || i >= lista.size() ? null : lista.get(i);
        cargando = true;
        variante.removeAllItems();
        variante.addItem("—");
        if (actual != null) {
            for (char v : new char[]{'A', 'B', 'C'}) {
                if (ctx.metodologia().variantes().containsKey("" + actual.microciclos() + v)) variante.addItem(String.valueOf(v));
            }
            variante.setSelectedItem(actual.variante() == null ? "—" : String.valueOf(actual.variante()));
            dias.setSelectedItem(actual.dias() == null ? "—" : String.valueOf(actual.dias()));
            calentamiento.setText(actual.calentamiento());
            auxiliares.setText(actual.auxiliares());
        } else {
            calentamiento.setText("");
            auxiliares.setText("");
        }
        cargando = false;
        recalcular();
    }

    // ---- cálculo y dibujo ----------------------------------------------------------------------------------

    private void recalcular() {
        resultado = null;
        contadores.setRowCount(0);
        grafico.removeAll();
        if (actual == null) {
            avisos.setText(" Creá un mesociclo en la pestaña Macrociclo.");
        } else if (actual.variante() == null || actual.dias() == null) {
            avisos.setText(" Elegí la variante y los días por microciclo.");
        } else {
            try {
                resultado = CalculadoraMesociclo.calcular(ctx.metodologia(), actual, ejs);
                avisos.setText(resultado.avisos().isEmpty() ? " " : " ⚠ " + String.join("   ⚠ ", resultado.avisos()));
                for (Contador c : resultado.contadores()) {
                    contadores.addRow(new Object[]{PanelEjercicios.titulo(c.tipo()), c.microciclo(),
                            c.dia() == 0 ? "sin ejercicios" : c.dia(), c.planificado(), c.asignado(),
                            c.asignado() - c.planificado(), c.sinAsignar()});
                }
                grafico.add(Graficos.volumenIntensidad(resultado.planificado(), resultado.real(), "Micro "));
            } catch (ParametrosInvalidosException e) {
                avisos.setText(" ⚠ " + e.getMessage());
            }
        }
        grafico.revalidate();
        grafico.repaint();
        llenarRegistro();
        dibujarPliometria();
        dibujarGrilla();
    }

    /** Planilla vs entrenado del último registro importado; si el registro no cuadra con el plan, el motivo va a avisos. */
    private void llenarRegistro() {
        registro.setRowCount(0);
        if (actual == null || resultado == null) return;
        try {
            Optional<RegistroCelular> guardado = ctx.registros.cargar(actual.id());
            if (guardado.isEmpty()) return;
            var plan = ArmadorPlan.armar(ctx.perfil.nombre(), actual, resultado, ctx.metodologia());
            for (ResumenSesion s : AnalisisRegistro.analizar(plan, guardado.get())) {
                for (ComparacionTipo c : s.porTipo()) {
                    registro.addRow(new Object[]{s.microciclo(), s.dia(), s.fecha(),
                            s.descanso() == null ? "" : s.descanso() + " · " + EscalaDescanso.deValor(s.descanso()).texto(),
                            s.sensacionFinal() == null ? "" : s.sensacionFinal() + " · " + EscalaFinal.deValor(s.sensacionFinal()).texto(),
                            decimal(s.esfuerzoPromedio()), PanelEjercicios.titulo(c.tipo()), c.volumenPlan(), c.volumenReal(),
                            decimal(c.imrPlan()), decimal(c.imrReal())});
                }
            }
        } catch (Exception e) {
            registro.setRowCount(0);
            avisos.setText(" ⚠ Registro: " + e.getMessage());
        }
    }

    /** Un decimal con coma; null = celda vacía. */
    private static String decimal(Double v) {
        return v == null ? "" : String.format("%.1f", v).replace('.', ',');
    }

    /** Planes de pliometría que caen en las fechas del mesociclo (se planifican en Macrociclo). */
    private void dibujarPliometria() {
        pliometria.removeAll();
        if (actual != null) {
            try {
                var planes = Armado.planesDelMesociclo(ctx.metodologia(), actual, ctx.planes.listar(ctx.perfil.id()),
                        ctx.ejercicios.listar(ctx.perfil.id()));
                if (planes.isEmpty()) {
                    pliometria.add(new JLabel("<html>No hay pliometría en las fechas de este mesociclo.<br>"
                            + "Los planes se crean, editan y borran en la pestaña <b>Macrociclo</b>.</html>"));
                }
                for (var pc : planes) pliometria.add(tablaPlio(pc));
            } catch (Exception e) {
                pliometria.add(new JLabel("No se pudo cargar la pliometría: " + e.getMessage()));
            }
        }
        pliometria.revalidate();
        pliometria.repaint();
    }

    private JPanel tablaPlio(com.rutina.pdf.PlanConDias pc) {
        int semanas = pc.semanas().size();
        Object[] cols = new Object[semanas + 2];
        cols[0] = "Día";
        cols[1] = "Ejercicio";
        for (int w = 1; w <= semanas; w++) {
            cols[w + 1] = "S" + w + (pc.semanasDentroDelMesociclo().contains(w) ? "" : " (fuera)");
        }
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        int dias = pc.semanas().get(0).size();
        for (int d = 0; d < dias; d++) {
            List<String> ejercicios = pc.semanas().get(0).get(d).ejercicios();
            if (ejercicios.isEmpty()) {
                Object[] fila = new Object[semanas + 2];
                fila[0] = d + 1;
                fila[1] = "(sin ejercicios: cargalos en Ejercicios → Pliometría)";
                m.addRow(fila);
            }
            for (int k = 0; k < ejercicios.size(); k++) {
                Object[] fila = new Object[semanas + 2];
                fila[0] = k == 0 ? d + 1 : "";
                fila[1] = ejercicios.get(k);
                for (int w = 0; w < semanas; w++) {
                    fila[w + 2] = pc.semanas().get(w).get(d).series() + " x " + com.rutina.service.calculo.Pliometria.SALTOS_POR_SERIE;
                }
                m.addRow(fila);
            }
        }
        JTable t = new JTable(m);
        t.getColumnModel().getColumn(0).setMaxWidth(40);
        t.getColumnModel().getColumn(1).setPreferredWidth(260);
        t.setPreferredScrollableViewportSize(t.getPreferredSize());
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Nivel " + pc.plan().nivel() + " · desde "
                + pc.plan().inicio().format(FECHA)));
        p.add(t.getTableHeader(), BorderLayout.NORTH);
        p.add(t, BorderLayout.CENTER);
        return p;
    }

    private void dibujarGrilla() {
        grilla.removeAll();
        if (actual == null || actual.dias() == null) {
            grilla.revalidate();
            grilla.repaint();
            return;
        }
        int n = actual.dias();
        grilla.setLayout(new GridLayout(1, n, 6, 0));
        for (int d = 1; d <= n; d++) {
            JPanel col = new JPanel();
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.setBorder(BorderFactory.createTitledBorder("DÍA " + d));
            final int dia = d;
            actual.bloques().stream().filter(b -> b.dia() == dia)
                    .sorted((a, b) -> Integer.compare(a.posicion(), b.posicion()))
                    .forEach(b -> col.add(panelBloque(b)));
            JButton agregar = new JButton("+ Agregar ejercicio");
            agregar.addActionListener(e -> agregar(dia));
            col.add(agregar);
            col.add(Box.createVerticalGlue());
            JPanel envoltura = new JPanel(new BorderLayout());
            envoltura.add(col, BorderLayout.NORTH);
            grilla.add(envoltura);
        }
        grilla.revalidate();
        grilla.repaint();
    }

    private JPanel panelBloque(Bloque b) {
        Ejercicio e = ejs.get(b.ejercicioId());
        JPanel p = new JPanel(new BorderLayout());
        String nombre = e == null ? "(ejercicio borrado)" : e.nombre();
        boolean compleja = Armado.esCompleja(e, actual);
        String modo = e == null ? "" : e.tipo() == Tipo.FUERZA ? (compleja ? "fuerza compleja" : "fuerza básica")
                : PanelEjercicios.titulo(e.tipo()).toLowerCase();
        p.setBorder(BorderFactory.createTitledBorder("E" + b.posicion() + " · " + nombre + " (" + modo + ")"));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        if (compleja) {
            JSpinner pct = new JSpinner(new SpinnerNumberModel(Math.round(b.pctBolsa() * 10) / 10.0, 0.0, 100.0, 5.0));
            pct.setPreferredSize(new Dimension(70, pct.getPreferredSize().height));
            // se guarda medio segundo después del último cambio (mantener apretada la flecha no guarda en cada paso)
            Timer espera = new Timer(500, ev -> cambiarPct(b.dia(), b.posicion(), (Double) pct.getValue()));
            espera.setRepeats(false);
            pct.addChangeListener(ev -> espera.restart());
            acciones.add(new JLabel("% de la bolsa:"));
            acciones.add(pct);
        }
        if (e != null && e.tipo() == Tipo.FUERZA) {
            JButton aprox = new JButton(compleja ? "Pasar a básica" : "Pasar a compleja");
            aprox.addActionListener(ev -> cambiarAproximacion(e, compleja ? Aproximacion.BASICA : Aproximacion.COMPLEJA));
            acciones.add(aprox);
        }
        JButton subir = new JButton("↑");
        subir.setToolTipText("Subir en el día");
        subir.addActionListener(ev -> mover(b, -1));
        JButton bajar = new JButton("↓");
        bajar.setToolTipText("Bajar en el día");
        bajar.addActionListener(ev -> mover(b, +1));
        acciones.add(subir);
        acciones.add(bajar);
        JButton quitar = new JButton("Quitar");
        quitar.addActionListener(ev -> quitar(b));
        acciones.add(quitar);
        p.add(acciones, BorderLayout.NORTH);

        ResultadoBloque rb = resultado == null ? null : resultado.bloques().stream()
                .filter(x -> x.bloque().equals(b)).findFirst().orElse(null);
        if (rb != null) p.add(tablaSeries(rb), BorderLayout.CENTER);
        return p;
    }

    private JTable tablaSeries(ResultadoBloque rb) {
        int semanas = rb.semanas().size();
        Object[] cols = new Object[semanas + 1];
        cols[0] = "Zona";
        for (int w = 1; w <= semanas; w++) cols[w] = "S" + w;
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        Object[] vol = new Object[semanas + 1];
        vol[0] = "Volumen";
        for (int w = 0; w < semanas; w++) vol[w + 1] = rb.semanas().get(w).volumen();
        m.addRow(vol);
        Double rm = actual.rmUsados().getOrDefault(rb.ejercicio().id(), rb.ejercicio().rm());
        boolean plantilla = rb.semanas().stream().anyMatch(s -> s.celda() != null);
        if (plantilla) {
            Object[] fila = new Object[semanas + 1];
            fila[0] = "Series";
            for (int w = 0; w < semanas; w++) {
                SemanaBloque s = rb.semanas().get(w);
                fila[w + 1] = s.celda() == null ? "" : s.celda().texto();
            }
            m.addRow(fila);
        } else {
            for (Zona z : Zona.values()) {
                Object[] fila = new Object[semanas + 1];
                boolean alguna = false;
                for (int w = 0; w < semanas; w++) {
                    List<List<Integer>> l = rb.semanas().get(w).seriesPorZona().get(z);
                    String t = l == null || l.isEmpty() ? "" : GeneradorSeries.notacion(l.get(0));
                    alguna |= !t.isEmpty();
                    fila[w + 1] = t;
                }
                if (!alguna) continue;
                fila[0] = z.pct() + "%" + (rm == null ? "" : " · " + fmt(Redondeo.kg(rm, z.pct(), ctx.metodologia().incrementoKg())));
                m.addRow(fila);
            }
        }
        JTable t = new JTable(m);
        t.getColumnModel().getColumn(0).setPreferredWidth(110);
        t.setPreferredScrollableViewportSize(t.getPreferredSize());
        return t;
    }

    private static String fmt(double v) {
        return v % 1 == 0 ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
    }

    // ---- cambios -------------------------------------------------------------------------------------------

    private Mesociclo con(List<Bloque> bloques, Map<Long, Aproximacion> aprox) {
        return new Mesociclo(actual.id(), actual.perfilId(), actual.inicio(), actual.microciclos(), actual.potencia(),
                actual.fuerza(), actual.variante(), actual.dias(), aprox, bloques, calentamiento.getText(),
                auxiliares.getText(), actual.rmUsados());
    }

    private void guardar(Mesociclo m) {
        try {
            actual = ctx.mesociclos.guardar(m);
            // se redibuja después: si se hiciera acá, se destruiría el componente cuyo evento se está atendiendo
            SwingUtilities.invokeLater(ctx::cambio);
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    private void cambiarVariante() {
        if (actual == null) return;
        String v = (String) variante.getSelectedItem();
        Character c = v == null || v.equals("—") ? null : v.charAt(0);
        guardar(new Mesociclo(actual.id(), actual.perfilId(), actual.inicio(), actual.microciclos(), actual.potencia(),
                actual.fuerza(), c, actual.dias(), actual.aproximaciones(), actual.bloques(), actual.calentamiento(),
                actual.auxiliares(), actual.rmUsados()));
    }

    private void cambiarDias() {
        if (actual == null) return;
        String v = (String) dias.getSelectedItem();
        Integer n = v == null || v.equals("—") ? null : Integer.parseInt(v);
        List<Bloque> bloques = actual.bloques();
        if (n != null) {
            List<Bloque> quedan = bloques.stream().filter(b -> b.dia() <= n).toList();
            if (quedan.size() < bloques.size() && JOptionPane.showConfirmDialog(this,
                    "Se van a quitar los ejercicios de los días que sobran. ¿Seguir?", "Días",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                elegir(selector.getSelectedIndex());
                return;
            }
            bloques = quedan;
        }
        Mesociclo m = new Mesociclo(actual.id(), actual.perfilId(), actual.inicio(), actual.microciclos(),
                actual.potencia(), actual.fuerza(), actual.variante(), n, actual.aproximaciones(), bloques,
                actual.calentamiento(), actual.auxiliares(), actual.rmUsados());
        guardar(new Mesociclo(m.id(), m.perfilId(), m.inicio(), m.microciclos(), m.potencia(), m.fuerza(), m.variante(),
                m.dias(), m.aproximaciones(), Armado.pctIguales(m, ejs), m.calentamiento(), m.auxiliares(), m.rmUsados()));
    }

    private void agregar(int dia) {
        Ejercicio e = DialogoElegirEjercicio.elegir(this, new ArrayList<>(ejs.values()));
        if (e == null) return;
        Map<Long, Aproximacion> aprox = new HashMap<>(actual.aproximaciones());
        if (e.tipo() == Tipo.FUERZA && !aprox.containsKey(e.id())) {
            Object[] opciones = {"Compleja (tabla cubana)", "Básica (plantilla)"};
            int r = JOptionPane.showOptionDialog(this, "¿Cómo se calcula " + e.nombre() + " en este mesociclo?\n"
                            + "(vale para todos los días donde aparezca)", "Aproximación", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
            if (r < 0) return;
            aprox.put(e.id(), r == 0 ? Aproximacion.COMPLEJA : Aproximacion.BASICA);
        }
        int pos = actual.bloques().stream().filter(b -> b.dia() == dia).mapToInt(Bloque::posicion).max().orElse(0) + 1;
        List<Bloque> bloques = new ArrayList<>(actual.bloques());
        bloques.add(new Bloque(dia, pos, e.id(), 100));
        Mesociclo m = con(bloques, aprox);
        guardar(con(Armado.pctIguales(m, ejs), aprox));
    }

    private void quitar(Bloque b) {
        List<Bloque> bloques = new ArrayList<>();
        for (Bloque x : actual.bloques()) {
            if (x.equals(b)) continue;
            int pos = x.dia() == b.dia() && x.posicion() > b.posicion() ? x.posicion() - 1 : x.posicion();
            bloques.add(new Bloque(x.dia(), pos, x.ejercicioId(), x.pctBolsa()));
        }
        Map<Long, Aproximacion> aprox = new LinkedHashMap<>(actual.aproximaciones());
        if (bloques.stream().noneMatch(x -> x.ejercicioId() == b.ejercicioId())) aprox.remove(b.ejercicioId());
        Mesociclo m = con(bloques, aprox);
        guardar(con(Armado.pctIguales(m, ejs), aprox));
    }

    private void cambiarPct(int dia, int posicion, double pct) {
        if (actual == null) return;
        List<Bloque> bloques = new ArrayList<>();
        for (Bloque x : actual.bloques()) {
            bloques.add(x.dia() == dia && x.posicion() == posicion ? new Bloque(dia, posicion, x.ejercicioId(), pct) : x);
        }
        guardar(con(bloques, actual.aproximaciones()));
    }

    private void mover(Bloque b, int delta) {
        guardar(con(Armado.mover(actual.bloques(), b.dia(), b.posicion(), delta), actual.aproximaciones()));
    }

    private void cambiarAproximacion(Ejercicio e, Aproximacion a) {
        Map<Long, Aproximacion> aprox = new HashMap<>(actual.aproximaciones());
        aprox.put(e.id(), a);
        Mesociclo m = con(actual.bloques(), aprox);
        guardar(con(Armado.pctIguales(m, ejs), aprox));
    }

    // ---- exportación ---------------------------------------------------------------------------------------

    private void exportar() {
        if (resultado == null) {
            JOptionPane.showMessageDialog(this, "El mesociclo todavía no se puede calcular: " + avisos.getText().trim());
            return;
        }
        Map<Long, Double> rms = new HashMap<>(actual.rmUsados());
        if (rms.isEmpty()) {
            for (Bloque b : actual.bloques()) {
                Ejercicio e = ejs.get(b.ejercicioId());
                if (e != null && e.rm() != null) rms.put(e.id(), e.rm());
            }
        }
        JFileChooser fc = new JFileChooser();
        File documentos = new File(System.getProperty("user.home"), "Documentos");
        fc.setCurrentDirectory(documentos.isDirectory() ? documentos : new File(System.getProperty("user.home")));
        fc.setSelectedFile(new File(fc.getCurrentDirectory(), "rutina-" + actual.inicio() + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destino = fc.getSelectedFile().toPath();
        if (!destino.toString().toLowerCase().endsWith(".pdf")) destino = Path.of(destino + ".pdf");
        try {
            Mesociclo m = new Mesociclo(actual.id(), actual.perfilId(), actual.inicio(), actual.microciclos(),
                    actual.potencia(), actual.fuerza(), actual.variante(), actual.dias(), actual.aproximaciones(),
                    actual.bloques(), calentamiento.getText(), auxiliares.getText(), Map.copyOf(rms));
            var planes = Armado.planesDelMesociclo(ctx.metodologia(), m, ctx.planes.listar(ctx.perfil.id()),
                    ctx.ejercicios.listar(ctx.perfil.id()));
            ExportadorPdf.exportar(destino, ctx.perfil.nombre(), m, resultado, planes, ctx.metodologia());
            guardar(m);
            if (Desktop.isDesktopSupported() && JOptionPane.showConfirmDialog(this, "PDF guardado en " + destino
                    + "\n¿Abrirlo?", "Exportado", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                Desktop.getDesktop().open(destino.toFile());
            }
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    /** Carpeta donde arrancan los diálogos de archivos: ~/Documentos si existe, si no la carpeta personal. */
    private static File carpetaInicial() {
        File documentos = new File(System.getProperty("user.home"), "Documentos");
        return documentos.isDirectory() ? documentos : new File(System.getProperty("user.home"));
    }

    private void planAlCelular() {
        if (resultado == null) {
            JOptionPane.showMessageDialog(this, "El mesociclo todavía no se puede calcular");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(carpetaInicial());
        String perfil = ctx.perfil.nombre().replaceAll("[\\\\/:*?\"<>|]", "_");
        fc.setSelectedFile(new File(fc.getCurrentDirectory(), "plan-" + perfil + "-" + actual.inicio() + ".json"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destino = fc.getSelectedFile().toPath();
        if (!destino.toString().toLowerCase().endsWith(".json")) destino = Path.of(destino + ".json");
        try {
            JsonRegistro.escribirPlan(destino, ArmadorPlan.armar(ctx.perfil.nombre(), actual, resultado, ctx.metodologia()));
            JOptionPane.showMessageDialog(this, "Plan guardado en " + destino
                    + "\nPasalo al celular y cargalo en la app (Inicio → Cargar plan).");
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    private void importarRegistro() {
        if (actual == null) {
            JOptionPane.showMessageDialog(this, "Elegí primero el mesociclo del registro.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(carpetaInicial());
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            RegistroCelular r = JsonRegistro.leerRegistro(fc.getSelectedFile().toPath());
            if (r.mesocicloId() != actual.id()) {
                JOptionPane.showMessageDialog(this, "El registro es de otro mesociclo (id " + r.mesocicloId()
                                + "; el elegido es el " + actual.id() + "). No se guardó.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ctx.registros.guardar(r);
            // se redibuja después, como en guardar(): no se destruyen componentes en medio de su evento
            SwingUtilities.invokeLater(this::recalcular);
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    private void recalcularRm() {
        if (actual == null) return;
        guardar(new Mesociclo(actual.id(), actual.perfilId(), actual.inicio(), actual.microciclos(), actual.potencia(),
                actual.fuerza(), actual.variante(), actual.dias(), actual.aproximaciones(), actual.bloques(),
                calentamiento.getText(), auxiliares.getText(), Map.of()));
        JOptionPane.showMessageDialog(this, "Listo: los kg se calculan con los 1RM actuales de cada ejercicio.");
    }
}
