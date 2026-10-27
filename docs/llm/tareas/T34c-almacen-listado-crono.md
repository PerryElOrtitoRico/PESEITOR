<!-- contrato: 10,14 | archivos: android/app/src/main/java/com/rutina/app/datos/Almacen.java, android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java -->
# T34c — Almacén: lista de planes, borrar un plan y cronómetros

## Objetivo
Devolvé DOS bloques: uno con los métodos de `Almacen.java` y otro con los tests de `AlmacenTest.java`.
Completar en `Almacen` solo los métodos marcados `// TODO T34c`: `planes()`, `borrarPlan(long)`, `crono(long, int, int)` y `crono(long, int, int, EstadoCrono)`; y los tests `listaYBorraPlanes` y `cronos` de `AlmacenTest`. No toques los demás métodos ni los imports.

## Código que ya existe
- `raiz` (`Path`), `archivoPlan(long)`, `archivoRegistro(long)`, `leerPlan(Path)` (devuelve `null` si el archivo no existe o está roto), `leerPreferencias()` → `JsonObject`, `guardarPreferencias(JsonObject)`.
- `record EstadoCrono(long acumuladoMs, Long desde)` y `Crono.nuevo()`. Las preferencias guardan los cronómetros en el objeto `"cronos"`: la clave es `"<id>-<micro>-<dia>"` y el valor un objeto `{"acumuladoMs": número, "desde": número o null}`.
- Gson `JsonObject`: `has`, `get`, `getAsJsonObject(clave)` (null si no está), `add`, `addProperty`, `remove`, `keySet()`.

## `planes()` (pasos en orden)
1. `List<PlanCelular> res = new ArrayList<>();`
2. `Path carpeta = raiz.resolve("planes");`
3. `if (!Files.isDirectory(carpeta)) return res;`
4. `List<Path> archivos = new ArrayList<>();`
5. `try (DirectoryStream<Path> ds = Files.newDirectoryStream(carpeta, "*.json")) { for (Path p : ds) archivos.add(p); } catch (IOException e) { return res; }`
6. `for (Path p : archivos) { PlanCelular plan = leerPlan(p); if (plan != null) res.add(plan); }`
7. `res.sort(Comparator.comparing(PlanCelular::inicio));`
8. `return res;`

## `borrarPlan(long id)` (pasos en orden)
1. `Files.deleteIfExists(archivoPlan(id));`
2. `Files.deleteIfExists(archivoRegistro(id));`
3. `JsonObject o = leerPreferencias();`
4. `if (o.has("cronos")) { JsonObject cronos = o.getAsJsonObject("cronos"); for (String k : new ArrayList<>(cronos.keySet())) { if (k.startsWith(id + "-")) cronos.remove(k); } }`
5. `if (o.has("activo") && !o.get("activo").isJsonNull() && o.get("activo").getAsLong() == id) o.remove("activo");`
6. `guardarPreferencias(o);`

## `crono(long id, int micro, int dia)` (pasos en orden)
1. `String clave = id + "-" + micro + "-" + dia;`
2. `JsonObject cronos = leerPreferencias().getAsJsonObject("cronos");`
3. `if (cronos == null || !cronos.has(clave)) return Crono.nuevo();`
4. `JsonObject c = cronos.getAsJsonObject(clave);`
5. `Long desde = c.get("desde").isJsonNull() ? null : c.get("desde").getAsLong();`
6. `return new EstadoCrono(c.get("acumuladoMs").getAsLong(), desde);`

## `crono(long id, int micro, int dia, EstadoCrono estado)` (pasos en orden)
1. `String clave = id + "-" + micro + "-" + dia;`
2. `JsonObject o = leerPreferencias();`
3. `JsonObject cronos = o.has("cronos") ? o.getAsJsonObject("cronos") : new JsonObject();`
4. `JsonObject c = new JsonObject();`
5. `c.addProperty("acumuladoMs", estado.acumuladoMs());`
6. `c.addProperty("desde", estado.desde());`
7. `cronos.add(clave, c);`
8. `o.add("cronos", cronos);`
9. `guardarPreferencias(o);`

## Tests (copiá estas aserciones tal cual, sin comentarios; `ponerPlan(id, inicio)` y `cuantos(carpeta, prefijo)` ya están en el archivo)
- `listaYBorraPlanes`:
  - `Almacen a = new Almacen(dir);`
  - `assertEquals(0, a.planes().size());`
  - `ponerPlan(5, "2026-12-01");`
  - `ponerPlan(2, "2026-10-26");`
  - `assertEquals(2L, a.planes().get(0).mesocicloId());`
  - `assertEquals(5L, a.planes().get(1).mesocicloId());`
  - `a.activo(2L);`
  - `a.borrarPlan(2);`
  - `assertEquals(1, a.planes().size());`
  - `assertNull(a.activo());`
  - `assertEquals(0, cuantos("planes", "2."));`
- `cronos`:
  - `Almacen a = new Almacen(dir);`
  - `assertEquals(Crono.nuevo(), a.crono(3, 1, 1));`
  - `a.crono(3, 1, 1, new EstadoCrono(60_000, 1_000L));`
  - `a.crono(3, 1, 2, new EstadoCrono(5_000, null));`
  - `assertEquals(new EstadoCrono(60_000, 1_000L), new Almacen(dir).crono(3, 1, 1));`
  - `assertEquals(new EstadoCrono(5_000, null), new Almacen(dir).crono(3, 1, 2));`
  - `assertEquals(Crono.nuevo(), new Almacen(dir).crono(4, 1, 1));`
