package com.rutina.service.datos;

import com.google.gson.reflect.TypeToken;
import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Mesociclo;
import com.rutina.model.ParametrosTipo;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mesociclos de un perfil; la grilla (bloques), las aproximaciones y los 1RM usados van como JSON. */
public final class MesocicloRepo {

    private static final Type APROX = new TypeToken<Map<Long, Aproximacion>>() {
    }.getType();
    private static final Type BLOQUES = new TypeToken<List<Bloque>>() {
    }.getType();
    private static final Type RMS = new TypeToken<Map<Long, Double>>() {
    }.getType();

    private final Database db;

    public MesocicloRepo(Database db) {
        this.db = db;
    }

    public List<Mesociclo> listar(long perfilId) throws SQLException {
        List<Mesociclo> res = new ArrayList<>();
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "SELECT id, perfil_id, inicio, microciclos, pot_vol, pot_imr, fza_vol, fza_imr, variante, dias,"
                        + " aprox_json, bloques_json, calentamiento, auxiliares, rm_json, objetivo, macro_id"
                        + " FROM mesociclo WHERE perfil_id = ? ORDER BY inicio, id")) {
            ps.setLong(1, perfilId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String variante = rs.getString(9);
                    int dias = rs.getInt(10);
                    Integer diasONull = rs.wasNull() ? null : dias;
                    Map<Long, Aproximacion> aprox = Database.GSON.fromJson(rs.getString(11), APROX);
                    List<Bloque> bloques = Database.GSON.fromJson(rs.getString(12), BLOQUES);
                    Map<Long, Double> rms = Database.GSON.fromJson(rs.getString(15), RMS);
                    long macro = rs.getLong(17);
                    Long macroONull = rs.wasNull() ? null : macro;
                    res.add(new Mesociclo(rs.getLong(1), rs.getLong(2), LocalDate.parse(rs.getString(3)), rs.getInt(4),
                            new ParametrosTipo(rs.getInt(5), rs.getInt(6)), new ParametrosTipo(rs.getInt(7), rs.getInt(8)),
                            variante == null ? null : variante.charAt(0), diasONull,
                            aprox == null ? Map.of() : aprox, bloques == null ? List.of() : bloques,
                            rs.getString(13), rs.getString(14), rms == null ? Map.of() : rms,
                            rs.getString(16) == null ? "" : rs.getString(16), macroONull));
                }
            }
        }
        return res;
    }

    /** Inserta si {@code id == 0}; si no, actualiza. Devuelve el mesociclo con su id. */
    public Mesociclo guardar(Mesociclo m) throws SQLException {
        if (m.id() == 0) {
            try (PreparedStatement ps = db.conexion().prepareStatement(
                    "INSERT INTO mesociclo (perfil_id, inicio, microciclos, pot_vol, pot_imr, fza_vol, fza_imr, variante,"
                            + " dias, aprox_json, bloques_json, calentamiento, auxiliares, rm_json, objetivo, macro_id)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                completar(ps, m);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    long id = rs.getLong(1);
                    return new Mesociclo(id, m.perfilId(), m.inicio(), m.microciclos(), m.potencia(), m.fuerza(),
                            m.variante(), m.dias(), m.aproximaciones(), m.bloques(), m.calentamiento(),
                            m.auxiliares(), m.rmUsados(), m.objetivo(), m.macroId());
                }
            }
        }
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "UPDATE mesociclo SET perfil_id = ?, inicio = ?, microciclos = ?, pot_vol = ?, pot_imr = ?, fza_vol = ?,"
                        + " fza_imr = ?, variante = ?, dias = ?, aprox_json = ?, bloques_json = ?, calentamiento = ?,"
                        + " auxiliares = ?, rm_json = ?, objetivo = ?, macro_id = ? WHERE id = ?")) {
            completar(ps, m);
            ps.setLong(17, m.id());
            ps.executeUpdate();
        }
        return m;
    }

    public void borrar(long id) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement("DELETE FROM mesociclo WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static void completar(PreparedStatement ps, Mesociclo m) throws SQLException {
        ps.setLong(1, m.perfilId());
        ps.setString(2, m.inicio().toString());
        ps.setInt(3, m.microciclos());
        ps.setInt(4, m.potencia().volumen());
        ps.setInt(5, m.potencia().imr());
        ps.setInt(6, m.fuerza().volumen());
        ps.setInt(7, m.fuerza().imr());
        if (m.variante() == null) ps.setNull(8, Types.VARCHAR);
        else ps.setString(8, String.valueOf(m.variante()));
        if (m.dias() == null) ps.setNull(9, Types.INTEGER);
        else ps.setInt(9, m.dias());
        ps.setString(10, Database.GSON.toJson(m.aproximaciones(), APROX));
        ps.setString(11, Database.GSON.toJson(m.bloques(), BLOQUES));
        ps.setString(12, m.calentamiento());
        ps.setString(13, m.auxiliares());
        ps.setString(14, Database.GSON.toJson(m.rmUsados(), RMS));
        ps.setString(15, m.objetivo() == null ? "" : m.objetivo().trim());
        if (m.macroId() == null) ps.setNull(16, Types.INTEGER);
        else ps.setLong(16, m.macroId());
    }
}
