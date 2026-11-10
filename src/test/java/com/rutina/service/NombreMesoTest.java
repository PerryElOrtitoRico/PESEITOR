package com.rutina.service;

import com.rutina.model.Mesociclo;
import com.rutina.model.ParametrosTipo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NombreMesoTest {

    private static Mesociclo mes(String objetivo, Integer dias) {
        return new Mesociclo(1, 1, LocalDate.of(2026, 11, 9), 4, new ParametrosTipo(0, 72), new ParametrosTipo(350, 76),
                null, dias, Map.of(), List.of(), "", "", Map.of(), objetivo, null);
    }

    @Test
    void etiquetaConYSinDias() {
        assertEquals("Sentadilla (4s3d)", NombreMeso.etiqueta(mes("Sentadilla", 3)));
        assertEquals("Sentadilla (4s)", NombreMeso.etiqueta(mes(" Sentadilla ", null)));
        assertNull(NombreMeso.etiqueta(mes("", 3)));
    }

    @Test
    void archivoEstandar() {
        assertEquals("2026-11-09_Fuerza_Sentadilla_4s3d.pdf", NombreMeso.archivo("", mes("Sentadilla", 3), "Fuerza", "pdf"));
        assertEquals("Franco_2026-11-09_Fuerza_PressBanca_4s3d.json",
                NombreMeso.archivo("Franco", mes("press banca", 3), "Fuerza", "json"));
        assertEquals("2026-11-09_Fuerza_Sentadilla_4s3d.pdf", NombreMeso.archivo("", mes("Sentadilla", 3), "Fuerza", "pdf"));
    }

    @Test
    void archivoSinAcentosNiVaciosNiBarras() {
        assertEquals("2026-11-09_Elevacion_4s.pdf", NombreMeso.archivo("", mes("Elevación/ ", null), null, "pdf"));
        assertEquals("2026-11-09_4s.pdf", NombreMeso.archivo("", mes(null, null), "", "pdf"));
    }
}
