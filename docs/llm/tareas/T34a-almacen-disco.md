<!-- contrato: 10,14 | archivos: android/app/src/main/java/com/rutina/app/datos/Almacen.java, android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java -->
# T34a — Almacén: escritura atómica y lectura con respaldo del archivo roto

## Objetivo
Completar en `Almacen` solo los métodos marcados `// TODO T34a`: `leerTexto`, `escribirAtomico`, `apartarRoto`, `leerPlan` y `leerRegistro`; y los tests `escribeYRelee` y `apartaElRoto` de `AlmacenTest`. Los demás métodos (también los marcados con otras tareas) no se tocan. No cambies los imports, la constante ni los helpers del test (`dir`, `ejemplo`, `ponerPlan`, `cuantos`).

## Código que ya existe
- `JsonRegistro.planDeJson(String)` → `PlanCelular` y `JsonRegistro.registroDeJson(String)` → `RegistroCelular`: lanzan `IllegalArgumentException` si el texto no es válido (JSON roto, formato o versión distintos).

## `leerTexto(Path archivo)` (pasos en orden)
1. `return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);`

## `escribirAtomico(Path destino, String texto)` (pasos en orden)
1. `Files.createDirectories(destino.getParent());`
2. `Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");`
3. `Files.write(tmp, texto.getBytes(StandardCharsets.UTF_8));`
4. `Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);`

## `apartarRoto(Path archivo)` (pasos en orden)
1. `Path roto = archivo.resolveSibling(archivo.getFileName() + ".roto-" + System.currentTimeMillis());`
2. `try { Files.move(archivo, roto, StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) { }`

## `leerPlan(Path archivo)` (pasos en orden)
1. `if (!Files.exists(archivo)) return null;`
2. `try { return JsonRegistro.planDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }`

## `leerRegistro(Path archivo)` (pasos en orden)
1. `if (!Files.exists(archivo)) return null;`
2. `try { return JsonRegistro.registroDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `escribeYRelee`:
  - `Path destino = dir.resolve("a").resolve("x.json");`
  - `Almacen.escribirAtomico(destino, "uno");`
  - `assertEquals("uno", Almacen.leerTexto(destino));`
  - `Almacen.escribirAtomico(destino, "dos");`
  - `assertEquals("dos", Almacen.leerTexto(destino));`
  - `assertEquals(1, cuantos("a", ""));`
- `apartaElRoto`:
  - `Files.createDirectories(dir.resolve("planes"));`
  - `Files.write(dir.resolve("planes").resolve("9.json"), "{roto".getBytes(StandardCharsets.UTF_8));`
  - `assertNull(Almacen.leerPlan(dir.resolve("planes").resolve("9.json")));`
  - `assertFalse(Files.exists(dir.resolve("planes").resolve("9.json")));`
  - `assertEquals(1, cuantos("planes", "9.json.roto-"));`
  - `assertNull(Almacen.leerRegistro(dir.resolve("registros").resolve("1.json")));`
