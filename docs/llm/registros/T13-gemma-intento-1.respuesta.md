```java
// src/main/java/com/rutina/registro/TextosRegistro.java
    public static String duracion(Integer duracionMin) {
        if (duracionMin == null) return "";
        return duracionMin + " min";
    }

    public static String notas(SesionPlan plan, SesionRegistro registro) {
        if (registro.ejercicios() == null) return "";
        List<String> partes = new ArrayList<>();
        for (EjercicioPlan ej : plan.ejercicios()) {
            for (EjercicioRegistro er : registro.ejercicios()) {
                if (ej.bloque().equals(er.bloque()) && er.nota() != null && !er.nota().isBlank()) {
                    partes.add(ej.nombre() + ": " + er.nota().strip().replaceAll("\\s+", " "));
                    break;
                }
            }
        }
        return String.join(" | ", partes);
    }
```

```java
// src/test/java/com/rutina/registro/TextosRegistroTest.java
    @Test
    void duracion() {
        assertEquals("75 min", TextosRegistro.duracion(75));
        assertEquals("1 min", TextosRegistro.duracion(1));
        assertEquals("", TextosRegistro.duracion(null));
    }

    @Test
    void notas() {
        var sr = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-2", null, Map.of(), "  dolor leve  "), new EjercicioRegistro("1-1", 3, Map.of(), "subí 2,5 kg")), null);
        assertEquals("Sentadilla: subí 2,5 kg | Press banca: dolor leve", TextosRegistro.notas(plan(), sr));
        var sr2 = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("2-1", null, Map.of(), "línea uno\nlínea dos")), null);
        assertEquals("Remo: línea uno línea dos", TextosRegistro.notas(plan(), sr2));
    }

    @Test
    void notasVacias() {
        var sin = new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-1", 3, Map.of(), null), new EjercicioRegistro("2-1", null, Map.of(), "   ")), null);
        assertEquals("", TextosRegistro.notas(plan(), sin));
        var nula = new SesionRegistro(1, 1, "2026-10-27", null, null, null, null);
        assertEquals("", TextosRegistro.notas(plan(), nula));
    }
```

<!-- 49 s · entrada 2309 · salida 678 · finish_reason stop -->
