<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, max_tokens 6888, razonamiento no -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los métodos marcados "lo completa otra subtarea" quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están (aunque todavía no se usen), nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Los datos son records y se leen con x(): s.microciclo(), e.filas(), s.sensacionFinal(). No existen getMicrociclo() ni getFilas().
- En los tests, copiá las aserciones de la tarea tal cual (mismos valores y fechas) y no escribas comentarios.
- No crees ni modifiques otros archivos.
- Devolvé solo los métodos que completaste, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/rutina/registro/Ejemplo.java
    public static int doble(int x) {
        return 2 * x;
    }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# RUTINEITOR — mejoras de octubre (contrato)

## 3. Redondeo de kg (mejora 2)
El cálculo ya redondea al múltiplo de `Metodologia.incrementoKg()`; la base del usuario tenía 1 kg. En `PanelPlantillas` el combo "Redondeo de kg" pasa a tener tres opciones `{"1 kg", "2,5 kg", "5 kg"}` ↔ `1.0, 2.5, 5.0` (al cargar: el índice del valor; si no coincide ninguno, "2,5 kg"; al guardar: el valor del índice). Nada más cambia.

---

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

---

# Archivos actuales

```java
// src/main/java/com/rutina/service/calculo/OpcionesRedondeo.java
package com.rutina.service.calculo;

/** Las opciones del combo "Redondeo de kg" de Plantillas: textos y múltiplos (kg) en el mismo orden. */
public final class OpcionesRedondeo {

    public static final String[] TEXTOS = {"1 kg", "2,5 kg", "5 kg"};
    public static final double[] VALORES = {1.0, 2.5, 5.0};

    private OpcionesRedondeo() {
    }

    /** Índice de la opción cuyo valor es igual a incrementoKg; si no coincide ninguna, 1 ("2,5 kg"). */
    public static int indiceDe(double incrementoKg) {
        // TODO T11b
        throw new UnsupportedOperationException();
    }

    /** Valor (kg) de la opción con ese índice. */
    public static double valorDe(int indice) {
        // TODO T11b
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/rutina/service/calculo/OpcionesRedondeoTest.java
package com.rutina.service.calculo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpcionesRedondeoTest {

    @Test
    void indice() {
        // TODO T11b
        throw new UnsupportedOperationException();
    }

    @Test
    void valor() {
        // TODO T11b
        throw new UnsupportedOperationException();
    }
}
```

Completá los `// TODO` que pide la tarea T11b. Devolvé **solo los métodos que completaste**, cada uno entero (firma y cuerpo), sin el resto de la clase: un bloque por archivo, con la ruta en la primera línea y después los métodos (y los imports nuevos, si agregaste). Archivos: src/main/java/com/rutina/service/calculo/OpcionesRedondeo.java, src/test/java/com/rutina/service/calculo/OpcionesRedondeoTest.java.
