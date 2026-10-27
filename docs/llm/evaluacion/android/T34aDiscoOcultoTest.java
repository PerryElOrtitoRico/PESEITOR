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

class T34aDiscoOcultoTest {

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
    void escribirAtomicoCreaCarpetaYReemplaza() throws IOException {
        Path destino = dir.resolve("a").resolve("b").resolve("x.json");
        Almacen.escribirAtomico(destino, "uno");
        assertEquals("uno", new String(Files.readAllBytes(destino), StandardCharsets.UTF_8));
        Almacen.escribirAtomico(destino, "dos ñandú ✓");
        assertEquals("dos ñandú ✓", Almacen.leerTexto(destino));
        assertEquals(1, cuantos("a/b", ""));
    }

    @Test
    void apartarRoto() throws IOException {
        escribir("planes/9.json", "{roto");
        Almacen.apartarRoto(dir.resolve("planes/9.json"));
        assertFalse(Files.exists(dir.resolve("planes/9.json")));
        assertEquals(1, cuantos("planes", "9.json.roto-"));
        Almacen.apartarRoto(dir.resolve("planes/noexiste.json"));
    }

    @Test
    void leerPlan() throws IOException {
        assertNull(Almacen.leerPlan(dir.resolve("planes/1.json")));
        escribir("planes/3.json", ejemplo("plan-ejemplo.json"));
        PlanCelular p = Almacen.leerPlan(dir.resolve("planes/3.json"));
        assertEquals(3L, p.mesocicloId());
        assertEquals("rutineitor-plan", p.formato());
        assertEquals(0, cuantos("planes", "3.json.roto-"));
    }

    @Test
    void leerPlanRotoSeAparta() throws IOException {
        escribir("planes/9.json", "{roto");
        assertNull(Almacen.leerPlan(dir.resolve("planes/9.json")));
        assertFalse(Files.exists(dir.resolve("planes/9.json")));
        assertEquals(1, cuantos("planes", "9.json.roto-"));
        escribir("planes/8.json", "{\"formato\":\"otra-cosa\",\"version\":1}");
        assertNull(Almacen.leerPlan(dir.resolve("planes/8.json")));
        assertEquals(1, cuantos("planes", "8.json.roto-"));
        escribir("planes/7.json", "");
        assertNull(Almacen.leerPlan(dir.resolve("planes/7.json")));
        assertEquals(1, cuantos("planes", "7.json.roto-"));
    }

    @Test
    void leerRegistro() throws IOException {
        assertNull(Almacen.leerRegistro(dir.resolve("registros/1.json")));
        escribir("registros/3.json", ejemplo("registro-ejemplo.json"));
        RegistroCelular r = Almacen.leerRegistro(dir.resolve("registros/3.json"));
        assertEquals(3L, r.mesocicloId());
        assertEquals(2, r.sesiones().size());
        escribir("registros/4.json", "[1,2");
        assertNull(Almacen.leerRegistro(dir.resolve("registros/4.json")));
        assertEquals(1, cuantos("registros", "4.json.roto-"));
        assertEquals(1, cuantos("registros", "3.json"));
    }
}
