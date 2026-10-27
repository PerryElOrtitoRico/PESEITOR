```java
// android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java
    @Test
    void escribeYRelee() throws IOException {
        Path destino = dir.resolve("a").resolve("x.json");
        Almacen.escribirAtomico(destino, "uno");
        assertEquals("uno", Almacen.leerTexto(destino));
        Almacen.escribirAtomico(destino, "dos");
        assertEquals("dos", Almacen.leerTexto(destino));
        assertEquals(1, cuantos("a", ""));
    }

    @Test
    void apartaElRoto() throws IOException {
        Files.createDirectories(dir.resolve("planes"));
        Files.write(dir.resolve("planes").resolve("9.json"), "{roto".getBytes(StandardCharsets.UTF_8));
        assertNull(Almacen.leerPlan(dir.resolve("planes").resolve("9.json")));
        assertFalse(Files.exists(dir.resolve("planes").resolve("9.json")));
        assertEquals(1, cuantos("planes", "9.json.roto-"));
        assertNull(Almacen.leerRegistro(dir.resolve("registros").resolve("1.json")));
    }
```

<!-- 25 s · entrada 3029 · salida 254 · finish_reason stop -->
