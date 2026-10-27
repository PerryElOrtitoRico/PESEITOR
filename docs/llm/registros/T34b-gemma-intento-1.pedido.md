<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 2268, razonamiento no -->

## system

Sos un programador Java 17 cuidadoso (la app corre en Android 8 o más: nada de Files.readString/writeString ni Path.of). Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Los datos son records y se leen con x(): s.microciclo(), e.filas(), s.sensacionFinal(). No existen getMicrociclo() ni getFilas().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé solo los métodos que completaste, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea. Ejemplo:
```java
// android/app/src/main/java/com/rutina/app/logica/Ejemplo.java
    public static int doble(int x) {
        return 2 * x;
    }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# RUTINEITOR — mejoras de octubre (contrato)

## 10. App de Android: reglas generales
La app nativa vive en `android/` (Gradle, Java 17, AndroidX; minSdk 26). Reemplaza a `celular/`, con la misma lógica en Java. La lógica va en `com.rutina.app.logica` (sesión, totales, cronómetro) y `com.rutina.app.datos` (archivos), **sin tocar nada de `android.*`**: se prueba con JUnit 5 en la JVM.
- Los records del registro se comparten con la PC y se leen con `x()`: `PlanCelular(formato, version, perfil, mesocicloId, inicio, microciclos, dias, sesiones)`, `SesionPlan(microciclo, dia, ejercicios)`, `EjercicioPlan(bloque, nombre, tipo, modo, rm, filas)`, `FilaPlan(etiqueta, pctMin, pctMax, series)`, `SerieReal(reps, kg)`, `RegistroCelular(formato, version, perfil, mesocicloId, sesiones)`, `SesionRegistro(microciclo, dia, fecha, descanso, sensacionFinal, ejercicios, duracionMin)`, `EjercicioRegistro(bloque, esfuerzo, filas, nota)`. `NotacionRegistro.parse(texto, planificadas)` y `NotacionRegistro.esValida(texto, planificadas)` ya existen. No existen `getX()`.
- Usar solo la API que hay en Android 8 (nivel 26): `java.nio.file.Files.readAllBytes/write/move/newDirectoryStream`, `java.nio.file.Paths.get`. **No** `Files.readString/writeString` ni `Path.of`.
- Los datos que reciben y devuelven las funciones son inmutables: nunca modificar un argumento; devolver listas y mapas nuevos.
- Es la traducción de `celular/logica.js`: las reglas son las mismas que las de las secciones 5 a 7 de este contrato.

## 14. Android: almacenamiento (`Almacen`)
Clase `com.rutina.app.datos.Almacen`, sobre una carpeta raíz (en el celular, el almacenamiento interno de la app). Disposición: `planes/<mesocicloId>.json`, `registros/<mesocicloId>.json`, `preferencias.json`. Los planes y registros se guardan con `JsonRegistro.planAJson` / `registroAJson` (el mismo formato que se importa y se exporta).
- `Almacen(Path raiz)`.
- **Escritura atómica**: escribir en un archivo temporal de la misma carpeta (`<nombre>.tmp`) y moverlo con `Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)`. Crear la carpeta si no existe.
- **Archivo dañado**: si un archivo de `planes/` o `registros/` no se puede leer (JSON roto o formato inválido), se lo renombra a `<nombre>.roto-<epoch ms>` en la misma carpeta y se sigue como si no existiera; nunca se cierra la app ni se pierden los demás archivos.
- `cargarDesdeTexto(String json)` → `Object`: lee el campo `"formato"` del JSON (no el nombre del archivo, que puede no tener `.json`; el texto puede traer espacios o saltos de línea alrededor). `"rutineitor-plan"` → valida con `JsonRegistro.planDeJson`, lo guarda (reemplaza el de igual `mesocicloId`, **conservando su registro**) y devuelve el `PlanCelular`. `"rutineitor-registro"` → valida con `JsonRegistro.registroDeJson`; si no hay un plan guardado con su `mesocicloId` lanza `IllegalArgumentException("Primero cargá el plan de este registro: no hay ningún plan con el id " + mesocicloId)`; si lo hay, guarda ese registro (reemplaza el que había) y lo devuelve. El texto puede traer una marca BOM (`\uFEFF`) o espacios al principio y al final: se descartan. Cualquier otro caso (JSON roto, otro formato) lanza `IllegalArgumentException` con un mensaje en español.
- `planes()` → `List<PlanCelular>` ordenados por `inicio` (texto `AAAA-MM-DD`); `plan(long id)` → `PlanCelular` o `null`; `borrarPlan(long id)` borra el plan, su registro y sus cronómetros, y si era el plan activo deja `activo()` en `null`.
- `registro(long id)` → el `RegistroCelular` guardado, o `Sesiones.registroVacio(plan)` si no hay (y `null` si tampoco hay plan); `guardarRegistro(RegistroCelular r)`.
- Preferencias (`preferencias.json`, objeto JSON): `activo()` → `Long` (id del plan activo, `null` si no hay) y `activo(Long id)`; `oscuro()` → `boolean` (por defecto `false`) y `oscuro(boolean)`; `crono(long id, int micro, int dia)` → `EstadoCrono` (`Crono.nuevo()` si no hay) y `crono(long id, int micro, int dia, EstadoCrono e)`, guardado con la clave `"<id>-<micro>-<dia>"`.

---

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

---

# Archivos actuales

```java
// android/app/src/main/java/com/rutina/app/datos/Almacen.java
package com.rutina.app.datos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.rutina.app.logica.Crono;
import com.rutina.app.logica.EstadoCrono;
import com.rutina.app.logica.Sesiones;
import com.rutina.registro.JsonRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Planes, registros y preferencias en archivos JSON dentro de una carpeta raíz:
 * {@code planes/<id>.json}, {@code registros/<id>.json} y {@code preferencias.json}.
 * Escritura atómica (temporal + mover) y, si un archivo está dañado, se lo aparta como {@code <nombre>.roto-<epoch ms>}.
 */
public class Almacen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private final Path raiz;

    public Almacen(Path raiz) {
        this.raiz = raiz;
    }

    // ---------- rutas ----------

    Path archivoPlan(long id) {
        return raiz.resolve("planes").resolve(id + ".json");
    }

    Path archivoRegistro(long id) {
        return raiz.resolve("registros").resolve(id + ".json");
    }

    Path archivoPreferencias() {
        return raiz.resolve("preferencias.json");
    }

    // ---------- disco (T34a) ----------

    /** Lee un archivo completo como texto UTF-8. */
    static String leerTexto(Path archivo) throws IOException {
        return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
    }

    /** Escribe el texto en un temporal de la misma carpeta y lo mueve sobre el destino (crea la carpeta si falta). */
    static void escribirAtomico(Path destino, String texto) throws IOException {
        Files.createDirectories(destino.getParent());
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.write(tmp, texto.getBytes(StandardCharsets.UTF_8));
        Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Renombra un archivo dañado a {@code <nombre>.roto-<epoch ms>} (misma carpeta). Si no puede, no hace nada. */
    static void apartarRoto(Path archivo) {
        Path roto = archivo.resolveSibling(archivo.getFileName() + ".roto-" + System.currentTimeMillis());
        try { Files.move(archivo, roto, StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) { }
    }

    /** El plan guardado en ese archivo; null si no existe; si está dañado lo aparta y devuelve null. */
    static PlanCelular leerPlan(Path archivo) {
        if (!Files.exists(archivo)) return null;
        try { return JsonRegistro.planDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }
    }

    /** El registro guardado en ese archivo; null si no existe; si está dañado lo aparta y devuelve null. */
    static RegistroCelular leerRegistro(Path archivo) {
        if (!Files.exists(archivo)) return null;
        try { return JsonRegistro.registroDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }
    }

    // ---------- planes y registros ----------

    /** El plan con ese id, o null. */
    public PlanCelular plan(long id) {
        return leerPlan(archivoPlan(id));
    }

    /** El registro del plan; vacío si todavía no se guardó nada; null si tampoco hay plan. */
    public RegistroCelular registro(long id) {
        RegistroCelular r = leerRegistro(archivoRegistro(id));
        if (r != null) return r;
        PlanCelular p = plan(id);
        return p == null ? null : Sesiones.registroVacio(p);
    }

    public void guardarRegistro(RegistroCelular r) throws IOException {
        escribirAtomico(archivoRegistro(r.mesocicloId()), JsonRegistro.registroAJson(r));
    }

    /**
     * Carga un plan (devuelve el {@link PlanCelular}) o un registro (devuelve el {@link RegistroCelular}) según el campo
     * "formato" del texto. Un plan de igual mesocicloId reemplaza al anterior y conserva su registro.
     *
     * @throws IllegalArgumentException con mensaje en español si no es un plan ni un registro válido, o si el registro no
     *                                  tiene plan
     */
    public Object cargarDesdeTexto(String json) throws IOException {
        String texto = json.replace("\uFEFF", "").strip();
        String formato = null;
        try {
            JsonElement e = JsonParser.parseString(texto);
            if (e.isJsonObject() && e.getAsJsonObject().has("formato")) formato = e.getAsJsonObject().get("formato").getAsString();
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException ex) {
            throw new IllegalArgumentException("El archivo no es un JSON válido: " + ex.getMessage());
        }
        if (PlanCelular.FORMATO.equals(formato)) {
            PlanCelular plan = JsonRegistro.planDeJson(texto);
            escribirAtomico(archivoPlan(plan.mesocicloId()), JsonRegistro.planAJson(plan));
            return plan;
        }
        if (RegistroCelular.FORMATO.equals(formato)) {
            RegistroCelular registro = JsonRegistro.registroDeJson(texto);
            if (plan(registro.mesocicloId()) == null) throw new IllegalArgumentException("Primero cargá el plan de este registro: no hay ningún plan con el id " + registro.mesocicloId());
            guardarRegistro(registro);
            return registro;
        }
        throw new IllegalArgumentException("El archivo no es un plan ni un registro de RUTINEITOR");
    }

    /** Los planes guardados, ordenados por inicio; los archivos dañados se apartan y se saltean. */
    public List<PlanCelular> planes() {
        // lo completa otra subtarea (T34c): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Borra el plan, su registro y sus cronómetros; si era el activo, deja activo() en null. */
    public void borrarPlan(long id) throws IOException {
        // lo completa otra subtarea (T34c): no lo cambies
        throw new UnsupportedOperationException();
    }

    // ---------- preferencias ----------

    /** El objeto de preferencias.json; vacío si no existe; si está dañado lo aparta y devuelve uno vacío. */
    JsonObject leerPreferencias() {
        // TODO T34b
        throw new UnsupportedOperationException();
    }

    void guardarPreferencias(JsonObject preferencias) throws IOException {
        // TODO T34b
        throw new UnsupportedOperationException();
    }

    /** Id del plan activo; null si no hay. */
    public Long activo() {
        // TODO T34b
        throw new UnsupportedOperationException();
    }

    /** Guarda el plan activo (null = ninguno). */
    public void activo(Long id) throws IOException {
        // TODO T34b
        throw new UnsupportedOperationException();
    }

    /** Si el modo oscuro está encendido (por defecto no). */
    public boolean oscuro() {
        // TODO T34b
        throw new UnsupportedOperationException();
    }

    public void oscuro(boolean oscuro) throws IOException {
        // TODO T34b
        throw new UnsupportedOperationException();
    }

    /** El cronómetro de esa sesión; {@code Crono.nuevo()} si no hay. */
    public EstadoCrono crono(long id, int micro, int dia) {
        // lo completa otra subtarea (T34c): no lo cambies
        throw new UnsupportedOperationException();
    }

    public void crono(long id, int micro, int dia, EstadoCrono estado) throws IOException {
        // lo completa otra subtarea (T34c): no lo cambies
        throw new UnsupportedOperationException();
    }
}
```

```java
// android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java
package com.rutina.app.datos;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.rutina.app.logica.Crono;
import com.rutina.app.logica.EstadoCrono;
import com.rutina.registro.JsonRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionPlan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlmacenTest {

    @TempDir
    Path dir;

    private static String ejemplo(String nombre) throws IOException {
        return new String(Files.readAllBytes(java.nio.file.Paths.get("../../docs/registro-sesiones/ejemplos/" + nombre)),
                StandardCharsets.UTF_8);
    }

    /** Escribe a mano un plan mínimo en planes/<id>.json (sin pasar por Almacen). */
    private void ponerPlan(long id, String inicio) throws IOException {
        PlanCelular p = new PlanCelular("rutineitor-plan", 1, "F", id, inicio, 1, 1, List.of(new SesionPlan(1, 1, List.of())));
        Files.createDirectories(dir.resolve("planes"));
        Files.write(dir.resolve("planes").resolve(id + ".json"), JsonRegistro.planAJson(p).getBytes(StandardCharsets.UTF_8));
    }

    private long cuantos(String carpeta, String prefijo) throws IOException {
        try (Stream<Path> s = Files.list(dir.resolve(carpeta))) {
            return s.filter(p -> p.getFileName().toString().startsWith(prefijo)).count();
        }
    }

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

    @Test
    void preferencias() throws IOException {
        // TODO T34b
    }

    @Test
    void listaYBorraPlanes() throws IOException {
        // lo completa otra subtarea (T34c): no lo cambies
    }

    @Test
    void cronos() throws IOException {
        // lo completa otra subtarea (T34c): no lo cambies
    }

    @Test
    void cargaPlanYRegistro() throws IOException {
        Almacen a = new Almacen(dir);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto(ejemplo("registro-ejemplo.json")));
        assertTrue(e.getMessage().contains("no hay ningún plan"));
        Object p = a.cargarDesdeTexto("  " + ejemplo("plan-ejemplo.json") + "  ");
        assertInstanceOf(PlanCelular.class, p);
        assertEquals(3L, a.plan(3).mesocicloId());
        Object r = a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));
        assertInstanceOf(RegistroCelular.class, r);
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        assertEquals(2, a.registro(3).sesiones().size());
        assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto("{roto"));
    }
}
```

Completá los `// TODO` que pide la tarea T34b. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: android/app/src/main/java/com/rutina/app/datos/Almacen.java, android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java.
