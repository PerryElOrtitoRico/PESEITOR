<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 6810, razonamiento no -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Los datos son records y se leen con x(): s.microciclo(), e.filas(), s.sensacionFinal(). No existen getMicrociclo() ni getFilas().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea: un bloque por archivo, nunca dos archivos en el mismo bloque. Ejemplo:
```java
// src/main/java/com/rutina/registro/Ejemplo.java
package com.rutina.registro;

public final class Ejemplo { }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# RUTINEITOR — mejoras de octubre (contrato)

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

---

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

---

# Archivos actuales

```java
// src/main/java/com/rutina/registro/EscalaEsfuerzo.java
package com.rutina.registro;

/** Sensación de un ejercicio (1 = muy liviano, 5 = al límite). */
public enum EscalaEsfuerzo {
    MUY_LIVIANO(1, "Muy liviano"),
    LIVIANO(2, "Liviano"),
    JUSTO(3, "Justo"),
    // TODO T11a
    PESADO(4, "Pesado"),
    AL_LIMITE(5, "Al límite");

    private final int valor;
    private final String texto;

    EscalaEsfuerzo(int valor, String texto) {
        this.valor = valor;
        this.texto = texto;
    }

    /** Valor numérico de la escala (el que va en el JSON). */
    public int valor() {
        return valor;
    }

    /** Texto que ve el usuario. */
    public String texto() {
        return texto;
    }

    /** Lanza IllegalArgumentException si no existe ese valor. */
    public static EscalaEsfuerzo deValor(int valor) {
        for (EscalaEsfuerzo e : values()) {
            if (e.valor == valor) return e;
        }
        throw new IllegalArgumentException("valor de EscalaEsfuerzo inexistente: " + valor);
    }
}
```

```java
// src/test/java/com/rutina/registro/EscalaEsfuerzoOrdenTest.java
package com.rutina.registro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EscalaEsfuerzoOrdenTest {

    @Test
    void alLimiteYPesadoIntercambiados() {
        // TODO T11a
        throw new UnsupportedOperationException();
    }
}
```

Completá los `// TODO` que pide la tarea T11a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/rutina/registro/EscalaEsfuerzo.java, src/test/java/com/rutina/registro/EscalaEsfuerzoOrdenTest.java.
