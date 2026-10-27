package com.rutina.registro;

import com.rutina.model.Tipo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextosRegistroTest {

    private static EjercicioPlan ej(String bloque, String nombre) {
        return new EjercicioPlan(bloque, nombre, Tipo.FUERZA, null, null, List.of());
    }

    private static SesionPlan plan() {
        return new SesionPlan(1, 1, List.of(ej("1-1", "Sentadilla"), ej("1-2", "Press banca"), ej("2-1", "Remo")));
    }

    @Test
    void duracion() {
        assertEquals("75 min", TextosRegistro.duracion(75));
        assertEquals("1 min", TextosRegistro.duracion(1));
        assertEquals("", TextosRegistro.duracion(null));
    }

    @Test
    void notas() {
        var sr = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-2", null, Map.of(), "  dolor leve  "), new EjercicioRegistro("1-1", 3, Map.of(), "subí 2,5 kg")), null);
        assertEquals("Sentadilla: subí 2,5 kg | Press banca: dolor leve", TextosRegistro.notas(plan(), sr));
        var sr2 = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("2-1", null, Map.of(), "línea uno\nlínea dos")), null);
        assertEquals("Remo: línea uno línea dos", TextosRegistro.notas(plan(), sr2));
    }

    @Test
    void notasVacias() {
        var sin = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-1", 3, Map.of(), null), new EjercicioRegistro("2-1", null, Map.of(), "   ")), null);
        assertEquals("", TextosRegistro.notas(plan(), sin));
        var nula = new SesionRegistro(1, 1, "2026-10-27", null, null, null, null);
        assertEquals("", TextosRegistro.notas(plan(), nula));
    }
}
