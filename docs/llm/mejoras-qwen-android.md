# Retrospectiva por pedido — RUTINEITOR Android (rama `llm/android`)

Método: `qwen-aprendizajes.md`. Herramientas nuevas para Android: `docs/llm/evaluar-android.sh <Txx>` (corre propios, compara firmas contra `evaluacion/firmas-android/`, copia y corre los ocultos de `evaluacion/android/`), `docs/llm/verificar_tarea.py <tarea.md>` (aplica los pasos de código de la tarea al esqueleto, corre propios + ocultos y restaura el esqueleto) y `pedir.py` adaptado (rutas `android/…`, prompt con Java 17 y API 26).

## Antes de pedir (Tarea 2)
- **El verificador encontró dos errores míos en los ocultos antes de mandar nada**: `"5.5 · 60"` para tres series (eran `5.5.5 · 60`) y un oculto de T31b que llamaba a `sesionDelRegistro` (de T31a, todavía sin hacer). Cambio: cada oculto solo usa métodos de su propia subtarea; los records se arman con `new` en el test. Regla ya conocida ("verificar cada aserción antes de mandar"), confirmada otra vez.
- Los esqueletos compilan (`compileDebugUnitTestJavaWithJavac`) con los cuerpos en `// TODO Txx` + `throw new UnsupportedOperationException()`; los tests propios con el cuerpo vacío y `// TODO Txx` (los fixtures `plan()`, `SERIES`, `FILAS` ya puestos).

## T31a — `Sesiones` simples (Gemma, intento 1)
67 s, entrada 4125, salida 1075. 5/5 ocultos, propios en verde, firmas iguales, sin cambios fuera de los métodos. Los pasos de código (incluido el bucle con `return` adentro y el `textoKg` aparte) se copiaron literales; Gemma solo les puso llaves. Qué falló: nada. Qué cambio: nada.

## T31b — `limpiarSesion` y `guardarSesion` (qwen, intento 1)
90 s, entrada 4695, salida 985. 6/6 ocultos. El bucle anidado con `continue` dado en Java literal salió bien. Nada que corregir.

## T32 — `Totales` (Gemma, intento 1)
34 s, entrada 2511, salida 537. 3/3 ocultos. Nada que corregir.

## T33 — `Crono` (Gemma, intento 1)
75 s, entrada 3420, salida 1112. 5/5 ocultos. `Locale.ROOT` y el `try/catch` de `Long.parseLong` dados en Java literal salieron al primer intento. Nada que corregir.

## Lo que aprendí de la Tarea 2
- Con la misma receta de las ramas anteriores (esqueleto + pasos de código en orden + aserciones exactas + `--metodos` + verificación previa) las 4 subtareas de Android pasaron al primer pedido, 0 correcciones.
- `restaurar.sh` hay que correrlo sobre los archivos de la tarea tras cada pedido (incluido `--solo-ver`, que también reescribe los marcadores); si no, el siguiente `aceptar.sh` no encuentra los `// TODO`.
