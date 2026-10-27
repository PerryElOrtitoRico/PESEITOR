# RUTINEITOR — mejoras de octubre (contrato)

Mejoras de la nota `Mejorias.md` del vault (27/10/2026). El programa de PC es Java 21 + Swing; la app del celular es `celular/` (HTML + JS sin librerías). El formato del registro y la notación están en `docs/registro-sesiones/00-especificacion.md`.

## 1. Reglas generales
- Java: paquete base `com.rutina`; el registro está en `com.rutina.registro`. **Los datos son records y se leen con `x()`**: `s.microciclo()`, `e.filas()`, `s.sensacionFinal()`. No existen `getX()`.
- JavaScript (`celular/logica.js`): archivo clásico, sin `import`/`export`, un único objeto global `const Logica = { ... };`. Funciones puras: no tocan el DOM ni `localStorage` y no modifican sus argumentos.
- Comentarios y mensajes en español.

## 2. Escala de esfuerzo (mejora 5.4)
Se intercambian los textos de 4 y 5. **Al límite** = salió, pero sin ninguna reserva; **Pesado** = fue demasiado, no salió completo.

| Valor | Texto |
|---|---|
| 1 | Muy liviano |
| 2 | Liviano |
| 3 | Justo |
| 4 | Al límite |
| 5 | Pesado |

- Java `EscalaEsfuerzo`: `AL_LIMITE(4, "Al límite")`, `PESADO(5, "Pesado")` (en ese orden); javadoc de la clase: `1 = muy liviano, 5 = pesado (no salió)`.
- Celular `index.html`, `ESCALAS.esfuerzo`: `[4, "Al límite"], [5, "Pesado"]`.
- `docs/registro-sesiones/00-especificacion.md` sección 2: la misma línea con el orden nuevo.

## 3. Redondeo de kg (mejora 2)
El cálculo ya redondea al múltiplo de `Metodologia.incrementoKg()`; la base del usuario tenía 1 kg. En `PanelPlantillas` el combo "Redondeo de kg" pasa a tener tres opciones `{"1 kg", "2,5 kg", "5 kg"}` ↔ `1.0, 2.5, 5.0` (al cargar: el índice del valor; si no coincide ninguno, "2,5 kg"; al guardar: el valor del índice). Nada más cambia.

## 4. Registro: campos nuevos (mejoras 5.1 y 5.3)
Opcionales y compatibles: la versión del registro sigue siendo 1, y un registro viejo sin estos campos se lee igual.
- Sesión: `"duracionMin"`: entero de 1 a 600, o `null`/ausente (no se midió).
- Ejercicio: `"nota"`: texto, o `null`/ausente.

Java:
- `SesionRegistro(int microciclo, int dia, String fecha, Integer descanso, @SerializedName("final") Integer sensacionFinal, List<EjercicioRegistro> ejercicios, Integer duracionMin)` (el campo nuevo al final).
- `EjercicioRegistro(String bloque, Integer esfuerzo, Map<String, String> filas, String nota)` (al final).
- `JsonRegistro.registroDeJson`:
  - `enRango(s.duracionMin(), 1, 600, donde + ": duración")`.
  - nota: `null` o solo espacios → `null`; si no, `strip()`; más de 500 caracteres (después del strip) → `IllegalArgumentException(donde + ", ejercicio " + bloque + ": la nota tiene más de 500 caracteres")`.
  - Las sesiones y ejercicios que arma conservan `duracionMin` y la nota limpia.
- Pestaña **Registro** de la PC: muestra la duración de cada sesión (`"75 min"`, vacío si es null) y la nota de cada ejercicio (vacía si es null). El texto lo arma un método estático testeable, no la interfaz.
- Todos los `new SesionRegistro(...)` / `new EjercicioRegistro(...)` existentes (código y tests) pasan `null` en el campo nuevo.

## 5. Celular: total de repeticiones (mejora 5.2)
Debajo del nombre de cada ejercicio: `Total: <hechas> reps (planificadas <plan>)`; si alguna casilla es inválida, `Total: — (planificadas <plan>)`.

```js
/** Suma de reps de una lista de series [{reps, kg}]. [] → 0. */
totalReps(series) {},

/** filasPlan: [{etiqueta, series}] del plan; textos: { etiqueta: texto } anotado.
 *  Por fila: texto ausente o en blanco → reps planificadas de esa fila; texto válido → reps de parseNotacion(texto, series);
 *  texto inválido → todo el resultado tiene hechas = null.
 *  Devuelve { planificadas, hechas }. */
totalesEjercicio(filasPlan, textos) {},
```
Ejemplos: filas `[{etiqueta:"A", series:[{reps:3,kg:80},{reps:3,kg:80}]}, {etiqueta:"B", series:[{reps:5,kg:60}]}]`:
- `textos {}` → `{planificadas: 11, hechas: 11}`
- `{A: "3.2"}` → `{planificadas: 11, hechas: 10}`
- `{B: "0"}` → `{planificadas: 11, hechas: 6}`
- `{A: "x"}` → `{planificadas: 11, hechas: null}`

## 6. Celular: notas por ejercicio (mejora 5.1)
Un `<textarea>` "Notas" debajo de las filas de cada ejercicio (máx. 500 caracteres, `maxlength`). Se guarda en el ejercicio como `nota`.
`Logica.limpiarSesion` cambia: a cada ejercicio le recorta la nota (`trim`); nota vacía → se borra la clave `nota`; un ejercicio se conserva si tiene filas **o** esfuerzo **o** nota.

## 7. Celular: cronómetro de la sesión (mejora 5.3)
En la pantalla de la sesión, arriba: el tiempo `h:mm:ss` y los botones **Iniciar** / **Pausar** / **Reanudar** y **Editar duración**. El estado se guarda en `localStorage` por plan + microciclo + día, así sobrevive a cerrar el navegador; el reloj sigue corriendo con la app cerrada (se calcula con la hora del sistema).

```js
/** Estado: { acumuladoMs, desde } (desde = epoch ms si está corriendo, null si está parado). */
cronoNuevo() {},                 // → { acumuladoMs: 0, desde: null }
cronoIniciar(estado, ahora) {},  // si ya corre, devuelve una copia igual; si no, { acumuladoMs, desde: ahora }
cronoPausar(estado, ahora) {},   // si corre: { acumuladoMs: acumuladoMs + (ahora - desde), desde: null }; si no, copia igual
cronoMs(estado, ahora) {},       // acumuladoMs + (desde === null ? 0 : ahora - desde)
cronoTexto(ms) {},               // "h:mm:ss" sin redondear: 0 → "0:00:00", 75_500 → "0:01:15", 3_725_000 → "1:02:05"
minutosDe(ms) {},                // 0 → null; si no, Math.max(1, Math.round(ms / 60000)): 29_999 → 1, 90_000 → 2, 4_500_000 → 75
/** "Editar duración": texto que escribe el usuario en minutos → entero 1..600, o null si es inválido.
 *  Acepta espacios alrededor; solo dígitos. "75" → 75, " 5 " → 5, "0" → null, "601" → null, "7,5" → null, "" → null. */
duracionValida(texto) {},
```
"Editar duración" (el botón seguro por si te olvidaste de pausar): `prompt("Duración en minutos", <minutos actuales>)`; si `duracionValida` da un número n, el estado pasa a `{ acumuladoMs: n * 60000, desde: null }`; si da null, aviso "Escribí los minutos, de 1 a 600" y no cambia nada.
Cada vez que se guarda la sesión: `duracionMin = minutosDe(cronoMs(estado, Date.now()))` (null si nunca se inició).

## 8. Compartir desde Android (mejora 4)
RUTINEITOR aparece en el menú **Compartir** de Android para recibir el **plan** (el `.json` que manda la PC por WhatsApp). Funciona solo con la app instalada desde Chrome ("Instalar app"/"Agregar a pantalla principal") desde la página publicada.
- `manifest.webmanifest` agrega:
```json
"share_target": {
  "action": "./compartir",
  "method": "POST",
  "enctype": "multipart/form-data",
  "params": { "files": [{ "name": "plan", "accept": ["application/json", ".json", "text/plain", "application/octet-stream"] }] }
}
```
- `sw.js`: `CACHE = "rutineitor-v3"`. Si el pedido es `POST` y la ruta termina en `/compartir`: lee `formData()`, toma el archivo `plan`, lo guarda con `cache.put("./compartido.json", new Response(texto))` en el cache `"rutineitor-compartido"` (el `activate` no lo borra) y responde `Response.redirect("./?compartido=1", 303)`.
- `index.html` al iniciar: si la URL tiene `compartido=1`, lee `./compartido.json` de ese cache, lo carga como si se hubiera elegido con "Cargar plan" (mismo camino y mismos errores que `cargarPlan`), lo borra del cache y deja la URL en `./` con `history.replaceState`.

## 9. No-objetivos
- No se cambia la notación, el formato del plan ni el cálculo del mesociclo.
- No se publica nada (`git push` a `pages`/`gh-pages`): lo decide el usuario.
