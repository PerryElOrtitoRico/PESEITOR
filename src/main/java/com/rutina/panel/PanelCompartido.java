package com.rutina.panel;

/** Guarda la conexión actual con el panel; permite cambiarla sin tocar a quienes la usan. */
public final class PanelCompartido implements AutoCloseable {

    private ConexionPanel actual;

    public PanelCompartido(ConexionPanel inicial) {
        this.actual = inicial;
    }

    public synchronized ConexionPanel get() {
        return actual;
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
