package com.rutina.panel;

import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionRegistro;

/** Manda al panel las sesiones de un registro importado. Nunca lanza ni espera a la red. */
public final class EnvioRegistro {

    private EnvioRegistro() {
    }

    public record Resumen(boolean panelActivo, int enviadas, int sinDuracion, int fueraDelPlan, int otras) {
    }

    public static Resumen enviar(ConexionPanel panel, long perfilId, boolean esPerfilDelPanel, PlanCelular plan,
                                 RegistroCelular registro, int macro, int meso, long ahoraMs) {
        if (!esPerfilDelPanel || panel == null || !panel.activo()) return new Resumen(false, 0, 0, 0, 0);
        int enviadas = 0, sinDuracion = 0, fueraDelPlan = 0, otras = 0;
        if (registro != null && registro.sesiones() != null) {
            for (SesionRegistro sesion : registro.sesiones()) {
                try {
                    var datos = new SesionAPanel.Datos(perfilId, registro.mesocicloId(), macro, meso, panel.dispositivo());
                    var res = SesionAPanel.convertir(datos, plan, sesion, ahoraMs);
                    if (res.registro() != null) {
                        panel.publicar(res.registro());
                        enviadas++;
                    } else if ("sin duración".equals(res.omitida()) || "duración fuera de rango".equals(res.omitida())) {
                        sinDuracion++;
                    } else if ("fuera del plan".equals(res.omitida())) {
                        fueraDelPlan++;
                    } else {
                        otras++;
                    }
                } catch (RuntimeException e) {
                    otras++;
                }
            }
        }
        return new Resumen(true, enviadas, sinDuracion, fueraDelPlan, otras);
    }

    /** Texto para el aviso al usuario; null si el panel no está activo. */
    public static String mensaje(Resumen r) {
        if (!r.panelActivo()) return null;
        StringBuilder sb = new StringBuilder("Se mandaron ").append(sesiones(r.enviadas())).append(" al panel.");
        if (r.sinDuracion() > 0) {
            sb.append(' ').append(r.sinDuracion()).append(r.sinDuracion() == 1 ? " quedó" : " quedaron")
                    .append(" sin mandar por no tener duración.");
        }
        if (r.fueraDelPlan() > 0) {
            sb.append(' ').append(r.fueraDelPlan()).append(r.fueraDelPlan() == 1 ? " no está" : " no están")
                    .append(" en el plan.");
        }
        if (r.otras() > 0) {
            sb.append(' ').append(r.otras()).append(r.otras() == 1 ? " no se pudo" : " no se pudieron")
                    .append(" convertir.");
        }
        return sb.append(" Si el panel no responde, quedan en cola y se mandan cuando vuelva la conexión.").toString();
    }

    private static String sesiones(int n) {
        return n + (n == 1 ? " sesión" : " sesiones");
    }
}
