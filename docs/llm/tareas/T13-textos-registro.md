<!-- contrato: 4 | archivos: src/main/java/com/rutina/registro/TextosRegistro.java, src/test/java/com/rutina/registro/TextosRegistroTest.java -->
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
