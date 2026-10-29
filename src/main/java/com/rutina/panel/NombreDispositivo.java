package com.rutina.panel;

import java.util.Map;

/** Nombre por defecto del dispositivo, sin consultar la red (un DNS lento no debe congelar la ventana). */
public final class NombreDispositivo {

    private NombreDispositivo() {
    }

    public static String porDefecto(Map<String, String> entorno) {
        for (String variable : new String[]{"HOSTNAME", "COMPUTERNAME"}) {
            String valor = entorno.get(variable);
            if (valor != null && !valor.isBlank()) return valor.trim();
        }
        return "pc";
    }
}
