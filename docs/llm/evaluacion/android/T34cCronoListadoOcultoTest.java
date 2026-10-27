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
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class T34cCronoListadoOcultoTest {

    @TempDir
    Path dir;

    static String ejemplo(String nombre) throws IOException {
        return new String(Files.readAllBytes(Paths.get("../../docs/registro-sesiones/ejemplos/" + nombre)), StandardCharsets.UTF_8);
    }

    void ponerPlan(long id, String inicio) throws IOException {
        PlanCelular p = new PlanCelular("rutineitor-plan", 1, "F", id, inicio, 1, 1, List.of(new SesionPlan(1, 1, List.of())));
        Files.createDirectories(dir.resolve("planes"));
        Files.write(dir.resolve("planes").resolve(id + ".json"), JsonRegistro.planAJson(p).getBytes(StandardCharsets.UTF_8));
    }

    void escribir(String relativo, String texto) throws IOException {
        Path p = dir.resolve(relativo);
        Files.createDirectories(p.getParent());
        Files.write(p, texto.getBytes(StandardCharsets.UTF_8));
    }

    long cuantos(String carpeta, String prefijo) throws IOException {
        if (!Files.isDirectory(dir.resolve(carpeta))) return 0;
        try (Stream<Path> s = Files.list(dir.resolve(carpeta))) {
            return s.filter(p -> p.getFileName().toString().startsWith(prefijo)).count();
        }
    }

    @Test
    void cronoPorDefectoYGuardado() throws IOException {
        Almacen a = new Almacen(dir);
        assertEquals(Crono.nuevo(), a.crono(3, 1, 1));
        a.crono(3, 1, 1, new EstadoCrono(60_000, 1_000L));
        a.crono(3, 1, 2, new EstadoCrono(5_000, null));
        assertEquals(new EstadoCrono(60_000, 1_000L), new Almacen(dir).crono(3, 1, 1));
        assertEquals(new EstadoCrono(5_000, null), new Almacen(dir).crono(3, 1, 2));
        assertEquals(Crono.nuevo(), new Almacen(dir).crono(3, 2, 1));
        assertEquals(Crono.nuevo(), new Almacen(dir).crono(4, 1, 1));
        a.crono(3, 1, 1, new EstadoCrono(7, null));
        assertEquals(new EstadoCrono(7, null), a.crono(3, 1, 1));
    }

    @Test
    void cronoConservaOtrasPreferencias() throws IOException {
        Almacen a = new Almacen(dir);
        a.oscuro(true);
        a.activo(3L);
        a.crono(3, 1, 1, new EstadoCrono(1, 2L));
        assertTrue(new Almacen(dir).oscuro());
        assertEquals(3L, new Almacen(dir).activo());
    }

    @Test
    void planesOrdenadosPorInicioYRotosApartados() throws IOException {
        Almacen a = new Almacen(dir);
        assertEquals(List.of(), a.planes());
        ponerPlan(5, "2026-12-01");
        ponerPlan(2, "2026-10-26");
        ponerPlan(7, "2026-11-09");
        escribir("planes/9.json", "{roto");
        escribir("planes/notas.txt", "no soy un plan");
        List<PlanCelular> ps = a.planes();
        assertEquals(List.of(2L, 7L, 5L), ps.stream().map(PlanCelular::mesocicloId).toList());
        assertEquals(1, cuantos("planes", "9.json.roto-"));
        assertEquals(3, a.planes().size());
    }

    @Test
    void borrarPlanBorraPlanRegistroYCronos() throws IOException {
        Almacen a = new Almacen(dir);
        ponerPlan(1, "2026-10-26");
        ponerPlan(10, "2026-11-26");
        escribir("registros/1.json", ejemplo("registro-ejemplo.json"));
        escribir("registros/10.json", ejemplo("registro-ejemplo.json"));
        a.crono(1, 1, 1, new EstadoCrono(5, null));
        a.crono(10, 1, 1, new EstadoCrono(6, null));
        a.activo(1L);
        a.oscuro(true);
        a.borrarPlan(1);
        assertFalse(Files.exists(dir.resolve("planes/1.json")));
        assertFalse(Files.exists(dir.resolve("registros/1.json")));
        assertEquals(Crono.nuevo(), a.crono(1, 1, 1));
        assertNull(a.activo());
        assertTrue(a.oscuro());
        assertTrue(Files.exists(dir.resolve("planes/10.json")));
        assertTrue(Files.exists(dir.resolve("registros/10.json")));
        assertEquals(new EstadoCrono(6, null), a.crono(10, 1, 1));
        a.activo(10L);
        a.borrarPlan(99);
        assertEquals(10L, a.activo());
    }
}
