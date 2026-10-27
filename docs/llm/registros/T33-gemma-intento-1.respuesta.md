```java
// android/app/src/main/java/com/rutina/app/logica/Crono.java
    public static EstadoCrono nuevo() {
        return new EstadoCrono(0, null);
    }

    public static EstadoCrono iniciar(EstadoCrono estado, long ahora) {
        if (estado.desde() != null) return estado;
        return new EstadoCrono(estado.acumuladoMs(), ahora);
    }

    public static EstadoCrono pausar(EstadoCrono estado, long ahora) {
        if (estado.desde() == null) return estado;
        return new EstadoCrono(estado.acumuladoMs() + (ahora - estado.desde()), null);
    }

    public static long ms(EstadoCrono estado, long ahora) {
        if (estado.desde() == null) return estado.acumuladoMs();
        return estado.acumuladoMs() + (ahora - estado.desde());
    }

    public static String texto(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);
    }

    public static Integer minutosDe(long ms) {
        if (ms <= 0) return null;
        return (int) Math.max(1, Math.round(ms / 60000.0));
    }

    public static Integer duracionValida(String texto) {
        if (texto == null) return null;
        String t = texto.strip();
        if (!t.matches("[0-9]+")) return null;
        long n;
        try {
            n = Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
        if (n < 1 || n > 600) return null;
        return (int) n;
    }
```

```java
// android/app/src/test/java/com/rutina/app/logica/CronoTest.java
    @Test
    void nuevoIniciarPausar() {
        assertEquals(new EstadoCrono(0, null), Crono.nuevo());
        EstadoCrono i = Crono.iniciar(Crono.nuevo(), 1_000);
        assertEquals(new EstadoCrono(0, 1_000L), i);
        assertEquals(i, Crono.iniciar(i, 5_000));
        EstadoCrono p = Crono.pausar(i, 4_000);
        assertEquals(new EstadoCrono(3_000, null), p);
        assertEquals(p, Crono.pausar(p, 9_000));
    }

    @Test
    void ms() {
        assertEquals(0L, Crono.ms(Crono.nuevo(), 99_999));
        assertEquals(63_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 4_000));
        assertEquals(3_660_000L, Crono.ms(new EstadoCrono(60_000, 1_000L), 3_601_000));
    }

    @Test
    void texto() {
        assertEquals("0:00:00", Crono.texto(0));
        assertEquals("0:01:15", Crono.texto(75_500));
        assertEquals("1:02:05", Crono.texto(3_725_000));
    }

    @Test
    void minutosDe() {
        assertEquals(1, Crono.minutosDe(29_999));
        assertEquals(2, Crono.minutosDe(90_000));
        assertEquals(75, Crono.minutosDe(4_500_000));
        assertNull(Crono.minutosDe(0));
    }

    @Test
    void duracionValida() {
        assertEquals(75, Crono.duracionValida("75"));
        assertEquals(5, Crono.duracionValida(" 5 "));
        assertNull(Crono.duracionValida("0"));
        assertNull(Crono.duracionValida("601"));
        assertNull(Crono.duracionValida("7,5"));
        assertNull(Crono.duracionValida(""));
    }
```

<!-- 75 s · entrada 3420 · salida 1112 · finish_reason stop -->
