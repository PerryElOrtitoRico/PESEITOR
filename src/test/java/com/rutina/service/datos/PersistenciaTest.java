package com.rutina.service.datos;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.ClavePlantilla;
import com.rutina.model.Mesociclo;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.PlanPliometria;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PersistenciaTest {

    @TempDir
    Path d;

    private Database abrir() throws Exception {
        return Database.abrir(d.resolve("r.db"));
    }

    @Test
    void baseNuevaTraeMetodologia() throws Exception {
        var m = new MetodologiaRepo(abrir()).cargar();
        assertEquals(10, m.variantes().size());
        assertEquals("3x6-70/75%", m.plantillas().get(new ClavePlantilla(Tipo.FUERZA, 3, 2)).semanas().get(1).get(0).texto());
        assertArrayEquals(new int[]{4, 4, 4, 4, 3, 3, 3, 2, 1, 1}, m.repsPorSerie().get(Tipo.FUERZA));
    }

    @Test
    void metodologiaSeGuardaYRestaura() throws Exception {
        var repo = new MetodologiaRepo(abrir());
        var m = repo.cargar();
        repo.guardar(new com.rutina.model.Metodologia(m.variantes(), m.diaPct(), m.ref(), m.repsPorSerie(),
                m.plantillas(), m.volPlio(), 1.0));
        assertEquals(1.0, repo.cargar().incrementoKg());
        repo.restaurar();
        assertEquals(2.5, repo.cargar().incrementoKg());
    }

    @Test
    void historialRm() throws Exception {
        var db = abrir();
        var p = new PerfilRepo(db).crear("Franco");
        var repo = new EjercicioRepo(db);
        var e = repo.listar(p.id()).get(0);
        repo.actualizarRm(e.id(), 120, LocalDate.of(2026, 11, 1));
        assertEquals(2, repo.historial(e.id()).size());
        assertEquals(120.0, repo.listar(p.id()).get(0).rm());
    }

    @Test
    void ejercicioSeEditaConSusRepsPropias() throws Exception {
        var db = abrir();
        var p = new PerfilRepo(db).crear("Franco");
        var repo = new EjercicioRepo(db);
        var e = repo.listar(p.id()).get(0);
        repo.guardar(new com.rutina.model.Ejercicio(e.id(), e.perfilId(), e.nombre(), e.tipo(), e.rm(), e.rmObjetivo(),
                85, Map.of(Zona.Z80, 5), null, e.orden(), true));
        var leido = repo.listar(p.id()).get(0);
        assertEquals(85, leido.techoPct());
        assertEquals(Map.of(Zona.Z80, 5), leido.repsPorSerie());
    }

    @Test
    void segundoPerfilSinRm() throws Exception {
        var db = abrir();
        var pr = new PerfilRepo(db);
        pr.crear("Franco");
        var amigo = pr.crear("Amigo");
        var ejs = new EjercicioRepo(db).listar(amigo.id());
        assertEquals(50, ejs.size());
        assertTrue(ejs.stream().allMatch(x -> x.rm() == null));
        assertEquals(2, pr.listar().size());
    }

    @Test
    void mesocicloIdaYVuelta() throws Exception {
        var db = abrir();
        var p = new PerfilRepo(db).crear("Franco");
        var repo = new MesocicloRepo(db);
        var mes = new Mesociclo(0, p.id(), LocalDate.of(2026, 10, 26), 5, new ParametrosTipo(250, 72),
                new ParametrosTipo(350, 76), 'A', 3, Map.of(9L, Aproximacion.BASICA),
                List.of(new Bloque(1, 1, 2L, 100), new Bloque(1, 2, 9L, 100)), "Lumbares", "Abdominales", Map.of(2L, 115.0));
        var guardado = repo.guardar(mes);
        assertTrue(guardado.id() > 0);
        var leido = repo.listar(p.id()).get(0);
        assertEquals(guardado, leido);
        repo.guardar(new Mesociclo(leido.id(), p.id(), leido.inicio(), 5, leido.potencia(), leido.fuerza(), null, null,
                Map.of(), List.of(), "", "", Map.of()));
        assertEquals(1, repo.listar(p.id()).size());
        assertNull(repo.listar(p.id()).get(0).variante());
        repo.borrar(leido.id());
        assertTrue(repo.listar(p.id()).isEmpty());
    }

    @Test
    void objetivoYMacrocicloIdaYVuelta() throws Exception {
        var db = abrir();
        var p = new PerfilRepo(db).crear("Franco");
        var macros = new MacrocicloRepo(db);
        var macro = macros.guardar(new com.rutina.model.Macrociclo(0, p.id(), "Temporada 2026", "Fuerza"));
        var repo = new MesocicloRepo(db);
        var mes = new Mesociclo(0, p.id(), LocalDate.of(2026, 11, 9), 4, new ParametrosTipo(0, 72),
                new ParametrosTipo(350, 76), null, null, Map.of(), List.of(), "", "", Map.of(), "Sentadilla", macro.id());
        var guardado = repo.guardar(mes);
        assertEquals(guardado, repo.listar(p.id()).get(0));
        assertEquals("Sentadilla", repo.listar(p.id()).get(0).objetivo());
        macros.borrar(macro.id());
        assertTrue(macros.listar(p.id()).isEmpty());
        assertNull(repo.listar(p.id()).get(0).macroId());
        assertEquals("Sentadilla", repo.listar(p.id()).get(0).objetivo());
    }

    @Test
    void planPliometria() throws Exception {
        var db = abrir();
        var p = new PerfilRepo(db).crear("Franco");
        var repo = new PlanPlioRepo(db);
        var plan = repo.guardar(new PlanPliometria(0, p.id(), 1, LocalDate.of(2026, 10, 26)));
        assertEquals(List.of(plan), repo.listar(p.id()));
    }

    @Test
    void datosPersistenAlReabrir() throws Exception {
        new PerfilRepo(abrir()).crear("Franco");
        assertEquals("Franco", new PerfilRepo(abrir()).listar().get(0).nombre());
    }

    @Test
    void archivoCorruptoSeRespalda() throws Exception {
        var f = d.resolve("r.db");
        Files.writeString(f, "basura que no es una base de datos sqlite, para nada, ni un poco");
        var db = Database.abrir(f);
        assertTrue(db.seRestauro());
        try (var l = Files.list(d)) {
            assertEquals(1, l.filter(x -> x.getFileName().toString().startsWith("r.db.bak-")).count());
        }
        assertEquals(10, new MetodologiaRepo(db).cargar().variantes().size());
    }

    @Test
    void elJournalSeRespaldaJuntoConLaBase() throws Exception {
        var f = d.resolve("r.db");
        Files.writeString(f, "basura que no es una base de datos sqlite, para nada, ni un poco");
        Files.writeString(d.resolve("r.db-journal"), "journal viejo");
        Database.abrir(f);
        // el journal viejo no queda junto a la base nueva (SQLite descarta uno inválido; uno válido se respalda)
        assertFalse(Files.exists(d.resolve("r.db-journal")) && Files.readString(d.resolve("r.db-journal")).equals("journal viejo"));
    }

    @Test
    void otrosErroresNoMuevenLaBase() throws Exception {
        // sin permiso de lectura no es una base dañada: no se toca y se informa el error
        var f = d.resolve("r.db");
        new PerfilRepo(Database.abrir(f)).crear("Franco");
        assertTrue(f.toFile().setReadable(false));
        try {
            assertThrows(java.sql.SQLException.class, () -> Database.abrir(f));
            try (var l = Files.list(d)) {
                assertEquals(0, l.filter(x -> x.getFileName().toString().contains(".bak-")).count());
            }
        } finally {
            f.toFile().setReadable(true);
        }
    }

    @Test
    void rutaPorDefectoNoEsRelativa() {
        assertTrue(Database.rutaPorDefecto().isAbsolute());
        assertTrue(Database.rutaPorDefecto().toString().endsWith("rutina/rutina.db"));
    }
}
