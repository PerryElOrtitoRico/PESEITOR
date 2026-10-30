package com.rutina.panel;

import com.rutina.model.Ejercicio;
import com.rutina.model.Tipo;
import com.rutina.service.datos.Database;
import com.rutina.service.datos.EjercicioRepo;
import com.rutina.service.datos.PerfilRepo;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import panel.cliente.ConexionPanel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class HistorialRmTest {

    private static final long AHORA = 1_700_000_000_000L;

    @TempDir
    Path carpeta;

    private HttpServer servidor;
    private final List<String> cuerpos = new CopyOnWriteArrayList<>();

    @BeforeEach
    void levantarServidor() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", ex -> {
            cuerpos.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        servidor.start();
    }

    @AfterEach
    void bajarServidor() {
        servidor.stop(0);
    }

    private ConexionPanel conexionActiva() throws Exception {
        ConexionPanel.guardarConfig(carpeta, "http://127.0.0.1:" + servidor.getAddress().getPort(),
                "token-de-prueba-123", "pc", true);
        return ConexionPanel.abrir(carpeta);
    }

    private static void esperar(BooleanSupplier cond) throws InterruptedException {
        long limite = System.nanoTime() + 5_000_000_000L;
        while (!cond.getAsBoolean() && System.nanoTime() < limite) Thread.sleep(20);
    }

    private static Ejercicio nuevo(long perfil, String nombre, boolean activo) {
        return new Ejercicio(0, perfil, nombre, Tipo.FUERZA, null, null, 100, Map.of(), null, 0, activo);
    }

    /** Perfil con los ejercicios iniciales; se vacían a mano para controlar los datos. */
    private static long perfilSinEjercicios(Database db) throws Exception {
        long perfil = new PerfilRepo(db).crear("Franco").id();
        try (var st = db.conexion().createStatement()) {
            st.executeUpdate("DELETE FROM historial_rm");
            st.executeUpdate("DELETE FROM ejercicio");
        }
        return perfil;
    }

    @Test
    void enviarTodoMandaUnoPorEjercicioYFecha() throws Exception {
        long idBanca;
        try (var db = Database.abrir(carpeta.resolve("r.db")); ConexionPanel c = conexionActiva()) {
            long perfil = perfilSinEjercicios(db);
            var repo = new EjercicioRepo(db);
            var banca = repo.guardar(nuevo(perfil, "Press banca", true));
            var inactivo = repo.guardar(nuevo(perfil, "Dominadas", false));
            idBanca = banca.id();
            repo.actualizarRm(banca.id(), 100, LocalDate.of(2026, 10, 1));
            repo.actualizarRm(banca.id(), 102.5, LocalDate.of(2026, 10, 1));
            repo.actualizarRm(banca.id(), 105, LocalDate.of(2026, 10, 20));
            repo.actualizarRm(inactivo.id(), 40, LocalDate.of(2026, 10, 3));

            var envio = HistorialRm.enviarTodo(c, repo, perfil, "pc", AHORA);
            assertEquals(new HistorialRm.Envio(3, List.of()), envio);
            esperar(() -> cuerpos.size() >= 3);
        }
        assertEquals(3, cuerpos.size(), cuerpos.toString());
        String todo = String.join("\n", cuerpos);
        assertTrue(todo.contains("\"idEnFuente\":\"rm:" + idBanca + ":2026-10-01\""), todo);
        assertTrue(todo.contains("\"idEnFuente\":\"rm:" + idBanca + ":2026-10-20\""), todo);
        assertTrue(todo.contains("\"valor\":102.5"), todo);
        assertFalse(todo.contains("\"valor\":100.0") || todo.contains("\"valor\":100,"), todo);
        assertTrue(todo.contains("\"valor\":105.0") || todo.contains("\"valor\":105,"), todo);
        assertTrue(todo.contains("\"metrica\":\"1rm:dominadas\""), todo);
    }

    @Test
    void dosEjerciciosConLaMismaClaveSeAvisan() throws Exception {
        long idA;
        try (var db = Database.abrir(carpeta.resolve("r.db")); ConexionPanel c = conexionActiva()) {
            long perfil = perfilSinEjercicios(db);
            var repo = new EjercicioRepo(db);
            var a = repo.guardar(nuevo(perfil, "Press banca", true));
            var b = repo.guardar(nuevo(perfil, "press  BANCA", true));
            idA = a.id();
            repo.actualizarRm(a.id(), 100, LocalDate.of(2026, 10, 1));
            repo.actualizarRm(b.id(), 90, LocalDate.of(2026, 10, 1));

            var envio = HistorialRm.enviarTodo(c, repo, perfil, "pc", AHORA);
            assertEquals(1, envio.enviados());
            assertEquals(List.of("La clave 1rm:press-banca está repetida: se manda solo «Press banca», no «press  BANCA»."),
                    envio.advertencias());
            esperar(() -> cuerpos.size() >= 1);
        }
        assertEquals(1, cuerpos.size(), cuerpos.toString());
        assertTrue(cuerpos.get(0).contains("\"idEnFuente\":\"rm:" + idA + ":2026-10-01\""), cuerpos.get(0));
        assertTrue(cuerpos.get(0).contains("\"valor\":100.0"), cuerpos.get(0));
    }

    @Test
    void claveVaciaSeAvisaYNoSeManda() throws Exception {
        try (var db = Database.abrir(carpeta.resolve("r.db")); ConexionPanel c = conexionActiva()) {
            long perfil = perfilSinEjercicios(db);
            var repo = new EjercicioRepo(db);
            var raro = repo.guardar(nuevo(perfil, "(***)", true));
            repo.actualizarRm(raro.id(), 50, LocalDate.of(2026, 10, 1));

            var envio = HistorialRm.enviarTodo(c, repo, perfil, "pc", AHORA);
            assertEquals(new HistorialRm.Envio(0, List.of("Sin clave utilizable para «(***)»: no se mandó.")), envio);
            c.vaciarAhora();
        }
        assertTrue(cuerpos.isEmpty());
    }

    @Test
    void panelInactivoNoPublicaNada() throws Exception {
        try (var db = Database.abrir(carpeta.resolve("r.db")); ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            long perfil = perfilSinEjercicios(db);
            var repo = new EjercicioRepo(db);
            var e = repo.guardar(nuevo(perfil, "Press banca", true));
            repo.actualizarRm(e.id(), 100, LocalDate.of(2026, 10, 1));
            assertEquals(new HistorialRm.Envio(0, List.of()), HistorialRm.enviarTodo(c, repo, perfil, "pc", AHORA));
        }
        assertFalse(Files.exists(carpeta.resolve("panel-cola.jsonl")));
        assertTrue(cuerpos.isEmpty());
    }
}
