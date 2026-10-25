package com.rutina.ui;

import com.rutina.model.Ejercicio;
import com.rutina.model.Metodologia;
import com.rutina.model.Perfil;
import com.rutina.service.datos.Database;
import com.rutina.service.datos.EjercicioRepo;
import com.rutina.service.datos.MesocicloRepo;
import com.rutina.service.datos.MetodologiaRepo;
import com.rutina.service.datos.PlanPlioRepo;
import com.rutina.service.datos.RegistroRepo;

import javax.swing.JOptionPane;
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
    final MetodologiaRepo metodologiaRepo;
    final RegistroRepo registros;
    private Metodologia metodologia;
    private final List<Runnable> oyentes = new ArrayList<>();

    Contexto(Database db, Perfil perfil) throws SQLException {
        this.db = db;
        this.perfil = perfil;
        this.ejercicios = new EjercicioRepo(db);
        this.mesociclos = new MesocicloRepo(db);
        this.planes = new PlanPlioRepo(db);
        this.metodologiaRepo = new MetodologiaRepo(db);
        this.registros = new RegistroRepo(db);
        this.metodologia = metodologiaRepo.cargar();
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
