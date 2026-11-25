package com.rutina.service.calculo;

import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.VarianteMicro;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Reparto del volumen de un tipo entre microciclos y días (spec §6.1). */
public final class DistribucionVolumen {

    private DistribucionVolumen() {
    }

    public static List<PlanMicro> porMicrociclo(Metodologia m, ParametrosTipo p, int microciclos, char variante)
            throws ParametrosInvalidosException {
        String clave = "" + microciclos + Character.toUpperCase(variante);
        VarianteMicro v = m.variantes().get(clave);
        if (v == null) {
            throw new ParametrosInvalidosException("La variante " + clave + " no existe para " + microciclos + " microciclos.");
        }
        int[] volumenes = Redondeo.mayorResto(p.volumen(), v.volPct());
        List<PlanMicro> res = new ArrayList<>();
        List<String> fuera = new ArrayList<>();
        for (int i = 0; i < microciclos; i++) {
            int imr = p.imr() + v.deltaImr()[i];
            if (!m.ref().containsKey(imr)) fuera.add("el microciclo " + (i + 1) + " queda en " + imr + " %");
            res.add(new PlanMicro(volumenes[i], imr));
        }
        if (!fuera.isEmpty()) {
            throw new ParametrosInvalidosException("Con IMR " + p.imr() + " % y la variante " + clave + ", "
                    + String.join(" y ", fuera) + ": fuera de la tabla de distribución ("
                    + minimo(m) + "–" + maximo(m) + " %).");
        }
        return res;
    }

    /** Fracción del volumen del microciclo para cada día; los días sin el tipo quedan en 0. */
    public static double[] pesosDias(Metodologia m, int dias, Set<Integer> diasConTipo) {
        double[] base = m.diaPct().get(dias);
        double[] res = new double[dias];
        double suma = 0;
        for (int d = 1; d <= dias; d++) {
            if (diasConTipo.contains(d)) suma += base[d - 1];
        }
        if (suma <= 0) return res;
        for (int d = 1; d <= dias; d++) {
            if (diasConTipo.contains(d)) res[d - 1] = base[d - 1] / suma;
        }
        return res;
    }

    /** Σ Vol% × ΔIMR en puntos de IMR: 0 si los días se compensan y la semana queda en el IMR del microciclo. */
    public static double desvioImrDias(double[] pesos, int[] deltas) {
        double s = 0;
        for (int d = 0; d < pesos.length; d++) s += pesos[d] * deltas[d];
        return s;
    }

    /**
     * IMR de cada día: el del microciclo más su ΔIMR. Si un día se sale de la tabla queda en el límite, y los
     * demás días con el tipo suben o bajan de a un punto, repartido, para que el promedio ponderado no cambie.
     */
    public static int[] imrDias(Metodologia m, int imr, double[] pesos, int[] deltas) {
        int min = minimo(m), max = maximo(m);
        int[] res = new int[pesos.length];
        boolean[] recortado = new boolean[pesos.length];
        double falta = 0;
        for (int d = 0; d < pesos.length; d++) {
            int crudo = imr + deltas[d];
            res[d] = Math.max(min, Math.min(max, crudo));
            recortado[d] = res[d] != crudo;
            falta += pesos[d] * (crudo - res[d]);
        }
        int[] ajuste = new int[pesos.length];
        while (true) {
            int paso = falta > 0 ? 1 : -1;
            int elegido = -1;
            for (int d = 0; d < pesos.length; d++) {
                if (pesos[d] <= 0 || recortado[d] || res[d] + paso < min || res[d] + paso > max) continue;
                if (Math.abs(falta - paso * pesos[d]) >= Math.abs(falta) - 1e-9) continue;
                // el día menos tocado hasta ahora; a igualdad, el de más volumen
                if (elegido < 0 || Math.abs(ajuste[d]) < Math.abs(ajuste[elegido])
                        || (Math.abs(ajuste[d]) == Math.abs(ajuste[elegido]) && pesos[d] > pesos[elegido])) elegido = d;
            }
            if (elegido < 0) return res;
            res[elegido] += paso;
            ajuste[elegido] += paso;
            falta -= paso * pesos[elegido];
        }
    }

    /** El IMR recortado al rango de la tabla de distribución. */
    public static int imrEnTabla(Metodologia m, int imr) {
        return Math.max(minimo(m), Math.min(maximo(m), imr));
    }

    private static int minimo(Metodologia m) {
        return m.ref().keySet().stream().mapToInt(Integer::intValue).min().orElse(0);
    }

    private static int maximo(Metodologia m) {
        return m.ref().keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }
}
