```java
// android/app/src/main/java/com/rutina/app/logica/Sesiones.java
    public static SesionPlan sesionDelPlan(PlanCelular plan, int micro, int dia) {
        for (SesionPlan s : plan.sesiones()) {
            if (s.microciclo() == micro && s.dia() == dia) {
                return s;
            }
        }
        return null;
    }

    public static SesionRegistro sesionDelRegistro(RegistroCelular registro, int micro, int dia) {
        for (SesionRegistro s : registro.sesiones()) {
            if (s.microciclo() == micro && s.dia() == dia) {
                return s;
            }
        }
        return null;
    }

    public static RegistroCelular registroVacio(PlanCelular plan) {
        return new RegistroCelular(RegistroCelular.FORMATO, RegistroCelular.VERSION, plan.perfil(), plan.mesocicloId(), new ArrayList<>());
    }

    private static String textoKg(double kg) {
        String t = String.valueOf(kg);
        if (t.endsWith(".0")) {
            t = t.substring(0, t.length() - 2);
        }
        return t.replace('.', ',');
    }

    public static String pista(List<SerieReal> series) {
        if (series.isEmpty()) {
            return "";
        }
        StringBuilder reps = new StringBuilder();
        for (SerieReal s : series) {
            if (reps.length() > 0) {
                reps.append(".");
            }
            reps.append(s.reps());
        }
        List<String> kgs = new ArrayList<>();
        for (SerieReal s : series) {
            if (s.kg() > 0) {
                String t = textoKg(s.kg());
                if (!kgs.contains(t)) {
                    kgs.add(t);
                }
            }
        }
        if (kgs.isEmpty()) {
            return reps.toString();
        }
        return reps.toString() + " · " + String.join("/", kgs);
    }

    public static String nombreArchivoRegistro(PlanCelular plan) {
        return "registro-" + plan.perfil().replaceAll("\\s", "_") + "-" + plan.inicio() + ".json";
    }
```

```java
// android/app/src/test/java/com/rutina/app/logica/SesionesTest.java
    @Test
    void sesionDelPlan() {
        assertEquals("1-1", Sesiones.sesionDelPlan(plan(), 1, 1).ejercicios().get(0).bloque());
        assertEquals(List.of(), Sesiones.sesionDelPlan(plan(), 2, 2).ejercicios());
        assertNull(Sesiones.sesionDelPlan(plan(), 3, 1));
    }

    @Test
    void sesionDelRegistro() {
        SesionRegistro a = new SesionRegistro(1, 1, "2026-10-27", 5, 3, List.of(), null);
        RegistroCelular r = new RegistroCelular("rutineitor-registro", 1, "F", 3L, List.of(a));
        assertEquals(a, Sesiones.sesionDelRegistro(r, 1, 1));
        assertNull(Sesiones.sesionDelRegistro(r, 1, 2));
    }

    @Test
    void registroVacio() {
        assertEquals(new RegistroCelular("rutineitor-registro", 1, "Franco Pérez", 3L, List.of()), Sesiones.registroVacio(plan()));
    }

    @Test
    void pista() {
        assertEquals("3.3.3 · 120/127,5", Sesiones.pista(SERIES));
        assertEquals("12.12", Sesiones.pista(List.of(new SerieReal(12, 0), new SerieReal(12, 0))));
        assertEquals("4.4 · 105", Sesiones.pista(List.of(new SerieReal(4, 105), new SerieReal(4, 105))));
        assertEquals("", Sesiones.pista(List.of()));
    }

    @Test
    void nombreArchivoRegistro() {
        assertEquals("registro-Franco_Pérez-2026-10-26.json", Sesiones.nombreArchivoRegistro(plan()));
    }
```

<!-- 67 s · entrada 4125 · salida 1075 · finish_reason stop -->
