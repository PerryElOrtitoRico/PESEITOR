<!-- contrato: 10,14 | archivos: android/app/src/main/java/com/rutina/app/datos/Almacen.java, android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java -->
# T34d — Almacén: cargar un plan o un registro desde texto

## Objetivo
Completar en `Almacen` solo `cargarDesdeTexto` (marcado `// TODO T34d`) y el test `cargaPlanYRegistro` de `AlmacenTest`. `plan(long)`, `guardarRegistro`, `escribirAtomico` y `archivoPlan` ya andan: usalos. No cambies los imports ni los helpers del test.

## Código que ya existe
- `PlanCelular.FORMATO` = `"rutineitor-plan"`, `RegistroCelular.FORMATO` = `"rutineitor-registro"`; `plan.mesocicloId()` y `registro.mesocicloId()` son `long`.
- `JsonRegistro.planDeJson(String)`, `JsonRegistro.planAJson(PlanCelular)`, `JsonRegistro.registroDeJson(String)`: los dos primeros lanzan `IllegalArgumentException` si el texto es inválido.
- `Almacen.plan(long id)` → `PlanCelular` o `null`; `Almacen.guardarRegistro(RegistroCelular)`; `Almacen.escribirAtomico(Path, String)`; `Almacen.archivoPlan(long id)` → `Path`.
- Gson: `JsonParser.parseString(String)` → `JsonElement`.

## `cargarDesdeTexto(String json)` (pasos en orden)
1. `String texto = json.replace("\uFEFF", "").strip();`
2. `String formato = null;`
3. `try {`
4. `    JsonElement e = JsonParser.parseString(texto);`
5. `    if (e.isJsonObject() && e.getAsJsonObject().has("formato")) formato = e.getAsJsonObject().get("formato").getAsString();`
6. `} catch (JsonParseException | IllegalStateException | UnsupportedOperationException ex) {`
7. `    throw new IllegalArgumentException("El archivo no es un JSON válido: " + ex.getMessage());`
8. `}`
9. `if (PlanCelular.FORMATO.equals(formato)) {`
10. `    PlanCelular plan = JsonRegistro.planDeJson(texto);`
11. `    escribirAtomico(archivoPlan(plan.mesocicloId()), JsonRegistro.planAJson(plan));`
12. `    return plan;`
13. `}`
14. `if (RegistroCelular.FORMATO.equals(formato)) {`
15. `    RegistroCelular registro = JsonRegistro.registroDeJson(texto);`
16. `    if (plan(registro.mesocicloId()) == null) throw new IllegalArgumentException("Primero cargá el plan de este registro: no hay ningún plan con el id " + registro.mesocicloId());`
17. `    guardarRegistro(registro);`
18. `    return registro;`
19. `}`
20. `throw new IllegalArgumentException("El archivo no es un plan ni un registro de RUTINEITOR");`

## Tests (copiá estas aserciones tal cual, sin comentarios; `ejemplo(nombre)` lee un archivo de ejemplo)
- `cargaPlanYRegistro`:
  - `Almacen a = new Almacen(dir);`
  - `IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto(ejemplo("registro-ejemplo.json")));`
  - `assertTrue(e.getMessage().contains("no hay ningún plan"));`
  - `Object p = a.cargarDesdeTexto("  " + ejemplo("plan-ejemplo.json") + "  ");`
  - `assertInstanceOf(PlanCelular.class, p);`
  - `assertEquals(3L, a.plan(3).mesocicloId());`
  - `Object r = a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));`
  - `assertInstanceOf(RegistroCelular.class, r);`
  - `a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));`
  - `assertEquals(2, a.registro(3).sesiones().size());`
  - `assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto("{roto"));`
