<!-- contrato: 1, 7 | archivos: celular/logica.js -->
# T22 — Funciones del cronómetro (`celular/logica.js`)

## Qué hacer
Completá solo las siete funciones marcadas `// TODO T22`. Las demás quedan exactamente como están. Devolvé el archivo completo. Todas son puras: devuelven objetos nuevos, nunca modifican `estado`.

## Pasos
`cronoNuevo()`:
```js
return { acumuladoMs: 0, desde: null };
```
`cronoIniciar(estado, ahora)`:
```js
if (estado.desde !== null) return { acumuladoMs: estado.acumuladoMs, desde: estado.desde };
return { acumuladoMs: estado.acumuladoMs, desde: ahora };
```
`cronoPausar(estado, ahora)`:
```js
if (estado.desde === null) return { acumuladoMs: estado.acumuladoMs, desde: null };
return { acumuladoMs: estado.acumuladoMs + (ahora - estado.desde), desde: null };
```
`cronoMs(estado, ahora)`:
```js
return estado.acumuladoMs + (estado.desde === null ? 0 : ahora - estado.desde);
```
`cronoTexto(ms)`:
```js
const total = Math.floor(ms / 1000);
const h = Math.floor(total / 3600);
const m = Math.floor((total % 3600) / 60);
const s = total % 60;
return h + ":" + String(m).padStart(2, "0") + ":" + String(s).padStart(2, "0");
```
`minutosDe(ms)`:
```js
if (!(ms > 0)) return null;
return Math.max(1, Math.round(ms / 60000));
```
`duracionValida(texto)`:
```js
if (typeof texto !== "string") return null;
const t = texto.trim();
if (!/^[0-9]+$/.test(t)) return null;
const n = Number(t);
if (n < 1 || n > 600) return null;
return n;
```

## No hagas
- No uses `Date.now()` ni toques la pantalla: la hora siempre llega por el parámetro `ahora`.
- No redondees los segundos en `cronoTexto` (999 ms → "0:00:00").
