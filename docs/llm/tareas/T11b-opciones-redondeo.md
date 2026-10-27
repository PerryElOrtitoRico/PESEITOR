<!-- contrato: 3 | archivos: src/main/java/com/rutina/service/calculo/OpcionesRedondeo.java, src/test/java/com/rutina/service/calculo/OpcionesRedondeoTest.java -->
# T11b — Opciones del combo "Redondeo de kg"

## Objetivo
Completar `OpcionesRedondeo.indiceDe` y `OpcionesRedondeo.valorDe`, y los dos tests de `OpcionesRedondeoTest`. No toques las constantes `TEXTOS` y `VALORES` ni el constructor.

## Código que ya existe
- `OpcionesRedondeo.VALORES` es `{1.0, 2.5, 5.0}`.

## `indiceDe(double incrementoKg)` (pasos en orden)
1. `for (int i = 0; i < VALORES.length; i++) { if (VALORES[i] == incrementoKg) return i; }`
2. `return 1;`

## `valorDe(int indice)` (pasos en orden)
1. `return VALORES[indice];`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `indice`:
  - `assertEquals(0, OpcionesRedondeo.indiceDe(1.0));`
  - `assertEquals(1, OpcionesRedondeo.indiceDe(2.5));`
  - `assertEquals(2, OpcionesRedondeo.indiceDe(5.0));`
  - `assertEquals(1, OpcionesRedondeo.indiceDe(3.0));`
- `valor`:
  - `assertEquals(1.0, OpcionesRedondeo.valorDe(0));`
  - `assertEquals(2.5, OpcionesRedondeo.valorDe(1));`
  - `assertEquals(5.0, OpcionesRedondeo.valorDe(2));`
