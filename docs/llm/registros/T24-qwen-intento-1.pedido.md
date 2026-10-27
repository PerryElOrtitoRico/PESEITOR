<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, max_tokens 6591, razonamiento no -->

## system

Sos un programador JavaScript (ES2020) cuidadoso. Te paso archivos con funciones marcadas `// TODO`: completá solo las que nombra la tarea; las marcadas "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- Sin librerías, sin npm, sin import/export. logica.js define un único objeto global `const Logica = { ... };` con funciones puras (no tocan el DOM ni localStorage, no modifican sus argumentos).
- No cambies nombres ni parámetros de funciones, ni lo que ya está hecho. No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea como comentario: un bloque por archivo, nunca dos archivos en el mismo bloque. Ejemplo:
```js
// celular/ejemplo.js
"use strict";
const Ejemplo = { doble(x) { return 2 * x; } };
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# RUTINEITOR — mejoras de octubre (contrato)

## 1. Reglas generales
- Java: paquete base `com.rutina`; el registro está en `com.rutina.registro`. **Los datos son records y se leen con `x()`**: `s.microciclo()`, `e.filas()`, `s.sensacionFinal()`. No existen `getX()`.
- JavaScript (`celular/logica.js`): archivo clásico, sin `import`/`export`, un único objeto global `const Logica = { ... };`. Funciones puras: no tocan el DOM ni `localStorage` y no modifican sus argumentos.
- Comentarios y mensajes en español.

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

---

# T24 — Recibir el plan compartido en `celular/sw.js`

## Qué hacer
Completá los dos `// TODO T24` de `sw.js`: el cuerpo de `recibirPlanCompartido` y el principio del listener `fetch`. `CACHE`, `CACHE_COMPARTIDO`, `install` y `activate` quedan como están. Devolvé el archivo completo.

## Pasos de `recibirPlanCompartido(pedido)`
```js
const datos = await pedido.formData();
const archivo = datos.get("plan");
if (archivo) {
  const texto = await archivo.text();
  const cache = await caches.open(CACHE_COMPARTIDO);
  await cache.put("./compartido.json", new Response(texto));
}
return Response.redirect("./?compartido=1", 303);
```

## Listener `fetch`
Antes de la línea `if (evento.request.method !== "GET") return;` (que se conserva) va este bloque:
```js
if (evento.request.method === "POST" && new URL(evento.request.url).pathname.endsWith("/compartir")) {
  evento.respondWith(recibirPlanCompartido(evento.request));
  return;
}
```
Un `GET` sigue yendo primero al cache y después a la red, como ahora.

## No hagas
- No uses `import`/`export`.
- No borres el cache `rutineitor-compartido` ni cambies el nombre de los caches.

---

# Archivos actuales

```js
// celular/sw.js
// Service worker de RUTINEITOR Registro: guarda la app en el cache para que abra sin conexión.
// Al cambiar cualquier archivo de celular/, subir la versión (rutineitor-v3, v4…) para que el celular la actualice.
"use strict";

const CACHE = "rutineitor-v3";
const CACHE_COMPARTIDO = "rutineitor-compartido";   // el plan que llega por Compartir; el activate no lo borra
const ARCHIVOS = ["./", "./index.html", "./logica.js", "./manifest.webmanifest", "./icono.png"];

self.addEventListener("install", evento => {
  evento.waitUntil(caches.open(CACHE).then(cache => cache.addAll(ARCHIVOS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", evento => {
  evento.waitUntil(
    caches.keys()
      .then(claves => Promise.all(claves.filter(c => c.startsWith("rutineitor-") && c !== CACHE && c !== CACHE_COMPARTIDO).map(c => caches.delete(c))))
      .then(() => self.clients.claim()));
});

/** Recibe el plan que Android manda por Compartir (POST multipart a ./compartir). */
async function recibirPlanCompartido(pedido) {
  // TODO T24
  throw new Error("T24");
}

// Primero el cache; si no está, la red.
self.addEventListener("fetch", evento => {
  // TODO T24
  if (evento.request.method !== "GET") return;
  evento.respondWith(caches.match(evento.request).then(respuesta => respuesta || fetch(evento.request)));
});
```

Completá los `// TODO` que pide la tarea T24. Devolvé estos archivos completos, cada uno en su bloque: celular/sw.js.
