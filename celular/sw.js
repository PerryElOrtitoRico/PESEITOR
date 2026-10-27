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
  const datos = await pedido.formData();
  const archivo = datos.get("plan");
  if (archivo) {
    const texto = await archivo.text();
    const cache = await caches.open(CACHE_COMPARTIDO);
    await cache.put("./compartido.json", new Response(texto));
  }
  return Response.redirect("./?compartido=1", 303);
}

// Primero el cache; si no está, la red.
self.addEventListener("fetch", evento => {
  if (evento.request.method === "POST" && new URL(evento.request.url).pathname.endsWith("/compartir")) {
    evento.respondWith(recibirPlanCompartido(evento.request));
    return;
  }
  if (evento.request.method !== "GET") return;
  evento.respondWith(caches.match(evento.request).then(respuesta => respuesta || fetch(evento.request)));
});
