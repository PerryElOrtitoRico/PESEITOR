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
