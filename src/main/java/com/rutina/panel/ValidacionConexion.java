package com.rutina.panel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Revisa la dirección y el token que se escriben en la ventana de conexión, antes de guardarlos o probarlos. */
public final class ValidacionConexion {

    private ValidacionConexion() {
    }

    /** Mensajes de error en español, en este orden: dirección y después token. Vacía si todo está bien. */
    public static List<String> errores(String url, String token) {
        List<String> errores = new ArrayList<>();
        String u = url == null ? "" : url.trim();
        if (u.isEmpty()) {
            errores.add("Falta la dirección del panel.");
        } else if (!empiezaConEsquema(u)) {
            errores.add("La dirección tiene que empezar con http:// o https://.");
        } else if (!tieneHost(u)) {
            errores.add("La dirección no es válida.");
        } else if (sinBarraFinal(u).endsWith("/api")) {
            errores.add("La dirección no debe terminar en /api: es la raíz del panel.");
        }
        String t = token == null ? "" : token.trim();
        if (t.isEmpty()) {
            errores.add("Falta el token.");
        } else if (tieneCaracterProhibido(token)) {
            errores.add("El token no puede tener espacios ni ; , \" \\.");
        }
        return errores;
    }

    private static boolean empiezaConEsquema(String u) {
        String minusculas = u.toLowerCase(Locale.ROOT);
        return minusculas.startsWith("http://") || minusculas.startsWith("https://");
    }

    private static boolean tieneHost(String u) {
        try {
            String host = new URI(u).getHost();
            return host != null && !host.isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static String sinBarraFinal(String u) {
        String res = u;
        while (res.endsWith("/")) res = res.substring(0, res.length() - 1);
        return res;
    }

    private static boolean tieneCaracterProhibido(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isWhitespace(c) || c == ';' || c == ',' || c == '"' || c == '\\') return true;
        }
        return false;
    }
}
