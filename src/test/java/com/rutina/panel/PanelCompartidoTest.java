package com.rutina.panel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PanelCompartidoTest {

    @TempDir
    Path carpeta;

    @Test
    void reemplazarCierraLaAnteriorYDevuelveLaNueva() throws Exception {
        Path c1 = carpeta.resolve("a");
        Path c2 = carpeta.resolve("b");
        ConexionPanel.guardarConfig(c1, "http://127.0.0.1:1", "token-de-prueba-123", "pc", true);
        ConexionPanel.guardarConfig(c2, "http://127.0.0.1:1", "token-de-prueba-123", "tablet", true);
        ConexionPanel vieja = ConexionPanel.abrir(c1);
        ConexionPanel nueva = ConexionPanel.abrir(c2);
        PanelCompartido p = new PanelCompartido(vieja);
        assertSame(vieja, p.get());
        p.reemplazar(nueva);
        assertSame(nueva, p.get());
        assertEquals("tablet", p.get().dispositivo());
        p.close();
        p.close(); // idempotente
    }
}
