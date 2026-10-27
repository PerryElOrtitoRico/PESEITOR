<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 3033, razonamiento no -->

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

# T34a — Almacén: escritura atómica y lectura con respaldo del archivo roto

## Objetivo
Completar en `Almacen` solo los métodos marcados `// TODO T34a`: `leerTexto`, `escribirAtomico`, `apartarRoto`, `leerPlan` y `leerRegistro`; y los tests `escribeYRelee` y `apartaElRoto` de `AlmacenTest`. Los demás métodos (también los marcados con otras tareas) no se tocan. No cambies los imports, la constante ni los helpers del test (`dir`, `ejemplo`, `ponerPlan`, `cuantos`).

## Código que ya existe
- `JsonRegistro.planDeJson(String)` → `PlanCelular` y `JsonRegistro.registroDeJson(String)` → `RegistroCelular`: lanzan `IllegalArgumentException` si el texto no es válido (JSON roto, formato o versión distintos).

## `leerTexto(Path archivo)` (pasos en orden)
1. `return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);`

## `escribirAtomico(Path destino, String texto)` (pasos en orden)
1. `Files.createDirectories(destino.getParent());`
2. `Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");`
3. `Files.write(tmp, texto.getBytes(StandardCharsets.UTF_8));`
4. `Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);`

## `apartarRoto(Path archivo)` (pasos en orden)
1. `Path roto = archivo.resolveSibling(archivo.getFileName() + ".roto-" + System.currentTimeMillis());`
2. `try { Files.move(archivo, roto, StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) { }`

## `leerPlan(Path archivo)` (pasos en orden)
1. `if (!Files.exists(archivo)) return null;`
2. `try { return JsonRegistro.planDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }`

## `leerRegistro(Path archivo)` (pasos en orden)
1. `if (!Files.exists(archivo)) return null;`
2. `try { return JsonRegistro.registroDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `escribeYRelee`:
  - `Path destino = dir.resolve("a").resolve("x.json");`
  - `Almacen.escribirAtomico(destino, "uno");`
  - `assertEquals("uno", Almacen.leerTexto(destino));`
  - `Almacen.escribirAtomico(destino, "dos");`
  - `assertEquals("dos", Almacen.leerTexto(destino));`
  - `assertEquals(1, cuantos("a", ""));`
- `apartaElRoto`:
  - `Files.createDirectories(dir.resolve("planes"));`
  - `Files.write(dir.resolve("planes").resolve("9.json"), "{roto".getBytes(StandardCharsets.UTF_8));`
  - `assertNull(Almacen.leerPlan(dir.resolve("planes").resolve("9.json")));`
  - `assertFalse(Files.exists(dir.resolve("planes").resolve("9.json")));`
  - `assertEquals(1, cuantos("planes", "9.json.roto-"));`
  - `assertNull(Almacen.leerRegistro(dir.resolve("registros").resolve("1.json")));`

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
        // TODO T34a
        throw new UnsupportedOperationException();
    }

    /** Escribe el texto en un temporal de la misma carpeta y lo mueve sobre el destino (crea la carpeta si falta). */
    static void escribirAtomico(Path destino, String texto) throws IOException {
        // TODO T34a
        throw new UnsupportedOperationException();
    }

    /** Renombra un archivo dañado a {@code <nombre>.roto-<epoch ms>} (misma carpeta). Si no puede, no hace nada. */
    static void apartarRoto(Path archivo) {
        // TODO T34a
        throw new UnsupportedOperationException();
    }

    /** El plan guardado en ese archivo; null si no existe; si está dañado lo aparta y devuelve null. */
    static PlanCelular leerPlan(Path archivo) {
        // TODO T34a
        throw new UnsupportedOperationException();
    }

    /** El registro guardado en ese archivo; null si no existe; si está dañado lo aparta y devuelve null. */
    static RegistroCelular leerRegistro(Path archivo) {
        // TODO T34a
        throw new UnsupportedOperationException();
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
        // lo completa otra subtarea (T34d): no lo cambies
        throw new UnsupportedOperationException();
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
        // lo completa otra subtarea (T34b): no lo cambies
        throw new UnsupportedOperationException();
    }

    void guardarPreferencias(JsonObject preferencias) throws IOException {
        // lo completa otra subtarea (T34b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Id del plan activo; null si no hay. */
    public Long activo() {
        // lo completa otra subtarea (T34b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Guarda el plan activo (null = ninguno). */
    public void activo(Long id) throws IOException {
        // lo completa otra subtarea (T34b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Si el modo oscuro está encendido (por defecto no). */
    public boolean oscuro() {
        // lo completa otra subtarea (T34b): no lo cambies
        throw new UnsupportedOperationException();
    }

    public void oscuro(boolean oscuro) throws IOException {
        // lo completa otra subtarea (T34b): no lo cambies
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
        // TODO T34a
    }

    @Test
    void apartaElRoto() throws IOException {
        // TODO T34a
    }

    @Test
    void preferencias() throws IOException {
        // lo completa otra subtarea (T34b): no lo cambies
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
        // lo completa otra subtarea (T34d): no lo cambies
    }
}
```

Completá los `// TODO` que pide la tarea T34a. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: android/app/src/main/java/com/rutina/app/datos/Almacen.java, android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java.
