package com.rutina.ui;

import com.rutina.model.Ejercicio;
import com.rutina.model.Metodologia;
import com.rutina.model.Perfil;
import com.rutina.panel.RmAPanel;
import com.rutina.service.datos.Database;
import com.rutina.service.datos.EjercicioRepo;
import com.rutina.service.datos.MacrocicloRepo;
import com.rutina.service.datos.MesocicloRepo;
import com.rutina.service.datos.MetodologiaRepo;
import com.rutina.service.datos.PerfilRepo;
import com.rutina.service.datos.PlanPlioRepo;
import com.rutina.service.datos.RegistroRepo;

import javax.swing.JOptionPane;
import panel.cliente.ConexionPanel;
import panel.cliente.PanelCompartido;

import java.awt.Component;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lo que comparten las pestañas: perfil, repositorios, metodología y aviso de cambios. */
public final class Contexto {

    final Database db;
    final Perfil perfil;
    final EjercicioRepo ejercicios;
    final MesocicloRepo mesociclos;
    final PlanPlioRepo planes;
    final MacrocicloRepo macros;
    final MetodologiaRepo metodologiaRepo;
    final RegistroRepo registros;
    private final PanelCompartido panel;
    private final boolean esPerfilDelPanel;
    private Metodologia metodologia;
    private final List<Runnable> oyentes = new ArrayList<>();

    Contexto(Database db, Perfil perfil, PanelCompartido panel) throws SQLException {
        this.db = db;
        this.panel = panel;
        this.perfil = perfil;
        this.ejercicios = new EjercicioRepo(db);
        this.mesociclos = new MesocicloRepo(db);
        this.planes = new PlanPlioRepo(db);
        this.macros = new MacrocicloRepo(db);
        this.metodologiaRepo = new MetodologiaRepo(db);
        this.registros = new RegistroRepo(db);
        this.metodologia = metodologiaRepo.cargar();
        List<Perfil> perfiles = new PerfilRepo(db).listar();
        this.esPerfilDelPanel = !perfiles.isEmpty() && perfiles.get(0).id() == perfil.id();
        if (esPerfilDelPanel) ejercicios.alRegistrarRm(this::mandarRm);
    }

    /** Cada 1RM nuevo del perfil del panel se manda; un fallo acá nunca debe afectar al guardado. */
    private void mandarRm(EjercicioRepo.RmRegistrado rm) {
        try {
            if (!esPerfilDelPanel || rm.perfilId() != perfil.id()) return;
            ConexionPanel conexion = panel.get();
            if (conexion == null || !conexion.activo()) return;
            if (!RmAPanel.esRepresentante(rm.ejercicioId(), rm.nombre(), ejercicios.listar(perfil.id()))) return;
            RmAPanel.aRegistro(rm, conexion.dispositivo(), System.currentTimeMillis()).ifPresent(conexion::publicar);
        } catch (RuntimeException | SQLException ignorada) {
            // el panel es opcional
        }
    }

    PanelCompartido panel() {
        return panel;
    }

    /** El panel recibe solo los datos del primer perfil (el de menor id). */
    boolean esPerfilDelPanel() {
        return esPerfilDelPanel;
    }

    Metodologia metodologia() {
        return metodologia;
    }

    void recargarMetodologia() throws SQLException {
        metodologia = metodologiaRepo.cargar();
    }

    Map<Long, Ejercicio> mapaEjercicios() throws SQLException {
        Map<Long, Ejercicio> m = new LinkedHashMap<>();
        for (Ejercicio e : ejercicios.listar(perfil.id())) m.put(e.id(), e);
        return m;
    }

    /** Algo cambió (ejercicios, plantillas, mesociclos): las pestañas se refrescan. */
    void cambio() {
        oyentes.forEach(Runnable::run);
    }

    void alCambiar(Runnable r) {
        oyentes.add(r);
    }

    static void error(Component padre, Exception e) {
        JOptionPane.showMessageDialog(padre, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
