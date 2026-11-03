package com.rutina.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rutina.registro.JsonRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionRegistro;
import com.rutina.service.datos.EjercicioRepo.RmRegistrado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import panel.core.ColaLocal;
import panel.cliente.ConexionPanel;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de extremo a extremo contra un panel de verdad (por ejemplo el de {@code scripts/demo.sh} del repo Panel).
 * Solo corre si están definidas PANEL_E2E_URL y PANEL_E2E_TOKEN; con {@code mvn test} común se omite en silencio.
 * Usa los archivos de docs/ejemplos, nunca datos reales.
 */
@EnabledIfEnvironmentVariable(named = "PANEL_E2E_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PANEL_E2E_TOKEN", matches = ".+")
class PanelExtremoAExtremoManualTest {

    private static final long PERFIL = 1;
    private static final int MACRO = 2026;
    private static final int MESO = 1;

    @TempDir
    Path carpeta;

    private final HttpClient http = HttpClient.newHttpClient();
    private final String url = quitarBarra(System.getenv("PANEL_E2E_URL"));
    private final String token = System.getenv("PANEL_E2E_TOKEN");

    private static String quitarBarra(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static void esperar(BooleanSupplier cond) throws InterruptedException {
        long limite = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < limite) {
            if (cond.getAsBoolean()) return;
            Thread.sleep(20);
        }
    }

    private JsonElement get(String ruta) {
        try {
            var req = HttpRequest.newBuilder(URI.create(url + ruta)).header("Authorization", "Bearer " + token).GET().build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resp.statusCode(), ruta + " -> " + resp.body());
            return JsonParser.parseString(resp.body());
        } catch (java.io.IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Registros de PESEITOR de un día. */
    private List<JsonObject> delDia(LocalDate fecha) {
        List<JsonObject> res = new ArrayList<>();
        for (JsonElement e : get("/api/dia/" + fecha).getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            if ("rutineitor".equals(o.get("fuente").getAsString())) res.add(o);
        }
        return res;
    }

    private JsonObject sesionDe(LocalDate fecha, String idEnFuente) {
        return delDia(fecha).stream().filter(o -> idEnFuente.equals(o.get("idEnFuente").getAsString()))
                .findFirst().orElse(null);
    }

    private static List<String> idsDeSesiones(List<JsonObject> registros) {
        return registros.stream().map(o -> o.get("idEnFuente").getAsString()).filter(i -> i.startsWith("ses:"))
                .sorted().toList();
    }

    /** Reescribe cada fecha: la sesión i del archivo cae en hoy - (n-1-i) días, o sea dentro de los últimos 5 días. */
    private static RegistroCelular conFechasRecientes(RegistroCelular r, LocalDate hoy) {
        List<SesionRegistro> nuevas = new ArrayList<>();
        int n = r.sesiones().size();
        assertTrue(n <= 5, "el ejemplo tiene que caber en 5 días");
        for (int i = 0; i < n; i++) {
            SesionRegistro s = r.sesiones().get(i);
            nuevas.add(new SesionRegistro(s.microciclo(), s.dia(), hoy.minusDays(n - 1 - i).toString(), s.descanso(),
                    s.sensacionFinal(), s.ejercicios(), s.duracionMin()));
        }
        return new RegistroCelular(r.formato(), r.version(), r.perfil(), r.mesocicloId(), nuevas);
    }

    private static JsonObject ejercicio(JsonObject sesion, int i) {
        JsonObject detalle = JsonParser.parseString(sesion.get("detalleJson").getAsString()).getAsJsonObject();
        JsonArray lista = detalle.getAsJsonArray("ejercicios");
        return lista.get(i).getAsJsonObject();
    }

    @Test
    void importarEntregaYReimportarNoDuplica() throws Exception {
        LocalDate hoy = LocalDate.now();
        PlanCelular plan = JsonRegistro.planDeJson(Files.readString(EjemplosTest.PLAN));
        RegistroCelular registro = conFechasRecientes(JsonRegistro.leerRegistro(EjemplosTest.REGISTRO), hoy);
        // sesiones del ejemplo, en orden: (1,1) (1,2) (2,1 sin duración) (2,2) (3,1 fuera del plan)
        LocalDate f11 = hoy.minusDays(4), f12 = hoy.minusDays(3), f21 = hoy.minusDays(2), f22 = hoy.minusDays(1),
                f31 = hoy;
        String id11 = "ses:" + PERFIL + ":3:1:1", id12 = "ses:" + PERFIL + ":3:1:2", id22 = "ses:" + PERFIL + ":3:2:2";

        ConexionPanel.guardarConfig(carpeta, url, token, "pc-prueba", true);
        long ahora = System.currentTimeMillis();
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            assertTrue(c.activo());
            var resumen = EnvioRegistro.enviar(c, PERFIL, true, plan, registro, MACRO, MESO, ahora - 10_000);
            assertEquals(new EnvioRegistro.Resumen(true, 3, 1, 1, 0, 0), resumen);
            c.vaciarAhora();

            // el rm de un ejercicio (hoy) viaja por el mismo camino
            var rm = new RmRegistrado(9, PERFIL, "Press banca", hoy, 102.5);
            c.publicar(RmAPanel.aRegistro(rm, "pc-prueba", ahora - 10_000).orElseThrow());
            c.vaciarAhora();

            esperar(() -> sesionDe(f11, id11) != null && sesionDe(f12, id12) != null && sesionDe(f22, id22) != null
                    && delDia(hoy).stream().anyMatch(o -> o.get("idEnFuente").getAsString().equals("rm:9:" + hoy)));
            assertEquals(0, c.pendientes(), "no tiene que quedar nada en la cola");

            // sesión (1,1): bajó series
            JsonObject s11 = sesionDe(f11, id11);
            assertNotNull(s11);
            assertEquals("Macro 2026 · Meso 1 · Micro 1 · Día 1", s11.get("resumen").getAsString());
            assertEquals("gimnasio", s11.get("area").getAsString());
            assertEquals("esfuerzo", s11.get("tipo").getAsString());
            assertEquals(62, s11.get("duracionMin").getAsInt());
            assertEquals(4, s11.get("esfuerzo").getAsInt());
            String det11 = s11.get("detalleJson").getAsString();
            assertTrue(det11.contains("\"planificado\"") && det11.contains("\"hecho\"")
                    && det11.contains("\"esfuerzoTexto\"") && det11.contains("\"aviso\""), det11);
            JsonObject sent = ejercicio(s11, 0);
            assertEquals("Sentadilla", sent.get("nombre").getAsString());
            assertEquals("2 × 4 @ 105 kg · 2 × 3 @ 120 kg · 1 × 3 @ 127,5 kg", sent.get("planificado").getAsString());
            assertEquals("2 × 4 @ 105 kg · 1 × 3 @ 120 kg · 1 × 3 @ 115 kg", sent.get("hecho").getAsString());
            assertEquals("bajó series", sent.get("aviso").getAsString());
            assertEquals(4, sent.get("esfuerzo").getAsInt());
            assertEquals("Al límite", sent.get("esfuerzoTexto").getAsString());
            JsonObject biceps = ejercicio(s11, 1);
            assertEquals(biceps.get("planificado").getAsString(), biceps.get("hecho").getAsString());
            assertFalse(biceps.has("aviso") && !biceps.get("aviso").isJsonNull());

            // sesión (1,2): casilla ilegible, sin "cómo terminé"
            JsonObject s12 = sesionDe(f12, id12);
            assertNotNull(s12);
            assertEquals("Macro 2026 · Meso 1 · Micro 1 · Día 2", s12.get("resumen").getAsString());
            assertEquals(48, s12.get("duracionMin").getAsInt());
            assertTrue(s12.get("esfuerzo") == null || s12.get("esfuerzo").isJsonNull());
            JsonObject frontal = ejercicio(s12, 0);
            assertEquals("casilla ilegible", frontal.get("aviso").getAsString());
            assertEquals("seis, me dolio la rodilla", frontal.get("hecho").getAsString());
            assertEquals("Pesado", frontal.get("esfuerzoTexto").getAsString());
            JsonObject press = ejercicio(s12, 1);
            assertEquals("Justo", press.get("esfuerzoTexto").getAsString());

            // sesión (2,2): casillas en blanco = como lo planificado
            JsonObject s22 = sesionDe(f22, id22);
            assertNotNull(s22);
            assertEquals("Macro 2026 · Meso 1 · Micro 2 · Día 2", s22.get("resumen").getAsString());
            assertEquals(55, s22.get("duracionMin").getAsInt());
            assertEquals(5, s22.get("esfuerzo").getAsInt());
            JsonObject comoPlan = ejercicio(s22, 0);
            assertEquals(comoPlan.get("planificado").getAsString(), comoPlan.get("hecho").getAsString());
            assertFalse(comoPlan.has("aviso") && !comoPlan.get("aviso").isJsonNull());
            assertEquals("Liviano", comoPlan.get("esfuerzoTexto").getAsString());

            // lo que no se manda: sin duración (f21) y fuera del plan (f31)
            assertEquals(List.of(), idsDeSesiones(delDia(f21)));
            assertEquals(List.of(), idsDeSesiones(delDia(f31)));

            // 1RM como resultado
            JsonObject resultado = delDia(hoy).stream().filter(o -> o.get("idEnFuente").getAsString().equals("rm:9:" + hoy))
                    .findFirst().orElseThrow();
            assertEquals("resultado", resultado.get("tipo").getAsString());
            assertEquals("1rm:press-banca", resultado.get("metrica").getAsString());
            assertEquals(102.5, resultado.get("valor").getAsDouble(), 1e-9);

            // el calendario del mes muestra los días con sesiones (no hace falta ninguna meta)
            for (LocalDate f : List.of(f11, f12, f22)) {
                JsonArray celdas = get("/api/mes/" + f.toString().substring(0, 7)).getAsJsonObject().getAsJsonArray("celdas");
                boolean hay = false;
                for (JsonElement ce : celdas) hay |= f.toString().equals(ce.getAsJsonObject().get("fecha").getAsString());
                assertTrue(hay, "falta la celda de " + f + " en " + celdas);
            }

            // segunda importación, más tarde: mismos ids, sin duplicados, gana lo más nuevo
            List<String> antes = todasLasSesiones(List.of(f11, f12, f21, f22, f31));
            assertEquals(List.of(id11, id12, id22), antes);
            var resumen2 = EnvioRegistro.enviar(c, PERFIL, true, plan, registro, MACRO, MESO, ahora);
            assertEquals(resumen, resumen2);
            c.vaciarAhora();
            esperar(() -> ahora == actualizadoEn(f11, id11) && ahora == actualizadoEn(f12, id12)
                    && ahora == actualizadoEn(f22, id22));
            assertEquals(ahora, actualizadoEn(f11, id11));
            assertEquals(ahora, actualizadoEn(f12, id12));
            assertEquals(ahora, actualizadoEn(f22, id22));
            assertEquals(antes, todasLasSesiones(List.of(f11, f12, f21, f22, f31)));
            assertEquals(0, c.pendientes());
        }
    }

    private long actualizadoEn(LocalDate fecha, String id) {
        JsonObject o = sesionDe(fecha, id);
        return o == null ? -1 : o.get("actualizadoEn").getAsLong();
    }

    /** Ids de sesión de PESEITOR en esos días, tal cual vienen (con repetidos, si los hubiera). */
    private List<String> todasLasSesiones(List<LocalDate> fechas) {
        List<String> ids = new ArrayList<>();
        for (LocalDate f : fechas) ids.addAll(idsDeSesiones(delDia(f)));
        return ids.stream().sorted().toList();
    }

    @Test
    void conElPanelCaidoTodoQuedaEnLaCola() throws Exception {
        PlanCelular plan = JsonRegistro.planDeJson(Files.readString(EjemplosTest.PLAN));
        RegistroCelular registro = conFechasRecientes(JsonRegistro.leerRegistro(EjemplosTest.REGISTRO), LocalDate.now());
        int puertoCerrado;
        try (ServerSocket s = new ServerSocket(0)) {
            puertoCerrado = s.getLocalPort();
        }
        ConexionPanel.guardarConfig(carpeta, "http://127.0.0.1:" + puertoCerrado, token, "pc-prueba", true);
        try (ConexionPanel c = ConexionPanel.abrir(carpeta)) {
            var resumen = assertDoesNotThrow(() ->
                    EnvioRegistro.enviar(c, PERFIL, true, plan, registro, MACRO, MESO, System.currentTimeMillis()));
            assertEquals(new EnvioRegistro.Resumen(true, 3, 1, 1, 0, 0), resumen);
            assertDoesNotThrow(c::vaciarAhora);
            assertEquals(3, c.pendientes());
        }
        var ids = new ColaLocal(carpeta.resolve("panel-cola.jsonl")).pendientes().stream()
                .map(r -> r.idEnFuente()).sorted().toList();
        assertEquals(List.of("ses:1:3:1:1", "ses:1:3:1:2", "ses:1:3:2:2"), ids);
        assertEquals(3, Files.readAllLines(carpeta.resolve("panel-cola.jsonl")).stream().filter(l -> !l.isBlank()).count());
    }
}
