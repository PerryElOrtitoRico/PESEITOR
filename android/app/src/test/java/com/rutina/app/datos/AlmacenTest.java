package com.rutina.app.datos;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.rutina.app.logica.Crono;
import com.rutina.app.logica.EstadoCrono;
import com.rutina.registro.JsonRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionPlan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlmacenTest {

    @TempDir
    Path dir;

    private static String ejemplo(String nombre) throws IOException {
        return new String(Files.readAllBytes(java.nio.file.Paths.get("../../docs/registro-sesiones/ejemplos/" + nombre)),
                StandardCharsets.UTF_8);
    }

    /** Escribe a mano un plan mínimo en planes/<id>.json (sin pasar por Almacen). */
    private void ponerPlan(long id, String inicio) throws IOException {
        PlanCelular p = new PlanCelular("rutineitor-plan", 1, "F", id, inicio, 1, 1, List.of(new SesionPlan(1, 1, List.of())));
        Files.createDirectories(dir.resolve("planes"));
        Files.write(dir.resolve("planes").resolve(id + ".json"), JsonRegistro.planAJson(p).getBytes(StandardCharsets.UTF_8));
    }

    private long cuantos(String carpeta, String prefijo) throws IOException {
        try (Stream<Path> s = Files.list(dir.resolve(carpeta))) {
            return s.filter(p -> p.getFileName().toString().startsWith(prefijo)).count();
        }
    }

    @Test
    void escribeYRelee() throws IOException {
        Path destino = dir.resolve("a").resolve("x.json");
        Almacen.escribirAtomico(destino, "uno");
        assertEquals("uno", Almacen.leerTexto(destino));
        Almacen.escribirAtomico(destino, "dos");
        assertEquals("dos", Almacen.leerTexto(destino));
        assertEquals(1, cuantos("a", ""));
    }

    @Test
    void apartaElRoto() throws IOException {
        Files.createDirectories(dir.resolve("planes"));
        Files.write(dir.resolve("planes").resolve("9.json"), "{roto".getBytes(StandardCharsets.UTF_8));
        assertNull(Almacen.leerPlan(dir.resolve("planes").resolve("9.json")));
        assertFalse(Files.exists(dir.resolve("planes").resolve("9.json")));
        assertEquals(1, cuantos("planes", "9.json.roto-"));
        assertNull(Almacen.leerRegistro(dir.resolve("registros").resolve("1.json")));
    }

    @Test
    void preferencias() throws IOException {
        Almacen a = new Almacen(dir);
        assertFalse(a.oscuro());
        assertNull(a.activo());
        a.oscuro(true);
        a.activo(3L);
        Almacen otra = new Almacen(dir);
        assertTrue(otra.oscuro());
        assertEquals(3L, otra.activo());
        otra.activo(null);
        assertNull(new Almacen(dir).activo());
        assertTrue(new Almacen(dir).oscuro());
    }

    @Test
    void listaYBorraPlanes() throws IOException {
        Almacen a = new Almacen(dir);
        assertEquals(0, a.planes().size());
        ponerPlan(5, "2026-12-01");
        ponerPlan(2, "2026-10-26");
        assertEquals(2L, a.planes().get(0).mesocicloId());
        assertEquals(5L, a.planes().get(1).mesocicloId());
        a.activo(2L);
        a.borrarPlan(2);
        assertEquals(1, a.planes().size());
        assertNull(a.activo());
        assertEquals(0, cuantos("planes", "2."));
    }

    @Test
    void cronos() throws IOException {
        Almacen a = new Almacen(dir);
        assertEquals(Crono.nuevo(), a.crono(3, 1, 1));
        a.crono(3, 1, 1, new EstadoCrono(60_000, 1_000L));
        a.crono(3, 1, 2, new EstadoCrono(5_000, null));
        assertEquals(new EstadoCrono(60_000, 1_000L), new Almacen(dir).crono(3, 1, 1));
        assertEquals(new EstadoCrono(5_000, null), new Almacen(dir).crono(3, 1, 2));
        assertEquals(Crono.nuevo(), new Almacen(dir).crono(4, 1, 1));
    }

    @Test
    void cargaPlanYRegistro() throws IOException {
        Almacen a = new Almacen(dir);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto(ejemplo("registro-ejemplo.json")));
        assertTrue(e.getMessage().contains("no hay ningún plan"));
        Object p = a.cargarDesdeTexto("  " + ejemplo("plan-ejemplo.json") + "  ");
        assertInstanceOf(PlanCelular.class, p);
        assertEquals(3L, a.plan(3).mesocicloId());
        Object r = a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));
        assertInstanceOf(RegistroCelular.class, r);
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        assertEquals(2, a.registro(3).sesiones().size());
        assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto("{roto"));
    }
}
