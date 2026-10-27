<!-- contrato: 1, 6 | archivos: celular/logica.js -->
# T23 — `limpiarSesion` con la nota de cada ejercicio (`celular/logica.js`)

## Qué hacer
Cambiá solo `limpiarSesion` (el `// TODO T23` marca dónde). Las demás funciones quedan como están. Devolvé el archivo completo.

## Cómo queda el bucle `for (const ej of sesion.ejercicios || [])`
El armado de `filas` y de `esfuerzo` queda igual que ahora. Después de calcular `esfuerzo`, los pasos son estos, en este orden (reemplazan al `if … continue` y al `ejercicios.push` actuales):
```js
const nota = typeof ej.nota === "string" ? ej.nota.trim() : "";
if (Object.keys(filas).length === 0 && esfuerzo === null && nota === "") continue;
const limpio = { bloque: ej.bloque, esfuerzo, filas };
if (nota !== "") limpio.nota = nota;
ejercicios.push(limpio);
```
Mantené el orden de las claves: `bloque`, `esfuerzo`, `filas` y, si hay nota, `nota` al final. Un ejercicio con nota vacía, en blanco, `null` o ausente **no** lleva la clave `nota`.

## No hagas
- No modifiques `sesion` ni sus ejercicios (se arma un objeto nuevo por ejercicio).
- No toques `guardarSesion` ni el resto.
