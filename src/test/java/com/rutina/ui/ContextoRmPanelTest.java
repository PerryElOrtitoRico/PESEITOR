package com.rutina.ui;

import com.rutina.model.Ejercicio;
import com.rutina.model.Perfil;
import com.rutina.model.Tipo;
import com.rutina.panel.ConexionPanel;
import com.rutina.panel.PanelCompartido;
import com.rutina.service.datos.Database;
import com.rutina.service.datos.PerfilRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import panel.core.ColaLocal;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContextoRmPanelTest {

    @TempDir
    Path d;

    private static Ejercicio nuevo(long perfil, String nombre) {
        return new Ejercicio(0, perfil, nombre, Tipo.FUERZA, null, null, 100, Map.of(), null, 0, true);
    }

    private static int enCola(Path carpeta) {
        return new ColaLocal(carpeta.resolve("panel-cola.jsonl")).pendientes().size();
    }

    @Test
    void soloElPerfilDelPanelMandaSusRm() throws Exception {
        Path datos = d.resolve("datos");
        java.nio.file.Files.createDirectories(datos);
        ConexionPanel.guardarConfig(datos, "http://127.0.0.1:1", "token-de-prueba-123", "pc", true);
        try (var db = Database.abrir(d.resolve("r.db")); var panel = new PanelCompartido(ConexionPanel.abrir(datos))) {
            var perfiles = new PerfilRepo(db);
            Perfil mio = perfiles.crear("Franco");
            Perfil amigo = perfiles.crear("Amigo");
            var ctxMio = new Contexto(db, mio, panel);
            var ctxAmigo = new Contexto(db, amigo, panel);

            var banca = ctxMio.ejercicios.guardar(nuevo(mio.id(), "Remo con barra"));
            ctxMio.ejercicios.actualizarRm(banca.id(), 80, LocalDate.of(2026, 10, 1));
            panel.get().vaciarAhora();
            assertEquals(1, enCola(datos));

            // un clon con la misma clave y mayor id no se manda
            var clon = ctxMio.ejercicios.guardar(nuevo(mio.id(), "remo  CON barra!"));
            ctxMio.ejercicios.actualizarRm(clon.id(), 70, LocalDate.of(2026, 10, 2));
            panel.get().vaciarAhora();
            assertEquals(1, enCola(datos));

            // el perfil del amigo nunca se manda
            var otro = ctxAmigo.ejercicios.guardar(nuevo(amigo.id(), "Remo con barra"));
            ctxAmigo.ejercicios.actualizarRm(otro.id(), 60, LocalDate.of(2026, 10, 3));
            panel.get().vaciarAhora();
            assertEquals(1, enCola(datos));
        }
    }
}
