package com.rutina.app.logica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DuracionSesionTest {

    @Test
    void estadoInicialRespetaElGuardadoYTomaLaDuracionDelRegistro() {
        assertEquals(new EstadoCrono(60_000, null), DuracionSesion.estadoInicial(new EstadoCrono(60_000, null), 45));
        assertEquals(new EstadoCrono(5_000, 1_000L), DuracionSesion.estadoInicial(new EstadoCrono(5_000, 1_000L), 45));
        assertEquals(new EstadoCrono(45 * 60_000L, null), DuracionSesion.estadoInicial(Crono.nuevo(), 45));
        assertEquals(Crono.nuevo(), DuracionSesion.estadoInicial(Crono.nuevo(), null));
    }

    @Test
    void duracionParaGuardar() {
        assertNull(DuracionSesion.paraGuardar(Crono.nuevo(), 5_000, null));
        assertEquals(45, DuracionSesion.paraGuardar(Crono.nuevo(), 5_000, 45));
        assertEquals(1, DuracionSesion.paraGuardar(new EstadoCrono(0, 1_000L), 2_000, null));
        assertEquals(75, DuracionSesion.paraGuardar(new EstadoCrono(4_500_000, null), 9_999_999, 45));
    }

    @Test
    void cronoQueSiguioCorriendoConLaAppCerrada() {
        EstadoCrono estado = new EstadoCrono(60_000, 1_000L);
        assertEquals(61, DuracionSesion.paraGuardar(estado, 3_601_000, null));
    }
}
