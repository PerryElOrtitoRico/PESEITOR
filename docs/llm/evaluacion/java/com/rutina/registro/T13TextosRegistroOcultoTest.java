package com.rutina.registro;

import com.rutina.model.Tipo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class T13TextosRegistroOcultoTest {

    private static EjercicioPlan ej(String bloque, String nombre) {
        return new EjercicioPlan(bloque, nombre, Tipo.HIPERTROFIA, null, 100.0, List.of());
    }

    private static SesionRegistro reg(EjercicioRegistro... es) {
        return new SesionRegistro(1, 1, "2026-10-27", 5, 4, List.of(es), 60);
    }

    private static EjercicioRegistro er(String bloque, String nota) {
        return new EjercicioRegistro(bloque, 3, Map.of(), nota);
    }

    @Test
    void duracion() {
        assertEquals("75 min", TextosRegistro.duracion(75));
        assertEquals("600 min", TextosRegistro.duracion(600));
        assertEquals("1 min", TextosRegistro.duracion(1));
        assertEquals("", TextosRegistro.duracion(null));
    }

    @Test
    void notasEnElOrdenDelPlan() {
        var plan = new SesionPlan(1, 1, List.of(ej("1-1", "A"), ej("1-2", "B"), ej("1-3", "C")));
        assertEquals("A: uno | B: dos | C: tres", TextosRegistro.notas(plan, reg(er("1-3", "tres"), er("1-1", "uno"), er("1-2", "dos"))));
    }

    @Test
    void soloLasConNota() {
        var plan = new SesionPlan(1, 1, List.of(ej("1-1", "A"), ej("1-2", "B"), ej("1-3", "C")));
        assertEquals("C: ok", TextosRegistro.notas(plan, reg(er("1-1", null), er("1-2", "  \t "), er("1-3", "ok"))));
        assertEquals("", TextosRegistro.notas(plan, reg()));
    }

    @Test
    void ignoraBloquesQueNoEstanEnElPlan() {
        var plan = new SesionPlan(1, 1, List.of(ej("1-1", "A")));
        assertEquals("A: x", TextosRegistro.notas(plan, reg(er("9-9", "fantasma"), er("1-1", "x"))));
        assertEquals("", TextosRegistro.notas(new SesionPlan(1, 1, List.of()), reg(er("1-1", "x"))));
    }

    @Test
    void espaciosYSaltosSeUnifican() {
        var plan = new SesionPlan(1, 1, List.of(ej("1-1", "Press militar")));
        assertEquals("Press militar: dolor en el hombro derecho", TextosRegistro.notas(plan,
                reg(er("1-1", "\n dolor  en el\r\nhombro\tderecho \n"))));
    }

    @Test
    void registroSinEjercicios() {
        var plan = new SesionPlan(1, 1, List.of(ej("1-1", "A")));
        assertEquals("", TextosRegistro.notas(plan, new SesionRegistro(1, 1, "2026-10-27", null, null, null, null)));
    }
}
