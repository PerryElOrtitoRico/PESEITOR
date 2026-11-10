package com.rutina.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** PDF del año planificado: las mismas filas que muestra el calendario de la pestaña Macrociclo. */
public final class ExportadorPdfAnio {

    private static final Font TITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 7, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 7);
    private static final Color FONDO = new Color(225, 225, 225);

    private ExportadorPdfAnio() {
    }

    public static void exportar(Path destino, String perfil, int anio, List<String> cabecera, List<String[]> filas)
            throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try (OutputStream out = Files.newOutputStream(destino)) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Paragraph t = new Paragraph("Macrociclo " + anio + " - " + perfil, TITULO);
            t.setSpacingAfter(6);
            doc.add(t);
            PdfPTable tabla = new PdfPTable(cabecera.size());
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            for (String c : cabecera) tabla.addCell(celda(c, NEGRITA, FONDO));
            for (String[] fila : filas) {
                for (String c : fila) tabla.addCell(celda(c, NORMAL, null));
            }
            doc.add(tabla);
            doc.close();
        } catch (DocumentException e) {
            throw new IOException("No se pudo armar el PDF: " + e.getMessage(), e);
        }
    }

    private static PdfPCell celda(String texto, Font f, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setPadding(2);
        if (fondo != null) c.setBackgroundColor(fondo);
        return c;
    }
}
