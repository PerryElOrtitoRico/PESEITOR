```java
// android/app/src/main/java/com/rutina/app/datos/Almacen.java
    public List<PlanCelular> planes() {
        List<PlanCelular> res = new ArrayList<>();
        Path carpeta = raiz.resolve("planes");
        if (!Files.isDirectory(carpeta)) return res;
        List<Path> archivos = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(carpeta, "*.json")) {
            for (Path p : ds) archivos.add(p);
        } catch (IOException e) {
            return res;
        }
        for (Path p : archivos) {
            PlanCelular plan = leerPlan(p);
            if (plan != null) res.add(plan);
        }
        res.sort(Comparator.comparing(PlanCelular::inicio));
        return res;
    }

    public void borrarPlan(long id) throws IOException {
        Files.deleteIfExists(archivoPlan(id));
        Files.deleteIfExists(archivoRegistro(id));
        JsonObject o = leerPreferencias();
        if (o.has("cronos")) {
            JsonObject cronos = o.getAsJsonObject("cronos");
            for (String k : new ArrayList<>(cronos.keySet())) {
                if (k.startsWith(id + "-")) cronos.remove(k);
            }
        }
        if (o.has("activo") && !o.get("activo").isJsonNull() && o.get("activo").getAsLong() == id) o.remove("activo");
        guardarPreferencias(o);
    }

    public EstadoCrono crono(long id, int micro, int dia) {
        String clave = id + "-" + micro + "-" + dia;
        JsonObject cronos = leerPreferencias().getAsJsonObject("cronos");
        if (cronos == null || !cronos.has(clave)) return Crono.nuevo();
        JsonObject c = cronos.getAsJsonObject(clave);
        Long desde = c.get("desde").isJsonNull() ? null : c.get("desde").getAsLong();
        return new EstadoCrono(c.get("acumuladoMs").getAsLong(), desde);
    }

    public void crono(long id, int micro, int dia, EstadoCrono estado) throws IOException {
        String clave = id + "-" + micro + "-" + dia;
        JsonObject o = leerPreferencias();
        JsonObject cronos = o.has("cronos") ? o.getAsJsonObject("cronos") : new JsonObject();
        JsonObject c = new JsonObject();
        c.addProperty("acumuladoMs", estado.acumuladoMs());
        c.addProperty("desde", estado.desde());
        cronos.add(clave, c);
        o.add("cronos", cronos);
        guardarPreferencias(o);
    }
```

```java
// android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java
    @Test
    void listaYBorraPlanes() throws IOException {
        Almacen a = new Almacen(dir);
        assertEquals(0, a.planes().size());
        ponerPlan(5, "2026-12-01");
        ponerPlan(2, "2026-10-26");
        assertEquals(2L, a.planes().get(0).mesocicloId());
        assertEquals(5L, a.planes().get(1).mesocicloId());
        a.activo(2L);
        a.borrarPlan(2);
        assertEquals(1, a.planes().size());
        assertNull(a.activo());
        assertEquals(0, cuantos("planes", "2."));
    }

    @Test
    void cronos() throws IOException {
        Almacen a = new Almacen(dir);
        assertEquals(Crono.nuevo(), a.crono(3, 1, 1));
        a.crono(3, 1, 1, new EstadoCrono(60_000, 1_000L));
        a.crono(3, 1, 2, new EstadoCrono(5_000, null));
        assertEquals(new EstadoCrono(60_000, 1_000L), new Almacen(dir).crono(3, 1, 1));
        assertEquals(new EstadoCrono(5_000, null), new Almacen(dir).crono(3, 1, 2));
        assertEquals(Crono.nuevo(), new Almacen(dir).crono(4, 1, 1));
    }
```

<!-- 90 s · entrada 6594 · salida 1088 · finish_reason stop -->
