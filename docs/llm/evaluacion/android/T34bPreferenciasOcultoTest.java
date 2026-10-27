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

class T34bPreferenciasOcultoTest {

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
    void porDefecto() {
        Almacen a = new Almacen(dir);
        assertFalse(a.oscuro());
        assertNull(a.activo());
        assertTrue(a.leerPreferencias().entrySet().isEmpty());
    }

    @Test
    void oscuroSeGuardaYSeRelee() throws IOException {
        new Almacen(dir).oscuro(true);
        assertTrue(new Almacen(dir).oscuro());
        new Almacen(dir).oscuro(false);
        assertFalse(new Almacen(dir).oscuro());
    }

    @Test
    void activoSeGuardaYSeBorra() throws IOException {
        Almacen a = new Almacen(dir);
        a.activo(3L);
        assertEquals(3L, new Almacen(dir).activo());
        a.oscuro(true);
        assertEquals(3L, new Almacen(dir).activo());
        assertTrue(new Almacen(dir).oscuro());
        a.activo(null);
        assertNull(new Almacen(dir).activo());
        assertTrue(new Almacen(dir).oscuro());
    }

    @Test
    void preferenciasRotasSeApartan() throws IOException {
        escribir("preferencias.json", "{roto");
        Almacen a = new Almacen(dir);
        assertFalse(a.oscuro());
        assertEquals(1, cuantos("", "preferencias.json.roto-"));
        a.oscuro(true);
        assertTrue(new Almacen(dir).oscuro());
        escribir("preferencias.json", "[1,2]");
        assertNull(new Almacen(dir).activo());
        assertFalse(Files.exists(dir.resolve("preferencias.json")));
        assertTrue(cuantos("", "preferencias.json.roto-") >= 1);
    }

    @Test
    void guardarYLeerPreferencias() throws IOException {
        Almacen a = new Almacen(dir);
        JsonObject o = new JsonObject();
        o.addProperty("x", 5);
        a.guardarPreferencias(o);
        assertEquals(5, a.leerPreferencias().get("x").getAsInt());
        assertEquals(0, cuantos("", "preferencias.json.tmp"));
    }
}
