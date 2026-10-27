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

## Antes de pedir (Tarea 3)
- `Almacen` se partió en cuatro subtareas con el cableado hecho por Claude (rutas `archivoPlan/archivoRegistro/archivoPreferencias`, `plan(id)`, `registro(id)`, `guardarRegistro`) y los helpers de disco como métodos package-private estáticos, para poder probarlos sin depender del resto.
- `verificar_tarea.py` ganó `--con otra.md,…`: aplica antes los pasos de las subtareas de las que depende (T34b necesita T34a, T34c necesita T34a+T34b, T34d necesita T34a). Primera corrida: 3 de 4 fallaban por dependencias de los ocultos (usaban `planes()` o `escribirAtomico` de otra subtarea). Cambio: cada oculto usa solo lo de su subtarea más lo declarado en `--con`; y un `T34zFocosOcultoTest` aparte, con todo hecho, para los focos 1–4 del plan.
- Se agregó al contrato (secciones 14) lo que no estaba: BOM `\uFEFF` al principio del texto (WhatsApp/Windows) y que `borrarPlan` deja `activo()` en `null` si era el activo.

## T34a — disco (qwen, intentos 1 y 2)
Intento 1: 34 s, entrada 4703, salida 329. Los 5 métodos salieron literales y 5/5 ocultos, pero **no devolvió el bloque del test** (`FALTA en la respuesta AlmacenTest`). Causa: con dos archivos en el pedido y `--metodos` contestó solo el primero; la tarea no decía cuántos bloques devolver. Intento 2 con `--solo AlmacenTest.java --metodos` (3029 de entrada, 25 s): `escribeYRelee` y `apartaElRoto` bien. Cambio: las tareas siguientes dicen "Devolvé DOS bloques: uno con los métodos de X y otro con los tests de Y".

## T34d — `cargarDesdeTexto` (qwen, intentos 1 y 2)
Intento 1: 199 s, entrada 4931, salida 2035. **Devolvió el archivo entero** (en vez de solo los métodos), sin ``` ni `//` delante de la ruta (una línea suelta `android/app/src/….java`), y otra vez sin el test. El código de `cargarDesdeTexto` era exactamente el de los pasos. `pedir.py` no extrajo nada. Cambios: (1) `RUTA_LINEA` acepta la ruta sin `//`; con `--reextraer` el injerto por nombre sacó el método bien (y los otros métodos, idénticos, no cambiaron); (2) intento 2 solo del test (3324 de entrada, 29 s). 5/5 ocultos. Lección: con 20 pasos de código y ~5000 tokens de entrada qwen empieza a ignorar "solo los métodos"; conviene dar menos contexto (el esqueleto entero del archivo pesa).

## T34b — preferencias (Gemma, intento 1)
50 s, entrada 5719, salida 614. 5/5 ocultos; devolvió los dos bloques (por la frase nueva). Nada que corregir.

## T34c — planes, borrar, cronómetros (Gemma, intento 1)
90 s, entrada 6594, salida 1088. 4/4 ocultos; dos bloques. El `try-with-resources` con `DirectoryStream` y el `keySet()` copiado antes de borrar salieron literales. Nada que corregir.

## Total Tarea 3
`T34z` (focos 1–4 con todo junto): 7/7. 0 correcciones de código; 2 pedidos extra de solo test (T34a, T34d) por bloques faltantes.

## T35 — textos de la pantalla de la sesión (Gemma, intento 1)
Verificada antes de mandar (5/5 con los pasos tal cual). 52 s, entrada 3128, salida 870, dos bloques (la frase "Devolvé DOS bloques" funcionó). 5/5 ocultos, 0 correcciones. `TextosSesion` deja los textos (título, fecha, reps, error y aviso de la casilla) como funciones puras y la `VistaEjercicio` de Claude solo los pone en pantalla.

## Tarea 6 (Claude): lo que no se delegó y por qué
- `VistaEjercicio`, `SesionActivity`, `Escalas`, layouts XML: vistas y cableado, hechos por Claude (regla de qwen-aprendizajes: el modelo solo recibe lógica pura).
- Se sacó del Activity la regla "cuándo guardar" a `GuardadoSesion.hayQueGuardar` (con 5 tests de Claude) porque el Spinner dispara `onItemSelected` solo al abrir la pantalla y sin esa regla abrir una sesión creaba una sesión vacía con fecha.
