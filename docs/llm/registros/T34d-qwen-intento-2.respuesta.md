```java
// android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java
    import com.google.gson.JsonParser;
    import com.rutina.registro.PlanCelular;
    import com.rutina.registro.RegistroCelular;

    @Test
    void cargaPlanYRegistro() throws IOException {
        Almacen a = new Almacen(dir);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto(ejemplo("registro-ejemplo.json")));
        assertTrue(e.getMessage().contains("no hay ningún plan"));
        Object p = a.cargarDesdeTexto("  " + ejemplo("plan-ejemplo.json") + "  ");
        assertInstanceOf(PlanCelular.class, p);
        assertEquals(3L, a.plan(3).mesocicloId());
        Object r = a.cargarDesdeTexto(ejemplo("registro-ejemplo.json"));
        assertInstanceOf(RegistroCelular.class, r);
        a.cargarDesdeTexto(ejemplo("plan-ejemplo.json"));
        assertEquals(2, a.registro(3).sesiones().size());
        assertThrows(IllegalArgumentException.class, () -> a.cargarDesdeTexto("{roto"));
    }
```

<!-- 29 s · entrada 3324 · salida 271 · finish_reason stop -->
