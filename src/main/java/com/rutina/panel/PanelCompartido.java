package com.rutina.panel;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Guarda la conexión actual con el panel; permite cambiarla sin tocar a quienes la usan. */
public final class PanelCompartido implements AutoCloseable {

    private ConexionPanel actual;

    public PanelCompartido(ConexionPanel inicial) {
        this.actual = inicial;
    }

    public synchronized ConexionPanel get() {
        return actual;
    }

    private final Object cambio = new Object();

    /**
     * Cierra la conexión actual y recién después abre la nueva, así nunca hay dos hilos de envío sobre la misma cola.
     * Mientras tanto {@code get()} devuelve una conexión inactiva (nunca null). Si {@code abrir} falla queda esa
     * conexión inactiva. Puede bloquear unos segundos: no llamar desde el hilo de Swing.
     */
    public void reabrir(Supplier<ConexionPanel> abrir) {
        reabrir(abrir, PanelCompartido::cerrar);
    }

    void reabrir(Supplier<ConexionPanel> abrir, Consumer<ConexionPanel> cerrador) {
        synchronized (cambio) {
            ConexionPanel anterior;
            synchronized (this) {
                anterior = actual;
                actual = ConexionPanel.abrir(null);
            }
            try {
                if (anterior != null) cerrador.accept(anterior);
            } catch (RuntimeException ignorada) {
                // se está descartando la conexión
            }
            try {
                ConexionPanel nueva = abrir.get();
                if (nueva != null) {
                    synchronized (this) {
                        actual = nueva;
                    }
                }
            } catch (RuntimeException ignorada) {
                // queda la conexión inactiva
            }
        }
    }

    /** Pone la nueva conexión y cierra la anterior. No lanza. */
    public void reemplazar(ConexionPanel nueva) {
        ConexionPanel anterior;
        synchronized (this) {
            anterior = actual;
            actual = nueva;
        }
        cerrar(anterior);
    }

    @Override
    public void close() {
        ConexionPanel anterior;
        synchronized (this) {
            anterior = actual;
        }
        cerrar(anterior);
    }

    private static void cerrar(ConexionPanel c) {
        try {
            if (c != null) c.close();
        } catch (RuntimeException ignorada) {
            // se está descartando la conexión
        }
    }
}
