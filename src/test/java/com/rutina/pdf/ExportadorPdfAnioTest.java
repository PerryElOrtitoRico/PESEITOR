package com.rutina.pdf;

import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportadorPdfAnioTest {

    @Test
    void elAnioEntraEnUnaHojaConLosMesesCombinados(@TempDir Path dir) throws Exception {
        List<String[]> filas = new ArrayList<>();
        for (int i = 0; i < 53; i++) {
            filas.add(new String[]{"05/01/2026", i % 4 == 0 ? "MES " + i : "", "Fuerza", "Sentadilla (4s3d)", "1 / 4",
                    "250 @ 72%", "350 @ 76%", "Nivel 1 · semana 1 / 4"});
        }
        Path pdf = dir.resolve("anio.pdf");
        ExportadorPdfAnio.exportar(pdf, "Franco", 2026,
                List.of("Semana", "Mes", "Macrociclo", "Mesociclo", "Micro", "Potencia", "Fuerza", "Pliometría"), filas);
        assertEquals(1, new PdfReader(pdf.toString()).getNumberOfPages());
    }
}
