package com.rutina.app.logica;

/** Cómo se relacionan el cronómetro de una sesión y la duración (en minutos) que se guarda en el registro. */
public final class DuracionSesion {

    private DuracionSesion() {
    }

    /**
     * El cronómetro con el que abrir la pantalla: el guardado, o, si nunca se arrancó pero el registro ya trae una duración
     * (anotada en la PC o importada), un cronómetro parado en esa duración.
     *
     * @param delAlmacen el estado guardado ({@code Crono.nuevo()} si no hay)
     * @param guardada   duracionMin de la sesión guardada; null si no hay
     */
    public static EstadoCrono estadoInicial(EstadoCrono delAlmacen, Integer guardada) {
        boolean nuncaArrancado = delAlmacen.acumuladoMs() == 0 && delAlmacen.desde() == null;
        if (nuncaArrancado && guardada != null) return new EstadoCrono(guardada * 60_000L, null);
        return delAlmacen;
    }

    /**
     * La duración a guardar en la sesión: los minutos del cronómetro; si nunca se arrancó, la que ya había (puede ser null).
     */
    public static Integer paraGuardar(EstadoCrono estado, long ahora, Integer guardada) {
        Integer minutos = Crono.minutosDe(Crono.ms(estado, ahora));
        return minutos != null ? minutos : guardada;
    }
}
