<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 5871, razonamiento no -->

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

## 12. Android: totales de repeticiones (`Totales`)
Clase `com.rutina.app.logica.Totales`.
- `totalReps(List<SerieReal> series)`: suma de las reps; lista vacía → 0.
- `totalesEjercicio(List<FilaPlan> filas, Map<String, String> textos)`: devuelve `int[]{planificadas, hechas}`. `planificadas` = suma de `totalReps(fila.series())`. `hechas`: por cada fila, si el texto de su etiqueta falta o está en blanco, cuentan las reps planificadas de esa fila; si es una notación válida (`NotacionRegistro.esValida(texto, fila.series())`), cuentan las reps de `NotacionRegistro.parse(texto, fila.series())`; si algún texto es inválido, `hechas = -1`.
Ejemplos con filas `A` (dos series de 3 reps, 80 kg) y `B` (una serie de 5 reps, 60 kg): textos vacíos → `{11, 11}`; `A = "3.2"` → `{11, 10}`; `B = "0"` → `{11, 6}`; `A = "x"` → `{11, -1}`.

---

# T32 — Total de repeticiones de un ejercicio

## Objetivo
Completar `Totales.totalReps` y `Totales.totalesEjercicio` y los dos tests de `TotalesTest`. No cambies los imports ni la constante `FILAS` del test.

## Código que ya existe
- Records (se leen con `x()`): `SerieReal(reps, kg)`, `FilaPlan(etiqueta, pctMin, pctMax, series)`.
- `NotacionRegistro.esValida(String texto, List<SerieReal> planificadas)` → boolean y `NotacionRegistro.parse(String texto, List<SerieReal> planificadas)` → `List<SerieReal>` (series realizadas; `"0"` da lista vacía).

## `totalReps(List<SerieReal> series)` (pasos en orden)
1. `int total = 0;`
2. `for (SerieReal s : series) total += s.reps();`
3. `return total;`

## `totalesEjercicio(List<FilaPlan> filas, Map<String, String> textos)` (pasos en orden)
1. `int planificadas = 0;`
2. `int hechas = 0;`
3. `for (FilaPlan fila : filas) {`
4. `    int plan = totalReps(fila.series());`
5. `    planificadas += plan;`
6. `    if (hechas == -1) continue;`
7. `    String texto = textos.get(fila.etiqueta());`
8. `    if (texto == null || texto.isBlank()) {`
9. `        hechas += plan;`
10. `    } else if (!NotacionRegistro.esValida(texto, fila.series())) {`
11. `        hechas = -1;`
12. `    } else {`
13. `        hechas += totalReps(NotacionRegistro.parse(texto, fila.series()));`
14. `    }`
15. `}`
16. `return new int[]{planificadas, hechas};`

## Tests (copiá estas aserciones tal cual, sin comentarios; `FILAS` ya está en el archivo)
- `totalReps`:
  - `assertEquals(0, Totales.totalReps(List.of()));`
  - `assertEquals(11, Totales.totalReps(List.of(new SerieReal(3, 80), new SerieReal(3, 80), new SerieReal(5, 0))));`
- `totalesEjercicio`:
  - `assertArrayEquals(new int[]{11, 11}, Totales.totalesEjercicio(FILAS, Map.of()));`
  - `assertArrayEquals(new int[]{11, 10}, Totales.totalesEjercicio(FILAS, Map.of("A", "3.2")));`
  - `assertArrayEquals(new int[]{11, 6}, Totales.totalesEjercicio(FILAS, Map.of("B", "0")));`
  - `assertArrayEquals(new int[]{11, -1}, Totales.totalesEjercicio(FILAS, Map.of("A", "x")));`

---

# Archivos actuales

```java
// android/app/src/main/java/com/rutina/app/logica/Totales.java
package com.rutina.app.logica;

import com.rutina.registro.FilaPlan;
import com.rutina.registro.NotacionRegistro;
import com.rutina.registro.SerieReal;

import java.util.List;
import java.util.Map;

/** Total de repeticiones de un ejercicio (traducción de celular/logica.js). */
public final class Totales {

    private Totales() {
    }

    /** Suma de las reps de una lista de series; lista vacía → 0. */
    public static int totalReps(List<SerieReal> series) {
        // TODO T32
        throw new UnsupportedOperationException();
    }

    /**
     * @param filas   filas del ejercicio en el plan
     * @param textos  etiqueta de la fila → lo que se anotó
     * @return {planificadas, hechas}; hechas = -1 si alguna casilla es inválida
     */
    public static int[] totalesEjercicio(List<FilaPlan> filas, Map<String, String> textos) {
        // TODO T32
        throw new UnsupportedOperationException();
    }
}
```

```java
// android/app/src/test/java/com/rutina/app/logica/TotalesTest.java
package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.registro.FilaPlan;
import com.rutina.registro.SerieReal;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TotalesTest {

    private static final List<FilaPlan> FILAS = List.of(
            new FilaPlan("A", null, null, List.of(new SerieReal(3, 80), new SerieReal(3, 80))),
            new FilaPlan("B", null, null, List.of(new SerieReal(5, 60))));

    @Test
    void totalReps() {
        // TODO T32
    }

    @Test
    void totalesEjercicio() {
        // TODO T32
    }
}
```

Completá los `// TODO` que pide la tarea T32. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: android/app/src/main/java/com/rutina/app/logica/Totales.java, android/app/src/test/java/com/rutina/app/logica/TotalesTest.java.
