package com.rutina.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rutina.model.CeldaPlantilla;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.Zona;
import com.rutina.service.calculo.DiaPlio;
import com.rutina.service.calculo.GeneradorSeries;
import com.rutina.service.calculo.Pliometria;
import com.rutina.service.calculo.Redondeo;
import com.rutina.service.calculo.ResultadoMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo.ResultadoBloque;
import com.rutina.service.calculo.ResultadoMesociclo.SemanaBloque;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** PDF para el gimnasio, con el formato de la planilla del entrenador (spec §7). */
public final class ExportadorPdf {

    /** Filas del PDF: zonas que se muestran juntas y su etiqueta en %. */
    private record Fila(List<Zona> zonas, double pctMin, double pctMax, String etiquetaPct) {
    }

    private static final List<Fila> FILAS = List.of(
            new Fila(List.of(Zona.Z55), 55, 55, "55%"),
            new Fila(List.of(Zona.Z60), 60, 60, "60%"),
            new Fila(List.of(Zona.Z65), 65, 65, "65%"),
            new Fila(List.of(Zona.Z70), 70, 70, "70%"),
            new Fila(List.of(Zona.Z75), 75, 75, "75%"),
            new Fila(List.of(Zona.Z80, Zona.Z85), 80, 87.5, "80-87,5%"),
            new Fila(List.of(Zona.Z90, Zona.Z95), 90, 95, "90-95%"),
            new Fila(List.of(Zona.Z100), 100, 100, "100%"));

    private static final Font TITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 7, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 7);
    private static final Font GRIS = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);
    private static final Color FONDO = new Color(225, 225, 225);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat KG = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR")));

    private ExportadorPdf() {
    }

    public static void exportar(Path destino, String perfil, Mesociclo mes, ResultadoMesociclo res,
                                List<PlanConDias> planes, Metodologia m) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try (OutputStream out = Files.newOutputStream(destino)) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            paginaGimnasio(doc, perfil, mes, res, m);
            for (PlanConDias plan : planes) {
                doc.newPage();
                paginaPliometria(doc, plan);
            }
            doc.close();
        } catch (DocumentException e) {
            throw new IOException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private static void paginaGimnasio(Document doc, String perfil, Mesociclo mes, ResultadoMesociclo res, Metodologia m)
            throws DocumentException {
        doc.add(new Paragraph("PESEITOR · " + perfil + " — mesociclo del " + mes.inicio().format(FECHA) + " al "
                + mes.fin().minusDays(1).format(FECHA) + " (" + mes.microciclos() + " microciclos, variante "
                + mes.microciclos() + mes.variante() + ")", TITULO));
        if (!vacio(mes.calentamiento())) doc.add(new Paragraph("Calentamiento: " + mes.calentamiento(), NORMAL));

        int dias = mes.dias();
        int filas = res.bloques().stream().mapToInt(b -> b.bloque().posicion()).max().orElse(0);
        PdfPTable grilla = new PdfPTable(dias);
        grilla.setWidthPercentage(100);
        grilla.setSpacingBefore(6);
        grilla.setHeaderRows(1);
        for (int d = 1; d <= dias; d++) grilla.addCell(celda("DÍA " + d, NEGRITA, FONDO));
        for (int pos = 1; pos <= filas; pos++) {
            for (int d = 1; d <= dias; d++) {
                ResultadoBloque rb = buscar(res, d, pos);
                PdfPCell c = rb == null ? new PdfPCell(new Phrase("")) : new PdfPCell(tablaBloque(rb, pos, mes, m));
                c.setBorder(Rectangle.NO_BORDER);
                c.setPadding(2);
                grilla.addCell(c);
            }
        }
        doc.add(grilla);
        if (!vacio(mes.auxiliares())) {
            Paragraph aux = new Paragraph("Auxiliares: " + mes.auxiliares(), NORMAL);
            aux.setSpacingBefore(6);
            doc.add(aux);
        }
    }

    private static PdfPTable tablaBloque(ResultadoBloque rb, int pos, Mesociclo mes, Metodologia m) {
        int semanas = rb.semanas().size();
        PdfPTable t = new PdfPTable(semanas + 1);
        t.setWidthPercentage(100);
        PdfPCell titulo = celda("E" + pos + "  " + rb.ejercicio().nombre(), NEGRITA, FONDO);
        titulo.setColspan(semanas + 1);
        t.addCell(titulo);
        t.addCell(celda("KG", NEGRITA, FONDO));
        for (int w = 1; w <= semanas; w++) t.addCell(celda("S" + w, NEGRITA, FONDO));
        t.addCell(celda("Vol", NEGRITA, null));
        for (SemanaBloque s : rb.semanas()) t.addCell(celda(String.valueOf(s.volumen()), NORMAL, null));

        Double rm = mes.rmUsados().getOrDefault(rb.ejercicio().id(), rb.ejercicio().rm());
        boolean plantilla = rb.semanas().stream().anyMatch(s -> s.celda() != null);
        if (plantilla) {
            t.addCell(celda("Series", NEGRITA, null));
            for (SemanaBloque s : rb.semanas()) {
                t.addCell(celda(s.celda() == null ? "" : textoCelda(s.celda(), rm, m), NORMAL, null));
            }
            return t;
        }
        for (Fila f : FILAS) {
            List<String> textos = new ArrayList<>();
            for (SemanaBloque s : rb.semanas()) textos.add(notacionFila(s.seriesPorZona(), f));
            if (textos.stream().allMatch(String::isEmpty)) continue;
            t.addCell(celda(etiqueta(f, rm, m), NEGRITA, null));
            for (String tx : textos) t.addCell(celda(tx, NORMAL, null));
        }
        return t;
    }

    private static String notacionFila(Map<Zona, List<List<Integer>>> series, Fila f) {
        List<String> partes = new ArrayList<>();
        for (Zona z : f.zonas()) {
            List<List<Integer>> l = series.get(z);
            if (l == null || l.isEmpty() || l.get(0).isEmpty()) continue;
            partes.add(GeneradorSeries.notacion(l.get(0)));
        }
        return String.join(".", partes);
    }

    private static String etiqueta(Fila f, Double rm, Metodologia m) {
        if (rm == null) return f.etiquetaPct();
        double min = Redondeo.kg(rm, f.pctMin(), m.incrementoKg());
        double max = Redondeo.kg(rm, f.pctMax(), m.incrementoKg());
        return min == max ? KG.format(min) : KG.format(min) + "-" + KG.format(max);
    }

    private static String textoCelda(CeldaPlantilla c, Double rm, Metodologia m) {
        String base = c.series() + "x" + c.reps();
        if (c.pctMin() == null) return base;
        if (rm == null) return c.texto();
        double min = Redondeo.kg(rm, c.pctMin(), m.incrementoKg());
        double max = Redondeo.kg(rm, c.pctMax(), m.incrementoKg());
        return base + " · " + (min == max ? KG.format(min) : KG.format(min) + "-" + KG.format(max));
    }

    private static void paginaPliometria(Document doc, PlanConDias p) throws DocumentException {
        doc.add(new Paragraph("Pliometría — nivel " + p.plan().nivel() + ", desde el "
                + p.plan().inicio().format(FECHA) + " (" + p.semanas().size() + " semanas, "
                + Pliometria.SALTOS_POR_SERIE + " saltos por serie)", TITULO));
        int dias = p.semanas().get(0).size();
        PdfPTable grilla = new PdfPTable(dias);
        grilla.setWidthPercentage(100);
        grilla.setSpacingBefore(6);
        for (int d = 0; d < dias; d++) {
            PdfPTable t = new PdfPTable(new float[]{4, 1, 1, 1, 1});
            t.setWidthPercentage(100);
            PdfPCell titulo = celda("DÍA " + (d + 1), NEGRITA, FONDO);
            titulo.setColspan(5);
            t.addCell(titulo);
            t.addCell(celda("Ejercicio", NEGRITA, FONDO));
            for (int w = 0; w < p.semanas().size(); w++) {
                Font f = p.semanasDentroDelMesociclo().contains(w + 1) ? NEGRITA : GRIS;
                t.addCell(celda("S" + (w + 1) + " (" + p.semanas().get(w).get(d).contactos() + ")", f, FONDO));
            }
            List<String> ejercicios = p.semanas().get(0).get(d).ejercicios();
            for (String ej : ejercicios) {
                t.addCell(celda(ej, NORMAL, null));
                for (int w = 0; w < p.semanas().size(); w++) {
                    DiaPlio dia = p.semanas().get(w).get(d);
                    Font f = p.semanasDentroDelMesociclo().contains(w + 1) ? NORMAL : GRIS;
                    t.addCell(celda(dia.series() + " x " + Pliometria.SALTOS_POR_SERIE, f, null));
                }
            }
            PdfPCell c = new PdfPCell(t);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(2);
            grilla.addCell(c);
        }
        doc.add(grilla);
    }

    private static ResultadoBloque buscar(ResultadoMesociclo res, int dia, int pos) {
        return res.bloques().stream()
                .filter(b -> b.bloque().dia() == dia && b.bloque().posicion() == pos)
                .findFirst().orElse(null);
    }

    private static PdfPCell celda(String texto, Font f, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(2);
        if (fondo != null) c.setBackgroundColor(fondo);
        return c;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
