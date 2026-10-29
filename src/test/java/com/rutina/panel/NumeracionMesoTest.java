package com.rutina.panel;

import com.rutina.model.Mesociclo;
import com.rutina.model.ParametrosTipo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class NumeracionMesoTest {

    private static Mesociclo mes(long id, LocalDate inicio) {
        return new Mesociclo(id, 1, inicio, 3, new ParametrosTipo(0, 72), new ParametrosTipo(200, 74),
                null, 3, java.util.Map.of(), List.of(), "", "", java.util.Map.of());
    }

    @Test
    void numeracionCuentaPorAnioYOrdenDeInicio() {
        var a = mes(30, LocalDate.of(2026, 1, 5));
        var b = mes(10, LocalDate.of(2026, 3, 2));
        var c = mes(20, LocalDate.of(2026, 6, 1));
        var lista = List.of(c, a, b);
        assertArrayEquals(new int[]{2026, 1}, NumeracionMeso.de(lista, a));
        assertArrayEquals(new int[]{2026, 2}, NumeracionMeso.de(lista, b));
        assertArrayEquals(new int[]{2026, 3}, NumeracionMeso.de(lista, c));
    }

    @Test
    void numeracionDeOtroAnioReiniciaMeso() {
        var a = mes(1, LocalDate.of(2025, 11, 3));
        var b = mes(2, LocalDate.of(2026, 1, 5));
        var c = mes(3, LocalDate.of(2025, 12, 1));
        var lista = List.of(a, b, c);
        assertArrayEquals(new int[]{2025, 2}, NumeracionMeso.de(lista, c));
        assertArrayEquals(new int[]{2026, 1}, NumeracionMeso.de(lista, b));
    }

    @Test
    void mismoInicioDesempataPorId() {
        var a = mes(7, LocalDate.of(2026, 2, 2));
        var b = mes(5, LocalDate.of(2026, 2, 2));
        var lista = List.of(a, b);
        assertArrayEquals(new int[]{2026, 1}, NumeracionMeso.de(lista, b));
        assertArrayEquals(new int[]{2026, 2}, NumeracionMeso.de(lista, a));
    }
}
