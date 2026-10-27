<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 5867, razonamiento no -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Los datos son records y se leen con x(): s.microciclo(), e.filas(), s.sensacionFinal(). No existen getMicrociclo() ni getFilas().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé solo los métodos que completaste, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/rutina/registro/Ejemplo.java
    public static int doble(int x) {
        return 2 * x;
    }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# RUTINEITOR — mejoras de octubre (contrato)

## 4. Registro: campos nuevos (mejoras 5.1 y 5.3)
Opcionales y compatibles: la versión del registro sigue siendo 1, y un registro viejo sin estos campos se lee igual.
- Sesión: `"duracionMin"`: entero de 1 a 600, o `null`/ausente (no se midió).
- Ejercicio: `"nota"`: texto, o `null`/ausente.

Java:
- `SesionRegistro(int microciclo, int dia, String fecha, Integer descanso, @SerializedName("final") Integer sensacionFinal, List<EjercicioRegistro> ejercicios, Integer duracionMin)` (el campo nuevo al final).
- `EjercicioRegistro(String bloque, Integer esfuerzo, Map<String, String> filas, String nota)` (al final).
- `JsonRegistro.registroDeJson`:
  - `enRango(s.duracionMin(), 1, 600, donde + ": duración")`.
  - nota: `null` o solo espacios → `null`; si no, `strip()`; más de 500 caracteres (después del strip) → `IllegalArgumentException(donde + ", ejercicio " + bloque + ": la nota tiene más de 500 caracteres")`.
  - Las sesiones y ejercicios que arma conservan `duracionMin` y la nota limpia.
- Pestaña **Registro** de la PC: muestra la duración de cada sesión (`"75 min"`, vacío si es null) y la nota de cada ejercicio (vacía si es null). El texto lo arma un método estático testeable, no la interfaz.
- Todos los `new SesionRegistro(...)` / `new EjercicioRegistro(...)` existentes (código y tests) pasan `null` en el campo nuevo.

---

# T13 — Textos de duración y notas para la pestaña Registro

## Objetivo
Completar `TextosRegistro.duracion` y `TextosRegistro.notas`, y los tres tests de `TextosRegistroTest` (los helpers `ej` y `plan` del test ya están: no los toques). La interfaz solo va a llamar a estos métodos.

## Código que ya existe
- Records, se leen con `x()`: `SesionPlan(int microciclo, int dia, List<EjercicioPlan> ejercicios)`; `EjercicioPlan(String bloque, String nombre, Tipo tipo, Aproximacion modo, Double rm, List<FilaPlan> filas)`; `SesionRegistro(int microciclo, int dia, String fecha, Integer descanso, Integer sensacionFinal, List<EjercicioRegistro> ejercicios, Integer duracionMin)`; `EjercicioRegistro(String bloque, Integer esfuerzo, Map<String,String> filas, String nota)`.
- `TextosRegistro` ya importa `ArrayList` y `List`.

## `duracion(Integer duracionMin)` (pasos en orden)
1. `if (duracionMin == null) return "";`
2. `return duracionMin + " min";`

## `notas(SesionPlan plan, SesionRegistro registro)` (pasos en orden)
1. `if (registro.ejercicios() == null) return "";`
2. `List<String> partes = new ArrayList<>();`
3. Recorrer los ejercicios del plan, y para cada uno buscar el del registro con el mismo bloque:
```java
        for (EjercicioPlan ej : plan.ejercicios()) {
            for (EjercicioRegistro er : registro.ejercicios()) {
                if (ej.bloque().equals(er.bloque()) && er.nota() != null && !er.nota().isBlank()) {
                    partes.add(ej.nombre() + ": " + er.nota().strip().replaceAll("\\s+", " "));
                    break;
                }
            }
        }
```
4. `return String.join(" | ", partes);`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `duracion`:
  - `assertEquals("75 min", TextosRegistro.duracion(75));`
  - `assertEquals("1 min", TextosRegistro.duracion(1));`
  - `assertEquals("", TextosRegistro.duracion(null));`
- `notas`:
  - `var sr = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-2", null, Map.of(), "  dolor leve  "), new EjercicioRegistro("1-1", 3, Map.of(), "subí 2,5 kg")), null);`
  - `assertEquals("Sentadilla: subí 2,5 kg | Press banca: dolor leve", TextosRegistro.notas(plan(), sr));`
  - `var sr2 = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("2-1", null, Map.of(), "línea uno\nlínea dos")), null);`
  - `assertEquals("Remo: línea uno línea dos", TextosRegistro.notas(plan(), sr2));`
- `notasVacias`:
  - `var sin = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-1", 3, Map.of(), null), new EjercicioRegistro("2-1", null, Map.of(), "   ")), null);`
  - `assertEquals("", TextosRegistro.notas(plan(), sin));`
  - `var nula = new SesionRegistro(1, 1, "2026-10-27", null, null, null, null);`
  - `assertEquals("", TextosRegistro.notas(plan(), nula));`

---

# Archivos actuales

```java
// src/main/java/com/rutina/registro/TextosRegistro.java
package com.rutina.registro;

import java.util.ArrayList;
import java.util.List;

/** Textos que muestra la pestaña Registro de la PC (sin Swing, para poder testearlos). */
public final class TextosRegistro {

    private TextosRegistro() {
    }

    /** "75 min" para 75; "" si no se midió (null). */
    public static String duracion(Integer duracionMin) {
        // TODO T13
        throw new UnsupportedOperationException();
    }

    /**
     * Notas de los ejercicios de una sesión, en el orden de los ejercicios del plan: "Nombre: nota" separadas por " | ".
     * Solo entran los ejercicios del plan que tienen nota no vacía en el registro (se empareja por bloque).
     * Los saltos de línea y espacios repetidos de la nota se reemplazan por un solo espacio. "" si no hay ninguna.
     */
    public static String notas(SesionPlan plan, SesionRegistro registro) {
        // TODO T13
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/rutina/registro/TextosRegistroTest.java
package com.rutina.registro;

import com.rutina.model.Tipo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextosRegistroTest {

    private static EjercicioPlan ej(String bloque, String nombre) {
        return new EjercicioPlan(bloque, nombre, Tipo.FUERZA, null, null, List.of());
    }

    private static SesionPlan plan() {
        return new SesionPlan(1, 1, List.of(ej("1-1", "Sentadilla"), ej("1-2", "Press banca"), ej("2-1", "Remo")));
    }

    @Test
    void duracion() {
        // TODO T13
        throw new UnsupportedOperationException();
    }

    @Test
    void notas() {
        // TODO T13
        throw new UnsupportedOperationException();
    }

    @Test
    void notasVacias() {
        // TODO T13
        throw new UnsupportedOperationException();
    }
}
```

Completá los `// TODO` que pide la tarea T13. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/rutina/registro/TextosRegistro.java, src/test/java/com/rutina/registro/TextosRegistroTest.java.
