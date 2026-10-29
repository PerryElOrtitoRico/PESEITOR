package com.rutina.panel;

import com.rutina.registro.SerieReal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Texto de una lista de series para el panel: {@code 3 × 5 @ 85 kg · 3 × 3 @ 90 kg}. */
public final class FormatoSeries {

    private FormatoSeries() {
    }

    /** Agrupa series iguales consecutivas; lista vacía → {@code —}. */
    public static String texto(List<SerieReal> series) {
        if (series.isEmpty()) return "—";
        List<String> grupos = new ArrayList<>();
        int i = 0;
        while (i < series.size()) {
            SerieReal s = series.get(i);
            int j = i;
            while (j < series.size() && series.get(j).equals(s)) j++;
            String grupo = (j - i) + " × " + s.reps();
            if (s.kg() != 0) grupo += " @ " + kg(s.kg()) + " kg";
            grupos.add(grupo);
            i = j;
        }
        return String.join(" · ", grupos);
    }

    /** Kilos como texto: enteros sin decimales, si no con coma decimal. */
    static String kg(double kg) {
        return BigDecimal.valueOf(kg).stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
