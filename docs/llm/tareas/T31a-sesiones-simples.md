<!-- contrato: 10,11 | archivos: android/app/src/main/java/com/rutina/app/logica/Sesiones.java, android/app/src/test/java/com/rutina/app/logica/SesionesTest.java -->
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
