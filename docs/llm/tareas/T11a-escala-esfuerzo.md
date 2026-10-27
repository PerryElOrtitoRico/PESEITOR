<!-- contrato: 2 | archivos: src/main/java/com/rutina/registro/EscalaEsfuerzo.java, src/test/java/com/rutina/registro/EscalaEsfuerzoOrdenTest.java -->
# T11a — Intercambiar "Pesado" y "Al límite"

## Objetivo
En `EscalaEsfuerzo` (enum) cambiar el orden y los valores de las dos últimas constantes y el javadoc de la clase, y completar el test `EscalaEsfuerzoOrdenTest`. No cambies nada más del enum (el constructor, `valor()`, `texto()` y `deValor` quedan igual).

## Pasos en `EscalaEsfuerzo` (en orden)
1. El javadoc de la clase queda exactamente así:
```java
/** Sensación de un ejercicio (1 = muy liviano, 5 = pesado (no salió)). */
```
2. Las dos últimas constantes (donde está el `// TODO T11a`, que se borra) quedan exactamente así, en este orden:
```java
    AL_LIMITE(4, "Al límite"),
    PESADO(5, "Pesado");
```
3. `MUY_LIVIANO`, `LIVIANO` y `JUSTO` no cambian.

## Test `alLimiteYPesadoIntercambiados` (copiá estas aserciones tal cual)
```java
        assertEquals(EscalaEsfuerzo.AL_LIMITE, EscalaEsfuerzo.deValor(4));
        assertEquals("Al límite", EscalaEsfuerzo.deValor(4).texto());
        assertEquals("Pesado", EscalaEsfuerzo.deValor(5).texto());
        assertEquals(EscalaEsfuerzo.PESADO, EscalaEsfuerzo.values()[4]);
```
