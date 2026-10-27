# Mejoras de octubre programadas con qwen y Gemma — plan

Origen: `Mejorias.md` del vault (Obsidian), 27/10/2026. Contrato: `00-especificacion.md`.

| Mejora | Qué | Sección | Rama |
|---|---|---|---|
| 2 | Redondeo de kg: opción "5 kg" en Plantillas (la base del usuario tenía 1 kg: lo cambia él en Plantillas → Redondeo de kg) | 3 | `llm/mejoras-pc` |
| 5.4 | Intercambiar "Pesado" y "Al límite" (PC y celular) | 2 | las dos (cada una su lado) |
| 5.1 / 5.3 (PC) | Leer `nota` y `duracionMin` del registro y mostrarlos en la pestaña Registro | 4 | `llm/mejoras-pc` |
| 5.2 | Total de repeticiones por ejercicio en el celular | 5 | `llm/mejoras-celular` |
| 5.1 | Notas por ejercicio en el celular | 6 | `llm/mejoras-celular` |
| 5.3 | Cronómetro de la sesión con "Editar duración" | 7 | `llm/mejoras-celular` |
| 4 | RUTINEITOR en el menú Compartir de Android (share target) | 8 | `llm/mejoras-celular` |

## Reparto
- **Opus** (sesión principal): este plan y el contrato; al final revisa las dos ramas, las une en `main` y corre todo.
- **Un subagente Sonnet por rama**, cada uno en su worktree, lleva el ciclo con los modelos locales: arma esqueletos, tareas (`tareas/T1x` PC, `tareas/T2x` celular) y tests ocultos (`evaluacion/`) a partir del contrato, verifica que los pasos de cada tarea pasen los ocultos antes de mandarla, y después pide, evalúa y corrige.
- Los modelos escriben el código. Claude arma la interfaz (componentes, DOM, listeners) y los modelos completan la lógica con pasos de código (ver `qwen-aprendizajes.md`).

## Herramientas
- `modelo.sh <qwen|gemma> <comando>`: candado de GPU compartido por los dos subagentes; carga el modelo si hace falta y corre el comando. **Todo pedido pasa por acá**: `docs/llm/modelo.sh gemma python3 docs/llm/pedir.py docs/llm/tareas/T21-….md gemma 1`.
- `pedir.py <tarea> <qwen|gemma> <intento> [--metodos] [--solo archivo] [--error log] [--razonar low|medium]`: Java o JS según la extensión del primer archivo de la tarea (JS: siempre archivo completo, sin `--metodos`). Pedidos y respuestas en `registros/`.
- `aceptar.sh T1x archivos…`: borra los `// TODO T1x` que quedaron.
- `mejoras-qwen-pc.md` / `mejoras-qwen-celular.md`: retrospectiva por pedido.

## Planilla
| Subtarea | Modelo | Ocultos | Correcciones | Quién |
|---|---|---|---|---|
| T11a escala de esfuerzo (Pesado/Al límite) | Gemma | 3/3 | 0 | Gemma |
| T11b opciones del combo de redondeo + cableado en PanelPlantillas | Gemma | 5/5 | 0 | Gemma (cableado: Claude) |
| T12 nota del registro (JsonRegistro.limpiarNota + tests) | Gemma | 7/7 | 0 | Gemma (cableado y null en llamados: Claude) |
| T13 textos de duración y notas (TextosRegistro) + columnas en la pestaña Registro | Gemma | 6/6 | 0 | Gemma (cableado: Claude) |
| T21 totales de reps (`totalReps`, `totalesEjercicio`) | Gemma | 4/4 | 0 | Gemma |
| T22 funciones del cronómetro (7 funciones) | Gemma | 7/7 | 0 | Gemma |
| T23 `limpiarSesion` con nota | qwen | 2/2 (+15 viejas) | 0 | qwen |
| T24 share target en `sw.js` | qwen | 4/4 | 0 | qwen |

## Android (rama `llm/android`, `tareas/T3x`)
Plan: `docs/superpowers/plans/2026-10-27-app-android.md`. Contrato: secciones 10 a 14 de `00-especificacion.md`. Herramientas: `evaluar-android.sh <Txx>`, `verificar_tarea.py <tarea.md>` (antes de pedir), `pedir.py` con rutas `android/…`. Retrospectiva: `mejoras-qwen-android.md`.

| Subtarea | Modelo | Ocultos | Correcciones | Quién |
|---|---|---|---|---|
| T31a `Sesiones`: búsquedas, registroVacio, pista, nombreArchivoRegistro | Gemma | 5/5 | 0 | Gemma |
| T31b `Sesiones`: limpiarSesion, guardarSesion | qwen | 6/6 | 0 | qwen |
| T32 `Totales` | Gemma | 3/3 | 0 | Gemma |
| T33 `Crono` | Gemma | 5/5 | 0 | Gemma |
| T34a `Almacen`: escritura atómica y lectura con respaldo | qwen | 5/5 | 0 (+1 pedido solo del test) | qwen |
| T34d `Almacen.cargarDesdeTexto` | qwen | 5/5 | 0 (+1 pedido solo del test; extractor ajustado) | qwen |
| T34b `Almacen`: preferencias | Gemma | 5/5 | 0 | Gemma |
| T34c `Almacen`: planes, borrar, cronómetros | Gemma | 4/4 (+7/7 focos T34z) | 0 | Gemma |
