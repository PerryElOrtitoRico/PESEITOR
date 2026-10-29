package com.rutina.panel;

import java.util.List;

/** Textos de la ventana de conexión que dependen de datos; nunca incluyen el token. */
public final class TextosPanel {

    private TextosPanel() {
    }

    /** Aviso tras mandar el historial de 1RM: distingue lo entregado de lo que quedó en cola. */
    public static String historialEnviado(int valores, int pendientes, List<String> advertencias) {
        StringBuilder sb = new StringBuilder(pendientes > 0
                ? "Se pusieron " + valores + " valores de 1RM en la cola; el panel no respondió, "
                  + "se mandarán cuando vuelva la conexión."
                : "Se mandaron " + valores + " valores de 1RM.");
        for (String a : advertencias) sb.append('\n').append(a);
        return sb.toString();
    }

    /** Mensaje de una excepción, o el nombre de su clase si no tiene. */
    public static String causa(Throwable t) {
        if (t == null) return "error desconocido";
        String m = t.getMessage();
        return m == null || m.isBlank() ? t.getClass().getSimpleName() : m;
    }
}
