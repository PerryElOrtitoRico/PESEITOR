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

/** Los focos de revisión del plan, con el Almacen completo (corre cuando están hechas todas las subtareas). */
class T34zFocosOcultoTest {

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
    void foco3GuardarYReleerConOtraInstancia() throws IOException {
        Almacen a = new Almacen(dir);
        PlanCelular plan = (PlanCelular) a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        a.activo(plan.mesocicloId());
        RegistroCelular r = (RegistroCelular) a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));
        a.guardarRegistro(r);
        Almacen otra = new Almacen(dir);
        assertEquals(plan, otra.plan(3));
        assertEquals(r, otra.registro(3));
        assertEquals(3L, otra.activo());
        assertEquals(0, cuantos("registros", "3.json.tmp"));
    }

    @Test
    void registroVacioSiNoHayYNullSiNoHayPlan() throws IOException {
        Almacen a = new Almacen(dir);
        assertNull(a.registro(3));
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        RegistroCelular r = a.registro(3);
        assertEquals("rutineitor-registro", r.formato());
        assertEquals(3L, r.mesocicloId());
        assertEquals("Franco", r.perfil());
        assertTrue(r.sesiones().isEmpty());
    }

    @Test
    void foco4MismoPlanConservaSuRegistro() throws IOException {
        Almacen a = new Almacen(dir);
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        assertEquals(2, a.registro(3).sesiones().size());
        assertEquals(1, a.planes().size());
    }

    @Test
    void foco1PlanSinNingunaPistaDeJson() throws IOException {
        Almacen a = new Almacen(dir);
        assertInstanceOf(PlanCelular.class, a.cargarDesdeTexto("  \r\n" + ejemplo("plan-ejemplo.json") + "\r\n "));
    }

    @Test
    void foco2ArchivosRotosNoTumbanNada() throws IOException {
        Almacen a = new Almacen(dir);
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        escribir("planes/9.json", "{roto");
        escribir("registros/9.json", "{roto");
        assertEquals(1, a.planes().size());
        assertNull(a.registro(9));
        assertEquals(3L, a.planes().get(0).mesocicloId());
        assertEquals(1, cuantos("planes", "9.json.roto-"));
        assertEquals(1, cuantos("registros", "9.json.roto-"));
        escribir("preferencias.json", "{roto");
        assertFalse(a.oscuro());
        assertNull(a.activo());
        assertEquals(1, a.planes().size());
    }

    @Test
    void borrarPlanDejaSinArchivosDeEseId() throws IOException {
        Almacen a = new Almacen(dir);
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));
        a.crono(3, 1, 1, new EstadoCrono(10, 20L));
        a.borrarPlan(3);
        assertNull(a.plan(3));
        assertNull(a.registro(3));
        assertEquals(0, cuantos("planes", "3."));
        assertEquals(0, cuantos("registros", "3."));
        assertEquals(Crono.nuevo(), a.crono(3, 1, 1));
        assertTrue(a.planes().isEmpty());
    }

    @Test
    void oscuroPorDefectoFalse() {
        assertFalse(new Almacen(dir).oscuro());
    }
}
