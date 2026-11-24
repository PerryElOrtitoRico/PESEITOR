package com.rutina.model;

import java.util.Map;

/**
 * Tablas del método, compartidas por todos los perfiles (pestaña Plantillas).
 *
 * @param variantes    "5A" → % de volumen y ΔIMR por microciclo
 * @param diaPct       cantidad de días (3, 4, 5) → fracción del volumen del microciclo por día
 * @param diaDeltaImr  cantidad de días → variación de IMR de cada día respecto del microciclo
 * @param ref          IMR (60–80) → pesos de repeticiones por zona (10 zonas, de 55 a 100 %)
 * @param repsPorSerie tipo → repeticiones por serie de cada zona (10 valores)
 * @param plantillas   plantillas de fuerza básica e hipertrofia
 * @param volPlio      nivel → contactos [semana][día]
 * @param incrementoKg múltiplo al que se redondean los kg
 */
public record Metodologia(
        Map<String, VarianteMicro> variantes,
        Map<Integer, double[]> diaPct,
        Map<Integer, int[]> diaDeltaImr,
        Map<Integer, double[]> ref,
        Map<Tipo, int[]> repsPorSerie,
        Map<ClavePlantilla, Plantilla> plantillas,
        Map<Integer, int[][]> volPlio,
        double incrementoKg) {
}
