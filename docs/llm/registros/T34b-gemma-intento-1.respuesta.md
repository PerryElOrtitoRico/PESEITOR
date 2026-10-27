```java
// android/app/src/main/java/com/rutina/app/datos/Almacen.java
    /** El objeto de preferencias.json; vacío si no existe; si está dañado lo aparta y devuelve uno vacío. */
    JsonObject leerPreferencias() {
        Path p = archivoPreferencias();
        if (!Files.exists(p)) return new JsonObject();
        try {
            JsonElement e = JsonParser.parseString(leerTexto(p));
            if (e.isJsonObject()) return e.getAsJsonObject();
        } catch (IOException | JsonParseException ex) {
            // Fall through to apartarRoto
        }
        apartarRoto(p);
        return new JsonObject();
    }

    void guardarPreferencias(JsonObject preferencias) throws IOException {
        escribirAtomico(archivoPreferencias(), GSON.toJson(preferencias));
    }

    /** Id del plan activo; null si no hay. */
    public Long activo() {
        JsonObject o = leerPreferencias();
        if (!o.has("activo") || o.get("activo").isJsonNull()) return null;
        return o.get("activo").getAsLong();
    }

    /** Guarda el plan activo (null = ninguno). */
    public void activo(Long id) throws IOException {
        JsonObject o = leerPreferencias();
        if (id == null) o.remove("activo"); else o.addProperty("activo", id);
        guardarPreferencias(o);
    }

    /** Si el modo oscuro está encendido (por defecto no). */
    public boolean oscuro() {
        JsonObject o = leerPreferencias();
        return o.has("oscuro") && o.get("oscuro").getAsBoolean();
    }

    public void oscuro(boolean oscuro) throws IOException {
        JsonObject o = leerPreferencias();
        o.addProperty("oscuro", oscuro);
        guardarPreferencias(o);
    }
```

```java
// android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java
    @Test
    void preferencias() throws IOException {
        Almacen a = new Almacen(dir);
        assertFalse(a.oscuro());
        assertNull(a.activo());
        a.oscuro(true);
        a.activo(3L);
        Almacen otra = new Almacen(dir);
        assertTrue(otra.oscuro());
        assertEquals(3L, otra.activo());
        otra.activo(null);
        assertNull(new Almacen(dir).activo());
        assertTrue(new Almacen(dir).oscuro());
    }
```

<!-- 50 s · entrada 5719 · salida 614 · finish_reason stop -->
