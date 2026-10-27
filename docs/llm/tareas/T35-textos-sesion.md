<!-- contrato: 10,15 | archivos: android/app/src/main/java/com/rutina/app/logica/TextosSesion.java, android/app/src/test/java/com/rutina/app/logica/TextosSesionTest.java -->
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
