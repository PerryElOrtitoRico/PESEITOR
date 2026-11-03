package com.rutina.ui;

import com.rutina.model.Perfil;
import com.rutina.service.datos.PerfilRepo;

import javax.swing.JOptionPane;
import java.sql.SQLException;
import java.util.List;

/** Elige el perfil al abrir: entra directo si hay uno solo, pide un nombre si no hay ninguno. */
final class SelectorPerfil {

    private static final String NUEVO = "+ Nuevo perfil…";

    private SelectorPerfil() {
    }

    /** null si el usuario canceló. */
    static Perfil elegir(PerfilRepo repo, boolean forzarLista) throws SQLException {
        List<Perfil> perfiles = repo.listar();
        if (perfiles.isEmpty()) return crear(repo, "Bienvenido. ¿Cómo te llamás? (se crea tu perfil)");
        if (perfiles.size() == 1 && !forzarLista) return perfiles.get(0);

        Object[] opciones = new Object[perfiles.size() + 1];
        for (int i = 0; i < perfiles.size(); i++) opciones[i] = perfiles.get(i).nombre();
        opciones[perfiles.size()] = NUEVO;
        Object elegido = JOptionPane.showInputDialog(null, "Perfil:", "PESEITOR",
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (elegido == null) return null;
        if (elegido == NUEVO) return crear(repo, "Nombre del nuevo perfil (arranca sin 1RM cargados):");
        return perfiles.stream().filter(p -> p.nombre().equals(elegido)).findFirst().orElse(null);
    }

    private static Perfil crear(PerfilRepo repo, String mensaje) throws SQLException {
        String nombre = JOptionPane.showInputDialog(null, mensaje, "Nuevo perfil", JOptionPane.QUESTION_MESSAGE);
        if (nombre == null || nombre.isBlank()) return null;
        return repo.crear(nombre.trim());
    }
}
