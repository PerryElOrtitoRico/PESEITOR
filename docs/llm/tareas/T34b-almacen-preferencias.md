<!-- contrato: 10,14 | archivos: android/app/src/main/java/com/rutina/app/datos/Almacen.java, android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java -->
# T34b — Almacén: preferencias (activo y modo oscuro)

## Objetivo
Devolvé DOS bloques: uno con los métodos de `Almacen.java` y otro con los tests de `AlmacenTest.java`.
Completar en `Almacen` solo los métodos marcados `// TODO T34b`: `leerPreferencias`, `guardarPreferencias`, `activo()`, `activo(Long)`, `oscuro()` y `oscuro(boolean)`; y el test `preferencias` de `AlmacenTest`. No toques los demás métodos ni los imports.

## Código que ya existe
- `archivoPreferencias()` → `Path` de `preferencias.json`; `leerTexto(Path)`, `escribirAtomico(Path, String)` y `apartarRoto(Path)` ya andan; la constante `GSON` (con `toJson`).
- Gson: `JsonObject` (`has`, `get`, `add`, `addProperty`, `remove`), `JsonElement` (`isJsonNull`, `getAsLong`, `getAsBoolean`, `isJsonObject`, `getAsJsonObject`), `JsonParser.parseString(String)`.

## `leerPreferencias()` (pasos en orden)
1. `Path p = archivoPreferencias();`
2. `if (!Files.exists(p)) return new JsonObject();`
3. `try {`
4. `    JsonElement e = JsonParser.parseString(leerTexto(p));`
5. `    if (e.isJsonObject()) return e.getAsJsonObject();`
6. `} catch (IOException | JsonParseException ex) {`
7. `}`
8. `apartarRoto(p);`
9. `return new JsonObject();`

## `guardarPreferencias(JsonObject preferencias)` (pasos en orden)
1. `escribirAtomico(archivoPreferencias(), GSON.toJson(preferencias));`

## `activo()` (pasos en orden)
1. `JsonObject o = leerPreferencias();`
2. `if (!o.has("activo") || o.get("activo").isJsonNull()) return null;`
3. `return o.get("activo").getAsLong();`

## `activo(Long id)` (pasos en orden)
1. `JsonObject o = leerPreferencias();`
2. `if (id == null) o.remove("activo"); else o.addProperty("activo", id);`
3. `guardarPreferencias(o);`

## `oscuro()` (pasos en orden)
1. `JsonObject o = leerPreferencias();`
2. `return o.has("oscuro") && o.get("oscuro").getAsBoolean();`

## `oscuro(boolean oscuro)` (pasos en orden)
1. `JsonObject o = leerPreferencias();`
2. `o.addProperty("oscuro", oscuro);`
3. `guardarPreferencias(o);`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `preferencias`:
  - `Almacen a = new Almacen(dir);`
  - `assertFalse(a.oscuro());`
  - `assertNull(a.activo());`
  - `a.oscuro(true);`
  - `a.activo(3L);`
  - `Almacen otra = new Almacen(dir);`
  - `assertTrue(otra.oscuro());`
  - `assertEquals(3L, otra.activo());`
  - `otra.activo(null);`
  - `assertNull(new Almacen(dir).activo());`
  - `assertTrue(new Almacen(dir).oscuro());`
