package com.rutina.panel;

import com.rutina.model.Ejercicio;
import com.rutina.model.PuntoRm;
import com.rutina.service.datos.EjercicioRepo;
import com.rutina.service.datos.EjercicioRepo.RmRegistrado;
import panel.cliente.Clave;
import panel.cliente.ConexionPanel;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manda al panel el historial de 1RM que ya existe en la base. */
public final class HistorialRm {

    private HistorialRm() {
    }

    public record Envio(int enviados, List<String> advertencias) {
    }

    /** Un registro por (ejercicio, fecha) con el último valor de ese día. Panel inactivo: no manda nada. */
    public static Envio enviarTodo(ConexionPanel panel, EjercicioRepo ejercicios, long perfilId, String dispositivo,
                                   long ahoraMs) throws SQLException {
        List<String> advertencias = new ArrayList<>();
        if (panel == null || !panel.activo()) return new Envio(0, advertencias);
        List<Ejercicio> todos = ejercicios.listar(perfilId);
        int enviados = 0;
        for (Ejercicio e : todos) {
            String clave = Clave.de(e.nombre());
            if (clave.isEmpty()) {
                advertencias.add("Sin clave utilizable para «" + e.nombre() + "»: no se mandó.");
                continue;
            }
            if (!RmAPanel.esRepresentante(e.id(), e.nombre(), todos)) {
                String representante = todos.stream()
                        .filter(o -> clave.equals(Clave.de(o.nombre()))).findFirst().orElse(e).nombre();
                advertencias.add("La clave 1rm:" + clave + " está repetida: se manda solo «" + representante
                        + "», no «" + e.nombre() + "».");
                continue;
            }
            Map<LocalDate, Double> ultimoDelDia = new LinkedHashMap<>();
            for (PuntoRm p : ejercicios.historial(e.id())) ultimoDelDia.put(p.fecha(), p.rm());
            for (var dia : ultimoDelDia.entrySet()) {
                var rm = new RmRegistrado(e.id(), perfilId, e.nombre(), dia.getKey(), dia.getValue());
                var registro = RmAPanel.aRegistro(rm, dispositivo, ahoraMs);
                if (registro.isPresent()) {
                    panel.publicar(registro.get());
                    enviados++;
                }
            }
        }
        return new Envio(enviados, advertencias);
    }
}
