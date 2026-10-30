package com.rutina.panel;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}
