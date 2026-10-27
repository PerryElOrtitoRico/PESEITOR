```java
// src/main/java/com/rutina/service/calculo/OpcionesRedondeo.java
    public static int indiceDe(double incrementoKg) {
        for (int i = 0; i < VALORES.length; i++) {
            if (VALORES[i] == incrementoKg) return i;
        }
        return 1;
    }

    public static double valorDe(int indice) {
        return VALORES[indice];
    }
```

```java
// src/test/java/com/rutina/service/calculo/OpcionesRedondeoTest.java
    @Test
    void indice() {
        assertEquals(0, OpcionesRedondeo.indiceDe(1.0));
        assertEquals(1, OpcionesRedondeo.indiceDe(2.5));
        assertEquals(2, OpcionesRedondeo.indiceDe(5.0));
        assertEquals(1, OpcionesRedondeo.indiceDe(3.0));
    }

    @Test
    void valor() {
        assertEquals(1.0, OpcionesRedondeo.valorDe(0));
        assertEquals(2.5, OpcionesRedondeo.valorDe(1));
        assertEquals(5.0, OpcionesRedondeo.valorDe(2));
    }
```

<!-- 17 s · entrada 1342 · salida 304 · finish_reason stop -->
