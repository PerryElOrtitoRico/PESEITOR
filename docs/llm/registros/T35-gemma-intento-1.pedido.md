<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 5288, razonamiento no -->

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

## 15. Android: textos de la pantalla de la sesión (`TextosSesion`)
Clase `com.rutina.app.logica.TextosSesion`, solo métodos estáticos que arman los textos que muestra la pantalla (mismos textos que la app web).
- `tituloSesion(int micro, int dia)`: `"Microciclo 1 · Día 2"` (separados por `" · "`).
- `lineaFecha(String fecha)`: `fecha` (AAAA-MM-DD) `null` → `"Sin anotar todavía"`; si no, `"Anotada el "` + `Fechas.larga(fecha)`: `"2026-10-27"` → `"Anotada el 27/10/2026"`.
- `repsTexto(int[] totales)`: `totales` = `{planificadas, hechas}` como lo devuelve `Totales.totalesEjercicio`. `"<hechas> / <planificadas> reps"`; si `hechas == -1`, `"— / <planificadas> reps"` (raya larga). `{11, 10}` → `"10 / 11 reps"`; `{11, -1}` → `"— / 11 reps"`.
- `errorCasilla(String texto, List<SerieReal> planificadas)`: el mensaje de error de la casilla. `null` si el texto es `null`, está en blanco o es una notación válida; si es inválida, el mensaje de la `IllegalArgumentException` que lanza `NotacionRegistro.parse(texto, planificadas)` (por ejemplo `serie inválida: "x" (reps de 1 a 99, kg con coma)`).
- `ojoCasilla(String texto, List<SerieReal> planificadas)`: el aviso gris. `null` si el texto es `null`, está en blanco o es inválido, o si tiene tantas series como las planificadas o menos (`"0"` da cero series); si tiene más, `"Ojo: <n> series (planificadas: <m>). Los decimales del peso van con coma."` con `n` las series escritas y `m` las planificadas.

---

# T35 — Textos de la pantalla de la sesión

## Objetivo
Completar los cinco métodos de `TextosSesion` y los cinco tests de `TextosSesionTest`. Devolvé DOS bloques: uno con los métodos de `TextosSesion.java` y otro con los tests de `TextosSesionTest.java`. No cambies los imports ni la constante `TRES` del test.

## Código que ya existe
- `Fechas.larga(String iso)` → `"27/10/2026"` (en `com.rutina.app.logica`).
- `NotacionRegistro.parse(String texto, List<SerieReal> planificadas)` → `List<SerieReal>`; lanza `IllegalArgumentException` con el motivo si el texto es inválido. `NotacionRegistro.esValida(texto, planificadas)` → boolean.

## `tituloSesion(int micro, int dia)` (pasos en orden)
1. `return "Microciclo " + micro + " · Día " + dia;`

## `lineaFecha(String fecha)` (pasos en orden)
1. `if (fecha == null) return "Sin anotar todavía";`
2. `return "Anotada el " + Fechas.larga(fecha);`

## `repsTexto(int[] totales)` (pasos en orden)
1. `String hechas = totales[1] == -1 ? "—" : String.valueOf(totales[1]);`
2. `return hechas + " / " + totales[0] + " reps";`

## `errorCasilla(String texto, List<SerieReal> planificadas)` (pasos en orden)
1. `if (texto == null || texto.isBlank()) return null;`
2. `try { NotacionRegistro.parse(texto, planificadas); return null; } catch (IllegalArgumentException e) { return e.getMessage(); }`

## `ojoCasilla(String texto, List<SerieReal> planificadas)` (pasos en orden)
1. `if (texto == null || texto.isBlank()) return null;`
2. `List<SerieReal> hechas;`
3. `try { hechas = NotacionRegistro.parse(texto, planificadas); } catch (IllegalArgumentException e) { return null; }`
4. `if (hechas.size() <= planificadas.size()) return null;`
5. `return "Ojo: " + hechas.size() + " series (planificadas: " + planificadas.size() + "). Los decimales del peso van con coma.";`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `titulo`:
  - `assertEquals("Microciclo 1 · Día 2", TextosSesion.tituloSesion(1, 2));`
- `fecha`:
  - `assertEquals("Sin anotar todavía", TextosSesion.lineaFecha(null));`
  - `assertEquals("Anotada el 27/10/2026", TextosSesion.lineaFecha("2026-10-27"));`
- `reps`:
  - `assertEquals("10 / 11 reps", TextosSesion.repsTexto(new int[]{11, 10}));`
  - `assertEquals("— / 11 reps", TextosSesion.repsTexto(new int[]{11, -1}));`
- `error`:
  - `assertNull(TextosSesion.errorCasilla("", TRES));`
  - `assertNull(TextosSesion.errorCasilla("3.3.3", TRES));`
  - `assertEquals("serie inválida: \"x\" (reps de 1 a 99, kg con coma)", TextosSesion.errorCasilla("x", TRES));`
  - `assertEquals("falta una serie entre puntos en \"3..3\"", TextosSesion.errorCasilla("3..3", TRES));`
- `ojo`:
  - `assertNull(TextosSesion.ojoCasilla("3.3.3", TRES));`
  - `assertNull(TextosSesion.ojoCasilla("x", TRES));`
  - `assertEquals("Ojo: 4 series (planificadas: 3). Los decimales del peso van con coma.", TextosSesion.ojoCasilla("3.3.3.3-50", TRES));`

---

# Archivos actuales

```java
// android/app/src/main/java/com/rutina/app/logica/TextosSesion.java
package com.rutina.app.logica;

import com.rutina.registro.NotacionRegistro;
import com.rutina.registro.SerieReal;

import java.util.List;

/** Textos que muestra la pantalla de la sesión (iguales a los de la app web). */
public final class TextosSesion {

    private TextosSesion() {
    }

    /** "Microciclo 1 · Día 2". */
    public static String tituloSesion(int micro, int dia) {
        // TODO T35
        throw new UnsupportedOperationException();
    }

    /** "Anotada el 27/10/2026", o "Sin anotar todavía" si la fecha es null. */
    public static String lineaFecha(String fecha) {
        // TODO T35
        throw new UnsupportedOperationException();
    }

    /** "<hechas> / <planificadas> reps" a partir de {planificadas, hechas}; "— / <planificadas> reps" si hechas es -1. */
    public static String repsTexto(int[] totales) {
        // TODO T35
        throw new UnsupportedOperationException();
    }

    /** Mensaje de error de la casilla; null si está vacía o es válida. */
    public static String errorCasilla(String texto, List<SerieReal> planificadas) {
        // TODO T35
        throw new UnsupportedOperationException();
    }

    /** Aviso gris "Ojo: N series (planificadas: M)…"; null si no corresponde. */
    public static String ojoCasilla(String texto, List<SerieReal> planificadas) {
        // TODO T35
        throw new UnsupportedOperationException();
    }
}
```

```java
// android/app/src/test/java/com/rutina/app/logica/TextosSesionTest.java
package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.registro.SerieReal;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextosSesionTest {

    private static final List<SerieReal> TRES =
            List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 127.5));

    @Test
    void titulo() {
        // TODO T35
    }

    @Test
    void fecha() {
        // TODO T35
    }

    @Test
    void reps() {
        // TODO T35
    }

    @Test
    void error() {
        // TODO T35
    }

    @Test
    void ojo() {
        // TODO T35
    }
}
```

Completá los `// TODO` que pide la tarea T35. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: android/app/src/main/java/com/rutina/app/logica/TextosSesion.java, android/app/src/test/java/com/rutina/app/logica/TextosSesionTest.java.
