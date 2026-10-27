<!-- contrato: 1, 8 | archivos: celular/sw.js -->
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
