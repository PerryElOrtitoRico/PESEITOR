<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 4386, razonamiento no -->

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

## 11. Android: sesión (`Sesiones`)
Clase `com.rutina.app.logica.Sesiones`, solo métodos estáticos.
- `sesionDelPlan(PlanCelular plan, int micro, int dia)`: la `SesionPlan` con ese microciclo y día, o `null`.
- `sesionDelRegistro(RegistroCelular registro, int micro, int dia)`: la `SesionRegistro` con ese microciclo y día, o `null`.
- `registroVacio(PlanCelular plan)`: `RegistroCelular` con `RegistroCelular.FORMATO`, `RegistroCelular.VERSION`, el perfil y el `mesocicloId` del plan, y la lista de sesiones vacía.
- `limpiarSesion(SesionRegistro s)`: copia de la sesión sin ruido. A cada ejercicio: las filas con texto en blanco (o `null`) se sacan y el resto se recorta con `strip()`; la nota se recorta y, si queda en blanco o es `null`, queda `null`. Un ejercicio se conserva solo si tiene filas, o esfuerzo (no `null`), o nota. Se conserva el orden. El resto de la sesión (microciclo, día, fecha, descanso, sensación final, duración) no cambia.
- `guardarSesion(RegistroCelular r, SesionRegistro s)`: `RegistroCelular` nuevo con la sesión pasada por `limpiarSesion`, que reemplaza a la de igual microciclo y día si existe; las sesiones quedan ordenadas por microciclo y después por día. Mismo formato, versión, perfil y mesocicloId. No modifica `r`.
- `pista(List<SerieReal> series)`: las reps separadas por `.`; si alguna serie tiene kg mayor que 0, sigue `" · "` y los kg distintos (mayores que 0) en orden de aparición separados por `/`, sin decimales si son enteros (`120`, no `120.0`) y con coma decimal (`127,5`). Lista vacía → `""`. Ejemplos: `[3x120, 3x120, 3x127.5]` → `"3.3.3 · 120/127,5"`; `[12x0, 12x0]` → `"12.12"`; `[4x105, 4x105]` → `"4.4 · 105"`.
- `nombreArchivoRegistro(PlanCelular plan)`: `"registro-" + perfil con cada espacio cambiado por "_" + "-" + inicio + ".json"`; `"Franco Pérez"` y `"2026-10-26"` → `"registro-Franco_Pérez-2026-10-26.json"`.

---

# T31a — Sesiones: búsquedas, registro vacío, pista y nombre del archivo

## Objetivo
Completar en `Sesiones` solo los métodos marcados `// TODO T31a`: `sesionDelPlan`, `sesionDelRegistro`, `registroVacio`, `pista`, `textoKg` y `nombreArchivoRegistro`; y los cinco tests de `SesionesTest` marcados `// TODO T31a`. `limpiarSesion` y `guardarSesion` los hace otra subtarea (T31b): no los toques ni borres sus imports. No cambies los imports ni los métodos `plan()` y `SERIES` del test.

## `sesionDelPlan(PlanCelular plan, int micro, int dia)` (pasos en orden)
1. `for (SesionPlan s : plan.sesiones()) { if (s.microciclo() == micro && s.dia() == dia) return s; }`
2. `return null;`

## `sesionDelRegistro(RegistroCelular registro, int micro, int dia)` (pasos en orden)
1. `for (SesionRegistro s : registro.sesiones()) { if (s.microciclo() == micro && s.dia() == dia) return s; }`
2. `return null;`

## `registroVacio(PlanCelular plan)` (pasos en orden)
1. `return new RegistroCelular(RegistroCelular.FORMATO, RegistroCelular.VERSION, plan.perfil(), plan.mesocicloId(), new ArrayList<>());`

## `textoKg(double kg)` (pasos en orden)
1. `String t = String.valueOf(kg);`
2. `if (t.endsWith(".0")) t = t.substring(0, t.length() - 2);`
3. `return t.replace('.', ',');`

## `pista(List<SerieReal> series)` (pasos en orden)
1. `if (series.isEmpty()) return "";`
2. `StringBuilder reps = new StringBuilder();`
3. `for (SerieReal s : series) { if (reps.length() > 0) reps.append("."); reps.append(s.reps()); }`
4. `List<String> kgs = new ArrayList<>();`
5. `for (SerieReal s : series) { if (s.kg() > 0) { String t = textoKg(s.kg()); if (!kgs.contains(t)) kgs.add(t); } }`
6. `if (kgs.isEmpty()) return reps.toString();`
7. `return reps + " · " + String.join("/", kgs);`

## `nombreArchivoRegistro(PlanCelular plan)` (pasos en orden)
1. `return "registro-" + plan.perfil().replaceAll("\\s", "_") + "-" + plan.inicio() + ".json";`

## Tests (copiá estas aserciones tal cual, sin comentarios; `plan()` y `SERIES` ya están en el archivo)
- `sesionDelPlan`:
  - `assertEquals("1-1", Sesiones.sesionDelPlan(plan(), 1, 1).ejercicios().get(0).bloque());`
  - `assertEquals(List.of(), Sesiones.sesionDelPlan(plan(), 2, 2).ejercicios());`
  - `assertNull(Sesiones.sesionDelPlan(plan(), 3, 1));`
- `sesionDelRegistro`:
  - `SesionRegistro a = new SesionRegistro(1, 1, "2026-10-27", 5, 3, List.of(), null);`
  - `RegistroCelular r = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of(a));`
  - `assertEquals(a, Sesiones.sesionDelRegistro(r, 1, 1));`
  - `assertNull(Sesiones.sesionDelRegistro(r, 1, 2));`
- `registroVacio`:
  - `assertEquals(new RegistroCelular("rutineitor-registro", 1, "Franco Pérez", 3L, List.of()), Sesiones.registroVacio(plan()));`
- `pista`:
  - `assertEquals("3.3.3 · 120/127,5", Sesiones.pista(SERIES));`
  - `assertEquals("12.12", Sesiones.pista(List.of(new SerieReal(12, 0), new SerieReal(12, 0))));`
  - `assertEquals("4.4 · 105", Sesiones.pista(List.of(new SerieReal(4, 105), new SerieReal(4, 105))));`
  - `assertEquals("", Sesiones.pista(List.of()));`
- `nombreArchivoRegistro`:
  - `assertEquals("registro-Franco_Pérez-2026-10-26.json", Sesiones.nombreArchivoRegistro(plan()));`

---

# Archivos actuales

```java
// android/app/src/main/java/com/rutina/app/logica/Sesiones.java
package com.rutina.app.logica;

import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SerieReal;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lógica pura de la sesión del celular (traducción de celular/logica.js). Nada modifica sus argumentos. */
public final class Sesiones {

    private Sesiones() {
    }

    /** La sesión del plan con ese microciclo y día, o null. */
    public static SesionPlan sesionDelPlan(PlanCelular plan, int micro, int dia) {
        // TODO T31a
        throw new UnsupportedOperationException();
    }

    /** La sesión del registro con ese microciclo y día, o null. */
    public static SesionRegistro sesionDelRegistro(RegistroCelular registro, int micro, int dia) {
        // TODO T31a
        throw new UnsupportedOperationException();
    }

    /** Registro sin sesiones para ese plan (mismo perfil y mesocicloId). */
    public static RegistroCelular registroVacio(PlanCelular plan) {
        // TODO T31a
        throw new UnsupportedOperationException();
    }

    /** Copia de la sesión sin ruido: filas en blanco fuera, textos recortados, nota recortada (en blanco → null);
     *  un ejercicio se conserva si tiene filas, esfuerzo o nota. */
    public static SesionRegistro limpiarSesion(SesionRegistro s) {
        // lo completa otra subtarea (T31b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Registro nuevo con la sesión (limpia) agregada o reemplazada, ordenadas por microciclo y día. */
    public static RegistroCelular guardarSesion(RegistroCelular r, SesionRegistro s) {
        // lo completa otra subtarea (T31b): no lo cambies
        throw new UnsupportedOperationException();
    }

    /** Pista gris de la casilla: "3.3.3 · 120/127,5", "12.12" o "" (lista vacía). */
    public static String pista(List<SerieReal> series) {
        // TODO T31a
        throw new UnsupportedOperationException();
    }

    /** Un peso sin ".0" final y con coma decimal: 120.0 → "120", 127.5 → "127,5". */
    private static String textoKg(double kg) {
        // TODO T31a
        throw new UnsupportedOperationException();
    }

    /** "registro-<perfil con _>-<inicio>.json". */
    public static String nombreArchivoRegistro(PlanCelular plan) {
        // TODO T31a
        throw new UnsupportedOperationException();
    }
}
```

```java
// android/app/src/test/java/com/rutina/app/logica/SesionesTest.java
package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;
import com.rutina.registro.EjercicioPlan;
import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.FilaPlan;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SerieReal;
import com.rutina.registro.SesionPlan;
import com.rutina.registro.SesionRegistro;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SesionesTest {

    private static final List<SerieReal> SERIES =
            List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 127.5));

    private static PlanCelular plan() {
        EjercicioPlan sentadilla = new EjercicioPlan("1-1", "Sentadilla", Tipo.FUERZA, Aproximacion.COMPLEJA, 150.0,
                List.of(new FilaPlan("80-85", 80, 85, SERIES)));
        return new PlanCelular("rutineitor-plan", 1, "Franco Pérez", 3L, "2026-10-26", 2, 2, List.of(
                new SesionPlan(1, 1, List.of(sentadilla)),
                new SesionPlan(1, 2, List.of()),
                new SesionPlan(2, 1, List.of()),
                new SesionPlan(2, 2, List.of())));
    }

    @Test
    void sesionDelPlan() {
        // TODO T31a
    }

    @Test
    void sesionDelRegistro() {
        // TODO T31a
    }

    @Test
    void registroVacio() {
        // TODO T31a
    }

    @Test
    void pista() {
        // TODO T31a
    }

    @Test
    void nombreArchivoRegistro() {
        // TODO T31a
    }

    @Test
    void limpiaSesion() {
        // lo completa otra subtarea (T31b): no lo cambies
    }

    @Test
    void guardaSesion() {
        // lo completa otra subtarea (T31b): no lo cambies
    }
}
```

Completá los `// TODO` que pide la tarea T31a. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: android/app/src/main/java/com/rutina/app/logica/Sesiones.java, android/app/src/test/java/com/rutina/app/logica/SesionesTest.java.
