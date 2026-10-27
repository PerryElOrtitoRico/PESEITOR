<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 4690, razonamiento no -->

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

# T34d — Almacén: cargar un plan o un registro desde texto

## Objetivo
Completar en `Almacen` solo `cargarDesdeTexto` (marcado `// TODO T34d`) y el test `cargaPlanYRegistro` de `AlmacenTest`. `plan(long)`, `guardarRegistro`, `escribirAtomico` y `archivoPlan` ya andan: usalos. No cambies los imports ni los helpers del test.

## Código que ya existe
- `PlanCelular.FORMATO` = `"rutineitor-plan"`, `RegistroCelular.FORMATO` = `"rutineitor-registro"`; `plan.mesocicloId()` y `registro.mesocicloId()` son `long`.
- `JsonRegistro.planDeJson(String)`, `JsonRegistro.planAJson(PlanCelular)`, `JsonRegistro.registroDeJson(String)`: los dos primeros lanzan `IllegalArgumentException` si el texto es inválido.
- `Almacen.plan(long id)` → `PlanCelular` o `null`; `Almacen.guardarRegistro(RegistroCelular)`; `Almacen.escribirAtomico(Path, String)`; `Almacen.archivoPlan(long id)` → `Path`.
- Gson: `JsonParser.parseString(String)` → `JsonElement`.

## `cargarDesdeTexto(String json)` (pasos en orden)
1. `String texto = json.replace("\uFEFF", "").strip();`
2. `String formato = null;`
3. `try {`
4. `    JsonElement e = JsonParser.parseString(texto);`
5. `    if (e.isJsonObject() && e.getAsJsonObject().has("formato")) formato = e.getAsJsonObject().get("formato").getAsString();`
6. `} catch (JsonParseException | IllegalStateException | UnsupportedOperationException ex) {`
7. `    throw new IllegalArgumentException("El archivo no es un JSON válido: " + ex.getMessage());`
8. `}`
9. `if (PlanCelular.FORMATO.equals(formato)) {`
10. `    PlanCelular plan = JsonRegistro.planDeJson(texto);`
11. `    escribirAtomico(archivoPlan(plan.mesocicloId()), JsonRegistro.planAJson(plan));`
12. `    return plan;`
13. `}`
14. `if (RegistroCelular.FORMATO.equals(formato)) {`
15. `    RegistroCelular registro = JsonRegistro.registroDeJson(texto);`
16. `    if (plan(registro.mesocicloId()) == null) throw new IllegalArgumentException("Primero cargá el plan de este registro: no hay ningún plan con el id " + registro.mesocicloId());`
17. `    guardarRegistro(registro);`
18. `    return registro;`
19. `}`
20. `throw new IllegalArgumentException("El archivo no es un plan ni un registro de RUTINEITOR");`

## Tests (copiá estas aserciones tal cual, sin comentarios; `ejemplo(nombre)` lee un archivo de ejemplo)
- `cargaPlanYRegistro`:
  - `Almacen a = new Almacen(dir);`
  - `IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto(ejemplo("registro-ejemplo.json")));`
  - `assertTrue(e.getMessage().contains("no hay ningún plan"));`
  - `Object p = a.cargarDesdeTexto("  " + ejemplo("plan-ejemplo.json") + "  ");`
  - `assertInstanceOf(PlanCelular.class, p);`
  - `assertEquals(3L, a.plan(3).mesocicloId());`
  - `Object r = a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));`
  - `assertInstanceOf(RegistroCelular.class, r);`
  - `a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));`
  - `assertEquals(2, a.registro(3).sesiones().size());`
  - `assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto("{roto"));`

---

# Archivos actuales

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
    }
}
```

Completá los `// TODO` que pide la tarea T34d. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java.
