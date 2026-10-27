# Retrospectiva por pedido — rama llm/mejoras-pc

Una entrada por pedido: tarea, modelo, intento, resultado, qué falló, por qué y qué se cambia en el pedido siguiente.

## T11a — Gemma, intento 1 (EscalaEsfuerzo: intercambio de Pesado y Al límite)
- Resultado: ocultos 3/3 al primer pedido, 27 s, entrada 1438, salida 523, `finish_reason stop`. Archivo completo (enum, no `--metodos`: las constantes no son métodos). `git diff` limpio: solo javadoc y las dos constantes.
- Qué falló: nada. `evaluar.sh` marcó `CAMBIÓ: EscalaEsfuerzo` porque `javap` incluye el orden de las constantes del enum; el cambio era el pedido, la firma de referencia se regeneró.
- Qué se cambia: nada. Pedir el código literal de las líneas (no describir "intercambiá") sigue funcionando.

## T11b — Gemma, intento 1 (OpcionesRedondeo)
- Resultado: ocultos 5/5 al primer pedido con `--metodos`, 17 s, entrada 1342, salida 304. Tests propios copiados tal cual, sin comentarios.
- Decisión mía: la lógica del combo (índice ↔ valor) salió de `PanelPlantillas` a `service.calculo.OpcionesRedondeo` para poder testearla sin Swing; el cableado del combo lo hice yo.
- Qué se cambia: nada.

## T12 — Gemma, intento 1 (JsonRegistro.limpiarNota + tests de duración y nota)
- Resultado: ocultos 7/7 al primer pedido con `--metodos`, 46 s, entrada 3737, salida 655. `git diff` fuera del método pedido: nada (lo único fuera de `limpiarNota` es mi cableado en `registroDeJson`). Tests propios con las 12 aserciones de la tarea, sin comentarios.
- Qué ayudó: yo dejé hecho en `registroDeJson` la llamada, el `enRango` de la duración y el armado de los records; el modelo recibió solo el helper de 4 pasos y los tests con los strings JSON ya escapados (las comillas escapadas se copiaron bien).
- Qué se cambia: nada.

## T13 — Gemma, intento 1 (TextosRegistro: duración y notas)
- Resultado: ocultos 6/6 al primer pedido con `--metodos`, 49 s, entrada 2309, salida 678. Tests propios copiados sin comentarios; el `git diff` no toca nada fuera de los dos métodos.
- Qué ayudó: el bucle anidado con `break` dado en Java literal (regla "construcciones raras en Java, no en prosa"); los ejemplos de la tarea con los `\n` ya escritos como en el código.
- Cableado (Claude): `PanelMesociclo.llenarRegistro` agrega las columnas "Duración" (después de "Final") y "Notas" (última, 420 px) llamando a `TextosRegistro`. Para no tocar `ResumenSesion` busco la `SesionRegistro` y la `SesionPlan` por (micro, día).
- Qué se cambia: nada. Balance de la rama: 4 subtareas, 4 al primer pedido con Gemma, 0 correcciones; ningún código de lógica lo escribí yo.
