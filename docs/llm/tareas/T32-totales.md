<!-- contrato: 10,12 | archivos: android/app/src/main/java/com/rutina/app/logica/Totales.java, android/app/src/test/java/com/rutina/app/logica/TotalesTest.java -->
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
