package com.rutina.registro;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistroCelularTest {

    private static SesionRegistro ses(int micro, int dia) {
        return new SesionRegistro(micro, dia, "2026-10-27", null, null, List.of(), null);
    }

    private static RegistroCelular reg(SesionRegistro... s) {
        return new RegistroCelular(RegistroCelular.FORMATO, 1, "F", 3, List.of(s));
    }

    @Test
    void cuentaLasSesionesQueSePerderian() {
        RegistroCelular guardado = reg(ses(1, 1), ses(1, 2), ses(2, 1));
        assertEquals(2, guardado.sesionesQueFaltanEn(reg(ses(1, 1))));
        assertEquals(0, guardado.sesionesQueFaltanEn(reg(ses(1, 1), ses(1, 2), ses(2, 1), ses(2, 2))));
        assertEquals(0, reg().sesionesQueFaltanEn(reg(ses(1, 1))));
    }
}
