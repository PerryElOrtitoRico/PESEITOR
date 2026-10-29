package com.rutina.panel;

import com.rutina.registro.SerieReal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatoSeriesTest {

    @Test
    void agrupaSeriesIguales() {
        List<SerieReal> series = List.of(new SerieReal(5, 85), new SerieReal(5, 85), new SerieReal(5, 85),
                new SerieReal(3, 90), new SerieReal(5, 85));
        assertEquals("3 × 5 @ 85 kg · 1 × 3 @ 90 kg · 1 × 5 @ 85 kg", FormatoSeries.texto(series));
    }

    @Test
    void omiteElKgCuandoEsCero() {
        assertEquals("4 × 12", FormatoSeries.texto(List.of(new SerieReal(12, 0), new SerieReal(12, 0),
                new SerieReal(12, 0), new SerieReal(12, 0))));
    }

    @Test
    void kgConDecimalesUsaComa() {
        assertEquals("2 × 5 @ 82,5 kg · 1 × 5 @ 82,25 kg",
                FormatoSeries.texto(List.of(new SerieReal(5, 82.5), new SerieReal(5, 82.5), new SerieReal(5, 82.25))));
    }

    @Test
    void listaVaciaEsRaya() {
        assertEquals("—", FormatoSeries.texto(List.of()));
    }
}
