<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 3742, razonamiento no -->

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

# T31b — Sesiones: limpiar y guardar una sesión

## Objetivo
Completar en `Sesiones` solo `limpiarSesion` y `guardarSesion` (marcados `// TODO T31b`) y los dos tests `limpiaSesion` y `guardaSesion` de `SesionesTest`. Los demás métodos ya están hechos: no los toques. No cambies los imports.

## Código que ya existe
- Records (se leen con `x()`): `SesionRegistro(microciclo, dia, fecha, descanso, sensacionFinal, ejercicios, duracionMin)`, `EjercicioRegistro(bloque, esfuerzo, filas, nota)`, `RegistroCelular(formato, version, perfil, mesocicloId, sesiones)`. `filas` es un `Map<String,String>` (etiqueta → texto).
- `Sesiones.sesionDelRegistro(registro, micro, dia)` ya anda.

## `limpiarSesion(SesionRegistro s)` (pasos en orden)
1. `List<EjercicioRegistro> limpios = new ArrayList<>();`
2. `for (EjercicioRegistro e : s.ejercicios()) {`
3. `    Map<String, String> filas = new LinkedHashMap<>();`
4. `    for (Map.Entry<String, String> f : e.filas().entrySet()) {`
5. `        if (f.getValue() != null && !f.getValue().isBlank()) filas.put(f.getKey(), f.getValue().strip());`
6. `    }`
7. `    String nota = (e.nota() == null || e.nota().isBlank()) ? null : e.nota().strip();`
8. `    if (filas.isEmpty() && e.esfuerzo() == null && nota == null) continue;`
9. `    limpios.add(new EjercicioRegistro(e.bloque(), e.esfuerzo(), filas, nota));`
10. `}`
11. `return new SesionRegistro(s.microciclo(), s.dia(), s.fecha(), s.descanso(), s.sensacionFinal(), limpios, s.duracionMin());`

## `guardarSesion(RegistroCelular r, SesionRegistro s)` (pasos en orden)
1. `SesionRegistro limpia = limpiarSesion(s);`
2. `List<SesionRegistro> sesiones = new ArrayList<>();`
3. `for (SesionRegistro o : r.sesiones()) { if (o.microciclo() != limpia.microciclo() || o.dia() != limpia.dia()) sesiones.add(o); }`
4. `sesiones.add(limpia);`
5. `sesiones.sort(Comparator.comparingInt(SesionRegistro::microciclo).thenComparingInt(SesionRegistro::dia));`
6. `return new RegistroCelular(r.formato(), r.version(), r.perfil(), r.mesocicloId(), sesiones);`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `limpiaSesion`:
  - `SesionRegistro s = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-1", null, Map.of("80-85", " 3.3 "), null), new EjercicioRegistro("1-2", null, Map.of("4x12", ""), null), new EjercicioRegistro("1-3", 2, Map.of(), "  subir peso  ")), null);`
  - `SesionRegistro l = Sesiones.limpiarSesion(s);`
  - `assertEquals(2, l.ejercicios().size());`
  - `assertEquals(Map.of("80-85", "3.3"), l.ejercicios().get(0).filas());`
  - `assertEquals("1-3", l.ejercicios().get(1).bloque());`
  - `assertEquals("subir peso", l.ejercicios().get(1).nota());`
  - `assertEquals(3, s.ejercicios().size());`
- `guardaSesion`:
  - `RegistroCelular r0 = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of());`
  - `SesionRegistro s22 = new SesionRegistro(2, 2, "2026-11-05", 4, null, List.of(), null);`
  - `SesionRegistro s11 = new SesionRegistro(1, 1, "2026-10-27", 5, 3, List.of(), null);`
  - `RegistroCelular r2 = Sesiones.guardarSesion(Sesiones.guardarSesion(r0, s22), s11);`
  - `assertEquals(List.of(), r0.sesiones());`
  - `assertEquals(1, r2.sesiones().get(0).microciclo());`
  - `assertEquals(2, r2.sesiones().get(1).microciclo());`
  - `RegistroCelular r3 = Sesiones.guardarSesion(r2, new SesionRegistro(1, 1, "2026-10-27", 1, 3, List.of(), null));`
  - `assertEquals(2, r3.sesiones().size());`
  - `assertEquals(1, Sesiones.sesionDelRegistro(r3, 1, 1).descanso());`

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
        for (SesionPlan s : plan.sesiones()) {
            if (s.microciclo() == micro && s.dia() == dia) {
                return s;
            }
        }
        return null;
    }

    /** La sesión del registro con ese microciclo y día, o null. */
    public static SesionRegistro sesionDelRegistro(RegistroCelular registro, int micro, int dia) {
        for (SesionRegistro s : registro.sesiones()) {
            if (s.microciclo() == micro && s.dia() == dia) {
                return s;
            }
        }
        return null;
    }

    /** Registro sin sesiones para ese plan (mismo perfil y mesocicloId). */
    public static RegistroCelular registroVacio(PlanCelular plan) {
        return new RegistroCelular(RegistroCelular.FORMATO, RegistroCelular.VERSION, plan.perfil(), plan.mesocicloId(), new ArrayList<>());
    }

    /** Copia de la sesión sin ruido: filas en blanco fuera, textos recortados, nota recortada (en blanco → null);
     *  un ejercicio se conserva si tiene filas, esfuerzo o nota. */
    public static SesionRegistro limpiarSesion(SesionRegistro s) {
        // TODO T31b
        throw new UnsupportedOperationException();
    }

    /** Registro nuevo con la sesión (limpia) agregada o reemplazada, ordenadas por microciclo y día. */
    public static RegistroCelular guardarSesion(RegistroCelular r, SesionRegistro s) {
        // TODO T31b
        throw new UnsupportedOperationException();
    }

    /** Pista gris de la casilla: "3.3.3 · 120/127,5", "12.12" o "" (lista vacía). */
    public static String pista(List<SerieReal> series) {
        if (series.isEmpty()) {
            return "";
        }
        StringBuilder reps = new StringBuilder();
        for (SerieReal s : series) {
            if (reps.length() > 0) {
                reps.append(".");
            }
            reps.append(s.reps());
        }
        List<String> kgs = new ArrayList<>();
        for (SerieReal s : series) {
            if (s.kg() > 0) {
                String t = textoKg(s.kg());
                if (!kgs.contains(t)) {
                    kgs.add(t);
                }
            }
        }
        if (kgs.isEmpty()) {
            return reps.toString();
        }
        return reps.toString() + " · " + String.join("/", kgs);
    }

    /** Un peso sin ".0" final y con coma decimal: 120.0 → "120", 127.5 → "127,5". */
    private static String textoKg(double kg) {
        String t = String.valueOf(kg);
        if (t.endsWith(".0")) {
            t = t.substring(0, t.length() - 2);
        }
        return t.replace('.', ',');
    }

    /** "registro-<perfil con _>-<inicio>.json". */
    public static String nombreArchivoRegistro(PlanCelular plan) {
        return "registro-" + plan.perfil().replaceAll("\\s", "_") + "-" + plan.inicio() + ".json";
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
        assertEquals("1-1", Sesiones.sesionDelPlan(plan(), 1, 1).ejercicios().get(0).bloque());
        assertEquals(List.of(), Sesiones.sesionDelPlan(plan(), 2, 2).ejercicios());
        assertNull(Sesiones.sesionDelPlan(plan(), 3, 1));
    }

    @Test
    void sesionDelRegistro() {
        SesionRegistro a = new SesionRegistro(1, 1, "2026-10-27", 5, 3, List.of(), null);
        RegistroCelular r = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of(a));
        assertEquals(a, Sesiones.sesionDelRegistro(r, 1, 1));
        assertNull(Sesiones.sesionDelRegistro(r, 1, 2));
    }

    @Test
    void registroVacio() {
        assertEquals(new RegistroCelular("rutineitor-registro", 1, "Franco Pérez", 3L, List.of()), Sesiones.registroVacio(plan()));
    }

    @Test
    void pista() {
        assertEquals("3.3.3 · 120/127,5", Sesiones.pista(SERIES));
        assertEquals("12.12", Sesiones.pista(List.of(new SerieReal(12, 0), new SerieReal(12, 0))));
        assertEquals("4.4 · 105", Sesiones.pista(List.of(new SerieReal(4, 105), new SerieReal(4, 105))));
        assertEquals("", Sesiones.pista(List.of()));
    }

    @Test
    void nombreArchivoRegistro() {
        assertEquals("registro-Franco_Pérez-2026-10-26.json", Sesiones.nombreArchivoRegistro(plan()));
    }

    @Test
    void limpiaSesion() {
        // TODO T31b
    }

    @Test
    void guardaSesion() {
        // TODO T31b
    }
}
```

Completá los `// TODO` que pide la tarea T31b. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: android/app/src/main/java/com/rutina/app/logica/Sesiones.java, android/app/src/test/java/com/rutina/app/logica/SesionesTest.java.
