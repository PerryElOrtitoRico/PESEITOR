package com.rutina.panel;

import com.rutina.model.Ejercicio;
import com.rutina.model.Tipo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClavesPanelTest {

    private static Ejercicio ej(long id, String nombre, Double rm) {
        return new Ejercicio(id, 7, nombre, Tipo.FUERZA, rm, null, 100, Map.of(), null, 0, true);
    }

    @Test
    void listaLasClavesDeLosEjerciciosConRm() {
        assertEquals(List.of("1rm:press-banca — Press banca", "1rm:sentadilla — Sentadilla"),
                ClavesPanel.lista(List.of(ej(1, "Press banca", 100.0), ej(2, "Sentadilla", 140.0))));
    }

    @Test
    void ignoraLosQueNoTienenRm() {
        assertEquals(List.of("1rm:sentadilla — Sentadilla"),
                ClavesPanel.lista(List.of(ej(1, "Press banca", null), ej(2, "Sentadilla", 140.0))));
    }

    @Test
    void sinClaveUtilizable() {
        assertEquals(List.of("(sin clave utilizable) — ¿¿??"), ClavesPanel.lista(List.of(ej(1, "¿¿??", 50.0))));
    }

    @Test
    void claveRepetidaSoloMandaLaDelMenorId() {
        assertEquals(List.of("1rm:press-banca — Press banca",
                        "1rm:press-banca — Press  Banca! (repetida: no se manda)"),
                ClavesPanel.lista(List.of(ej(5, "Press  Banca!", 90.0), ej(2, "Press banca", 100.0))));
    }

    @Test
    void ordenadasPorId() {
        assertEquals(List.of("1rm:a — A", "1rm:b — B", "1rm:c — C"),
                ClavesPanel.lista(List.of(ej(3, "C", 1.0), ej(1, "A", 1.0), ej(2, "B", 1.0))));
    }
}
