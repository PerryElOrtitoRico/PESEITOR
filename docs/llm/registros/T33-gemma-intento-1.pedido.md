<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 5294, razonamiento no -->

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

## 13. Android: cronómetro (`Crono`, `EstadoCrono`)
`record EstadoCrono(long acumuladoMs, Long desde)` en `com.rutina.app.logica` (`desde` = hora del sistema en ms si corre, `null` si está parado). `Crono`, solo métodos estáticos:
- `nuevo()` → `EstadoCrono(0, null)`.
- `iniciar(estado, ahora)`: si ya corre devuelve un estado igual; si no, `EstadoCrono(acumuladoMs, ahora)`.
- `pausar(estado, ahora)`: si corre, `EstadoCrono(acumuladoMs + (ahora - desde), null)`; si no, un estado igual.
- `ms(estado, ahora)`: `acumuladoMs` más `ahora - desde` si corre. `ms(new EstadoCrono(60_000, 1_000L), 3_601_000)` = `3_660_000` (el cronómetro siguió corriendo con la app cerrada).
- `texto(long ms)`: `"h:mm:ss"` sin redondear (segundos hacia abajo), con `Locale.ROOT`: `0` → `"0:00:00"`, `75_500` → `"0:01:15"`, `3_725_000` → `"1:02:05"`.
- `minutosDe(long ms)` → `Integer`: `ms <= 0` → `null`; si no, `Math.max(1, Math.round(ms / 60000.0))` como entero: `29_999` → 1, `90_000` → 2, `4_500_000` → 75.
- `duracionValida(String texto)` → `Integer`: lo que escribe el usuario en minutos. `null` → `null`; se recorta con `strip()`; tiene que ser solo dígitos; el valor tiene que estar entre 1 y 600, si no `null`. `"75"` → 75, `" 5 "` → 5, `"0"`, `"601"`, `"7,5"`, `""` → `null`.

---

# T33 — Funciones del cronómetro

## Objetivo
Completar los siete métodos de `Crono` y los cinco tests de `CronoTest`. No cambies los imports.

## Código que ya existe
- `record EstadoCrono(long acumuladoMs, Long desde)`: se lee con `e.acumuladoMs()` y `e.desde()` (`desde` es `null` si está parado).

## `nuevo()` (pasos en orden)
1. `return new EstadoCrono(0, null);`

## `iniciar(EstadoCrono estado, long ahora)` (pasos en orden)
1. `if (estado.desde() != null) return estado;`
2. `return new EstadoCrono(estado.acumuladoMs(), ahora);`

## `pausar(EstadoCrono estado, long ahora)` (pasos en orden)
1. `if (estado.desde() == null) return estado;`
2. `return new EstadoCrono(estado.acumuladoMs() + (ahora - estado.desde()), null);`

## `ms(EstadoCrono estado, long ahora)` (pasos en orden)
1. `if (estado.desde() == null) return estado.acumuladoMs();`
2. `return estado.acumuladoMs() + (ahora - estado.desde());`

## `texto(long ms)` (pasos en orden)
1. `long total = ms / 1000;`
2. `long h = total / 3600;`
3. `long m = (total % 3600) / 60;`
4. `long s = total % 60;`
5. `return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);`

## `minutosDe(long ms)` (pasos en orden)
1. `if (ms <= 0) return null;`
2. `return (int) Math.max(1, Math.round(ms / 60000.0));`

## `duracionValida(String texto)` (pasos en orden)
1. `if (texto == null) return null;`
2. `String t = texto.strip();`
3. `if (!t.matches("[0-9]+")) return null;`
4. `long n;`
5. `try { n = Long.parseLong(t); } catch (NumberFormatException e) { return null; }`
6. `if (n < 1 || n > 600) return null;`
7. `return (int) n;`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `nuevoIniciarPausar`:
  - `assertEquals(new EstadoCrono(0, null), Crono.nuevo());`
  - `EstadoCrono i = Crono.iniciar(Crono.nuevo(), 1_000);`
  - `assertEquals(new EstadoCrono(0, 1_000L), i);`
  - `assertEquals(i, Crono.iniciar(i, 5_000));`
  - `EstadoCrono p = Crono.pausar(i, 4_000);`
  - `assertEquals(new EstadoCrono(3_000, null), p);`
  - `assertEquals(p, Crono.pausar(p, 9_000));`
- `ms`:
  - `assertEquals(0L, Crono.ms(Crono.nuevo(), 99_999));`
  - `assertEquals(63_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 4_000));`
  - `assertEquals(3_660_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 3_601_000));`
- `texto`:
  - `assertEquals("0:00:00", Crono.texto(0));`
  - `assertEquals("0:01:15", Crono.texto(75_500));`
  - `assertEquals("1:02:05", Crono.texto(3_725_000));`
- `minutosDe`:
  - `assertEquals(1, Crono.minutosDe(29_999));`
  - `assertEquals(2, Crono.minutosDe(90_000));`
  - `assertEquals(75, Crono.minutosDe(4_500_000));`
  - `assertNull(Crono.minutosDe(0));`
- `duracionValida`:
  - `assertEquals(75, Crono.duracionValida("75"));`
  - `assertEquals(5, Crono.duracionValida(" 5 "));`
  - `assertNull(Crono.duracionValida("0"));`
  - `assertNull(Crono.duracionValida("601"));`
  - `assertNull(Crono.duracionValida("7,5"));`
  - `assertNull(Crono.duracionValida(""));`

---

# Archivos actuales

```java
// android/app/src/main/java/com/rutina/app/logica/Crono.java
package com.rutina.app.logica;

import java.util.Locale;

/** Funciones del cronómetro de la sesión (traducción de celular/logica.js). El estado es inmutable. */
public final class Crono {

    private Crono() {
    }

    /** Cronómetro en cero y parado. */
    public static EstadoCrono nuevo() {
        // TODO T33
        throw new UnsupportedOperationException();
    }

    /** Si ya corre devuelve un estado igual; si no, arranca en {@code ahora}. */
    public static EstadoCrono iniciar(EstadoCrono estado, long ahora) {
        // TODO T33
        throw new UnsupportedOperationException();
    }

    /** Si corre, suma lo corrido a acumuladoMs y lo para; si no, devuelve un estado igual. */
    public static EstadoCrono pausar(EstadoCrono estado, long ahora) {
        // TODO T33
        throw new UnsupportedOperationException();
    }

    /** Milisegundos contados a la hora {@code ahora}. */
    public static long ms(EstadoCrono estado, long ahora) {
        // TODO T33
        throw new UnsupportedOperationException();
    }

    /** "h:mm:ss" sin redondear: 0 → "0:00:00", 75_500 → "0:01:15", 3_725_000 → "1:02:05". */
    public static String texto(long ms) {
        // TODO T33
        throw new UnsupportedOperationException();
    }

    /** Minutos para guardar: ms <= 0 → null; si no, al minuto más cercano y al menos 1. */
    public static Integer minutosDe(long ms) {
        // TODO T33
        throw new UnsupportedOperationException();
    }

    /** Minutos escritos por el usuario → entero de 1 a 600, o null si es inválido ("75", " 5 " sí; "0", "601", "7,5", "" no). */
    public static Integer duracionValida(String texto) {
        // TODO T33
        throw new UnsupportedOperationException();
    }
}
```

```java
// android/app/src/test/java/com/rutina/app/logica/CronoTest.java
package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CronoTest {

    @Test
    void nuevoIniciarPausar() {
        // TODO T33
    }

    @Test
    void ms() {
        // TODO T33
    }

    @Test
    void texto() {
        // TODO T33
    }

    @Test
    void minutosDe() {
        // TODO T33
    }

    @Test
    void duracionValida() {
        // TODO T33
    }
}
```

Completá los `// TODO` que pide la tarea T33. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: android/app/src/main/java/com/rutina/app/logica/Crono.java, android/app/src/test/java/com/rutina/app/logica/CronoTest.java.
