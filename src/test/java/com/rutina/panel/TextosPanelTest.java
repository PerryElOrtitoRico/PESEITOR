package com.rutina.panel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextosPanelTest {

    @Test
    void historialEntregado() {
        assertEquals("Se mandaron 4 valores de 1RM.\naviso 1\naviso 2",
                TextosPanel.historialEnviado(4, 0, List.of("aviso 1", "aviso 2")));
    }

    @Test
    void historialQueSoloQuedoEnCola() {
        assertEquals("Se pusieron 4 valores de 1RM en la cola; el panel no respondió, "
                        + "se mandarán cuando vuelva la conexión.",
                TextosPanel.historialEnviado(4, 4, List.of()));
    }

    @Test
    void causaSinMensajeUsaElNombreDeLaClase() {
        assertEquals("IllegalStateException", TextosPanel.causa(new IllegalStateException()));
        assertEquals("IllegalStateException", TextosPanel.causa(new IllegalStateException("  ")));
        assertEquals("boom", TextosPanel.causa(new RuntimeException("boom")));
    }

    @Test
    void nombreDeDispositivo() {
        assertEquals("mi-pc", NombreDispositivo.porDefecto(Map.of("HOSTNAME", "mi-pc", "COMPUTERNAME", "WIN")));
        assertEquals("WIN", NombreDispositivo.porDefecto(Map.of("HOSTNAME", "  ", "COMPUTERNAME", "WIN")));
        assertEquals("WIN", NombreDispositivo.porDefecto(Map.of("COMPUTERNAME", "WIN")));
        assertEquals("pc", NombreDispositivo.porDefecto(Map.of("HOSTNAME", "", "COMPUTERNAME", " ")));
        assertEquals("pc", NombreDispositivo.porDefecto(Map.of()));
    }
}
