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

## 10. App de Android: reglas generales
La app nativa vive en `android/` (Gradle, Java 17, AndroidX; minSdk 26). Reemplaza a `celular/`, con la misma lógica en Java. La lógica va en `com.rutina.app.logica` (sesión, totales, cronómetro) y `com.rutina.app.datos` (archivos), **sin tocar nada de `android.*`**: se prueba con JUnit 5 en la JVM.
- Los records del registro se comparten con la PC y se leen con `x()`: `PlanCelular(formato, version, perfil, mesocicloId, inicio, microciclos, dias, sesiones)`, `SesionPlan(microciclo, dia, ejercicios)`, `EjercicioPlan(bloque, nombre, tipo, modo, rm, filas)`, `FilaPlan(etiqueta, pctMin, pctMax, series)`, `SerieReal(reps, kg)`, `RegistroCelular(formato, version, perfil, mesocicloId, sesiones)`, `SesionRegistro(microciclo, dia, fecha, descanso, sensacionFinal, ejercicios, duracionMin)`, `EjercicioRegistro(bloque, esfuerzo, filas, nota)`. `NotacionRegistro.parse(texto, planificadas)` y `NotacionRegistro.esValida(texto, planificadas)` ya existen. No existen `getX()`.
- Usar solo la API que hay en Android 8 (nivel 26): `java.nio.file.Files.readAllBytes/write/move/newDirectoryStream`, `java.nio.file.Paths.get`. **No** `Files.readString/writeString` ni `Path.of`.
- Los datos que reciben y devuelven las funciones son inmutables: nunca modificar un argumento; devolver listas y mapas nuevos.
- Es la traducción de `celular/logica.js`: las reglas son las mismas que las de las secciones 5 a 7 de este contrato.

## 11. Android: sesión (`Sesiones`)
Clase `com.rutina.app.logica.Sesiones`, solo métodos estáticos.
- `sesionDelPlan(PlanCelular plan, int micro, int dia)`: la `SesionPlan` con ese microciclo y día, o `null`.
- `sesionDelRegistro(RegistroCelular registro, int micro, int dia)`: la `SesionRegistro` con ese microciclo y día, o `null`.
- `registroVacio(PlanCelular plan)`: `RegistroCelular` con `RegistroCelular.FORMATO`, `RegistroCelular.VERSION`, el perfil y el `mesocicloId` del plan, y la lista de sesiones vacía.
- `limpiarSesion(SesionRegistro s)`: copia de la sesión sin ruido. A cada ejercicio: las filas con texto en blanco (o `null`) se sacan y el resto se recorta con `strip()`; la nota se recorta y, si queda en blanco o es `null`, queda `null`. Un ejercicio se conserva solo si tiene filas, o esfuerzo (no `null`), o nota. Se conserva el orden. El resto de la sesión (microciclo, día, fecha, descanso, sensación final, duración) no cambia.
- `guardarSesion(RegistroCelular r, SesionRegistro s)`: `RegistroCelular` nuevo con la sesión pasada por `limpiarSesion`, que reemplaza a la de igual microciclo y día si existe; las sesiones quedan ordenadas por microciclo y después por día. Mismo formato, versión, perfil y mesocicloId. No modifica `r`.
- `pista(List<SerieReal> series)`: las reps separadas por `.`; si alguna serie tiene kg mayor que 0, sigue `" · "` y los kg distintos (mayores que 0) en orden de aparición separados por `/`, sin decimales si son enteros (`120`, no `120.0`) y con coma decimal (`127,5`). Lista vacía → `""`. Ejemplos: `[3x120, 3x120, 3x127.5]` → `"3.3.3 · 120/127,5"`; `[12x0, 12x0]` → `"12.12"`; `[4x105, 4x105]` → `"4.4 · 105"`.
- `nombreArchivoRegistro(PlanCelular plan)`: `"registro-" + perfil con cada espacio cambiado por "_" + "-" + inicio + ".json"`; `"Franco Pérez"` y `"2026-10-26"` → `"registro-Franco_Pérez-2026-10-26.json"`.

## 12. Android: totales de repeticiones (`Totales`)
Clase `com.rutina.app.logica.Totales`.
- `totalReps(List<SerieReal> series)`: suma de las reps; lista vacía → 0.
- `totalesEjercicio(List<FilaPlan> filas, Map<String, String> textos)`: devuelve `int[]{planificadas, hechas}`. `planificadas` = suma de `totalReps(fila.series())`. `hechas`: por cada fila, si el texto de su etiqueta falta o está en blanco, cuentan las reps planificadas de esa fila; si es una notación válida (`NotacionRegistro.esValida(texto, fila.series())`), cuentan las reps de `NotacionRegistro.parse(texto, fila.series())`; si algún texto es inválido, `hechas = -1`.
Ejemplos con filas `A` (dos series de 3 reps, 80 kg) y `B` (una serie de 5 reps, 60 kg): textos vacíos → `{11, 11}`; `A = "3.2"` → `{11, 10}`; `B = "0"` → `{11, 6}`; `A = "x"` → `{11, -1}`.

## 13. Android: cronómetro (`Crono`, `EstadoCrono`)
`record EstadoCrono(long acumuladoMs, Long desde)` en `com.rutina.app.logica` (`desde` = hora del sistema en ms si corre, `null` si está parado). `Crono`, solo métodos estáticos:
- `nuevo()` → `EstadoCrono(0, null)`.
- `iniciar(estado, ahora)`: si ya corre devuelve un estado igual; si no, `EstadoCrono(acumuladoMs, ahora)`.
- `pausar(estado, ahora)`: si corre, `EstadoCrono(acumuladoMs + (ahora - desde), null)`; si no, un estado igual.
- `ms(estado, ahora)`: `acumuladoMs` más `ahora - desde` si corre. `ms(new EstadoCrono(60_000, 1_000L), 3_601_000)` = `3_660_000` (el cronómetro siguió corriendo con la app cerrada).
- `texto(long ms)`: `"h:mm:ss"` sin redondear (segundos hacia abajo), con `Locale.ROOT`: `0` → `"0:00:00"`, `75_500` → `"0:01:15"`, `3_725_000` → `"1:02:05"`.
- `minutosDe(long ms)` → `Integer`: `ms <= 0` → `null`; si no, `Math.max(1, Math.round(ms / 60000.0))` como entero: `29_999` → 1, `90_000` → 2, `4_500_000` → 75.
- `duracionValida(String texto)` → `Integer`: lo que escribe el usuario en minutos. `null` → `null`; se recorta con `strip()`; tiene que ser solo dígitos; el valor tiene que estar entre 1 y 600, si no `null`. `"75"` → 75, `" 5 "` → 5, `"0"`, `"601"`, `"7,5"`, `""` → `null`.

## 14. Android: almacenamiento (`Almacen`)
Clase `com.rutina.app.datos.Almacen`, sobre una carpeta raíz (en el celular, el almacenamiento interno de la app). Disposición: `planes/<mesocicloId>.json`, `registros/<mesocicloId>.json`, `preferencias.json`. Los planes y registros se guardan con `JsonRegistro.planAJson` / `registroAJson` (el mismo formato que se importa y se exporta).
- `Almacen(Path raiz)`.
- **Escritura atómica**: escribir en un archivo temporal de la misma carpeta (`<nombre>.tmp`) y moverlo con `Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)`. Crear la carpeta si no existe.
- **Archivo dañado**: si un archivo de `planes/` o `registros/` no se puede leer (JSON roto o formato inválido), se lo renombra a `<nombre>.roto-<epoch ms>` en la misma carpeta y se sigue como si no existiera; nunca se cierra la app ni se pierden los demás archivos.
- `cargarDesdeTexto(String json)` → `Object`: lee el campo `"formato"` del JSON (no el nombre del archivo, que puede no tener `.json`; el texto puede traer espacios o saltos de línea alrededor). `"rutineitor-plan"` → valida con `JsonRegistro.planDeJson`, lo guarda (reemplaza el de igual `mesocicloId`, **conservando su registro**) y devuelve el `PlanCelular`. `"rutineitor-registro"` → valida con `JsonRegistro.registroDeJson`; si no hay un plan guardado con su `mesocicloId` lanza `IllegalArgumentException("Primero cargá el plan de este registro: no hay ningún plan con el id " + mesocicloId)`; si lo hay, guarda ese registro (reemplaza el que había) y lo devuelve. El texto puede traer una marca BOM (`\uFEFF`) o espacios al principio y al final: se descartan. Cualquier otro caso (JSON roto, otro formato) lanza `IllegalArgumentException` con un mensaje en español.
- `planes()` → `List<PlanCelular>` ordenados por `inicio` (texto `AAAA-MM-DD`); `plan(long id)` → `PlanCelular` o `null`; `borrarPlan(long id)` borra el plan, su registro y sus cronómetros, y si era el plan activo deja `activo()` en `null`.
- `registro(long id)` → el `RegistroCelular` guardado, o `Sesiones.registroVacio(plan)` si no hay (y `null` si tampoco hay plan); `guardarRegistro(RegistroCelular r)`.
- Preferencias (`preferencias.json`, objeto JSON): `activo()` → `Long` (id del plan activo, `null` si no hay) y `activo(Long id)`; `oscuro()` → `boolean` (por defecto `false`) y `oscuro(boolean)`; `crono(long id, int micro, int dia)` → `EstadoCrono` (`Crono.nuevo()` si no hay) y `crono(long id, int micro, int dia, EstadoCrono e)`, guardado con la clave `"<id>-<micro>-<dia>"`.
