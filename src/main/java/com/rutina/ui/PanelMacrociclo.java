package com.rutina.ui;

import com.rutina.model.Macrociclo;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.PlanPliometria;
import com.rutina.model.Tipo;
import com.rutina.pdf.ExportadorPdfAnio;
import com.rutina.service.NombreMeso;
import com.rutina.service.calculo.CalculadoraMesociclo;
import com.rutina.service.calculo.DistribucionVolumen;
import com.rutina.service.calculo.ParametrosInvalidosException;
import com.rutina.service.calculo.PlanMicro;
import com.rutina.service.calculo.ResultadoMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo.PuntoGrafico;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Pestaña Macrociclo: calendario del año con mesociclos y planes de pliometría, y gráfico anual. */
final class PanelMacrociclo extends JPanel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ES = Locale.forLanguageTag("es-AR");

    private final Contexto ctx;
    private final Consumer<Mesociclo> abrir;
    private final JSpinner anio = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Semana (lunes)", "Mes", "Macrociclo", "Mesociclo", "Microciclo", "Potencia (reps @ IMR)",
                    "Fuerza (reps @ IMR)", "Pliometría"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JPanel grafico = new JPanel(new BorderLayout());
    private final List<LocalDate> lunes = new ArrayList<>();
    private List<Mesociclo> mesociclos = List.of();
    private List<PlanPliometria> planes = List.of();
    private List<Macrociclo> macros = List.of();
    private final javax.swing.DefaultListModel<Macrociclo> modeloMacros = new javax.swing.DefaultListModel<>();
    private final javax.swing.JList<Macrociclo> listaMacros = new javax.swing.JList<>(modeloMacros);
    private final javax.swing.DefaultListModel<PlanPliometria> modeloPlanes = new javax.swing.DefaultListModel<>();
    private final javax.swing.JList<PlanPliometria> listaPlanes = new javax.swing.JList<>(modeloPlanes);

    PanelMacrociclo(Contexto ctx, Consumer<Mesociclo> abrir) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.abrir = abrir;

        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        arriba.add(new JLabel("Año:"));
        arriba.add(anio);
        anio.setEditor(new JSpinner.NumberEditor(anio, "#"));
        anio.addChangeListener(e -> refrescar());
        boton(arriba, "Nuevo mesociclo", this::nuevoMesociclo);
        boton(arriba, "Editar", this::editarMesociclo);
        boton(arriba, "Borrar", this::borrarMesociclo);
        boton(arriba, "Armar →", this::armar);
        boton(arriba, "Exportar año (PDF)", this::exportarAnio);
        add(arriba, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) dobleClick();
            }
        });
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), grafico);
        split.setResizeWeight(0.6);
        JPanel derecha = new JPanel(new GridLayout(2, 1));
        derecha.add(panelMacros());
        derecha.add(panelPlanes());
        JSplitPane conPlanes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, split, derecha);
        conPlanes.setResizeWeight(1.0);
        add(conPlanes, BorderLayout.CENTER);
        add(new JLabel("  Doble click en una semana: arma su mesociclo o, si solo tiene pliometría, edita ese plan."), BorderLayout.SOUTH);
        ctx.alCambiar(this::refrescar);
        refrescar();
    }

    private static void boton(JPanel p, String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        p.add(b);
    }

    private void refrescar() {
        try {
            mesociclos = ctx.mesociclos.listar(ctx.perfil.id());
            planes = ctx.planes.listar(ctx.perfil.id());
            macros = ctx.macros.listar(ctx.perfil.id());
            Macrociclo macroElegido = listaMacros.getSelectedValue();
            modeloMacros.clear();
            macros.forEach(modeloMacros::addElement);
            if (macroElegido != null) {
                for (int i = 0; i < modeloMacros.size(); i++) {
                    if (modeloMacros.get(i).id() == macroElegido.id()) listaMacros.setSelectedIndex(i);
                }
            }
            PlanPliometria elegido = listaPlanes.getSelectedValue();
            modeloPlanes.clear();
            planes.forEach(modeloPlanes::addElement);
            if (elegido != null) {
                for (int i = 0; i < modeloPlanes.size(); i++) {
                    if (modeloPlanes.get(i).id() == elegido.id()) listaPlanes.setSelectedIndex(i);
                }
            }
        } catch (Exception e) {
            Contexto.error(this, e);
            return;
        }
        int y = (Integer) anio.getValue();
        lunes.clear();
        modelo.setRowCount(0);
        LocalDate d = LocalDate.of(y, 1, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        int mesAnterior = -1;
        Metodologia m = ctx.metodologia();
        while (d.getYear() == y) {
            lunes.add(d);
            String mes = d.getMonthValue() != mesAnterior ? d.getMonth().getDisplayName(TextStyle.FULL, ES).toUpperCase(ES) : "";
            mesAnterior = d.getMonthValue();
            Object[] fila = {d.format(FECHA), mes, "", "", "", "", "", ""};
            Mesociclo mc = mesocicloEn(d);
            if (mc != null) {
                int micro = (int) java.time.temporal.ChronoUnit.WEEKS.between(mc.inicio(), d);
                Macrociclo macro = macroDe(mc);
                fila[2] = macro == null ? "" : macro.nombre();
                String nombre = NombreMeso.etiqueta(mc);
                fila[3] = nombre != null ? nombre
                        : "Mesociclo " + (mesociclos.indexOf(mc) + 1) + (mc.variante() != null ? " (" + mc.microciclos() + mc.variante() + ")" : "");
                fila[4] = (micro + 1) + " / " + mc.microciclos();
                fila[5] = textoMicro(m, mc, mc.potencia(), micro);
                fila[6] = textoMicro(m, mc, mc.fuerza(), micro);
            }
            PlanPliometria p = planEn(d);
            if (p != null) {
                int sem = (int) java.time.temporal.ChronoUnit.WEEKS.between(p.inicio(), d);
                fila[7] = "Nivel " + p.nivel() + " · semana " + (sem + 1) + " / 4";
            }
            modelo.addRow(fila);
            d = d.plusWeeks(1);
        }
        actualizarGrafico(y);
    }

    /** Volumen e IMR planificados de ese microciclo; sin variante elegida, el total del mesociclo. */
    private static String textoMicro(Metodologia m, Mesociclo mc, ParametrosTipo p, int micro) {
        if (p.volumen() <= 0) return "";
        if (mc.variante() == null) return micro == 0 ? "total " + p.volumen() + " @ " + p.imr() + "%" : "";
        try {
            PlanMicro pm = DistribucionVolumen.porMicrociclo(m, p, mc.microciclos(), mc.variante()).get(micro);
            return pm.volumen() + " @ " + pm.imr() + "%";
        } catch (ParametrosInvalidosException e) {
            return "⚠ " + e.getMessage();
        }
    }

    private Macrociclo macroDe(Mesociclo mc) {
        if (mc.macroId() == null) return null;
        return macros.stream().filter(x -> x.id() == mc.macroId()).findFirst().orElse(null);
    }

    private Mesociclo mesocicloEn(LocalDate lunesSemana) {
        return mesociclos.stream().filter(mc -> !lunesSemana.isBefore(mc.inicio()) && lunesSemana.isBefore(mc.fin()))
                .findFirst().orElse(null);
    }

    private PlanPliometria planEn(LocalDate lunesSemana) {
        return planes.stream().filter(p -> !lunesSemana.isBefore(p.inicio()) && lunesSemana.isBefore(p.inicio().plusWeeks(4)))
                .findFirst().orElse(null);
    }

    private LocalDate semanaSeleccionada() {
        int r = tabla.getSelectedRow();
        return r < 0 ? null : lunes.get(r);
    }

    private Mesociclo mesocicloSeleccionado() {
        LocalDate d = semanaSeleccionada();
        Mesociclo mc = d == null ? null : mesocicloEn(d);
        if (mc == null) JOptionPane.showMessageDialog(this, "Elegí en el calendario una semana de un mesociclo.");
        return mc;
    }

    private void actualizarGrafico(int y) {
        List<PuntoGrafico> plan = new ArrayList<>();
        List<PuntoGrafico> real = new ArrayList<>();
        int i = 0;
        for (Mesociclo mc : mesociclos) {
            if (mc.inicio().getYear() != y) continue;
            i++;
            for (Tipo t : List.of(Tipo.POTENCIA, Tipo.FUERZA)) {
                ParametrosTipo p = t == Tipo.POTENCIA ? mc.potencia() : mc.fuerza();
                if (p.volumen() > 0) plan.add(new PuntoGrafico(t, i, p.volumen(), p.imr()));
            }
            if (mc.variante() == null || mc.dias() == null || mc.bloques().isEmpty()) continue;
            try {
                ResultadoMesociclo res = CalculadoraMesociclo.calcular(ctx.metodologia(), mc, ctx.mapaEjercicios());
                for (Tipo t : List.of(Tipo.POTENCIA, Tipo.FUERZA)) {
                    int vol = 0;
                    double suma = 0;
                    for (PuntoGrafico p : res.real()) {
                        if (p.tipo() != t) continue;
                        vol += p.volumen();
                        suma += p.volumen() * p.imr();
                    }
                    if (vol > 0) real.add(new PuntoGrafico(t, i, vol, suma / vol));
                }
            } catch (Exception ignored) {
                // un mesociclo con parámetros inválidos se ve en su pestaña; acá solo falta su "planilla"
            }
        }
        grafico.removeAll();
        grafico.add(Graficos.volumenIntensidad(plan, real, "Meso "), BorderLayout.CENTER);
        grafico.revalidate();
        grafico.repaint();
    }

    // ---- acciones ------------------------------------------------------------------------------------------

    private void nuevoMesociclo() {
        LocalDate d = semanaSeleccionada();
        if (d == null && !mesociclos.isEmpty()) d = mesociclos.get(mesociclos.size() - 1).fin();
        if (d == null) d = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        dialogoMesociclo(new Mesociclo(0, ctx.perfil.id(), d, 4, new ParametrosTipo(250, 72),
                new ParametrosTipo(350, 76), null, null, java.util.Map.of(), List.of(), "", "", java.util.Map.of()));
    }

    private void editarMesociclo() {
        Mesociclo mc = mesocicloSeleccionado();
        if (mc != null) dialogoMesociclo(mc);
    }

    private void dialogoMesociclo(Mesociclo mc) {
        JTextField inicio = new JTextField(mc.inicio().format(FECHA), 10);
        JSpinner micro = new JSpinner(new SpinnerNumberModel(mc.microciclos(), 2, 5, 1));
        JTextField objetivo = new JTextField(mc.objetivo() == null ? "" : mc.objetivo(), 14);
        objetivo.setToolTipText("Ejercicio o meta del mesociclo, ej. Sentadilla");
        JComboBox<Object> macro = new JComboBox<>();
        macro.addItem("(ninguno)");
        macros.forEach(macro::addItem);
        macro.setRenderer((l, v, i, sel, foco) -> new JLabel(v instanceof Macrociclo x ? x.nombre() : String.valueOf(v)));
        macro.setSelectedItem(macros.stream().filter(x -> mc.macroId() != null && x.id() == mc.macroId()).findFirst().orElse(null));
        if (macro.getSelectedItem() == null) macro.setSelectedIndex(0);
        JSpinner potVol = new JSpinner(new SpinnerNumberModel(mc.potencia().volumen(), 0, 5000, 10));
        JSpinner potImr = new JSpinner(new SpinnerNumberModel(Math.max(mc.potencia().imr(), 60), 60, 80, 1));
        JSpinner fzaVol = new JSpinner(new SpinnerNumberModel(mc.fuerza().volumen(), 0, 5000, 10));
        JSpinner fzaImr = new JSpinner(new SpinnerNumberModel(Math.max(mc.fuerza().imr(), 60), 60, 80, 1));
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Inicio (lunes, dd/mm/aaaa)"));
        p.add(inicio);
        p.add(new JLabel("Objetivo del mesociclo"));
        p.add(objetivo);
        p.add(new JLabel("Macrociclo"));
        p.add(macro);
        p.add(new JLabel("Microciclos (semanas)"));
        p.add(micro);
        p.add(new JLabel("Potencia: volumen total (reps)"));
        p.add(potVol);
        p.add(new JLabel("Potencia: IMR %"));
        p.add(potImr);
        p.add(new JLabel("Fuerza: volumen total (reps)"));
        p.add(fzaVol);
        p.add(new JLabel("Fuerza: IMR %"));
        p.add(fzaImr);
        while (true) {
            int r = JOptionPane.showConfirmDialog(this, p, mc.id() == 0 ? "Nuevo mesociclo" : "Editar mesociclo",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return;
            try {
                LocalDate d = LocalDate.parse(inicio.getText().trim(), FECHA).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                int n = (Integer) micro.getValue();
                ParametrosTipo pot = new ParametrosTipo((Integer) potVol.getValue(), (Integer) potImr.getValue());
                ParametrosTipo fza = new ParametrosTipo((Integer) fzaVol.getValue(), (Integer) fzaImr.getValue());
                validar(mc.id(), d, n, pot, fza);
                // si cambió la cantidad de microciclos, la variante elegida puede dejar de existir
                Character variante = n == mc.microciclos() ? mc.variante() : null;
                ctx.mesociclos.guardar(new Mesociclo(mc.id(), mc.perfilId(), d, n, pot, fza, variante, mc.dias(),
                        mc.aproximaciones(), mc.bloques(), mc.calentamiento(), mc.auxiliares(), mc.rmUsados(),
                        objetivo.getText().trim(), macro.getSelectedItem() instanceof Macrociclo x ? x.id() : null));
                ctx.cambio();
                return;
            } catch (java.time.format.DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Fecha inválida: usá dd/mm/aaaa.");
            } catch (Exception e) {
                Contexto.error(this, e);
            }
        }
    }

    /** No se superpone con otro mesociclo y existe al menos una variante que deja los IMR dentro de la tabla. */
    private void validar(long id, LocalDate inicio, int n, ParametrosTipo pot, ParametrosTipo fza) {
        LocalDate fin = inicio.plusWeeks(n);
        for (Mesociclo o : mesociclos) {
            if (o.id() != id && inicio.isBefore(o.fin()) && o.inicio().isBefore(fin)) {
                throw new IllegalArgumentException("Se superpone con el mesociclo que empieza el " + o.inicio().format(FECHA) + ".");
            }
        }
        List<String> errores = new ArrayList<>();
        for (char v : new char[]{'A', 'B', 'C'}) {
            if (!ctx.metodologia().variantes().containsKey("" + n + v)) continue;
            try {
                if (pot.volumen() > 0) DistribucionVolumen.porMicrociclo(ctx.metodologia(), pot, n, v);
                if (fza.volumen() > 0) DistribucionVolumen.porMicrociclo(ctx.metodologia(), fza, n, v);
                return;
            } catch (ParametrosInvalidosException e) {
                errores.add(e.getMessage());
            }
        }
        throw new IllegalArgumentException("Ninguna variante sirve con esos IMR:\n" + String.join("\n", errores));
    }

    private void borrarMesociclo() {
        Mesociclo mc = mesocicloSeleccionado();
        if (mc == null) return;
        if (JOptionPane.showConfirmDialog(this, "¿Borrar el mesociclo del " + mc.inicio().format(FECHA)
                + " con todo lo armado?", "Borrar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            ctx.mesociclos.borrar(mc.id());
            ctx.registros.borrar(mc.id());
            ctx.cambio();
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    private void armar() {
        Mesociclo mc = mesocicloSeleccionado();
        if (mc != null) abrir.accept(mc);
    }

    private void exportarAnio() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        java.io.File docs = new java.io.File(System.getProperty("user.home"), "Documentos");
        fc.setCurrentDirectory(docs.isDirectory() ? docs : new java.io.File(System.getProperty("user.home")));
        int y = (Integer) anio.getValue();
        fc.setSelectedFile(new java.io.File(fc.getCurrentDirectory(), "Macrociclo_" + y + ".pdf"));
        if (fc.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        java.nio.file.Path destino = fc.getSelectedFile().toPath();
        if (!destino.toString().toLowerCase().endsWith(".pdf")) destino = java.nio.file.Path.of(destino + ".pdf");
        List<String> cabecera = new ArrayList<>();
        for (int c = 0; c < modelo.getColumnCount(); c++) cabecera.add(modelo.getColumnName(c));
        List<String[]> filas = new ArrayList<>();
        for (int r = 0; r < modelo.getRowCount(); r++) {
            String[] f = new String[modelo.getColumnCount()];
            for (int c = 0; c < f.length; c++) f[c] = String.valueOf(modelo.getValueAt(r, c));
            filas.add(f);
        }
        try {
            ExportadorPdfAnio.exportar(destino, ctx.perfil.nombre(), y, cabecera, filas);
            JOptionPane.showMessageDialog(this, "PDF guardado en " + destino);
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    // ---- macrociclos ----------------------------------------------------------------------------------------------

    private JPanel panelMacros() {
        listaMacros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaMacros.setCellRenderer((l, m, i, sel, foco) -> {
            JLabel lb = new JLabel("<html><b>" + m.nombre() + "</b><br>" + m.objetivo() + "</html>");
            lb.setOpaque(true);
            lb.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));
            lb.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            lb.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            return lb;
        });
        listaMacros.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && listaMacros.getSelectedValue() != null) dialogoMacro(listaMacros.getSelectedValue());
            }
        });
        JPanel botones = new JPanel(new GridLayout(1, 3, 4, 4));
        boton(botones, "Nuevo", () -> dialogoMacro(new Macrociclo(0, ctx.perfil.id(), "", "")));
        boton(botones, "Editar", () -> {
            if (listaMacros.getSelectedValue() != null) dialogoMacro(listaMacros.getSelectedValue());
            else JOptionPane.showMessageDialog(this, "Elegí un macrociclo en la lista.");
        });
        boton(botones, "Borrar", this::borrarMacro);
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(javax.swing.BorderFactory.createTitledBorder("Macrociclos"));
        p.add(new JScrollPane(listaMacros), BorderLayout.CENTER);
        p.add(botones, BorderLayout.SOUTH);
        p.setPreferredSize(new java.awt.Dimension(250, 200));
        return p;
    }

    /** Crear (id 0) o editar un macrociclo: nombre y objetivo. Los mesociclos se asignan en su diálogo. */
    private void dialogoMacro(Macrociclo macro) {
        JTextField nombre = new JTextField(macro.nombre(), 14);
        JTextField objetivo = new JTextField(macro.objetivo(), 14);
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Nombre"));
        p.add(nombre);
        p.add(new JLabel("Objetivo (ej. Fuerza)"));
        p.add(objetivo);
        while (true) {
            if (JOptionPane.showConfirmDialog(this, p, macro.id() == 0 ? "Nuevo macrociclo" : "Editar macrociclo",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            if (nombre.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Poné un nombre.");
                continue;
            }
            try {
                ctx.macros.guardar(new Macrociclo(macro.id(), macro.perfilId(), nombre.getText().trim(), objetivo.getText().trim()));
                ctx.cambio();
                return;
            } catch (Exception e) {
                Contexto.error(this, e);
            }
        }
    }

    private void borrarMacro() {
        Macrociclo m = listaMacros.getSelectedValue();
        if (m == null) {
            JOptionPane.showMessageDialog(this, "Elegí un macrociclo en la lista.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Borrar el macrociclo \"" + m.nombre()
                + "\"? Sus mesociclos no se borran, quedan sin macrociclo.", "Borrar", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        try {
            ctx.macros.borrar(m.id());
            ctx.cambio();
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    // ---- planes de pliometría -----------------------------------------------------------------------------------

    private JPanel panelPlanes() {
        listaPlanes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaPlanes.setCellRenderer((l, p, i, sel, foco) -> {
            JLabel lb = new JLabel("<html><b>Nivel " + p.nivel() + "</b><br>" + p.inicio().format(FECHA) + " → "
                    + p.inicio().plusWeeks(4).minusDays(1).format(FECHA) + "</html>");
            lb.setOpaque(true);
            lb.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));
            lb.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            lb.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            return lb;
        });
        listaPlanes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && listaPlanes.getSelectedValue() != null) dialogoPlan(listaPlanes.getSelectedValue());
            }
        });
        JPanel botones = new JPanel(new GridLayout(1, 3, 4, 4));
        boton(botones, "Nuevo", this::nuevoPlan);
        boton(botones, "Editar", () -> {
            PlanPliometria p = planElegido();
            if (p != null) dialogoPlan(p);
        });
        boton(botones, "Borrar", this::borrarPlan);
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(javax.swing.BorderFactory.createTitledBorder("Planes de pliometría"));
        p.add(new JScrollPane(listaPlanes), BorderLayout.CENTER);
        p.add(botones, BorderLayout.SOUTH);
        p.setPreferredSize(new java.awt.Dimension(250, 200));
        return p;
    }

    /** El plan elegido en la lista o, si no hay, el de la semana elegida en el calendario. */
    private PlanPliometria planElegido() {
        PlanPliometria p = listaPlanes.getSelectedValue();
        if (p == null) {
            LocalDate d = semanaSeleccionada();
            p = d == null ? null : planEn(d);
        }
        if (p == null) JOptionPane.showMessageDialog(this, "Elegí un plan en la lista \"Planes de pliometría\".");
        return p;
    }

    private void dobleClick() {
        LocalDate d = semanaSeleccionada();
        if (d == null) return;
        Mesociclo mc = mesocicloEn(d);
        if (mc != null) {
            abrir.accept(mc);
            return;
        }
        PlanPliometria p = planEn(d);
        if (p != null) dialogoPlan(p);
    }

    private void nuevoPlan() {
        LocalDate d = semanaSeleccionada();
        if (d == null) d = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        dialogoPlan(new PlanPliometria(0, ctx.perfil.id(), 1, d));
    }

    /** Crear (id 0) o editar un plan: nivel y fecha de inicio. */
    private void dialogoPlan(PlanPliometria plan) {
        JComboBox<Integer> nivel = new JComboBox<>(new Integer[]{1, 2, 3});
        nivel.setSelectedItem(plan.nivel());
        JTextField inicio = new JTextField(plan.inicio().format(FECHA), 10);
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Nivel"));
        p.add(nivel);
        p.add(new JLabel("Inicio (lunes, dd/mm/aaaa)"));
        p.add(inicio);
        String titulo = plan.id() == 0 ? "Nuevo plan de pliometría (4 semanas)" : "Editar plan de pliometría";
        while (true) {
            if (JOptionPane.showConfirmDialog(this, p, titulo, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                LocalDate ini = LocalDate.parse(inicio.getText().trim(), FECHA).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                PlanPliometria nuevo = new PlanPliometria(plan.id(), plan.perfilId(), (Integer) nivel.getSelectedItem(), ini);
                PlanPliometria choca = com.rutina.service.Armado.planQueSeSuperpone(nuevo, planes);
                if (choca != null) {
                    throw new IllegalArgumentException("Se superpone con el plan que empieza el " + choca.inicio().format(FECHA) + ".");
                }
                ctx.planes.guardar(nuevo);
                ctx.cambio();
                return;
            } catch (java.time.format.DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Fecha inválida: usá dd/mm/aaaa.");
            } catch (Exception e) {
                Contexto.error(this, e);
            }
        }
    }

    private void borrarPlan() {
        PlanPliometria p = planElegido();
        if (p == null) return;
        if (JOptionPane.showConfirmDialog(this, "¿Borrar el plan de pliometría nivel " + p.nivel() + " del "
                + p.inicio().format(FECHA) + "?", "Borrar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            ctx.planes.borrar(p.id());
            ctx.cambio();
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }
}
