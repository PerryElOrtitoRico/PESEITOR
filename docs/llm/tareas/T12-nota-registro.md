<!-- contrato: 4 | archivos: src/main/java/com/rutina/registro/JsonRegistro.java, src/test/java/com/rutina/registro/JsonRegistroCamposNuevosTest.java -->
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
