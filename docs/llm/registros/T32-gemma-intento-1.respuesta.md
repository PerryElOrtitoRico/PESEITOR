// android/app/src/main/java/com/rutina/app/logica/Totales.java
    public static int totalReps(List<SerieReal> series) {
        int total = 0;
        for (SerieReal s : series) {
            total += s.reps();
        }
        return total;
    }

    public static int[] totalesEjercicio(List<FilaPlan> filas, Map<String, String> textos) {
        int planificadas = 0;
        int hechas = 0;
        for (FilaPlan fila : filas) {
            int plan = totalReps(fila.series());
            planificadas += plan;
            if (hechas == -1) continue;
            String texto = textos.get(fila.etiqueta());
            if (texto == null || texto.isBlank()) {
                hechas += plan;
            } else if (!NotacionRegistro.esValida(texto, fila.series())) {
                hechas = -1;
            } else {
                hechas += totalReps(NotacionRegistro.parse(texto, fila.series()));
            }
        }
        return new int[]{planificadas, hechas};
    }

// android/app/src/test/java/com/rutina/app/logica/TotalesTest.java
    @Test
    void totalReps() {
        assertEquals(0, Totales.totalReps(List.of()));
        assertEquals(11, Totales.totalReps(List.of(new SerieReal(3, 80), new SerieReal(3, 80), new SerieReal(5, 0))));
    }

    @Test
    void totalesEjercicio() {
        assertArrayEquals(new int[]{11, 11}, Totales.totalesEjercicio(FILAS, Map.of()));
        assertArrayEquals(new int[]{11, 10}, Totales.totalesEjercicio(FILAS, Map.of("A", "3.2")));
        assertArrayEquals(new int[]{11, 6}, Totales.totalesEjercicio(FILAS, Map.of("B", "0")));
        assertArrayEquals(new int[]{11, -1}, Totales.totalesEjercicio(FILAS, Map.of("A", "x")));
    }

<!-- 34 s · entrada 2511 · salida 537 · finish_reason stop -->
