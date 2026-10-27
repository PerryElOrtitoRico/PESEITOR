// android/app/src/main/java/com/rutina/app/logica/TextosSesion.java
    public static String tituloSesion(int micro, int dia) {
        return "Microciclo " + micro + " · Día " + dia;
    }

    public static String lineaFecha(String fecha) {
        if (fecha == null) {
            return "Sin anotar todavía";
        }
        return "Anotada el " + Fechas.larga(fecha);
    }

    public static String repsTexto(int[] totales) {
        String hechas = totales[1] == -1 ? "—" : String.valueOf(totales[1]);
        return hechas + " / " + totales[0] + " reps";
    }

    public static String errorCasilla(String texto, List<SerieReal> planificadas) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            NotacionRegistro.parse(texto, planificadas);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public static String ojoCasilla(String texto, List<SerieReal> planificadas) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        List<SerieReal> hechas;
        try {
            hechas = NotacionRegistro.parse(texto, planificadas);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (hechas.size() <= planificadas.size()) {
            return null;
        }
        return "Ojo: " + hechas.size() + " series (planificadas: " + planificadas.size() + "). Los decimales del peso van con coma.";
    }

// android/app/src/test/java/com/rutina/app/logica/TextosSesionTest.java
    @Test
    void titulo() {
        assertEquals("Microciclo 1 · Día 2", TextosSesion.tituloSesion(1, 2));
    }

    @Test
    void fecha() {
        assertEquals("Sin anotar todavía", TextosSesion.lineaFecha(null));
        assertEquals("Anotada el 27/10/2026", TextosSesion.lineaFecha("2026-10-27"));
    }

    @Test
    void reps() {
        assertEquals("10 / 11 reps", TextosSesion.repsTexto(new int[]{11, 10}));
        assertEquals("— / 11 reps", TextosSesion.repsTexto(new int[]{11, -1}));
    }

    @Test
    void error() {
        assertNull(TextosSesion.errorCasilla("", TRES));
        assertNull(TextosSesion.errorCasilla("3.3.3", TRES));
        assertEquals("serie inválida: \"x\" (reps de 1 a 99, kg con coma)", TextosSesion.errorCasilla("x", TRES));
        assertEquals("falta una serie entre puntos en \"3..3\"", TextosSesion.errorCasilla("3..3", TRES));
    }

    @Test
    void ojo() {
        assertNull(TextosSesion.ojoCasilla("3.3.3", TRES));
        assertNull(TextosSesion.ojoCasilla("x", TRES));
        assertEquals("Ojo: 4 series (planificadas: 3). Los decimales del peso van con coma.", TextosSesion.ojoCasilla("3.3.3.3-50", TRES));
    }

<!-- 52 s · entrada 3128 · salida 870 · finish_reason stop -->
