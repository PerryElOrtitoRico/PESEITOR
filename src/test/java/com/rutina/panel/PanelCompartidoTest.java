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

    @Test
    void reabrirCierraLaViejaAntesDeAbrirLaNueva() {
        ConexionPanel vieja = ConexionPanel.abrir(null);
        ConexionPanel nueva = ConexionPanel.abrir(null);
        java.util.List<String> eventos = new java.util.ArrayList<>();
        PanelCompartido p = new PanelCompartido(vieja);
        p.reabrir(() -> {
            eventos.add("abrir");
            assertNotNull(p.get());
            assertNotSame(vieja, p.get());
            return nueva;
        }, c -> {
            assertSame(vieja, c);
            eventos.add("cerrar");
        });
        assertEquals(java.util.List.of("cerrar", "abrir"), eventos);
        assertSame(nueva, p.get());
    }

    @Test
    void reabrirConSupplierQueFallaDejaUnaConexionInactiva() {
        PanelCompartido p = new PanelCompartido(ConexionPanel.abrir(null));
        p.reabrir(() -> {
            throw new IllegalStateException("falla");
        });
        assertNotNull(p.get());
        assertFalse(p.get().activo());
    }

    @Test
    void getNuncaDevuelveNullDuranteElCambio() throws Exception {
        PanelCompartido p = new PanelCompartido(ConexionPanel.abrir(null));
        java.util.concurrent.atomic.AtomicBoolean seguir = new java.util.concurrent.atomic.AtomicBoolean(true);
        java.util.concurrent.atomic.AtomicInteger nulos = new java.util.concurrent.atomic.AtomicInteger();
        Thread lector = new Thread(() -> {
            while (seguir.get()) {
                if (p.get() == null) nulos.incrementAndGet();
            }
        });
        lector.start();
        for (int i = 0; i < 200; i++) {
            p.reabrir(() -> ConexionPanel.abrir(null));
        }
        seguir.set(false);
        lector.join(2000);
        assertEquals(0, nulos.get());
    }
}
