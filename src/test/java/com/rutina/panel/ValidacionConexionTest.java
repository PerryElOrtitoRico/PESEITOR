package com.rutina.panel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidacionConexionTest {

    private static final String TOKEN = "token-de-prueba-123";

    @Test
    void urlValidaConPuertoYTokenValidoNoDanErrores() {
        assertEquals(List.of(), ValidacionConexion.errores("http://192.168.1.13:7070", TOKEN));
        assertEquals(List.of(), ValidacionConexion.errores("HTTPS://panel.casa/", TOKEN));
    }

    @Test
    void urlVacia() {
        assertEquals(List.of("Falta la dirección del panel."), ValidacionConexion.errores("  ", TOKEN));
        assertEquals(List.of("Falta la dirección del panel."), ValidacionConexion.errores(null, TOKEN));
    }

    @Test
    void urlSinEsquema() {
        assertEquals(List.of("La dirección tiene que empezar con http:// o https://."),
                ValidacionConexion.errores("192.168.1.13:7070", TOKEN));
    }

    @Test
    void urlSinHost() {
        assertEquals(List.of("La dirección no es válida."), ValidacionConexion.errores("http://", TOKEN));
        assertEquals(List.of("La dirección no es válida."), ValidacionConexion.errores("http://a b", TOKEN));
    }

    @Test
    void urlTerminadaEnApi() {
        String msg = "La dirección no debe terminar en /api: es la raíz del panel.";
        assertEquals(List.of(msg), ValidacionConexion.errores("http://panel:7070/api", TOKEN));
        assertEquals(List.of(msg), ValidacionConexion.errores("http://panel:7070/api/", TOKEN));
    }

    @Test
    void tokenEnBlanco() {
        assertEquals(List.of("Falta el token."), ValidacionConexion.errores("http://panel:7070", "   "));
        assertEquals(List.of("Falta el token."), ValidacionConexion.errores("http://panel:7070", null));
    }

    @Test
    void tokenConCaracteresProhibidos() {
        String msg = "El token no puede tener espacios ni ; , \" \\.";
        for (String malo : new String[]{"abc def", "abc;def", "abc,def", "abc\"def", "abc\\def", "abc\tdef"}) {
            assertEquals(List.of(msg), ValidacionConexion.errores("http://panel:7070", malo), malo);
        }
    }

    @Test
    void variosErroresJuntosEnOrden() {
        assertEquals(List.of("Falta la dirección del panel.", "Falta el token."), ValidacionConexion.errores("", ""));
        assertEquals(List.of("La dirección tiene que empezar con http:// o https://.",
                        "El token no puede tener espacios ni ; , \" \\."),
                ValidacionConexion.errores("panel", "a b"));
    }
}
