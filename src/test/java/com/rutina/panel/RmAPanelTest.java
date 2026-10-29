package com.rutina.panel;

import com.rutina.model.Ejercicio;
import com.rutina.model.Tipo;
import com.rutina.service.datos.EjercicioRepo.RmRegistrado;
import org.junit.jupiter.api.Test;
import panel.core.Registro;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RmAPanelTest {

    private static final long AHORA = 1_700_000_000_000L;

    private static Ejercicio ej(long id, String nombre) {
        return new Ejercicio(id, 7, nombre, Tipo.FUERZA, null, null, 100, Map.of(), null, 0, true);
    }

    @Test
    void rmARegistroTieneIdMetricaYValor() {
        var rm = new RmRegistrado(5, 7, "Press banca", LocalDate.of(2026, 10, 27), 102.5);
        Registro r = RmAPanel.aRegistro(rm, "pc", AHORA).orElseThrow();
        assertEquals(new Registro("resultado", "rutineitor", "rm:5:2026-10-27", "gimnasio", "2026-10-27", null,
                "1RM Press banca: 102,5 kg", null, null, "1rm:press-banca", 102.5, "pc", AHORA, false), r);
        assertEquals(List.of(), r.errores());
    }

    @Test
    void resumenSinDecimalesSiEsEntero() {
        var rm = new RmRegistrado(5, 7, "Dominadas", LocalDate.of(2026, 10, 27), 120.0);
        assertEquals("1RM Dominadas: 120 kg", RmAPanel.aRegistro(rm, "pc", AHORA).orElseThrow().resumen());
    }

    @Test
    void dosRmDelMismoDiaComparteId() {
        var a = RmAPanel.aRegistro(new RmRegistrado(5, 7, "Press banca", LocalDate.of(2026, 10, 27), 100), "pc", AHORA)
                .orElseThrow();
        var b = RmAPanel.aRegistro(new RmRegistrado(5, 7, "Press banca", LocalDate.of(2026, 10, 27), 105), "pc", AHORA + 9)
                .orElseThrow();
        assertEquals(a.idEnFuente(), b.idEnFuente());
        var otroDia = RmAPanel.aRegistro(new RmRegistrado(5, 7, "Press banca", LocalDate.of(2026, 10, 28), 105), "pc", AHORA)
                .orElseThrow();
        assertNotEquals(a.idEnFuente(), otroDia.idEnFuente());
    }

    @Test
    void claveVaciaNoDaRegistro() {
        var rm = new RmRegistrado(5, 7, "(***)", LocalDate.of(2026, 10, 27), 100);
        assertEquals(Optional.empty(), RmAPanel.aRegistro(rm, "pc", AHORA));
    }

    @Test
    void esRepresentanteSoloElDeMenorIdConLaMismaClave() {
        var lista = List.of(ej(1, "Press banca"), ej(2, "press  BANCA"), ej(3, "Dominadas"), ej(4, "(*)"), ej(5, "!!"));
        assertTrue(RmAPanel.esRepresentante(1, "Press banca", lista));
        assertFalse(RmAPanel.esRepresentante(2, "press  BANCA", lista));
        assertTrue(RmAPanel.esRepresentante(3, "Dominadas", lista));
        // claves vacías no se comparan entre sí
        assertTrue(RmAPanel.esRepresentante(5, "!!", lista));
    }
}
