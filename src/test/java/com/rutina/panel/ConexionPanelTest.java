package com.rutina.panel;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import panel.core.PanelClient;
import panel.core.Registro;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ConexionPanelTest {

    private record Peticion(String metodo, String ruta, String autorizacion, String cuerpo) {
    }

    @TempDir
    Path carpeta;

    private HttpServer servidor;
    private final List<Peticion> recibidas = new CopyOnWriteArrayList<>();

    @BeforeEach
    void levantarServidor() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", ex -> {
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            recibidas.add(new Peticion(ex.getRequestMethod(), ex.getRequestURI().getPath(),
                    ex.getRequestHeaders().getFirst("Authorization"), cuerpo));
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        servidor.start();
    }

    @AfterEach
    void bajarServidor() {
        servidor.stop(0);
    }

    private String urlServidor() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private static Registro registro(String id) {
        return new Registro("esfuerzo", "rutineitor", id, "gimnasio", "2026-10-28", 60, "Sesión A", null, 3,
                null, null, "pc", 1_000L, false);
    }

    private void esperarPeticiones(int cantidad) throws InterruptedException {
        long limite = System.nanoTime() + 5_000_000_000L;
        while (recibidas.size() < cantidad && System.nanoTime() < limite) {
            Thread.sleep(20);
        }
    }

    @Test
    void inactivaNoCreaLaColaNiHaceRed() throws Exception {
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            assertFalse(c.activo());
            c.publicar(registro("s1"));
            c.vaciarAhora();
            assertEquals(PanelClient.Conexion.SIN_RESPUESTA, c.probar());
        }
        assertFalse(Files.exists(carpeta.resolve("panel-cola.jsonl")));
        assertTrue(recibidas.isEmpty());
    }

    @Test
    void activaEntregaLoPublicado() throws Exception {
        ConexionPanel.guardarConfig(carpeta, urlServidor(), "token-de-prueba-123", "pc", true);
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            assertTrue(c.activo());
            c.publicar(registro("s1"));
            c.vaciarAhora();
            esperarPeticiones(1);
        }
        assertEquals(1, recibidas.size());
        Peticion p = recibidas.get(0);
        assertEquals("POST", p.metodo());
        assertEquals("/api/registros", p.ruta());
        assertEquals("Bearer token-de-prueba-123", p.autorizacion());
        assertTrue(p.cuerpo().contains("\"idEnFuente\":\"s1\""), p.cuerpo());
    }

    @Test
    void guardarConfigYReabrirLaActiva() throws Exception {
        Path anidada = carpeta.resolve("datos/rutina");
        ConexionPanel.guardarConfig(anidada, "http://192.168.1.13:7070", "tok=en;raro #!ñ\\", "tablet", true);
        try (ConexionPanel c = ConexionPanel.abrir(anidada)) {
            assertTrue(c.activo());
            assertEquals("http://192.168.1.13:7070", c.url());
            assertEquals("tablet", c.dispositivo());
        }
        var cfg = panel.core.ConfigPanel.cargar(anidada.resolve("panel-cliente.properties"));
        assertEquals("tok=en;raro #!ñ\\", cfg.token());
    }

    @Test
    void guardarConfigConEnviarFalseQuedaInactiva() throws Exception {
        ConexionPanel.guardarConfig(carpeta, urlServidor(), "token-de-prueba-123", "pc", false);
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            assertFalse(c.activo());
            c.publicar(registro("s1"));
        }
        assertFalse(Files.exists(carpeta.resolve("panel-cola.jsonl")));
        assertTrue(recibidas.isEmpty());
    }

    @Test
    void guardarConfigDejaPermisos600() throws Exception {
        Assumptions.assumeTrue(carpeta.getFileSystem().supportedFileAttributeViews().contains("posix"));
        ConexionPanel.guardarConfig(carpeta, urlServidor(), "token-de-prueba-123", "pc", true);
        Path archivo = carpeta.resolve("panel-cliente.properties");
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(archivo)));
        // reescribir tampoco los abre
        ConexionPanel.guardarConfig(carpeta, urlServidor(), "otro-token-123456", "pc", true);
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(archivo)));
    }

    @Test
    void elTokenNoApareceEnToString() throws Exception {
        ConexionPanel.guardarConfig(carpeta, urlServidor(), "token-secreto-xyz", "pc", true);
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            assertFalse(c.toString().contains("token-secreto-xyz"));
        }
    }
}
