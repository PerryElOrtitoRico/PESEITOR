package com.rutina.app.ui;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Leer y escribir archivos que da el sistema (content://), siempre como texto UTF-8. */
final class Archivos {

    /** Un plan o un registro pesan unos pocos KB; más que esto es otro archivo. */
    private static final int MAXIMO = 5 * 1024 * 1024;

    private Archivos() {
    }

    static String leerTexto(ContentResolver cr, Uri uri) throws IOException {
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) throw new IOException("No se pudo abrir el archivo");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAXIMO) throw new IOException("El archivo es demasiado grande para ser un plan o un registro");
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    static void escribirTexto(ContentResolver cr, Uri uri, String texto) throws IOException {
        try (OutputStream out = cr.openOutputStream(uri)) {
            if (out == null) throw new IOException("No se pudo abrir el archivo para guardar");
            out.write(texto.getBytes(StandardCharsets.UTF_8));
        }
    }
}
