package com.rutina.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;
import com.rutina.registro.EjercicioPlan;
import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.FilaPlan;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.SerieReal;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SesionAPanelTest {

    private static final SesionAPanel.Datos DATOS = new SesionAPanel.Datos(7, 12, 2026, 2, "celu");
    private static final long AHORA = 1_700_000_000_000L;
    private static final String DIA = "Macro 2026 · Meso 2 · Micro 3 · Día 1";

    private static List<SerieReal> n(int cuantas, int reps, double kg) {
        return java.util.Collections.nCopies(cuantas, new SerieReal(reps, kg));
    }

    private static EjercicioPlan ejercicio(String bloque, String nombre, FilaPlan... filas) {
        return new EjercicioPlan(bloque, nombre, Tipo.values()[0], Aproximacion.values()[0], 100.0, List.of(filas));
    }

    private static PlanCelular plan(EjercicioPlan... ejercicios) {
        return new PlanCelular(PlanCelular.FORMATO, PlanCelular.VERSION, "yo", 12, "2026-03-01", 4, 3,
                List.of(new SesionPlan(3, 1, List.of(ejercicios))));
    }

    private static PlanCelular planBasico() {
        return plan(
                ejercicio("1-1", "Press banca", new FilaPlan("80-85", 80, 85, n(3, 5, 85))),
                ejercicio("1-2", "Sentadilla", new FilaPlan("70", 70, 70, n(3, 8, 100))));
    }

    private static SesionRegistro registro(Integer duracion, Integer fin, EjercicioRegistro... ejs) {
        return new SesionRegistro(3, 1, "2026-05-04", 3, fin, List.of(ejs), duracion);
    }

    private static EjercicioRegistro anotado(String bloque, Integer esfuerzo, String etiqueta, String texto) {
        return new EjercicioRegistro(bloque, esfuerzo, Map.of(etiqueta, texto), null);
    }

    private static JsonObject ejercicioJson(SesionAPanel.Resultado r, int i) {
        assertNull(r.omitida());
        assertTrue(r.registro().errores().isEmpty());
        JsonObject o = JsonParser.parseString(r.registro().detalleJson()).getAsJsonObject();
        return o.getAsJsonArray("ejercicios").get(i).getAsJsonObject();
    }

    private static SesionAPanel.Resultado convertir(PlanCelular plan, SesionRegistro s) {
        return SesionAPanel.convertir(DATOS, plan, s, AHORA);
    }

    @Test
    void convierteUnaSesionCompleta() {
        var r = convertir(planBasico(), registro(65, 4));
        assertNull(r.omitida());
        var reg = r.registro();
        assertTrue(reg.errores().isEmpty());
        assertEquals("esfuerzo", reg.tipo());
        assertEquals("rutineitor", reg.fuente());
        assertEquals("ses:7:12:3:1", reg.idEnFuente());
        assertEquals("gimnasio", reg.area());
        assertEquals("2026-05-04", reg.fecha());
        assertEquals(65, reg.duracionMin());
        assertEquals(4, reg.esfuerzo());
        assertEquals(DIA, reg.resumen());
        assertNull(reg.metrica());
        assertNull(reg.valor());
        assertEquals("celu", reg.dispositivo());
        assertEquals(AHORA, reg.actualizadoEn());
        assertFalse(reg.borrado());
        JsonObject d = JsonParser.parseString(reg.detalleJson()).getAsJsonObject();
        assertEquals(DIA, d.get("dia").getAsString());
        JsonArray ejs = d.getAsJsonArray("ejercicios");
        assertEquals(2, ejs.size());
        assertEquals("Press banca", ejs.get(0).getAsJsonObject().get("nombre").getAsString());
        assertEquals("Sentadilla", ejs.get(1).getAsJsonObject().get("nombre").getAsString());
    }

    @Test
    void planificadoYHechoSalenDelPlanYDeLaCasilla() {
        var r = convertir(planBasico(), registro(60, 3, anotado("1-1", null, "80-85", "5.5.3-90")));
        JsonObject e = ejercicioJson(r, 0);
        assertEquals("3 × 5 @ 85 kg", e.get("planificado").getAsString());
        assertEquals("2 × 5 @ 85 kg · 1 × 3 @ 90 kg", e.get("hecho").getAsString());
    }

    @Test
    void casillaVaciaSignificaComoPlanificado() {
        var r = convertir(planBasico(), registro(60, 3, anotado("1-1", null, "80-85", "  ")));
        JsonObject e = ejercicioJson(r, 0);
        assertEquals(e.get("planificado").getAsString(), e.get("hecho").getAsString());
        assertFalse(e.has("aviso"));
    }

    @Test
    void avisoBajoSeries() {
        var r = convertir(planBasico(), registro(60, 3, anotado("1-1", null, "80-85", "5.5")));
        assertEquals("bajó series", ejercicioJson(r, 0).get("aviso").getAsString());
    }

    @Test
    void avisoBajoReps() {
        var r = convertir(planBasico(), registro(60, 3, anotado("1-1", null, "80-85", "5.5.3")));
        JsonObject e = ejercicioJson(r, 0);
        assertEquals("2 × 5 @ 85 kg · 1 × 3 @ 85 kg", e.get("hecho").getAsString());
        assertEquals("bajó reps", e.get("aviso").getAsString());
    }

    @Test
    void avisoBajoPeso() {
        var r = convertir(planBasico(), registro(60, 3, anotado("1-1", null, "80-85", "5-80.5-80.5-80")));
        JsonObject e = ejercicioJson(r, 0);
        assertEquals("3 × 5 @ 80 kg", e.get("hecho").getAsString());
        assertEquals("bajó peso", e.get("aviso").getAsString());
    }

    @Test
    void sinAvisoSiHizoLoPlanificado() {
        var r = convertir(planBasico(), registro(60, 3, anotado("1-1", null, "80-85", "5.5.5")));
        assertFalse(ejercicioJson(r, 0).has("aviso"));
    }

    @Test
    void sinAvisoDePesoSiElPesoPlanificadoEsCero() {
        var plan = plan(ejercicio("1-1", "Dominadas", new FilaPlan("4x12", null, null, n(2, 12, 0))));
        var r = convertir(plan, registro(60, 3, anotado("1-1", null, "4x12", "12-5.12-5")));
        assertFalse(ejercicioJson(r, 0).has("aviso"));
    }

    @Test
    void casillaIlegibleNoLanzaYAvisa() {
        var r = convertir(planBasico(), registro(60, 3, anotado("1-1", null, "80-85", "  me dolió el codo ")));
        JsonObject e = ejercicioJson(r, 0);
        assertEquals("me dolió el codo", e.get("hecho").getAsString());
        assertEquals("casilla ilegible", e.get("aviso").getAsString());
        JsonObject otro = ejercicioJson(r, 1);
        assertEquals(otro.get("planificado").getAsString(), otro.get("hecho").getAsString());
        assertFalse(otro.has("aviso"));
    }

    @Test
    void esfuerzoTextoViajaConElTextoDeLaEscala() {
        var r = convertir(planBasico(), registro(60, 3, new EjercicioRegistro("1-1", 4, Map.of(), null)));
        JsonObject e = ejercicioJson(r, 0);
        assertEquals(4, e.get("esfuerzo").getAsInt());
        assertEquals("Al límite", e.get("esfuerzoTexto").getAsString());
        assertFalse(ejercicioJson(r, 1).has("esfuerzo"));
    }

    @Test
    void esfuerzoInvalidoDeUnEjercicioSeOmite() {
        var r = convertir(planBasico(), registro(60, 3, new EjercicioRegistro("1-1", 9, Map.of(), null)));
        JsonObject e = ejercicioJson(r, 0);
        assertFalse(e.has("esfuerzo"));
        assertFalse(e.has("esfuerzoTexto"));
    }

    @Test
    void sinDuracionSeOmite() {
        var r = convertir(planBasico(), registro(null, 3));
        assertNull(r.registro());
        assertEquals("sin duración", r.omitida());
        assertEquals("sin duración", convertir(planBasico(), registro(0, 3)).omitida());
    }

    @Test
    void duracionFueraDeRangoSeOmite() {
        assertEquals("duración fuera de rango", convertir(planBasico(), registro(601, 3)).omitida());
        assertEquals("sin duración", convertir(planBasico(), registro(0, 3)).omitida());
        assertNull(convertir(planBasico(), registro(600, 3)).omitida());
    }

    @Test
    void sesionQueNoEstaEnElPlanSeOmite() {
        var s = new SesionRegistro(4, 1, "2026-05-04", null, 3, List.of(), 60);
        var r = convertir(planBasico(), s);
        assertNull(r.registro());
        assertEquals("fuera del plan", r.omitida());
    }

    @Test
    void fechaInvalidaSeOmite() {
        var s = new SesionRegistro(3, 1, "04/05/2026", null, 3, List.of(), 60);
        assertEquals("fecha inválida", convertir(planBasico(), s).omitida());
        var s2 = new SesionRegistro(3, 1, null, null, 3, List.of(), 60);
        assertEquals("fecha inválida", convertir(planBasico(), s2).omitida());
    }

    @Test
    void sinSensacionFinalDejaEsfuerzoNulo() {
        var r = convertir(planBasico(), registro(60, null));
        assertNull(r.omitida());
        assertTrue(r.registro().errores().isEmpty());
        assertNull(r.registro().esfuerzo());
        var fuera = convertir(planBasico(), registro(60, 7));
        assertTrue(fuera.registro().errores().isEmpty());
        assertNull(fuera.registro().esfuerzo());
    }

    @Test
    void sesionSinEjerciciosAnotadosUsaLoPlanificado() {
        var conNull = new SesionRegistro(3, 1, "2026-05-04", null, 3, null, 60);
        var r = convertir(planBasico(), conNull);
        assertEquals("3 × 5 @ 85 kg", ejercicioJson(r, 0).get("hecho").getAsString());
        var vacia = convertir(planBasico(), registro(60, 3));
        assertEquals("3 × 8 @ 100 kg", ejercicioJson(vacia, 1).get("hecho").getAsString());
        assertFalse(ejercicioJson(vacia, 1).has("aviso"));
    }

    @Test
    void ejerciciosSoloDelRegistroSeIgnoran() {
        var r = convertir(planBasico(), registro(60, 3, anotado("9-9", 3, "x", "5")));
        assertEquals(2, JsonParser.parseString(r.registro().detalleJson()).getAsJsonObject()
                .getAsJsonArray("ejercicios").size());
    }
}
