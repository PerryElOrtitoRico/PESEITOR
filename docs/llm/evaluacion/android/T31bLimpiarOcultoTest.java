package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionRegistro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class T31bLimpiarOcultoTest {

    private static EjercicioRegistro ej(String bloque, Integer esfuerzo, Map<String, String> filas, String nota) {
        return new EjercicioRegistro(bloque, esfuerzo, filas, nota);
    }

    private static RegistroCelular vacio() {
        return new RegistroCelular("rutineitor-registro", 1, "Franco Pérez", 3L, List.of());
    }

    @Test
    void limpiarSesion() {
        Map<String, String> f1 = new HashMap<>();
        f1.put("70", "  ");
        f1.put("80-85", " 3.3 ");
        SesionRegistro s = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(
                ej("1-1", null, f1, null),
                ej("1-2", null, Map.of("4x12", ""), null),
                ej("1-3", 2, Map.of(), null)), null);
        SesionRegistro l = Sesiones.limpiarSesion(s);
        assertEquals(List.of(
                ej("1-1", null, Map.of("80-85", "3.3"), null),
                ej("1-3", 2, Map.of(), null)), l.ejercicios());
        assertEquals(2, f1.size());
        assertEquals(3, s.ejercicios().size());
        assertEquals(1, l.microciclo());
        assertEquals("2026-10-27", l.fecha());
    }

    @Test
    void limpiarSesionNotas() {
        SesionRegistro s = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(
                ej("1-1", null, Map.of("70", "  "), "  subir peso  "),
                ej("1-2", null, Map.of("4x12", ""), "   "),
                ej("1-3", 2, Map.of(), ""),
                ej("1-4", null, Map.of("70", " 3.3 "), "dolor leve"),
                ej("1-5", null, Map.of("70", "3"), null),
                ej("1-6", 4, Map.of(), null)), 75);
        SesionRegistro l = Sesiones.limpiarSesion(s);
        assertEquals(List.of(
                ej("1-1", null, Map.of(), "subir peso"),
                ej("1-3", 2, Map.of(), null),
                ej("1-4", null, Map.of("70", "3.3"), "dolor leve"),
                ej("1-5", null, Map.of("70", "3"), null),
                ej("1-6", 4, Map.of(), null)), l.ejercicios());
        assertEquals(75, l.duracionMin());
    }

    @Test
    void limpiarSesionConservaElResto() {
        SesionRegistro s = new SesionRegistro(2, 3, "2026-11-05", 4, 5, List.of(), 60);
        assertEquals(s, Sesiones.limpiarSesion(s));
    }

    @Test
    void guardarSesionAgregaOrdenaYReemplaza() {
        RegistroCelular r0 = vacio();
        SesionRegistro s22 = new SesionRegistro(2, 2, "2026-11-05", 4, null, List.of(), null);
        SesionRegistro s11 = new SesionRegistro(1, 1, "2026-10-27", 5, 3,
                List.of(ej("1-1", 4, Map.of("80-85", "3.3-115"), null)), null);
        RegistroCelular r1 = Sesiones.guardarSesion(r0, s22);
        RegistroCelular r2 = Sesiones.guardarSesion(r1, s11);
        assertEquals(List.of(), r0.sesiones());
        assertEquals(1, r1.sesiones().size());
        assertEquals(List.of("1-1", "2-2"), r2.sesiones().stream().map(x -> x.microciclo() + "-" + x.dia()).toList());
        SesionRegistro s11b = new SesionRegistro(1, 1, "2026-10-27", 1, 3, s11.ejercicios(), null);
        RegistroCelular r3 = Sesiones.guardarSesion(r2, s11b);
        assertEquals(2, r3.sesiones().size());
        assertEquals(1, r3.sesiones().get(0).descanso());
        assertEquals(1, r3.sesiones().get(0).microciclo());
        assertEquals(2, r3.sesiones().get(1).microciclo());
        assertEquals("rutineitor-registro", r3.formato());
        assertEquals(1, r3.version());
        assertEquals("Franco Pérez", r3.perfil());
        assertEquals(3L, r3.mesocicloId());
    }

    @Test
    void guardarSesionOrdenaPorMicroYDia() {
        RegistroCelular r = vacio();
        for (int[] md : new int[][]{{2, 1}, {1, 2}, {2, 2}, {1, 1}}) {
            r = Sesiones.guardarSesion(r, new SesionRegistro(md[0], md[1], "2026-10-27", null, null, new ArrayList<>(), null));
        }
        assertEquals(List.of("1-1", "1-2", "2-1", "2-2"),
                r.sesiones().stream().map(x -> x.microciclo() + "-" + x.dia()).toList());
    }

    @Test
    void guardarSesionLimpia() {
        SesionRegistro s = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(
                ej("1-1", null, Map.of("70", "  "), null),
                ej("1-2", null, Map.of(), " ok ")), null);
        RegistroCelular r = Sesiones.guardarSesion(vacio(), s);
        assertEquals(List.of(ej("1-2", null, Map.of(), "ok")), r.sesiones().get(0).ejercicios());
    }
}
