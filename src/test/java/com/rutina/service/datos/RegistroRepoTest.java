package com.rutina.service.datos;

import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionRegistro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegistroRepoTest {

    @TempDir
    Path d;

    private static RegistroCelular registro(long mesocicloId, String casilla) {
        return new RegistroCelular(RegistroCelular.FORMATO, 1, "Franco", mesocicloId, List.of(
                new SesionRegistro(1, 1, "2026-10-27", 5, 4, List.of(new EjercicioRegistro("1-1", 3, Map.of("80-85", casilla), null)), null)));
    }

    private static int filas(Database db) throws Exception {
        try (Statement st = db.conexion().createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM registro")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void sinRegistroEsVacio() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            assertEquals(Optional.empty(), new RegistroRepo(db).cargar(3));
        }
    }

    @Test
    void guardarYCargar() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            var repo = new RegistroRepo(db);
            repo.guardar(registro(3, "3.3-115"));
            assertEquals(Optional.of(registro(3, "3.3-115")), repo.cargar(3));
            assertEquals(Optional.empty(), repo.cargar(4));
        }
    }

    @Test
    void reimportarReemplaza() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            var repo = new RegistroRepo(db);
            repo.guardar(registro(3, "3.3-115"));
            repo.guardar(registro(3, "2.2"));
            assertEquals(registro(3, "2.2"), repo.cargar(3).orElseThrow());
            assertEquals(1, filas(db));
        }
    }

    @Test
    void borrar() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            var repo = new RegistroRepo(db);
            repo.guardar(registro(3, "3"));
            repo.borrar(3);
            assertTrue(repo.cargar(3).isEmpty());
        }
    }

    @Test
    void reabrirConserva() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            new RegistroRepo(db).guardar(registro(5, "3"));
        }
        try (var db = Database.abrir(d.resolve("r.db"))) {
            assertEquals(registro(5, "3"), new RegistroRepo(db).cargar(5).orElseThrow());
        }
    }

    @Test
    void baseViejaSinTablaRegistro() throws Exception {
        // una base creada antes de esta función (sin la tabla registro) se abre y gana la tabla sin perder datos
        Path f = d.resolve("vieja.db");
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + f); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE perfil (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL)");
            st.execute("INSERT INTO perfil (nombre) VALUES ('Viejo')");
        }
        try (var db = Database.abrir(f)) {
            assertFalse(db.seRestauro());
            assertEquals("Viejo", new PerfilRepo(db).listar().get(0).nombre());
            new RegistroRepo(db).guardar(registro(1, "3"));
            assertEquals(1, filas(db));
        }
    }
}
