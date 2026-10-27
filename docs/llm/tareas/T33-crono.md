<!-- contrato: 10,13 | archivos: android/app/src/main/java/com/rutina/app/logica/Crono.java, android/app/src/test/java/com/rutina/app/logica/CronoTest.java -->
# T33 — Funciones del cronómetro

## Objetivo
Completar los siete métodos de `Crono` y los cinco tests de `CronoTest`. No cambies los imports.

## Código que ya existe
- `record EstadoCrono(long acumuladoMs, Long desde)`: se lee con `e.acumuladoMs()` y `e.desde()` (`desde` es `null` si está parado).

## `nuevo()` (pasos en orden)
1. `return new EstadoCrono(0, null);`

## `iniciar(EstadoCrono estado, long ahora)` (pasos en orden)
1. `if (estado.desde() != null) return estado;`
2. `return new EstadoCrono(estado.acumuladoMs(), ahora);`

## `pausar(EstadoCrono estado, long ahora)` (pasos en orden)
1. `if (estado.desde() == null) return estado;`
2. `return new EstadoCrono(estado.acumuladoMs() + (ahora - estado.desde()), null);`

## `ms(EstadoCrono estado, long ahora)` (pasos en orden)
1. `if (estado.desde() == null) return estado.acumuladoMs();`
2. `return estado.acumuladoMs() + (ahora - estado.desde());`

## `texto(long ms)` (pasos en orden)
1. `long total = ms / 1000;`
2. `long h = total / 3600;`
3. `long m = (total % 3600) / 60;`
4. `long s = total % 60;`
5. `return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);`

## `minutosDe(long ms)` (pasos en orden)
1. `if (ms <= 0) return null;`
2. `return (int) Math.max(1, Math.round(ms / 60000.0));`

## `duracionValida(String texto)` (pasos en orden)
1. `if (texto == null) return null;`
2. `String t = texto.strip();`
3. `if (!t.matches("[0-9]+")) return null;`
4. `long n;`
5. `try { n = Long.parseLong(t); } catch (NumberFormatException e) { return null; }`
6. `if (n < 1 || n > 600) return null;`
7. `return (int) n;`

## Tests (copiá estas aserciones tal cual, sin comentarios)
- `nuevoIniciarPausar`:
  - `assertEquals(new EstadoCrono(0, null), Crono.nuevo());`
  - `EstadoCrono i = Crono.iniciar(Crono.nuevo(), 1_000);`
  - `assertEquals(new EstadoCrono(0, 1_000L), i);`
  - `assertEquals(i, Crono.iniciar(i, 5_000));`
  - `EstadoCrono p = Crono.pausar(i, 4_000);`
  - `assertEquals(new EstadoCrono(3_000, null), p);`
  - `assertEquals(p, Crono.pausar(p, 9_000));`
- `ms`:
  - `assertEquals(0L, Crono.ms(Crono.nuevo(), 99_999));`
  - `assertEquals(63_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 4_000));`
  - `assertEquals(3_660_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 3_601_000));`
- `texto`:
  - `assertEquals("0:00:00", Crono.texto(0));`
  - `assertEquals("0:01:15", Crono.texto(75_500));`
  - `assertEquals("1:02:05", Crono.texto(3_725_000));`
- `minutosDe`:
  - `assertEquals(1, Crono.minutosDe(29_999));`
  - `assertEquals(2, Crono.minutosDe(90_000));`
  - `assertEquals(75, Crono.minutosDe(4_500_000));`
  - `assertNull(Crono.minutosDe(0));`
- `duracionValida`:
  - `assertEquals(75, Crono.duracionValida("75"));`
  - `assertEquals(5, Crono.duracionValida(" 5 "));`
  - `assertNull(Crono.duracionValida("0"));`
  - `assertNull(Crono.duracionValida("601"));`
  - `assertNull(Crono.duracionValida("7,5"));`
  - `assertNull(Crono.duracionValida(""));`
