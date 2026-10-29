package com.rutina.ui;

import com.rutina.panel.ClavesPanel;
import com.rutina.panel.ConexionPanel;
import com.rutina.panel.HistorialRm;
import com.rutina.panel.NombreDispositivo;
import com.rutina.panel.TextosPanel;
import com.rutina.panel.ValidacionConexion;
import com.rutina.service.datos.Database;
import panel.core.ConfigPanel;
import panel.core.PanelClient;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Ventana "Conexión con el panel": dirección, token, dispositivo, pruebas y envío del historial de 1RM. */
final class DialogoPanel extends JDialog {

    private final Contexto ctx;
    private final Path carpetaDatos = Database.rutaPorDefecto().getParent();

    private final JTextField url = new JTextField(32);
    private final JPasswordField token = new JPasswordField(32);
    private final JCheckBox mostrar = new JCheckBox("Mostrar");
    private final JTextField dispositivo = new JTextField(32);
    private final JCheckBox enviar = new JCheckBox("Enviar al panel");
    private final JButton probar = new JButton("Probar conexión");
    private final JButton enviarRm = new JButton("Enviar historial de 1RM");
    private final JButton guardar = new JButton("Guardar");
    private final JButton cancelar = new JButton("Cancelar");
    private final JLabel estado = new JLabel(" ");

    private DialogoPanel(JFrame dueno, Contexto ctx) {
        super(dueno, "Conexión con el panel", true);
        this.ctx = ctx;
        prellenar();

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        int fila = 0;
        if (!ctx.esPerfilDelPanel()) {
            JLabel nota = new JLabel("Este perfil no se manda al panel (solo el primero).");
            GridBagConstraints c = celda(0, fila++);
            c.gridwidth = 3;
            formulario.add(nota, c);
        }
        formulario.add(new JLabel("Dirección del panel:"), celda(0, fila));
        GridBagConstraints campo = celda(1, fila++);
        campo.gridwidth = 2;
        formulario.add(url, campo);
        formulario.add(new JLabel("Token:"), celda(0, fila));
        formulario.add(token, celda(1, fila));
        formulario.add(mostrar, celda(2, fila++));
        formulario.add(new JLabel("Dispositivo:"), celda(0, fila));
        campo = celda(1, fila++);
        campo.gridwidth = 2;
        formulario.add(dispositivo, campo);
        campo = celda(1, fila++);
        campo.gridwidth = 2;
        formulario.add(enviar, campo);
        campo = celda(0, fila++);
        campo.gridwidth = 3;
        formulario.add(estado, campo);

        JTextArea claves = new JTextArea(6, 40);
        claves.setEditable(false);
        claves.setText(clavesDelPerfil());
        claves.setCaretPosition(0);
        JPanel abajoClaves = new JPanel(new BorderLayout(0, 4));
        abajoClaves.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        abajoClaves.add(new JLabel("Claves de 1RM para usar al crear metas en el panel:"), BorderLayout.NORTH);
        abajoClaves.add(new JScrollPane(claves), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(probar);
        botones.add(enviarRm);
        botones.add(Box.createHorizontalStrut(12));
        botones.add(guardar);
        botones.add(cancelar);

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(formulario, BorderLayout.NORTH);
        centro.add(abajoClaves, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        char ocultar = token.getEchoChar();
        mostrar.addActionListener(e -> token.setEchoChar(mostrar.isSelected() ? (char) 0 : ocultar));
        probar.addActionListener(e -> probarConexion());
        enviarRm.addActionListener(e -> enviarHistorial());
        guardar.addActionListener(e -> guardarConfiguracion());
        cancelar.addActionListener(e -> dispose());

        ConexionPanel guardada = ctx.panel().get();
        enviarRm.setEnabled(ctx.esPerfilDelPanel() && guardada != null && guardada.activo());
        enviarRm.setToolTipText("Usa la conexión guardada, no lo escrito acá sin guardar.");
        getRootPane().setDefaultButton(guardar);
        pack();
        setLocationRelativeTo(dueno);
    }

    static void mostrar(JFrame dueno, Contexto ctx) {
        new DialogoPanel(dueno, ctx).setVisible(true);
    }

    private static GridBagConstraints celda(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.fill = x == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        c.weightx = x == 1 ? 1 : 0;
        c.insets = new Insets(3, 3, 3, 3);
        return c;
    }

    private void prellenar() {
        ConfigPanel cfg = ConfigPanel.cargar(carpetaDatos.resolve("panel-cliente.properties"));
        url.setText(cfg.url());
        token.setText(cfg.token());
        dispositivo.setText(cfg.dispositivo().isEmpty() ? nombreDeEstaPc() : cfg.dispositivo());
        boolean hayDatos = !cfg.url().isEmpty() && !cfg.token().isEmpty();
        enviar.setSelected(!hayDatos || cfg.activo());
    }

    private static String nombreDeEstaPc() {
        return NombreDispositivo.porDefecto(System.getenv());
    }

    private String clavesDelPerfil() {
        try {
            return String.join("\n", ClavesPanel.lista(ctx.ejercicios.listar(ctx.perfil.id())));
        } catch (Exception e) {
            return "No se pudieron leer los ejercicios.";
        }
    }

    private String urlEscrita() {
        return url.getText().trim();
    }

    private String tokenEscrito() {
        return new String(token.getPassword()).trim();
    }

    private String dispositivoEscrito() {
        String d = dispositivo.getText().trim();
        return d.isEmpty() ? nombreDeEstaPc() : d;
    }

    /** Muestra los errores de validación; devuelve true si no había ninguno. */
    private boolean validar() {
        List<String> errores = ValidacionConexion.errores(urlEscrita(), tokenEscrito());
        if (errores.isEmpty()) return true;
        JOptionPane.showMessageDialog(this, String.join("\n", errores), "Revisá los datos", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private void ocupado(boolean si, String texto) {
        for (var b : new JButton[]{probar, enviarRm, guardar, cancelar}) b.setEnabled(!si);
        if (!si) {
            ConexionPanel guardada = ctx.panel().get();
            enviarRm.setEnabled(ctx.esPerfilDelPanel() && guardada != null && guardada.activo());
        }
        estado.setText(texto);
        setCursor(si ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR) : java.awt.Cursor.getDefaultCursor());
    }

    private void probarConexion() {
        if (!validar()) return;
        String u = urlEscrita();
        String t = tokenEscrito();
        ocupado(true, "Probando…");
        new SwingWorker<PanelClient.Conexion, Void>() {
            @Override
            protected PanelClient.Conexion doInBackground() {
                return ConexionPanel.probarValores(u, t);
            }

            @Override
            protected void done() {
                ocupado(false, " ");
                PanelClient.Conexion res;
                try {
                    res = get();
                } catch (InterruptedException | ExecutionException e) {
                    res = PanelClient.Conexion.SIN_RESPUESTA;
                }
                String texto = switch (res) {
                    case OK -> "Conectado";
                    case NO_AUTORIZADO -> "Token inválido";
                    case SIN_RESPUESTA -> "No responde";
                };
                JOptionPane.showMessageDialog(DialogoPanel.this, texto, "Probar conexión",
                        res == PanelClient.Conexion.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            }
        }.execute();
    }

    private void enviarHistorial() {
        ConexionPanel conexion = ctx.panel().get();
        if (conexion == null || !conexion.activo()) return;
        ocupado(true, "Enviando…");
        new SwingWorker<HistorialRm.Envio, Void>() {
            private volatile int pendientes;

            @Override
            protected HistorialRm.Envio doInBackground() throws Exception {
                HistorialRm.Envio envio = HistorialRm.enviarTodo(conexion, ctx.ejercicios, ctx.perfil.id(),
                        conexion.dispositivo(), System.currentTimeMillis());
                conexion.vaciarAhora();
                pendientes = conexion.pendientes();
                return envio;
            }

            @Override
            protected void done() {
                ocupado(false, " ");
                try {
                    HistorialRm.Envio envio = get();
                    String texto = TextosPanel.historialEnviado(envio.enviados(), pendientes, envio.advertencias());
                    JOptionPane.showMessageDialog(DialogoPanel.this, texto, "Historial de 1RM",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(DialogoPanel.this,
                            "No se pudo leer el historial: " + TextosPanel.causa(e.getCause()),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void guardarConfiguracion() {
        if (!validar()) return;
        String u = urlEscrita();
        String t = tokenEscrito();
        String d = dispositivoEscrito();
        boolean enviarAlPanel = enviar.isSelected();
        ocupado(true, "Guardando…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ConexionPanel.guardarConfig(carpetaDatos, u, t, d, enviarAlPanel);
                ctx.panel().reabrir(() -> ConexionPanel.abrir(carpetaDatos));
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ocupado(false, " ");
                } catch (ExecutionException e) {
                    ocupado(false, " ");
                    JOptionPane.showMessageDialog(DialogoPanel.this,
                            "No se pudo guardar la configuración: " + TextosPanel.causa(e.getCause()), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
