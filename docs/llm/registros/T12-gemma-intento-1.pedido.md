<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 4284, razonamiento no -->

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

# T12 — Nota del ejercicio en el registro

## Objetivo
Completar **solo** `JsonRegistro.limpiarNota` y los tres tests de `JsonRegistroCamposNuevosTest`. `registroDeJson` ya la llama (y ya valida `duracionMin`): no lo toques ni toques nada más de `JsonRegistro`.

## Código que ya existe
- `SesionRegistro(int microciclo, int dia, String fecha, Integer descanso, Integer sensacionFinal, List<EjercicioRegistro> ejercicios, Integer duracionMin)` y `EjercicioRegistro(String bloque, Integer esfuerzo, Map<String,String> filas, String nota)` son records: se leen con `s.duracionMin()` y `e.nota()`.
- `registroDeJson` llama `limpiarNota(e.nota(), donde + ", ejercicio " + e.bloque())`, así que `donde` ya viene con el lugar del error.

## `limpiarNota(nota, donde)` (pasos en orden)
1. `if (nota == null || nota.isBlank()) return null;`
2. `String limpia = nota.strip();`
3. `if (limpia.length() > 500) throw new IllegalArgumentException(donde + ": la nota tiene más de 500 caracteres");`
4. `return limpia;`

## Tests (copiá estas aserciones tal cual, sin comentarios; usá el método `con(...)` que ya está)
- `limpiaNota`:
  - `assertNull(JsonRegistro.limpiarNota(null, "x"));`
  - `assertNull(JsonRegistro.limpiarNota("   ", "x"));`
  - `assertEquals("hola", JsonRegistro.limpiarNota("  hola  ", "x"));`
  - `assertEquals(500, JsonRegistro.limpiarNota("a".repeat(500), "x").length());`
  - `var e = assertThrows(IllegalArgumentException.class, () -> JsonRegistro.limpiarNota("a".repeat(501), "sesión 1"));`
  - `assertEquals("sesión 1: la nota tiene más de 500 caracteres", e.getMessage());`
- `leeDuracionYNota`:
  - `var r = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":75,\"ejercicios\":[{\"bloque\":\"1-1\",\"nota\":\"  subí 2,5 kg  \"}]}]"));`
  - `assertEquals(75, r.sesiones().get(0).duracionMin());`
  - `assertEquals("subí 2,5 kg", r.sesiones().get(0).ejercicios().get(0).nota());`
  - `var vieja = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"ejercicios\":[{\"bloque\":\"1-1\"}]}]"));`
  - `assertNull(vieja.sesiones().get(0).duracionMin());`
  - `assertNull(vieja.sesiones().get(0).ejercicios().get(0).nota());`
- `duracionFueraDeRango`:
  - `assertThrows(IllegalArgumentException.class, () -> JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":0}]")));`
  - `assertThrows(IllegalArgumentException.class, () -> JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"duracionMin\":601}]")));`

---

# Archivos actuales

```java
// src/main/java/com/rutina/registro/JsonRegistro.java
package com.rutina.registro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lectura y escritura de los archivos que van y vienen del celular. */
public final class JsonRegistro {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private JsonRegistro() {
    }

    /** JSON con sangría; los null se escriben (por ejemplo "rm": null). */
    public static String planAJson(PlanCelular plan) {
        return GSON.toJson(plan);
    }

    /**
     * @throws IllegalArgumentException si no es un plan válido: formato distinto de "rutineitor-plan",
     *                                  version distinta de 1, o JSON mal formado (mensaje en español).
     */
    public static PlanCelular planDeJson(String json) {
        PlanCelular p;
        try {
            p = GSON.fromJson(json, PlanCelular.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("El archivo del plan no es un JSON válido: " + e.getMessage(), e);
        }
        if (p == null) throw new IllegalArgumentException("El archivo del plan está vacío");
        if (!PlanCelular.FORMATO.equals(p.formato())) {
            throw new IllegalArgumentException("No es un plan de RUTINEITOR (formato \"" + p.formato() + "\", se esperaba \""
                    + PlanCelular.FORMATO + "\")");
        }
        if (p.version() != PlanCelular.VERSION) {
            throw new IllegalArgumentException("Versión del plan no soportada: " + p.version() + " (se esperaba "
                    + PlanCelular.VERSION + ")");
        }
        if (p.sesiones() == null) throw new IllegalArgumentException("El plan no tiene la lista de sesiones");
        return p;
    }

    /** Escribe en UTF-8. */
    public static void escribirPlan(Path destino, PlanCelular plan) throws IOException {
        Files.writeString(destino, planAJson(plan), StandardCharsets.UTF_8);
    }

    /** @throws IllegalArgumentException si el archivo no es un registro válido (formato, versión, fechas, rangos, repetidos). */
    public static RegistroCelular registroDeJson(String json) {
        RegistroCelular r;
        try {
            r = GSON.fromJson(json, RegistroCelular.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("El archivo del registro no es un JSON válido: " + e.getMessage(), e);
        }
        if (r == null) throw new IllegalArgumentException("El archivo del registro está vacío");
        if (!RegistroCelular.FORMATO.equals(r.formato())) {
            throw new IllegalArgumentException("No es un registro de RUTINEITOR (formato \"" + r.formato() + "\", se esperaba \""
                    + RegistroCelular.FORMATO + "\")");
        }
        if (r.version() != RegistroCelular.VERSION) {
            throw new IllegalArgumentException("Versión del registro no soportada: " + r.version() + " (se esperaba "
                    + RegistroCelular.VERSION + ")");
        }
        List<SesionRegistro> sesiones = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (SesionRegistro s : r.sesiones() == null ? List.<SesionRegistro>of() : r.sesiones()) {
            if (s == null) throw new IllegalArgumentException("Hay una sesión vacía (null) en el registro");
            String donde = "sesión microciclo " + s.microciclo() + ", día " + s.dia();
            if (s.microciclo() < 1) throw new IllegalArgumentException(donde + ": el microciclo tiene que ser 1 o más");
            if (s.dia() < 1) throw new IllegalArgumentException(donde + ": el día tiene que ser 1 o más");
            if (s.fecha() == null) throw new IllegalArgumentException(donde + ": falta la fecha");
            try {
                LocalDate.parse(s.fecha());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(donde + ": fecha inválida \"" + s.fecha() + "\" (formato AAAA-MM-DD)");
            }
            enRango(s.descanso(), 1, 6, donde + ": descanso");
            enRango(s.sensacionFinal(), 1, 5, donde + ": sensación final");
            enRango(s.duracionMin(), 1, 600, donde + ": duración");
            if (!vistas.add(s.microciclo() + "-" + s.dia())) {
                throw new IllegalArgumentException(donde + ": está repetida en el registro");
            }
            List<EjercicioRegistro> ejercicios = new ArrayList<>();
            for (EjercicioRegistro e : s.ejercicios() == null ? List.<EjercicioRegistro>of() : s.ejercicios()) {
                if (e == null) throw new IllegalArgumentException(donde + ": hay un ejercicio vacío (null)");
                enRango(e.esfuerzo(), 1, 5, donde + ", ejercicio " + e.bloque() + ": esfuerzo");
                Map<String, String> filas = e.filas() == null ? Map.of() : e.filas();
                String nota = limpiarNota(e.nota(), donde + ", ejercicio " + e.bloque());
                ejercicios.add(new EjercicioRegistro(e.bloque(), e.esfuerzo(), filas, nota));
            }
            sesiones.add(new SesionRegistro(s.microciclo(), s.dia(), s.fecha(), s.descanso(), s.sensacionFinal(),
                    List.copyOf(ejercicios), s.duracionMin()));
        }
        return new RegistroCelular(r.formato(), r.version(), r.perfil(), r.mesocicloId(), List.copyOf(sesiones));
    }

    public static RegistroCelular leerRegistro(Path archivo) throws IOException {
        return registroDeJson(Files.readString(archivo, StandardCharsets.UTF_8));
    }

    /** JSON con sangría (la clave de la sensación final es "final"). */
    public static String registroAJson(RegistroCelular r) {
        return GSON.toJson(r);
    }

    /**
     * Nota limpia de un ejercicio: null o en blanco da null; si no, la nota sin espacios en los extremos.
     *
     * @param donde de qué sesión y ejercicio es (empieza el mensaje de error)
     * @throws IllegalArgumentException si, después del strip, tiene más de 500 caracteres
     */
    static String limpiarNota(String nota, String donde) {
        // TODO T12
        throw new UnsupportedOperationException();
    }

    private static void enRango(Integer valor, int min, int max, String que) {
        if (valor != null && (valor < min || valor > max)) {
            throw new IllegalArgumentException(que + " fuera de rango: " + valor + " (va de " + min + " a " + max + ")");
        }
    }
}
```

```java
// src/test/java/com/rutina/registro/JsonRegistroCamposNuevosTest.java
package com.rutina.registro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonRegistroCamposNuevosTest {

    private static String con(String sesiones) {
        return "{\"formato\":\"rutineitor-registro\",\"version\":1,\"perfil\":\"F\",\"mesocicloId\":3,\"sesiones\":" + sesiones + "}";
    }

    @Test
    void limpiaNota() {
        // TODO T12
        throw new UnsupportedOperationException();
    }

    @Test
    void leeDuracionYNota() {
        // TODO T12
        throw new UnsupportedOperationException();
    }

    @Test
    void duracionFueraDeRango() {
        // TODO T12
        throw new UnsupportedOperationException();
    }
}
```

Completá los `// TODO` que pide la tarea T12. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/rutina/registro/JsonRegistro.java, src/test/java/com/rutina/registro/JsonRegistroCamposNuevosTest.java.
