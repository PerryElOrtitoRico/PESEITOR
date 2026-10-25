package com.rutina.service.datos;

import com.rutina.registro.JsonRegistro;
import com.rutina.registro.RegistroCelular;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/** Último registro del celular importado para cada mesociclo (uno por mesociclo; importar de nuevo lo reemplaza). */
public final class RegistroRepo {

    private final Database db;

    public RegistroRepo(Database db) {
        this.db = db;
    }

    /** Inserta o reemplaza el registro de r.mesocicloId(); importado = fecha y hora actuales (ISO). */
    public void guardar(RegistroCelular r) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "INSERT OR REPLACE INTO registro (mesociclo_id, json, importado) VALUES (?, ?, ?)")) {
            ps.setLong(1, r.mesocicloId());
            ps.setString(2, JsonRegistro.registroAJson(r));
            ps.setString(3, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
        }
    }

    public Optional<RegistroCelular> cargar(long mesocicloId) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement("SELECT json FROM registro WHERE mesociclo_id = ?")) {
            ps.setLong(1, mesocicloId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(JsonRegistro.registroDeJson(rs.getString(1)));
            }
        }
    }

    public void borrar(long mesocicloId) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement("DELETE FROM registro WHERE mesociclo_id = ?")) {
            ps.setLong(1, mesocicloId);
            ps.executeUpdate();
        }
    }
}
