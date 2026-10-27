# Retrospectiva por pedido — rama llm/mejoras-celular

Una entrada por pedido: tarea, modelo, intento, resultado, qué falló, por qué y qué se cambia en el pedido siguiente.

## T21 — totales de reps · Gemma · intento 1 · OK (4/4 ocultos de T21)
- 174 s, entrada 3836, salida 2777 tokens, `finish_reason: stop`. Copió los pasos tal cual; `git diff` limpio fuera de las funciones pedidas.
- Único ruido: `pedir.py` deja los `// TODO Txx` de otras subtareas como `// lo completa otra subtarea (Txx): no lo cambies` en el archivo. Se agregó `docs/llm/restaurar.sh` para volverlos a `// TODO` después de cada pedido.
- Con archivo completo (JS) la respuesta es larga (2777 tokens para ~230 líneas): el contexto de 8192 alcanza para el intento 1 pero no deja mucho margen para correcciones; en T22 (7 funciones) vigilar `finish_reason`.

## T22 — funciones del cronómetro · Gemma · intento 1 · OK (7/7 ocultos de T22)
- 206 s, entrada 4345, salida 2932, `finish_reason: stop`. Las siete funciones copiadas de los pasos; diff limpio fuera de ellas. Sin cambios para el pedido siguiente (T23 con qwen: el archivo ya mide más, la entrada pasa de ~4400 tokens, así que sin razonamiento).

## T23 — `limpiarSesion` con nota · qwen · intento 1 · OK (2/2 ocultos de T23; las 15 pruebas viejas siguen en verde)
- 278 s, entrada 3708, salida 2871, `finish_reason: stop`. Copió los cinco pasos en el lugar del `// TODO T23`; diff limpio, el resto del archivo idéntico.
- El esqueleto con el cuerpo viejo funcionando + un `// TODO` donde cambiar (en vez de `throw`) funcionó bien para una modificación de función existente.
