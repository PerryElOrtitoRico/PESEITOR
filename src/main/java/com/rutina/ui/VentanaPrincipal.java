package com.rutina.ui;

import com.rutina.model.Mesociclo;
import com.rutina.model.Perfil;
import com.rutina.panel.ConexionPanel;
import com.rutina.panel.PanelCompartido;
import com.rutina.service.datos.Database;
import com.rutina.service.datos.PerfilRepo;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Ventana con las pestañas Ejercicios, Plantillas, Macrociclo, Mesociclo y Progreso. */
public final class VentanaPrincipal extends JFrame {

    private final JTabbedPane pestanas = new JTabbedPane();
    private final PanelMesociclo panelMesociclo;
    private boolean cerrarBaseAlSalir = true;

    private VentanaPrincipal(Contexto ctx) {
        super("RUTINEITOR — " + ctx.perfil.nombre());
        setIconImages(iconos());
        panelMesociclo = new PanelMesociclo(ctx);
        pestanas.addTab("Ejercicios", new PanelEjercicios(ctx));
        pestanas.addTab("Plantillas", new PanelPlantillas(ctx));
        pestanas.addTab("Macrociclo", new PanelMacrociclo(ctx, this::abrirMesociclo));
        pestanas.addTab("Mesociclo", panelMesociclo);
        pestanas.addTab("Progreso", new PanelProgreso(ctx));
        setContentPane(pestanas);

        JMenuBar barra = new JMenuBar();
        JMenu menu = new JMenu("Perfil");
        JMenuItem cambiar = new JMenuItem("Cambiar de perfil…");
        cambiar.addActionListener(e -> cambiarPerfil(ctx));
        menu.add(cambiar);
        barra.add(menu);
        JMenu menuPanel = new JMenu("Panel");
        JMenuItem conexion = new JMenuItem("Conexión con el panel…");
        conexion.addActionListener(e -> DialogoPanel.mostrar(this, ctx));
        menuPanel.add(conexion);
        barra.add(menuPanel);
        setJMenuBar(barra);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (!cerrarBaseAlSalir) return;
                try {
                    ctx.panel().close(); // primero: así el hilo de envío no sobrevive a un fallo al cerrar la base
                } catch (Exception ignored) {
                    // se está cerrando el programa
                }
                try {
                    ctx.db.close();
                } catch (Exception ignored) {
                    // se está cerrando el programa
                }
            }
        });
        setSize(1280, 820);
        setLocationRelativeTo(null);
    }

    private void abrirMesociclo(Mesociclo m) {
        panelMesociclo.mostrar(m.id());
        pestanas.setSelectedComponent(panelMesociclo);
    }

    private void cambiarPerfil(Contexto ctx) {
        try {
            Perfil p = SelectorPerfil.elegir(new PerfilRepo(ctx.db), true);
            if (p == null || p.id() == ctx.perfil.id()) return;
            new VentanaPrincipal(new Contexto(ctx.db, p, ctx.panel())).setVisible(true);
            cerrarBaseAlSalir = false; // la base sigue abierta: la usa la ventana nueva
            dispose();
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    /** El ícono en varios tamaños (barra de tareas, alt+tab, título); si falta alguno, se omite. */
    static java.util.List<java.awt.Image> iconos() {
        java.util.List<java.awt.Image> res = new java.util.ArrayList<>();
        for (int s : new int[]{16, 32, 48, 64, 128, 256}) {
            java.net.URL url = VentanaPrincipal.class.getResource("/icono/rutineitor-" + s + ".png");
            if (url != null) res.add(new javax.swing.ImageIcon(url).getImage());
        }
        return res;
    }

    /** Abre la base, elige el perfil y muestra la ventana. */
    public static void iniciar() {
        try {
            Database db = Database.abrir(Database.rutaPorDefecto());
            if (db.seRestauro()) {
                JOptionPane.showMessageDialog(null, "El archivo de datos estaba dañado: se guardó una copia como "
                                + Database.rutaPorDefecto().getFileName() + ".bak-… y se empezó uno nuevo.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
            }
            Perfil p = SelectorPerfil.elegir(new PerfilRepo(db), false);
            if (p == null) {
                db.close();
                return;
            }
            PanelCompartido panel = new PanelCompartido(ConexionPanel.abrir(Database.rutaPorDefecto().getParent()));
            try {
                new VentanaPrincipal(new Contexto(db, p, panel)).setVisible(true);
            } catch (Exception e) {
                panel.close();
                throw e;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No se pudo abrir la base de datos en " + Database.rutaPorDefecto()
                    + ":\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
