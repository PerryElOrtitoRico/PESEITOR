package com.rutina.service.datos;

import com.rutina.model.Metodologia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Tablas del método (compartidas por todos los perfiles), guardadas como un JSON. */
public final class MetodologiaRepo {

    private final Database db;

    public MetodologiaRepo(Database db) {
        this.db = db;
    }

    /** Si la base es nueva, carga los valores originales. */
    static void inicializar(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM metodologia")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        escribir(c, DatosIniciales.metodologia());
    }

    public Metodologia cargar() throws SQLException {
        try (Statement st = db.conexion().createStatement();
             ResultSet rs = st.executeQuery("SELECT json FROM metodologia WHERE id = 1")) {
            if (!rs.next()) return DatosIniciales.metodologia();
            Metodologia m = Database.GSON.fromJson(rs.getString(1), Metodologia.class);
            if (m.diaDeltaImr() != null) return m;
            return new Metodologia(m.variantes(), m.diaPct(), DatosIniciales.diaDeltaImr(), m.ref(), m.repsPorSerie(),
                    m.plantillas(), m.volPlio(), m.incrementoKg());
        }
    }

    public void guardar(Metodologia m) throws SQLException {
        escribir(db.conexion(), m);
    }

    public void restaurar() throws SQLException {
        escribir(db.conexion(), DatosIniciales.metodologia());
    }

    private static void escribir(Connection c, Metodologia m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO metodologia (id, json) VALUES (1, ?)")) {
            ps.setString(1, Database.GSON.toJson(m));
            ps.executeUpdate();
        }
    }
}
