<!-- contrato: 10,11 | archivos: android/app/src/main/java/com/rutina/app/logica/Sesiones.java, android/app/src/test/java/com/rutina/app/logica/SesionesTest.java -->
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
