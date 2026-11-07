package com.rutina.panel;

import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;
import com.rutina.registro.EjercicioPlan;
import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.FilaPlan;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SerieReal;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import panel.core.ColaLocal;
import panel.cliente.ConexionPanel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class EnvioRegistroTest {

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

    private static PlanCelular plan(int... dias) {
        var filas = List.of(new FilaPlan("80", 80, 80, Collections.nCopies(3, new SerieReal(5, 80))));
        var ej = List.of(new EjercicioPlan("1-1", "Press banca", Tipo.values()[0], Aproximacion.values()[0], 100.0, filas));
        var sesiones = new java.util.ArrayList<SesionPlan>();
        for (int d : dias) sesiones.add(new SesionPlan(1, d, ej));
        return new PlanCelular(PlanCelular.FORMATO, PlanCelular.VERSION, "yo", 12, "2026-03-01", 4, 3, sesiones);
    }

    private static SesionRegistro ses(int dia, Integer duracion) {
        return new SesionRegistro(1, dia, "2026-05-0" + dia, 3, 3, List.of(), duracion);
    }

    private static RegistroCelular reg(SesionRegistro... s) {
        return new RegistroCelular(RegistroCelular.FORMATO, RegistroCelular.VERSION, "yo", 12, List.of(s));
    }

    private static List<String> ids(ColaLocal cola) {
        return cola.pendientes().stream().map(r -> r.idEnFuente()).sorted().toList();
    }

    @Test
    void enviaLasSesionesConvertibles() throws Exception {
        try (ConexionPanel c = conexionActiva()) {
            var r = EnvioRegistro.enviar(c, 7, plan(1, 2), reg(ses(1, 60), ses(2, 45)), 2026, 2, AHORA);
            assertEquals(new EnvioRegistro.Resumen(true, 2, 0, 0, 0, 0), r);
            esperar(() -> cuerpos.size() >= 2);
        }
        assertEquals(2, cuerpos.size());
        assertTrue(cuerpos.stream().anyMatch(b -> b.contains("\"idEnFuente\":\"ses:7:12:1:1\"")), cuerpos.toString());
        assertTrue(cuerpos.stream().anyMatch(b -> b.contains("\"idEnFuente\":\"ses:7:12:1:2\"")), cuerpos.toString());
    }

    @Test
    void cuentaSinDuracionYFueraDelPlan() throws Exception {
        try (ConexionPanel c = conexionActiva()) {
            var r = EnvioRegistro.enviar(c, 7, plan(1, 2, 3),
                    reg(ses(1, 60), ses(2, null), ses(3, 700), ses(4, 30)), 2026, 2, AHORA);
            assertEquals(new EnvioRegistro.Resumen(true, 1, 2, 1, 0, 0), r);
        }
    }

    @Test
    void fechaInvalidaCuentaEnOtras() throws Exception {
        try (ConexionPanel c = conexionActiva()) {
            var mala = new SesionRegistro(1, 1, "no-es-fecha", 3, 3, List.of(), 60);
            var r = EnvioRegistro.enviar(c, 7, plan(1), reg(mala), 2026, 2, AHORA);
            assertEquals(new EnvioRegistro.Resumen(true, 0, 0, 0, 1, 0), r);
        }
    }

    @Test
    void reimportarDaLosMismosIdYPisaConLoMasNuevo() throws Exception {
        ConexionPanel.guardarConfig(carpeta, "http://127.0.0.1:" + servidor.getAddress().getPort(),
                "token-de-prueba-123", "pc", true);
        servidor.stop(0); // sin red: se ve lo que queda en cola
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            var registro = reg(ses(1, 60), ses(2, 45));
            EnvioRegistro.enviar(c, 7, plan(1, 2), registro, 2026, 2, AHORA);
            var primera = new ColaLocal(carpeta.resolve("panel-cola.jsonl")).pendientes();
            EnvioRegistro.enviar(c, 7, plan(1, 2), registro, 2026, 2, AHORA + 5_000);
            var cola = new ColaLocal(carpeta.resolve("panel-cola.jsonl"));
            assertEquals(List.of("ses:7:12:1:1", "ses:7:12:1:2"), ids(cola));
            assertEquals(2, primera.size());
            for (var p : cola.pendientes()) assertEquals(AHORA + 5_000, p.actualizadoEn());
        }
    }

    @Test
    void reimportarNoDuplicaLoRecibidoPorElServidor() throws Exception {
        try (ConexionPanel c = conexionActiva()) {
            EnvioRegistro.enviar(c, 7, plan(1), reg(ses(1, 60)), 2026, 2, AHORA);
            esperar(() -> cuerpos.size() >= 1);
            EnvioRegistro.enviar(c, 7, plan(1), reg(ses(1, 60)), 2026, 2, AHORA + 5_000);
            esperar(() -> cuerpos.size() >= 2);
        }
        assertEquals(2, cuerpos.size());
        assertTrue(cuerpos.get(0).contains("\"actualizadoEn\":" + AHORA), cuerpos.get(0));
        assertTrue(cuerpos.get(1).contains("\"actualizadoEn\":" + (AHORA + 5_000)), cuerpos.get(1));
        for (String b : cuerpos) assertTrue(b.contains("\"idEnFuente\":\"ses:7:12:1:1\""), b);
    }

    @Test
    void panelInactivoNoPublicaNada() throws Exception {
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            var r = EnvioRegistro.enviar(c, 7, plan(1), reg(ses(1, 60)), 2026, 2, AHORA);
            assertEquals(new EnvioRegistro.Resumen(false, 0, 0, 0, 0, 0), r);
        }
        assertFalse(Files.exists(carpeta.resolve("panel-cola.jsonl")));
        assertTrue(cuerpos.isEmpty());
    }

    @Test
    void servidorCaidoDejaLasSesionesEnLaCola() throws Exception {
        ConexionPanel.guardarConfig(carpeta, "http://127.0.0.1:" + servidor.getAddress().getPort(),
                "token-de-prueba-123", "pc", true);
        servidor.stop(0);
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            var r = EnvioRegistro.enviar(c, 7, plan(1, 2), reg(ses(1, 60), ses(2, 45)), 2026, 2, AHORA);
            assertEquals(new EnvioRegistro.Resumen(true, 2, 0, 0, 0, 0), r);
            c.vaciarAhora();
        }
        assertEquals(List.of("ses:7:12:1:1", "ses:7:12:1:2"),
                ids(new ColaLocal(carpeta.resolve("panel-cola.jsonl"))));
    }

    @Test
    void unaCasillaIlegibleNoImpideLasDemas() throws Exception {
        try (ConexionPanel c = conexionActiva()) {
            var rara = new SesionRegistro(1, 1, "2026-05-01", 3, 3, List.of(
                    new EjercicioRegistro("1-1", null, Map.of("80", "  me dolió el codo "), null)), 60);
            var r = EnvioRegistro.enviar(c, 7, plan(1, 2), reg(rara, ses(2, 45)), 2026, 2, AHORA);
            assertEquals(new EnvioRegistro.Resumen(true, 2, 0, 0, 0, 0), r);
        }
    }

    @Test
    void registroSinSesionesNoEnviaNada() throws Exception {
        try (ConexionPanel c = conexionActiva()) {
            var r = EnvioRegistro.enviar(c, 7, plan(1),
                    new RegistroCelular(RegistroCelular.FORMATO, RegistroCelular.VERSION, "yo", 12, null),
                    2026, 2, AHORA);
            assertEquals(new EnvioRegistro.Resumen(true, 0, 0, 0, 0, 0), r);
        }
    }

    @Test
    void mensajeDelResumen() {
        String cola = " Si el panel no responde, quedan en cola y se mandan cuando vuelva la conexión.";
        assertNull(EnvioRegistro.mensaje(new EnvioRegistro.Resumen(false, 3, 0, 0, 0, 0)));
        assertEquals("Se mandaron 1 sesión al panel." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 1, 0, 0, 0, 0)));
        assertEquals("Se mandaron 3 sesiones al panel." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 3, 0, 0, 0, 0)));
        assertEquals("Se mandaron 0 sesiones al panel." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 0, 0, 0, 0, 0)));
        assertEquals("Se mandaron 2 sesiones al panel. 1 quedó sin mandar por no tener duración." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 2, 1, 0, 0, 0)));
        assertEquals("Se mandaron 2 sesiones al panel. 4 quedaron sin mandar por no tener duración."
                        + " 1 no está en el plan. 3 no se pudieron convertir." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 2, 4, 1, 3, 0)));
        assertEquals("Se mandaron 1 sesión al panel. 2 no están en el plan." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 1, 0, 2, 0, 0)));
        assertEquals("Se mandaron 1 sesión al panel. 1 no se pudo convertir." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 1, 0, 0, 1, 0)));
    }

    @Test
    void laSesionConvertidaQueNoSePudoEncolarCuentaAparte() {
        java.util.List<String> publicadas = new java.util.ArrayList<>();
        java.util.function.Consumer<panel.core.Registro> publicador = r -> {
            if (r.idEnFuente().endsWith(":1")) throw new IllegalStateException("cola rota");
            publicadas.add(r.idEnFuente());
        };
        var r = EnvioRegistro.enviar(publicador, "pc", 7, plan(1, 2), reg(ses(1, 60), ses(2, 45)), 2026, 2, AHORA);
        assertEquals(new EnvioRegistro.Resumen(true, 1, 0, 0, 0, 1), r);
        assertEquals(1, publicadas.size());
    }

    @Test
    void mensajeConSesionesNoEncoladas() {
        String cola = " Si el panel no responde, quedan en cola y se mandan cuando vuelva la conexión.";
        assertEquals("Se mandaron 1 sesión al panel. 1 no se pudo encolar." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 1, 0, 0, 0, 1)));
        assertEquals("Se mandaron 2 sesiones al panel. 1 no se pudo convertir. 3 no se pudieron encolar." + cola,
                EnvioRegistro.mensaje(new EnvioRegistro.Resumen(true, 2, 0, 0, 1, 3)));
    }
}
