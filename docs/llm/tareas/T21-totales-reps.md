<!-- contrato: 1, 5 | archivos: celular/logica.js -->
# T21 — Total de repeticiones (`celular/logica.js`)

## Qué hacer
Completá solo las dos funciones marcadas `// TODO T21`: `totalReps` y `totalesEjercicio`. Las demás funciones de `Logica` quedan exactamente como están (las marcadas "lo completa otra subtarea" no las toques). Devolvé el archivo completo.

## Pasos de `totalReps(series)`
```js
let total = 0;
for (const s of series) total += s.reps;
return total;
```

## Pasos de `totalesEjercicio(filasPlan, textos)`
Escribilos en este orden (usan `Logica.totalReps`, `Logica.esNotacionValida` y `Logica.parseNotacion`, que ya existen):
```js
let planificadas = 0;
let hechas = 0;
for (const fila of filasPlan) {
  const plan = Logica.totalReps(fila.series);
  planificadas += plan;
  if (hechas === null) continue;
  const texto = textos[fila.etiqueta];
  if (typeof texto !== "string" || texto.trim() === "") {
    hechas += plan;
    continue;
  }
  if (!Logica.esNotacionValida(texto, fila.series)) {
    hechas = null;
    continue;
  }
  hechas += Logica.totalReps(Logica.parseNotacion(texto, fila.series));
}
return { planificadas, hechas };
```
Con las filas del ejemplo de la sección 5 tiene que dar exactamente los cuatro resultados de la especificación.

## No hagas
- No modifiques `filasPlan` ni `textos`.
- No toques `parseNotacion`, `limpiarSesion` ni el resto.
