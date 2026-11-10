package com.rutina.service.datos;

import com.rutina.model.Macrociclo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Macrociclos de un perfil. Borrar uno deja sus mesociclos sueltos, no los borra. */
public final class MacrocicloRepo {

    private final Database db;

    public MacrocicloRepo(Database db) {
        this.db = db;
    }

    public List<Macrociclo> listar(long perfilId) throws SQLException {
        List<Macrociclo> res = new ArrayList<>();
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "SELECT id, perfil_id, nombre, objetivo FROM macrociclo WHERE perfil_id = ? ORDER BY id")) {
            ps.setLong(1, perfilId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) res.add(new Macrociclo(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)));
            }
        }
        return res;
    }

    /** Inserta si {@code id == 0}; si no, actualiza. Devuelve el macrociclo con su id. */
    public Macrociclo guardar(Macrociclo m) throws SQLException {
        if (m.id() == 0) {
            try (PreparedStatement ps = db.conexion().prepareStatement(
                    "INSERT INTO macrociclo (perfil_id, nombre, objetivo) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, m.perfilId());
                ps.setString(2, m.nombre());
                ps.setString(3, m.objetivo());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return new Macrociclo(rs.getLong(1), m.perfilId(), m.nombre(), m.objetivo());
                }
            }
        }
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "UPDATE macrociclo SET nombre = ?, objetivo = ? WHERE id = ?")) {
            ps.setString(1, m.nombre());
            ps.setString(2, m.objetivo());
            ps.setLong(3, m.id());
            ps.executeUpdate();
        }
        return m;
    }

    public void borrar(long id) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement("UPDATE mesociclo SET macro_id = NULL WHERE macro_id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = db.conexion().prepareStatement("DELETE FROM macrociclo WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
