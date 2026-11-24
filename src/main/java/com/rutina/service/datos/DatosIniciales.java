package com.rutina.service.datos;

import com.rutina.model.CeldaPlantilla;
import com.rutina.model.Ejercicio;
import com.rutina.model.ClavePlantilla;
import com.rutina.model.Metodologia;
import com.rutina.model.Plantilla;
import com.rutina.model.Tipo;
import com.rutina.model.VarianteMicro;
import com.rutina.service.calculo.Plantillas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Valores originales de las planillas del usuario (Rutina.xlsx y Aproximacion hip.-Fza.Basica.xlsx).
 * Se cargan al crear la base de datos y al "restaurar valores originales".
 */
public final class DatosIniciales {

    private DatosIniciales() {
    }

    public static Metodologia metodologia() {
        // redondeo de a 2,5 kg: los discos más chicos de un gimnasio común son de 1,25 kg por lado
        return new Metodologia(variantes(), diaPct(), diaDeltaImr(), ref(), repsPorSerie(), plantillas(), volPlio(), 2.5);
    }

    // Hoja Calculos: % de volumen y ΔIMR por microciclo
    private static Map<String, VarianteMicro> variantes() {
        Map<String, VarianteMicro> v = new LinkedHashMap<>();
        v.put("5A", new VarianteMicro(new double[]{21, 23, 26, 18, 12}, new int[]{0, -1, -2, 2, 3}));
        v.put("5B", new VarianteMicro(new double[]{20, 22, 25, 18, 15}, new int[]{0, -1, -2, 1, 3}));
        v.put("4A", new VarianteMicro(new double[]{28, 22, 32, 18}, new int[]{0, 2, -3, 3}));
        v.put("4B", new VarianteMicro(new double[]{22, 28, 32, 18}, new int[]{2, 0, -3, 3}));
        v.put("4C", new VarianteMicro(new double[]{24, 26, 28, 22}, new int[]{1, -1, -2, 2}));
        v.put("3A", new VarianteMicro(new double[]{20, 30, 50}, new int[]{4, 2, -3}));
        v.put("3B", new VarianteMicro(new double[]{25, 35, 40}, new int[]{3, 1, -2}));
        v.put("3C", new VarianteMicro(new double[]{20, 35, 45}, new int[]{4, 2, -3}));
        v.put("2A", new VarianteMicro(new double[]{40, 60}, new int[]{5, -3}));
        v.put("2B", new VarianteMicro(new double[]{45, 55}, new int[]{3, -3}));
        return v;
    }

    // Hoja Calculos: frecuencia porcentual por día
    private static Map<Integer, double[]> diaPct() {
        Map<Integer, double[]> d = new LinkedHashMap<>();
        d.put(3, new double[]{0.35, 0.45, 0.20});
        d.put(4, new double[]{0.26, 0.24, 0.28, 0.22});
        d.put(5, new double[]{0.20, 0.15, 0.30, 0.10, 0.25});
        return d;
    }

    /** Sin variación: cada día con el IMR del microciclo, hasta que se ajuste en Plantillas → Días. */
    static Map<Integer, int[]> diaDeltaImr() {
        Map<Integer, int[]> d = new LinkedHashMap<>();
        for (int dias = 3; dias <= 5; dias++) d.put(dias, new int[dias]);
        return d;
    }

    // Hoja Ref: % de repeticiones por zona (55…100) según el IMR. Filas 65 y 66 corregidas según el libro.
    private static Map<Integer, double[]> ref() {
        int[][] filas = {
                {55, 40, 15, 10},
                {30, 35, 20, 15},
                {25, 30, 25, 20},
                {20, 35, 20, 15, 10},
                {15, 25, 35, 15, 10},
                {15, 20, 30, 20, 15},
                {14, 18, 30, 18, 12, 8},
                {10, 20, 25, 20, 15, 10},
                {10, 18, 20, 26, 12, 8, 6},
                {10, 15, 18, 25, 14, 10, 8},
                {12, 14, 16, 18, 16, 12, 8, 4},
                {10, 12, 14, 16, 24, 12, 8, 4},
                {8, 12, 14, 16, 18, 16, 12, 4},
                {8, 10, 12, 16, 18, 16, 14, 6},
                {8, 10, 11, 12, 13, 22, 18, 6},
                {8, 7, 12, 11, 13, 22, 18, 6, 3},
                {6, 7, 11, 12, 13, 21, 19, 6, 3, 2},
                {6, 6, 9, 12, 13, 20, 20, 7, 4, 3},
                {6, 6, 9, 9, 12, 19, 21, 8, 6, 4},
                {6, 6, 9, 9, 9, 15, 22, 11, 8, 5},
                {6, 6, 9, 9, 9, 12, 15, 17, 9, 8},
        };
        Map<Integer, double[]> r = new LinkedHashMap<>();
        for (int i = 0; i < filas.length; i++) {
            double[] fila = new double[10];
            for (int z = 0; z < filas[i].length; z++) fila[z] = filas[i][z];
            r.put(60 + i, fila);
        }
        return r;
    }

    private static Map<Tipo, int[]> repsPorSerie() {
        Map<Tipo, int[]> m = new LinkedHashMap<>();
        m.put(Tipo.POTENCIA, new int[]{4, 4, 4, 4, 3, 3, 3, 2, 1, 1});
        m.put(Tipo.FUERZA, new int[]{4, 4, 4, 4, 3, 3, 3, 2, 1, 1});
        return m;
    }

    // Aproximacion hip.-Fza.Basica.xlsx: celdas en orden semana 1 día 1, semana 1 día 2, …
    private static Map<ClavePlantilla, Plantilla> plantillas() {
        Map<ClavePlantilla, Plantilla> p = new LinkedHashMap<>();
        agregar(p, Tipo.HIPERTROFIA, 2, "4x12 4x10 | 4x10 3x8");
        agregar(p, Tipo.HIPERTROFIA, 2, "4x12 4x10 | 4x10 3x12 | 3x12 3x8");
        agregar(p, Tipo.HIPERTROFIA, 2, "4x12 4x10 | 4x10 3x12 | 3x12 3x10 | 4x10 3x8");
        agregar(p, Tipo.HIPERTROFIA, 2, "4x12 4x10 | 4x10 3x12 | 3x12 3x10 | 4x10 3x12 | 4x10 3x8");
        agregar(p, Tipo.FUERZA, 2, "3x5-75/80% 3x3-90% | 3x4-80/85% 3x2-95%");
        agregar(p, Tipo.FUERZA, 2, "3x5-75/80% 3x3-90% | 3x6-70/75% 3x4-80/85% | 3x4-80/85% 3x2-95%");
        agregar(p, Tipo.FUERZA, 2, "3x6-70/75% 3x4-80/85% | 3x5-75/80% 3x3-90% | 3x6-70/75% 3x4-80/85% | 3x4-80/85% 3x2-95%");
        agregar(p, Tipo.FUERZA, 2, "3x6-70/75% 3x4-80/85% | 3x5-75/80% 3x3-90% | 3x6-70/75% 3x4-80/85% | 3x4-80/85% 3x2-95% | 3x6-70/75% 3x4-80/85%");
        agregar(p, Tipo.HIPERTROFIA, 1, "4x12 | 4x10");
        agregar(p, Tipo.HIPERTROFIA, 1, "4x12 | 4x10 | 4x8");
        agregar(p, Tipo.HIPERTROFIA, 1, "4x12 | 4x10 | 4x8 | 4x10");
        agregar(p, Tipo.HIPERTROFIA, 1, "4x12 | 4x10 | 4x8 | 4x10 | 4x8");
        agregar(p, Tipo.FUERZA, 1, "4x5-85% | 4x3-90%");
        agregar(p, Tipo.FUERZA, 1, "4x6-70% | 4x5-80% | 4x2-95%");
        agregar(p, Tipo.FUERZA, 1, "4x6-70% | 4x4-85% | 4x5-80% | 4x2-95%");
        agregar(p, Tipo.FUERZA, 1, "4x6-70% | 4x4-85% | 4x5-80% | 4x3-90% | 4x2-95%");
        // 3 a 5 días por semana: inferidas a partir de las de 2 días
        for (Tipo t : List.of(Tipo.FUERZA, Tipo.HIPERTROFIA)) {
            for (int semanas = 2; semanas <= 5; semanas++) {
                Plantilla dos = p.get(new ClavePlantilla(t, semanas, 2));
                for (int dias = 3; dias <= 5; dias++) {
                    p.put(new ClavePlantilla(t, semanas, dias), Plantillas.inferir(dos, t, dias));
                }
            }
        }
        return p;
    }

    /** {@code semanas}: semanas separadas por "|", celdas de cada semana separadas por espacio. */
    private static void agregar(Map<ClavePlantilla, Plantilla> p, Tipo tipo, int dias, String semanas) {
        List<List<CeldaPlantilla>> filas = new ArrayList<>();
        for (String semana : semanas.split("\\|")) {
            filas.add(Arrays.stream(semana.trim().split("\\s+")).map(CeldaPlantilla::parse).toList());
        }
        p.put(new ClavePlantilla(tipo, filas.size(), dias), new Plantilla(filas, false));
    }

    /** Hojas Ejercicios y Pliometria. {@code id} y {@code perfilId} en 0: se asignan al insertar. */
    public static List<Ejercicio> ejercicios() {
        List<Ejercicio> e = new ArrayList<>();
        Object[][] filas = {
                {"Arranque de potencia colgado", 85.0, 90.0},
                {"Cargada de potencia colgado", 115.0, 120.0},
                {"Arranque de potencia", 80.0, null},
                {"Cargada de potencia", 110.0, null},
                {"Arranque de colgado", 90.0, null},
                {"Cargada de colgado", 115.0, null},
                {"Arranque", 90.0, null},
                {"Cargada", 115.0, null},
                {"Sentadilla", 140.0, null},
                {"Sentadila frontal", 130.0, null},
                {"Press frontal con impulso", 90.0, 95.0},
                {"Press frontal", 60.0, null},
                {"Empuje de barra", 30.0, null},
                {"Press con manuerna arrodillado", null, null},
                {"Empuje con mancuerna", null, null},
                {"Press banco plano", 90.0, null},
                {"Press banco plano con mancuerna", 25.0, null},
                {"Press banco inclinado", null, null},
                {"Press banco inclinado con mancuerna", null, null},
                {"Fondos", 50.0, null},
                {"Dominadas prono", 30.0, null},
                {"Dominadas supina", 25.0, null},
                {"Remo acostado", null, null},
                {"Remo inclinado", null, null},
                {"Remo con mancuerna", 40.0, null},
                {"Biceps con barra w", null, null},
                {"Biceps con mancuerna", null, null},
                {"Triceps tras nuca", null, null},
                {"Triceps frontal", null, null},
                {"Tiron de cargada", 180.0, null},
                {"Tiron de arranque", 120.0, null},
                {"Bulgaras (mancuerna)", 30.0, null},
        };
        for (int i = 0; i < filas.length; i++) {
            String nombre = (String) filas[i][0];
            e.add(new Ejercicio(0, 0, nombre, tipoPorNombre(nombre), (Double) filas[i][1], (Double) filas[i][2],
                    100, Map.of(), null, i, true));
        }
        String[][] plio = {
                {"1A", "Con 2 pies, dos saltos delante y uno atras", "2 pies salto de costado",
                        "1 pie, en el lugar, salto adelante y atras",
                        "1 pie, en sentido del reloj, arrancando del centro para adelante, saltar y volver siempre al centro",
                        "1 pie, salto en el lugar a los costados", "Salto con soga"},
                {"1B", "1 pie en cada cajon y salto rapido en el lugar", "Pie de adelante en cajon y salto con el de atras",
                        "Pie de atras en cajón, con el de delante salto a los costados sobre obstaculo bajo",
                        "Del piso, subo el pie delantero al cajon (pie de atras en el piso)",
                        "Del centro a cajones con una pierna", "Del centro con ambas piernas, rotando piernas delantera a cajon"},
                {"1C", "Del centro, al costado, al centro, al otro costado, centro, adelante, atras [2]",
                        "Delante dos veces, vuelvo de costado izquierdo y luego derecho [3]",
                        "Lado a lado salto sobre cajon [2]", "1 pie delante al cajón, bajo, izquierda y derecha, subo [1]",
                        "Entre 2 cajones, del centro, izquierda centro derecha [1]",
                        "1 pie salto al cajon, caigo al piso, izquierda centro derecha, atras al cajon, caigo, izquierda centro derecha y repito [1]"},
        };
        for (String[] grupo : plio) {
            for (int k = 1; k < grupo.length; k++) {
                e.add(new Ejercicio(0, 0, grupo[k], Tipo.PLIOMETRIA, null, null, 100, Map.of(), grupo[0], k - 1, true));
            }
        }
        return e;
    }

    /** Arranques, cargadas y empujes → potencia; bíceps y tríceps → hipertrofia; el resto → fuerza. */
    private static Tipo tipoPorNombre(String nombre) {
        String n = nombre.toLowerCase();
        if (n.startsWith("tiron")) return Tipo.FUERZA;
        if (n.contains("arranque") || n.contains("cargada") || n.startsWith("empuje")) return Tipo.POTENCIA;
        if (n.startsWith("biceps") || n.startsWith("triceps")) return Tipo.HIPERTROFIA;
        return Tipo.FUERZA;
    }

    // Hoja Calculos: contactos por día de cada nivel de pliometría
    private static Map<Integer, int[][]> volPlio() {
        Map<Integer, int[][]> v = new LinkedHashMap<>();
        v.put(1, new int[][]{{250, 300, 250}, {300, 250, 300}, {300, 300, 300}, {350, 300, 350}});
        v.put(2, new int[][]{{250, 250, 200}, {300, 250, 250}, {300, 300, 250}, {300, 300, 300}});
        v.put(3, new int[][]{{250, 300}, {300, 300}, {350, 300}, {350, 350}});
        return v;
    }
}
