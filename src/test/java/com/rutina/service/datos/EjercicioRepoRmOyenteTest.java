package com.rutina.service.datos;

import com.rutina.model.Ejercicio;
import com.rutina.model.Tipo;
import com.rutina.service.datos.EjercicioRepo.RmRegistrado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EjercicioRepoRmOyenteTest {

    @TempDir
    Path d;

    private static Ejercicio nuevo(long perfil, Double rm) {
        return new Ejercicio(0, perfil, "Press banca", Tipo.FUERZA, rm, null, 100, Map.of(), null, 0, true);
    }

    @Test
    void elOyenteSeLlamaAlCambiarElRm() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            long perfil = new PerfilRepo(db).crear("Franco").id();
            var repo = new EjercicioRepo(db);
            List<RmRegistrado> vistos = new ArrayList<>();
            repo.alRegistrarRm(vistos::add);
            var e = repo.guardar(nuevo(perfil, 100.0));
            assertEquals(List.of(new RmRegistrado(e.id(), perfil, "Press banca", LocalDate.now(), 100.0)), vistos);
            vistos.clear();
            repo.guardar(new Ejercicio(e.id(), perfil, "Press banca", Tipo.FUERZA, 105.0, null, 100, Map.of(), null, 0, true));
            assertEquals(List.of(new RmRegistrado(e.id(), perfil, "Press banca", LocalDate.now(), 105.0)), vistos);
            vistos.clear();
            repo.actualizarRm(e.id(), 110, LocalDate.of(2026, 11, 1));
            assertEquals(List.of(new RmRegistrado(e.id(), perfil, "Press banca", LocalDate.of(2026, 11, 1), 110.0)), vistos);
        }
    }

    @Test
    void noSeLlamaSiElRmNoCambia() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            long perfil = new PerfilRepo(db).crear("Franco").id();
            var repo = new EjercicioRepo(db);
            var e = repo.guardar(nuevo(perfil, 100.0));
            List<RmRegistrado> vistos = new ArrayList<>();
            repo.alRegistrarRm(vistos::add);
            repo.guardar(new Ejercicio(e.id(), perfil, "Otro nombre", Tipo.FUERZA, 100.0, null, 100, Map.of(), null, 0, true));
            assertEquals(List.of(), vistos);
        }
    }

    @Test
    void noSeLlamaSiElRmEsNulo() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            long perfil = new PerfilRepo(db).crear("Franco").id();
            var repo = new EjercicioRepo(db);
            List<RmRegistrado> vistos = new ArrayList<>();
            repo.alRegistrarRm(vistos::add);
            var e = repo.guardar(nuevo(perfil, null));
            repo.guardar(new Ejercicio(e.id(), perfil, "Press banca", Tipo.FUERZA, null, null, 100, Map.of(), null, 1, true));
            assertEquals(List.of(), vistos);
        }
    }

    @Test
    void unaExcepcionDelOyenteNoRompeElGuardado() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            long perfil = new PerfilRepo(db).crear("Franco").id();
            var repo = new EjercicioRepo(db);
            repo.alRegistrarRm(rm -> {
                throw new IllegalStateException("falla del oyente");
            });
            var e = assertDoesNotThrow(() -> repo.guardar(nuevo(perfil, 100.0)));
            assertEquals(1, repo.historial(e.id()).size());
            assertDoesNotThrow(() -> repo.actualizarRm(e.id(), 101, LocalDate.of(2026, 11, 2)));
            assertEquals(2, repo.historial(e.id()).size());
        }
    }

    @Test
    void conNullSeQuitaElOyente() throws Exception {
        try (var db = Database.abrir(d.resolve("r.db"))) {
            long perfil = new PerfilRepo(db).crear("Franco").id();
            var repo = new EjercicioRepo(db);
            List<RmRegistrado> vistos = new ArrayList<>();
            repo.alRegistrarRm(vistos::add);
            repo.alRegistrarRm(null);
            repo.guardar(nuevo(perfil, 100.0));
            assertEquals(List.of(), vistos);
        }
    }
}
