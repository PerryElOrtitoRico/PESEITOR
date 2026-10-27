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

class T34dCargarOcultoTest {

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
    void cargaUnPlan() throws IOException {
        Almacen a = new Almacen(dir);
        Object o = a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        assertInstanceOf(PlanCelular.class, o);
        assertEquals(3L, ((PlanCelular) o).mesocicloId());
        assertTrue(Files.exists(dir.resolve("planes/3.json")));
        assertEquals(3L, a.plan(3).mesocicloId());
    }

    @Test
    void planConEspaciosBomYSinNombreDeArchivo() throws IOException {
        Almacen a = new Almacen(dir);
        Object o = a.cargarDesdeTexto("﻿  \n\n" + ejemplo("plan-ejemplo.json") + "\n\n   ");
        assertInstanceOf(PlanCelular.class, o);
        assertNotNull(a.plan(3));
    }

    @Test
    void cargaUnRegistroSoloConPlan() throws IOException {
        Almacen a = new Almacen(dir);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> a.cargarDesdeTexto(ejemplo("registro-ejemplo.json")));
        assertTrue(e.getMessage().contains("no hay ningún plan"), e.getMessage());
        assertEquals(0, cuantos("registros", ""));
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        Object o = a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));
        assertInstanceOf(RegistroCelular.class, o);
        assertEquals(2, a.registro(3).sesiones().size());
        assertTrue(Files.exists(dir.resolve("registros/3.json")));
    }

    @Test
    void cargarDeNuevoElPlanConservaElRegistro() throws IOException {
        Almacen a = new Almacen(dir);
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        assertEquals(1, cuantos("planes", ""));
        assertEquals(2, a.registro(3).sesiones().size());
    }

    @Test
    void textosInvalidos() throws IOException {
        Almacen a = new Almacen(dir);
        for (String t : new String[]{"", "   ", "{roto", "[1,2,3]", "42", "null", "{\"formato\":\"otra-cosa\"}", "{\"x\":1}",
                "{\"formato\":5}", "{\"formato\":null}", "{\"formato\":\"rutineitor-plan\",\"version\":2,\"sesiones\":[]}"}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto(t), "[" + t + "]");
            assertFalse(e.getMessage() == null || e.getMessage().isBlank());
        }
        assertEquals(0, cuantos("planes", ""));
    }
}
