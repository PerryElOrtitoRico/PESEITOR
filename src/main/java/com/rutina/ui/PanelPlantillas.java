package com.rutina.ui;

import com.rutina.model.CeldaPlantilla;
import com.rutina.model.ClavePlantilla;
import com.rutina.model.Metodologia;
import com.rutina.model.Plantilla;
import com.rutina.model.Tipo;
import com.rutina.model.VarianteMicro;
import com.rutina.model.Zona;
import com.rutina.service.calculo.OpcionesRedondeo;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

/** Pestaña Plantillas: las tablas del método, editables, separadas de la lista de ejercicios. */
final class PanelPlantillas extends JPanel {

    private final Contexto ctx;
    private Metodologia m;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JComboBox<String> incremento = new JComboBox<>(OpcionesRedondeo.TEXTOS);
    /** Tablas visibles: al guardar se confirma la celda que se esté editando. */
    private static final java.util.List<JTable> TABLAS = new ArrayList<>();

    PanelPlantillas(Contexto ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        add(new JLabel("  Tablas del método. Son compartidas por todos los perfiles: tocá solo lo que sepas para qué sirve."),
                BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(new JLabel("Redondeo de kg:"));
        botones.add(incremento);
        JButton guardar = new JButton("Guardar");
        guardar.addActionListener(e -> guardar());
        JButton descartar = new JButton("Descartar cambios");
        descartar.addActionListener(e -> cargar());
        JButton restaurar = new JButton("Restaurar valores originales");
        restaurar.addActionListener(e -> restaurar());
        botones.add(guardar);
        botones.add(descartar);
        botones.add(restaurar);
        add(botones, BorderLayout.SOUTH);
        cargar();
    }

    /** Copia de trabajo: se lee de la base, así editar no toca la metodología en uso hasta guardar. */
    private void cargar() {
        try {
            m = ctx.metodologiaRepo.cargar();
        } catch (Exception e) {
            Contexto.error(this, e);
            return;
        }
        int sel = Math.max(0, tabs.getSelectedIndex());
        tabs.removeAll();
        tabs.addTab("Microciclos", tablaVariantes());
        tabs.addTab("Días", tablaDias());
        tabs.addTab("Distribución por intensidad", tablaRef());
        tabs.addTab("Reps por serie", tablaReps());
        tabs.addTab("Fuerza básica e hipertrofia", new EditorPlantillas());
        tabs.addTab("Pliometría", new EditorPlio());
        tabs.setSelectedIndex(Math.min(sel, tabs.getTabCount() - 1));
        incremento.setSelectedIndex(OpcionesRedondeo.indiceDe(m.incrementoKg()));
    }

    private void guardar() {
        for (JTable t : TABLAS) {
            if (t.isEditing()) t.getCellEditor().stopCellEditing();
        }
        List<String> problemas = new ArrayList<>();
        m.variantes().forEach((k, v) -> {
            double s = Arrays.stream(v.volPct()).sum();
            if (Math.abs(s - 100) > 0.01) problemas.add("La variante " + k + " suma " + s + " % de volumen.");
        });
        m.diaPct().forEach((k, v) -> {
            double s = Arrays.stream(v).sum() * 100;
            if (Math.abs(s - 100) > 0.01) problemas.add("Los días de " + k + " suman " + Math.round(s) + " %.");
        });
        if (!problemas.isEmpty()) {
            JOptionPane.showMessageDialog(this, String.join("\n", problemas) + "\nCorregilo antes de guardar.",
                    "No se guardó", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Metodologia nueva = new Metodologia(m.variantes(), m.diaPct(), m.ref(), m.repsPorSerie(), m.plantillas(),
                    m.volPlio(), OpcionesRedondeo.valorDe(incremento.getSelectedIndex()));
            ctx.metodologiaRepo.guardar(nueva);
            ctx.recargarMetodologia();
            ctx.cambio();
            cargar();
            JOptionPane.showMessageDialog(this, "Guardado.");
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    private void restaurar() {
        if (JOptionPane.showConfirmDialog(this, "¿Volver todas las tablas a los valores de tus planillas originales?",
                "Restaurar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            ctx.metodologiaRepo.restaurar();
            ctx.recargarMetodologia();
            ctx.cambio();
            cargar();
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    // ---- tablas -------------------------------------------------------------------------------------------

    /** Modelo genérico: celdas numéricas leídas y escritas por funciones; null = celda que no existe. */
    private interface Celdas {
        Object leer(int fila, int col);

        /** false si el valor no es válido. */
        boolean escribir(int fila, int col, String valor);
    }

    private static JScrollPane tabla(int filas, String[] columnas, IntFunction<String> nombreFila, Celdas celdas) {
        AbstractTableModel modelo = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return filas;
            }

            @Override
            public int getColumnCount() {
                return columnas.length + 1;
            }

            @Override
            public String getColumnName(int c) {
                return c == 0 ? "" : columnas[c - 1];
            }

            @Override
            public Object getValueAt(int r, int c) {
                if (c == 0) return nombreFila.apply(r);
                Object v = celdas.leer(r, c - 1);
                return v == null ? "" : v;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return c > 0 && celdas.leer(r, c - 1) != null;
            }

            @Override
            public void setValueAt(Object v, int r, int c) {
                if (!celdas.escribir(r, c - 1, v.toString().trim())) {
                    JOptionPane.showMessageDialog(null, "Valor inválido: \"" + v + "\"", "Plantillas",
                            JOptionPane.WARNING_MESSAGE);
                }
                fireTableRowsUpdated(r, r);
            }
        };
        JTable t = new JTable(modelo);
        t.setCellSelectionEnabled(true);
        t.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        TABLAS.removeIf(x -> !x.isDisplayable());
        TABLAS.add(t);
        return new JScrollPane(t);
    }

    private static Double num(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String fmt(double v) {
        return v % 1 == 0 ? String.valueOf((long) v) : String.valueOf(Math.round(v * 100) / 100.0);
    }

    private static String[] zonas() {
        return Arrays.stream(Zona.values()).map(z -> z.pct() + "%").toArray(String[]::new);
    }

    private JScrollPane tablaVariantes() {
        List<String> claves = new ArrayList<>(m.variantes().keySet());
        String[] cols = {"Vol% 1", "Vol% 2", "Vol% 3", "Vol% 4", "Vol% 5", "ΔIMR 1", "ΔIMR 2", "ΔIMR 3", "ΔIMR 4", "ΔIMR 5"};
        return tabla(claves.size(), cols, claves::get, new Celdas() {
            public Object leer(int f, int c) {
                VarianteMicro v = m.variantes().get(claves.get(f));
                int i = c % 5;
                if (i >= v.volPct().length) return null;
                return c < 5 ? fmt(v.volPct()[i]) : String.valueOf(v.deltaImr()[i]);
            }

            public boolean escribir(int f, int c, String s) {
                VarianteMicro v = m.variantes().get(claves.get(f));
                Double d = num(s);
                if (d == null) return false;
                if (c < 5) v.volPct()[c] = d;
                else v.deltaImr()[c - 5] = (int) Math.round(d);
                return true;
            }
        });
    }

    private JScrollPane tablaDias() {
        List<Integer> claves = new ArrayList<>(m.diaPct().keySet());
        return tabla(claves.size(), new String[]{"Día 1 %", "Día 2 %", "Día 3 %", "Día 4 %", "Día 5 %"},
                f -> claves.get(f) + " días", new Celdas() {
                    public Object leer(int f, int c) {
                        double[] v = m.diaPct().get(claves.get(f));
                        return c < v.length ? fmt(v[c] * 100) : null;
                    }

                    public boolean escribir(int f, int c, String s) {
                        Double d = num(s);
                        if (d == null || d < 0) return false;
                        m.diaPct().get(claves.get(f))[c] = d / 100;
                        return true;
                    }
                });
    }

    private JScrollPane tablaRef() {
        List<Integer> claves = new ArrayList<>(m.ref().keySet());
        return tabla(claves.size(), zonas(), f -> "IMR " + claves.get(f) + " %", new Celdas() {
            public Object leer(int f, int c) {
                double v = m.ref().get(claves.get(f))[c];
                return v == 0 ? "0" : fmt(v);
            }

            public boolean escribir(int f, int c, String s) {
                Double d = num(s);
                if (d == null || d < 0) return false;
                m.ref().get(claves.get(f))[c] = d;
                return true;
            }
        });
    }

    private JScrollPane tablaReps() {
        List<Tipo> claves = List.of(Tipo.POTENCIA, Tipo.FUERZA);
        return tabla(2, zonas(), f -> PanelEjercicios.titulo(claves.get(f)), new Celdas() {
            public Object leer(int f, int c) {
                return String.valueOf(m.repsPorSerie().get(claves.get(f))[c]);
            }

            public boolean escribir(int f, int c, String s) {
                Double d = num(s);
                if (d == null || d < 1) return false;
                m.repsPorSerie().get(claves.get(f))[c] = (int) Math.round(d);
                return true;
            }
        });
    }

    /** Plantillas de fuerza básica e hipertrofia, elegidas por tipo / semanas / días. */
    private final class EditorPlantillas extends JPanel {
        private final JComboBox<String> tipo = new JComboBox<>(new String[]{"Fuerza básica", "Hipertrofia"});
        private final JComboBox<Integer> semanas = new JComboBox<>(new Integer[]{2, 3, 4, 5});
        private final JComboBox<Integer> dias = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        private final JLabel estado = new JLabel();
        private final JPanel centro = new JPanel(new BorderLayout());

        EditorPlantillas() {
            super(new BorderLayout());
            JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
            arriba.add(tipo);
            arriba.add(new JLabel("Semanas:"));
            arriba.add(semanas);
            arriba.add(new JLabel("Veces por semana:"));
            arriba.add(dias);
            arriba.add(estado);
            add(arriba, BorderLayout.NORTH);
            add(centro, BorderLayout.CENTER);
            add(new JLabel("  Formato: 4x12, 3x3-90%, 3x5-75/80%. Al editar una plantilla inferida deja de estar marcada."),
                    BorderLayout.SOUTH);
            tipo.addActionListener(e -> mostrar());
            semanas.addActionListener(e -> mostrar());
            dias.addActionListener(e -> mostrar());
            mostrar();
        }

        private ClavePlantilla clave() {
            return new ClavePlantilla(tipo.getSelectedIndex() == 0 ? Tipo.FUERZA : Tipo.HIPERTROFIA,
                    (Integer) semanas.getSelectedItem(), (Integer) dias.getSelectedItem());
        }

        private void mostrar() {
            ClavePlantilla k = clave();
            Plantilla p = m.plantillas().get(k);
            centro.removeAll();
            if (p == null) {
                estado.setText("  (no existe)");
            } else {
                estado.setText(p.inferida() ? "  INFERIDA: revisala" : "  original / editada");
                String[] cols = new String[k.diasPorSemana()];
                for (int i = 0; i < cols.length; i++) cols[i] = "Vez " + (i + 1);
                centro.add(tabla(k.semanas(), cols, f -> "Semana " + (f + 1), new Celdas() {
                    public Object leer(int f, int c) {
                        return m.plantillas().get(k).semanas().get(f).get(c).texto();
                    }

                    public boolean escribir(int f, int c, String s) {
                        CeldaPlantilla nueva;
                        try {
                            nueva = CeldaPlantilla.parse(s);
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                        List<List<CeldaPlantilla>> filas = new ArrayList<>();
                        for (List<CeldaPlantilla> sem : m.plantillas().get(k).semanas()) filas.add(new ArrayList<>(sem));
                        filas.get(f).set(c, nueva);
                        m.plantillas().put(k, new Plantilla(filas, false));
                        estado.setText("  original / editada");
                        return true;
                    }
                }), BorderLayout.CENTER);
            }
            centro.revalidate();
            centro.repaint();
        }
    }

    /** Contactos por día de cada nivel de pliometría. */
    private final class EditorPlio extends JPanel {
        private final JComboBox<Integer> nivel = new JComboBox<>(new Integer[]{1, 2, 3});
        private final JPanel centro = new JPanel(new BorderLayout());

        EditorPlio() {
            super(new BorderLayout());
            JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
            arriba.add(new JLabel("Nivel:"));
            arriba.add(nivel);
            arriba.add(new JLabel("  Contactos por día. Cada ejercicio hace contactos ÷ 60 series de 10 saltos."));
            add(arriba, BorderLayout.NORTH);
            add(centro, BorderLayout.CENTER);
            nivel.addActionListener(e -> mostrar());
            mostrar();
        }

        private void mostrar() {
            int[][] v = m.volPlio().get((Integer) nivel.getSelectedItem());
            String[] cols = new String[v[0].length];
            for (int i = 0; i < cols.length; i++) cols[i] = "Día " + (i + 1);
            centro.removeAll();
            centro.add(tabla(v.length, cols, f -> "Semana " + (f + 1), new Celdas() {
                public Object leer(int f, int c) {
                    return String.valueOf(v[f][c]);
                }

                public boolean escribir(int f, int c, String s) {
                    Double d = num(s);
                    if (d == null || d < 0) return false;
                    v[f][c] = (int) Math.round(d);
                    return true;
                }
            }), BorderLayout.CENTER);
            centro.revalidate();
        }
    }
}
