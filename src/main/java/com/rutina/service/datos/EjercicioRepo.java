package com.rutina.service.datos;

import com.google.gson.reflect.TypeToken;
import com.rutina.model.Ejercicio;
import com.rutina.model.PuntoRm;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;

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
import java.util.function.Consumer;

/** Ejercicios de un perfil y el historial de sus 1RM. */
public final class EjercicioRepo {

    private static final Type REPS = new TypeToken<Map<Zona, Integer>>() {
    }.getType();

    /** Un 1RM que acaba de quedar en el historial. */
    public record RmRegistrado(long ejercicioId, long perfilId, String nombre, LocalDate fecha, double rm) {
    }

    private final Database db;
    private Consumer<RmRegistrado> oyenteRm;

    public EjercicioRepo(Database db) {
        this.db = db;
    }

    public List<Ejercicio> listar(long perfilId) throws SQLException {
        List<Ejercicio> res = new ArrayList<>();
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "SELECT id, perfil_id, nombre, tipo, rm, rm_objetivo, techo, reps_json, grupo_plio, orden, activo"
                        + " FROM ejercicio WHERE perfil_id = ? ORDER BY id")) {
            ps.setLong(1, perfilId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<Zona, Integer> reps = Database.GSON.fromJson(rs.getString(8), REPS);
                    res.add(new Ejercicio(rs.getLong(1), rs.getLong(2), rs.getString(3), Tipo.valueOf(rs.getString(4)),
                            doble(rs, 5), doble(rs, 6), rs.getInt(7),
                            reps == null ? Map.of() : reps,
                            rs.getString(9), rs.getInt(10), rs.getInt(11) != 0));
                }
            }
        }
        return res;
    }

    /** Avisa cada vez que se agrega un 1RM al historial (un solo oyente; null lo quita). */
    public void alRegistrarRm(Consumer<RmRegistrado> oyente) {
        this.oyenteRm = oyente;
    }

    /** Inserta si {@code id == 0}; si el 1RM cambió, lo agrega al historial con fecha de hoy. */
    public Ejercicio guardar(Ejercicio e) throws SQLException {
        if (e.id() == 0) {
            long id;
            try (PreparedStatement ps = db.conexion().prepareStatement(
                    "INSERT INTO ejercicio (perfil_id, nombre, tipo, rm, rm_objetivo, techo, reps_json, grupo_plio, orden, activo)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, e.perfilId());
                completar(ps, 2, e);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    id = rs.getLong(1);
                }
            }
            if (e.rm() != null) agregarHistorial(id, e.perfilId(), e.nombre(), e.rm(), LocalDate.now());
            return new Ejercicio(id, e.perfilId(), e.nombre(), e.tipo(), e.rm(), e.rmObjetivo(), e.techoPct(),
                    e.repsPorSerie(), e.grupoPlio(), e.orden(), e.activo());
        }
        Double anterior = rmActual(e.id());
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "UPDATE ejercicio SET nombre = ?, tipo = ?, rm = ?, rm_objetivo = ?, techo = ?, reps_json = ?,"
                        + " grupo_plio = ?, orden = ?, activo = ? WHERE id = ?")) {
            completar(ps, 1, e);
            ps.setLong(10, e.id());
            ps.executeUpdate();
        }
        if (e.rm() != null && !e.rm().equals(anterior)) agregarHistorial(e.id(), e.perfilId(), e.nombre(), e.rm(), LocalDate.now());
        return e;
    }

    public void actualizarRm(long id, double rm, LocalDate fecha) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement("UPDATE ejercicio SET rm = ? WHERE id = ?")) {
            ps.setDouble(1, rm);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "SELECT perfil_id, nombre FROM ejercicio WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) agregarHistorial(id, rs.getLong(1), rs.getString(2), rm, fecha);
                else agregarHistorial(id, rm, fecha);
            }
        }
    }

    public List<PuntoRm> historial(long id) throws SQLException {
        List<PuntoRm> res = new ArrayList<>();
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "SELECT fecha, rm FROM historial_rm WHERE ejercicio_id = ? ORDER BY fecha, id")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) res.add(new PuntoRm(LocalDate.parse(rs.getString(1)), rs.getDouble(2)));
            }
        }
        return res;
    }

    /** Agrega al historial y avisa al oyente; lo que haga el oyente nunca afecta al guardado. */
    private void agregarHistorial(long id, long perfilId, String nombre, double rm, LocalDate fecha) throws SQLException {
        agregarHistorial(id, rm, fecha);
        Consumer<RmRegistrado> oyente = oyenteRm;
        if (oyente == null) return;
        try {
            oyente.accept(new RmRegistrado(id, perfilId, nombre, fecha, rm));
        } catch (RuntimeException ignorada) {
            // el cambio ya está en la base
        }
    }

    private void agregarHistorial(long id, double rm, LocalDate fecha) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "INSERT INTO historial_rm (ejercicio_id, fecha, rm) VALUES (?, ?, ?)")) {
            ps.setLong(1, id);
            ps.setString(2, fecha.toString());
            ps.setDouble(3, rm);
            ps.executeUpdate();
        }
    }

    private Double rmActual(long id) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement("SELECT rm FROM ejercicio WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? doble(rs, 1) : null;
            }
        }
    }

    /** Completa nombre…activo (9 parámetros) a partir de {@code desde}. */
    private static void completar(PreparedStatement ps, int desde, Ejercicio e) throws SQLException {
        ps.setString(desde, e.nombre());
        ps.setString(desde + 1, e.tipo().name());
        setDoble(ps, desde + 2, e.rm());
        setDoble(ps, desde + 3, e.rmObjetivo());
        ps.setInt(desde + 4, e.techoPct());
        ps.setString(desde + 5, Database.GSON.toJson(e.repsPorSerie() == null ? Map.of() : e.repsPorSerie(), REPS));
        ps.setString(desde + 6, e.grupoPlio());
        ps.setInt(desde + 7, e.orden());
        ps.setInt(desde + 8, e.activo() ? 1 : 0);
    }

    private static void setDoble(PreparedStatement ps, int i, Double v) throws SQLException {
        if (v == null) ps.setNull(i, Types.REAL);
        else ps.setDouble(i, v);
    }

    private static Double doble(ResultSet rs, int i) throws SQLException {
        double v = rs.getDouble(i);
        return rs.wasNull() ? null : v;
    }
}
