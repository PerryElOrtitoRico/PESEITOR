package com.rutina.service.datos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Archivo SQLite del programa: esquema, carga inicial y respaldo si el archivo está dañado. */
public final class Database implements AutoCloseable {

    /** Gson compartido; los mapas con claves record (plantillas) necesitan la serialización compleja. */
    static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().create();

    private static final String[] ESQUEMA = {
            "CREATE TABLE IF NOT EXISTS perfil (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS ejercicio (id INTEGER PRIMARY KEY AUTOINCREMENT, perfil_id INTEGER NOT NULL,"
                    + " nombre TEXT NOT NULL, tipo TEXT NOT NULL, rm REAL, rm_objetivo REAL, techo INTEGER NOT NULL,"
                    + " reps_json TEXT, grupo_plio TEXT, orden INTEGER NOT NULL, activo INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS historial_rm (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " ejercicio_id INTEGER NOT NULL, fecha TEXT NOT NULL, rm REAL NOT NULL)",
            "CREATE TABLE IF NOT EXISTS metodologia (id INTEGER PRIMARY KEY, json TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS mesociclo (id INTEGER PRIMARY KEY AUTOINCREMENT, perfil_id INTEGER NOT NULL,"
                    + " inicio TEXT NOT NULL, microciclos INTEGER NOT NULL, pot_vol INTEGER, pot_imr INTEGER,"
                    + " fza_vol INTEGER, fza_imr INTEGER, variante TEXT, dias INTEGER, aprox_json TEXT,"
                    + " bloques_json TEXT, calentamiento TEXT, auxiliares TEXT, rm_json TEXT)",
            "CREATE TABLE IF NOT EXISTS plan_pliometria (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " perfil_id INTEGER NOT NULL, nivel INTEGER NOT NULL, inicio TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS registro (mesociclo_id INTEGER PRIMARY KEY, json TEXT NOT NULL, importado TEXT NOT NULL)",
    };

    private final Connection conexion;
    private final boolean seRestauro;

    private Database(Connection conexion, boolean seRestauro) {
        this.conexion = conexion;
        this.seRestauro = seRestauro;
    }

    /** $XDG_DATA_HOME/rutina/rutina.db, o ~/.local/share/rutina/rutina.db. */
    public static Path rutaPorDefecto() {
        String xdg = System.getenv("XDG_DATA_HOME");
        Path base = xdg != null && !xdg.isBlank()
                ? Path.of(xdg)
                : Path.of(System.getProperty("user.home"), ".local", "share");
        return base.toAbsolutePath().resolve("rutina").resolve("rutina.db");
    }

    public static Database abrir(Path archivo) throws IOException, SQLException {
        Files.createDirectories(archivo.toAbsolutePath().getParent());
        try {
            return new Database(preparar(archivo), false);
        } catch (SQLException e) {
            // Solo un archivo dañado se reemplaza; un bloqueo, permiso o error de disco se informa sin tocar nada
            if (!estaDanada(e)) throw e;
            String sello = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            for (String sufijo : new String[]{"", "-journal", "-wal", "-shm"}) {
                Path f = archivo.resolveSibling(archivo.getFileName() + sufijo);
                if (Files.exists(f)) Files.move(f, f.resolveSibling(f.getFileName() + ".bak-" + sello));
            }
            return new Database(preparar(archivo), true);
        }
    }

    private static boolean estaDanada(SQLException e) {
        int codigo = e instanceof SQLiteException s ? s.getResultCode().code : e.getErrorCode();
        int primario = codigo & 0xFF;
        return primario == SQLiteErrorCode.SQLITE_NOTADB.code || primario == SQLiteErrorCode.SQLITE_CORRUPT.code;
    }

    private static Connection preparar(Path archivo) throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + archivo.toAbsolutePath());
        try {
            try (Statement st = c.createStatement()) {
                for (String sql : ESQUEMA) st.execute(sql);
            }
            MetodologiaRepo.inicializar(c);
            return c;
        } catch (SQLException e) {
            c.close();
            throw e;
        }
    }

    public Connection conexion() {
        return conexion;
    }

    /** true si el archivo estaba dañado y se reemplazó por uno nuevo (el viejo quedó como .bak-…). */
    public boolean seRestauro() {
        return seRestauro;
    }

    @Override
    public void close() throws SQLException {
        conexion.close();
    }
}
