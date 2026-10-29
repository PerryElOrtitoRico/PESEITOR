package com.rutina.panel;

import panel.core.ColaLocal;
import panel.core.ConfigPanel;
import panel.core.PanelClient;
import panel.core.Registro;
import panel.core.Sincronizador;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;

/**
 * Conexión de RUTINEITOR con el Panel: lee {@code panel-cliente.properties}, guarda lo pendiente en
 * {@code panel-cola.jsonl} y lo envía en segundo plano. Sin configuración activa no crea la cola, ni hilos, ni
 * toca la red.
 */
public final class ConexionPanel implements AutoCloseable {

    static final String ARCHIVO_CONFIG = "panel-cliente.properties";
    static final String ARCHIVO_COLA = "panel-cola.jsonl";

    private final ConfigPanel cfg;
    private final PanelClient cliente;
    private final ColaLocal cola;
    private final Sincronizador sincronizador;

    private ConexionPanel(ConfigPanel cfg, PanelClient cliente, Sincronizador sincronizador, ColaLocal cola) {
        this.cfg = cfg;
        this.cola = cola;
        this.cliente = cliente;
        this.sincronizador = sincronizador;
    }

    /** Abre la conexión con la configuración de la carpeta de datos. Nunca lanza: ante cualquier fallo queda inactiva. */
    public static ConexionPanel abrir(Path carpetaDatos) {
        try {
            ConfigPanel cfg = ConfigPanel.cargar(carpetaDatos.resolve(ARCHIVO_CONFIG));
            if (!cfg.activo()) {
                return new ConexionPanel(cfg, null, null, null);
            }
            ColaLocal cola = new ColaLocal(carpetaDatos.resolve(ARCHIVO_COLA));
            PanelClient cliente = new PanelClient(cfg, PanelClient.httpPorDefecto());
            Sincronizador sincronizador = new Sincronizador(cfg, cola, cliente);
            sincronizador.iniciar();
            return new ConexionPanel(cfg, cliente, sincronizador, cola);
        } catch (RuntimeException e) {
            return new ConexionPanel(inactiva(), null, null, null);
        }
    }

    /** Configuración inactiva: se carga un "archivo" que es una carpeta, que nunca se puede leer. */
    private static ConfigPanel inactiva() {
        return ConfigPanel.cargar(Path.of(System.getProperty("user.dir", ".")));
    }

    /**
     * Prueba los valores escritos en la ventana sin guardarlos: los pasa por una carpeta temporal que se borra
     * siempre. Hace red de forma síncrona (no llamar desde el hilo de Swing). Nunca lanza: ante un fallo, SIN_RESPUESTA.
     */
    public static PanelClient.Conexion probarValores(String url, String token) {
        Path carpeta = null;
        try {
            carpeta = Files.createTempDirectory("rutineitor-panel-prueba");
            guardarConfig(carpeta, url, token, "prueba", true);
            ConfigPanel cfg = ConfigPanel.cargar(carpeta.resolve(ARCHIVO_CONFIG));
            return new PanelClient(cfg, PanelClient.httpPorDefecto()).probar();
        } catch (IOException | RuntimeException e) {
            return PanelClient.Conexion.SIN_RESPUESTA;
        } finally {
            borrar(carpeta);
        }
    }

    private static void borrar(Path carpeta) {
        if (carpeta == null) return;
        try {
            Files.deleteIfExists(carpeta.resolve(ARCHIVO_CONFIG));
            Files.deleteIfExists(carpeta);
        } catch (IOException | RuntimeException ignorada) {
            // era una carpeta temporal
        }
    }

    /** Escribe la configuración (UTF-8; permisos 600 si el sistema de archivos es POSIX). */
    public static void guardarConfig(Path carpetaDatos, String url, String token, String dispositivo,
                                     boolean enviar) throws IOException {
        Files.createDirectories(carpetaDatos);
        Path archivo = carpetaDatos.resolve(ARCHIVO_CONFIG);
        restringirPermisos(archivo);
        Properties props = new Properties();
        props.setProperty("url", url);
        props.setProperty("token", token);
        props.setProperty("dispositivo", dispositivo);
        props.setProperty("enviar", Boolean.toString(enviar));
        try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            props.store(w, "Conexion de RUTINEITOR con el Panel");
        }
    }

    /** Crea el archivo con permisos 600 antes de escribir el token (o los corrige si ya existía). */
    private static void restringirPermisos(Path archivo) throws IOException {
        if (!archivo.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        Set<PosixFilePermission> soloDueno = PosixFilePermissions.fromString("rw-------");
        if (Files.exists(archivo)) {
            Files.setPosixFilePermissions(archivo, soloDueno);
        } else {
            Files.createFile(archivo, PosixFilePermissions.asFileAttribute(soloDueno));
        }
    }

    public boolean activo() {
        return cfg.activo();
    }

    /** Encola el registro y avisa al hilo de envío. No hace nada si la conexión está inactiva. */
    public void publicar(Registro r) {
        if (sincronizador != null) {
            sincronizador.publicar(r);
        }
    }

    /**
     * Intenta enviar ya todo lo pendiente. Hace red de forma síncrona: no llamar desde el hilo de eventos de Swing.
     */
    public void vaciarAhora() {
        if (sincronizador != null) {
            sincronizador.vaciarAhora();
        }
    }

    /** Prueba la conexión (red síncrona, no llamar desde el hilo de Swing). Inactiva: SIN_RESPUESTA sin red. */
    public PanelClient.Conexion probar() {
        return cliente == null ? PanelClient.Conexion.SIN_RESPUESTA : cliente.probar();
    }

    /** Cuántos registros siguen en la cola sin entregar (lee el archivo; no llamar desde el hilo de Swing). */
    public int pendientes() {
        return cola == null ? 0 : cola.pendientes().size();
    }

    public String url() {
        return cfg.url();
    }

    public String dispositivo() {
        return cfg.dispositivo();
    }

    /** Detiene el hilo de envío. Idempotente; no borra la cola. */
    @Override
    public void close() {
        if (sincronizador != null) {
            sincronizador.close();
        }
    }
}
