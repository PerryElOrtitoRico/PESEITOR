// android/app/src/main/java/com/rutina/app/datos/Almacen.java
    static String leerTexto(Path archivo) throws IOException {
        return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
    }

    static void escribirAtomico(Path destino, String texto) throws IOException {
        Files.createDirectories(destino.getParent());
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.write(tmp, texto.getBytes(StandardCharsets.UTF_8));
        Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    static void apartarRoto(Path archivo) {
        Path roto = archivo.resolveSibling(archivo.getFileName() + ".roto-" + System.currentTimeMillis());
        try { Files.move(archivo, roto, StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) { }
    }

    static PlanCelular leerPlan(Path archivo) {
        if (!Files.exists(archivo)) return null;
        try { return JsonRegistro.planDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }
    }

    static RegistroCelular leerRegistro(Path archivo) {
        if (!Files.exists(archivo)) return null;
        try { return JsonRegistro.registroDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }
    }

<!-- 34 s · entrada 4703 · salida 329 · finish_reason stop -->
