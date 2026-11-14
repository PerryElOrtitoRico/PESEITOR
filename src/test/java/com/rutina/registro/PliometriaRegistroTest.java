package com.rutina.registro;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PliometriaRegistroTest {

    private static SesionPlioRegistro plio(String inicio, int semana, int dia, String nota) {
        return new SesionPlioRegistro(inicio, semana, dia, "2026-11-12", 25,
                List.of(new EjercicioPlioRegistro(1, nota)));
    }

    private static RegistroCelular reg(SesionPlioRegistro... s) {
        return new RegistroCelular(RegistroCelular.FORMATO, 1, "F", 3, List.of(), List.of(s));
    }

    @Test
    void elRegistroConPliometriaVaYVuelveIgual() {
        RegistroCelular r = reg(plio("2026-11-02", 1, 2, "Aterricé blando"));
        assertEquals(r, JsonRegistro.registroDeJson(JsonRegistro.registroAJson(r)));
    }

    @Test
    void unRegistroViejoSinPliometriaQuedaConListaVacia() {
        String viejo = "{\"formato\":\"rutineitor-registro\",\"version\":1,\"perfil\":\"F\",\"mesocicloId\":3,\"sesiones\":[]}";
        assertEquals(List.of(), JsonRegistro.registroDeJson(viejo).pliometria());
    }

    @Test
    void unPlanViejoSinPliometriaQuedaConListaVacia() {
        String viejo = "{\"formato\":\"rutineitor-plan\",\"version\":1,\"perfil\":\"F\",\"mesocicloId\":3,\"inicio\":\"2026-10-26\","
                + "\"microciclos\":1,\"dias\":1,\"sesiones\":[]}";
        assertEquals(List.of(), JsonRegistro.planDeJson(viejo).pliometria());
    }

    @Test
    void elPlanConPliometriaVaYVuelveIgual() {
        var dia = new DiaPlioCelular(1, 120, 2, 10, List.of("Salto al cajón", "Skipping"));
        PlanCelular p = new PlanCelular(PlanCelular.FORMATO, 1, "F", 3, "2026-10-26", 1, 1, List.of(),
                List.of(new PlanPlio(1, "2026-11-02", List.of(List.of(dia)))));
        assertEquals(p, JsonRegistro.planDeJson(JsonRegistro.planAJson(p)));
    }

    @Test
    void laNotaSeRecortaYLaVaciaQuedaNull() {
        var r = JsonRegistro.registroDeJson(JsonRegistro.registroAJson(reg(plio("2026-11-02", 1, 1, "  hola  "))));
        assertEquals("hola", r.pliometria().get(0).ejercicios().get(0).nota());
        var en_blanco = JsonRegistro.registroDeJson(JsonRegistro.registroAJson(reg(plio("2026-11-02", 1, 1, "  "))));
        assertNull(en_blanco.pliometria().get(0).ejercicios().get(0).nota());
    }

    @Test
    void rechazaNotaLargaRepetidaYFechaMala() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonRegistro.registroDeJson(JsonRegistro.registroAJson(reg(plio("2026-11-02", 1, 1, "x".repeat(501))))));
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.registroDeJson(JsonRegistro.registroAJson(
                reg(plio("2026-11-02", 1, 1, "a"), plio("2026-11-02", 1, 1, "b")))));
        var mala = new SesionPlioRegistro("2026-11-02", 1, 1, "12/11", null, List.of());
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.registroDeJson(JsonRegistro.registroAJson(reg(mala))));
    }

    @Test
    void lasSesionesDePliometriaCuentanComoQueSePierden() {
        RegistroCelular guardado = reg(plio("2026-11-02", 1, 1, "a"), plio("2026-11-02", 1, 2, "b"));
        assertEquals(1, guardado.sesionesQueFaltanEn(reg(plio("2026-11-02", 1, 1, "a"))));
        assertEquals(0, guardado.sesionesQueFaltanEn(guardado));
        assertTrue(guardado.conSesiones(List.of()).pliometria().size() == 2);
    }
}
