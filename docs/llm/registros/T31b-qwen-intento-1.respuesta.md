// android/app/src/main/java/com/rutina/app/logica/Sesiones.java
    public static SesionRegistro limpiarSesion(SesionRegistro s) {
        List<EjercicioRegistro> limpios = new ArrayList<>();
        for (EjercicioRegistro e : s.ejercicios()) {
            Map<String, String> filas = new LinkedHashMap<>();
            for (Map.Entry<String, String> f : e.filas().entrySet()) {
                if (f.getValue() != null && !f.getValue().isBlank()) {
                    filas.put(f.getKey(), f.getValue().strip());
                }
            }
            String nota = (e.nota() == null || e.nota().isBlank()) ? null : e.nota().strip();
            if (filas.isEmpty() && e.esfuerzo() == null && nota == null) {
                continue;
            }
            limpios.add(new EjercicioRegistro(e.bloque(), e.esfuerzo(), filas, nota));
        }
        return new SesionRegistro(s.microciclo(), s.dia(), s.fecha(), s.descanso(), s.sensacionFinal(), limpios, s.duracionMin());
    }

    public static RegistroCelular guardarSesion(RegistroCelular r, SesionRegistro s) {
        SesionRegistro limpia = limpiarSesion(s);
        List<SesionRegistro> sesiones = new ArrayList<>();
        for (SesionRegistro o : r.sesiones()) {
            if (o.microciclo() != limpia.microciclo() || o.dia() != limpia.dia()) {
                sesiones.add(o);
            }
        }
        sesiones.add(limpia);
        sesiones.sort(Comparator.comparingInt(SesionRegistro::microciclo).thenComparingInt(SesionRegistro::dia));
        return new RegistroCelular(r.formato(), r.version(), r.perfil(), r.mesocicloId(), sesiones);
    }

// android/app/src/test/java/com/rutina/app/logica/SesionesTest.java
    @Test
    void limpiaSesion() {
        SesionRegistro s = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-1", null, Map.of("80-85", " 3.3 "), null), new EjercicioRegistro("1-2", null, Map.of("4x12", ""), null), new EjercicioRegistro("1-3", 2, Map.of(), "  subir peso  ")), null);
        SesionRegistro l = Sesiones.limpiarSesion(s);
        assertEquals(2, l.ejercicios().size());
        assertEquals(Map.of("80-85", "3.3"), l.ejercicios().get(0).filas());
        assertEquals("1-3", l.ejercicios().get(1).bloque());
        assertEquals("subir peso", l.ejercicios().get(1).nota());
        assertEquals(3, s.ejercicios().size());
    }

    @Test
    void guardaSesion() {
        RegistroCelular r0 = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of());
        SesionRegistro s22 = new SesionRegistro(2, 2, "2026-11-05", 4, null, List.of(), null);
        SesionRegistro s11 = new SesionRegistro(1, 1, "2026-10-27", 5, 3, List.of(), null);
        RegistroCelular r2 = Sesiones.guardarSesion(Sesiones.guardarSesion(r0, s22), s11);
        assertEquals(List.of(), r0.sesiones());
        assertEquals(1, r2.sesiones().get(0).microciclo());
        assertEquals(2, r2.sesiones().get(1).microciclo());
        RegistroCelular r3 = Sesiones.guardarSesion(r2, new SesionRegistro(1, 1, "2026-10-27", 1, 3, List.of(), null));
        assertEquals(2, r3.sesiones().size());
        assertEquals(1, Sesiones.sesionDelRegistro(r3, 1, 1).descanso());
    }

<!-- 90 s · entrada 4695 · salida 985 · finish_reason stop -->
