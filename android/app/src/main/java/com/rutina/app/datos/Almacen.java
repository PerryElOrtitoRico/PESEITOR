package com.rutina.app.datos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.rutina.app.logica.Crono;
import com.rutina.app.logica.EstadoCrono;
import com.rutina.app.logica.Sesiones;
import com.rutina.registro.JsonRegistro;
import com.rutina.registro.PlanCelular;
import com.rutina.registro.RegistroCelular;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Planes, registros y preferencias en archivos JSON dentro de una carpeta raíz:
 * {@code planes/<id>.json}, {@code registros/<id>.json} y {@code preferencias.json}.
 * Escritura atómica (temporal + mover) y, si un archivo está dañado, se lo aparta como {@code <nombre>.roto-<epoch ms>}.
 */
public class Almacen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private final Path raiz;

    public Almacen(Path raiz) {
        this.raiz = raiz;
    }

    // ---------- rutas ----------

    Path archivoPlan(long id) {
        return raiz.resolve("planes").resolve(id + ".json");
    }

    Path archivoRegistro(long id) {
        return raiz.resolve("registros").resolve(id + ".json");
    }

    Path archivoPreferencias() {
        return raiz.resolve("preferencias.json");
    }

    // ---------- disco (T34a) ----------

    /** Lee un archivo completo como texto UTF-8. */
    static String leerTexto(Path archivo) throws IOException {
        return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
    }

    /** Escribe el texto en un temporal de la misma carpeta y lo mueve sobre el destino (crea la carpeta si falta). */
    static void escribirAtomico(Path destino, String texto) throws IOException {
        Files.createDirectories(destino.getParent());
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.write(tmp, texto.getBytes(StandardCharsets.UTF_8));
        Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Renombra un archivo dañado a {@code <nombre>.roto-<epoch ms>} (misma carpeta). Si no puede, no hace nada. */
    static void apartarRoto(Path archivo) {
        Path roto = archivo.resolveSibling(archivo.getFileName() + ".roto-" + System.currentTimeMillis());
        try { Files.move(archivo, roto, StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) { }
    }

    /** El plan guardado en ese archivo; null si no existe; si está dañado lo aparta y devuelve null. */
    static PlanCelular leerPlan(Path archivo) {
        if (!Files.exists(archivo)) return null;
        try { return JsonRegistro.planDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }
    }

    /** El registro guardado en ese archivo; null si no existe; si está dañado lo aparta y devuelve null. */
    static RegistroCelular leerRegistro(Path archivo) {
        if (!Files.exists(archivo)) return null;
        try { return JsonRegistro.registroDeJson(leerTexto(archivo)); } catch (IOException | IllegalArgumentException e) { apartarRoto(archivo); return null; }
    }

    // ---------- planes y registros ----------

    /** El plan con ese id, o null. */
    public PlanCelular plan(long id) {
        return leerPlan(archivoPlan(id));
    }

    /** El registro del plan; vacío si todavía no se guardó nada; null si tampoco hay plan. */
    public RegistroCelular registro(long id) {
        RegistroCelular r = leerRegistro(archivoRegistro(id));
        if (r != null) return r;
        PlanCelular p = plan(id);
        return p == null ? null : Sesiones.registroVacio(p);
    }

    public void guardarRegistro(RegistroCelular r) throws IOException {
        escribirAtomico(archivoRegistro(r.mesocicloId()), JsonRegistro.registroAJson(r));
    }

    /**
     * Carga un plan (devuelve el {@link PlanCelular}) o un registro (devuelve el {@link RegistroCelular}) según el campo
     * "formato" del texto. Un plan de igual mesocicloId reemplaza al anterior y conserva su registro.
     *
     * @throws IllegalArgumentException con mensaje en español si no es un plan ni un registro válido, o si el registro no
     *                                  tiene plan
     */
    public Object cargarDesdeTexto(String json) throws IOException {
        String texto = json.replace("\uFEFF", "").strip();
        String formato = null;
        try {
            JsonElement e = JsonParser.parseString(texto);
            if (e.isJsonObject() && e.getAsJsonObject().has("formato")) formato = e.getAsJsonObject().get("formato").getAsString();
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException ex) {
            throw new IllegalArgumentException("El archivo no es un JSON válido: " + ex.getMessage());
        }
        if (PlanCelular.FORMATO.equals(formato)) {
            PlanCelular plan = JsonRegistro.planDeJson(texto);
            escribirAtomico(archivoPlan(plan.mesocicloId()), JsonRegistro.planAJson(plan));
            return plan;
        }
        if (RegistroCelular.FORMATO.equals(formato)) {
            RegistroCelular registro = JsonRegistro.registroDeJson(texto);
            if (plan(registro.mesocicloId()) == null) throw new IllegalArgumentException("Primero cargá el plan de este registro: no hay ningún plan con el id " + registro.mesocicloId());
            guardarRegistro(registro);
            return registro;
        }
        throw new IllegalArgumentException("El archivo no es un plan ni un registro de RUTINEITOR");
    }

    /** Los planes guardados, ordenados por inicio; los archivos dañados se apartan y se saltean. */
    public List<PlanCelular> planes() {
        List<PlanCelular> res = new ArrayList<>();
        Path carpeta = raiz.resolve("planes");
        if (!Files.isDirectory(carpeta)) return res;
        List<Path> archivos = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(carpeta, "*.json")) {
            for (Path p : ds) archivos.add(p);
        } catch (IOException e) {
            return res;
        }
        for (Path p : archivos) {
            PlanCelular plan = leerPlan(p);
            if (plan != null) res.add(plan);
        }
        res.sort(Comparator.comparing(PlanCelular::inicio));
        return res;
    }

    /** Borra el plan, su registro y sus cronómetros; si era el activo, deja activo() en null. */
    public void borrarPlan(long id) throws IOException {
        Files.deleteIfExists(archivoPlan(id));
        Files.deleteIfExists(archivoRegistro(id));
        JsonObject o = leerPreferencias();
        if (o.has("cronos")) {
            JsonObject cronos = o.getAsJsonObject("cronos");
            for (String k : new ArrayList<>(cronos.keySet())) {
                if (k.startsWith(id + "-")) cronos.remove(k);
            }
        }
        if (o.has("activo") && !o.get("activo").isJsonNull() && o.get("activo").getAsLong() == id) o.remove("activo");
        guardarPreferencias(o);
    }

    // ---------- preferencias ----------

    /** El objeto de preferencias.json; vacío si no existe; si está dañado lo aparta y devuelve uno vacío. */
    JsonObject leerPreferencias() {
        Path p = archivoPreferencias();
        if (!Files.exists(p)) return new JsonObject();
        try {
            JsonElement e = JsonParser.parseString(leerTexto(p));
            if (e.isJsonObject()) return e.getAsJsonObject();
        } catch (IOException | JsonParseException ex) {
            // Fall through to apartarRoto
        }
        apartarRoto(p);
        return new JsonObject();
    }

    void guardarPreferencias(JsonObject preferencias) throws IOException {
        escribirAtomico(archivoPreferencias(), GSON.toJson(preferencias));
    }

    /** Id del plan activo; null si no hay. */
    public Long activo() {
        JsonObject o = leerPreferencias();
        if (!o.has("activo") || o.get("activo").isJsonNull()) return null;
        return o.get("activo").getAsLong();
    }

    /** Guarda el plan activo (null = ninguno). */
    public void activo(Long id) throws IOException {
        JsonObject o = leerPreferencias();
        if (id == null) o.remove("activo"); else o.addProperty("activo", id);
        guardarPreferencias(o);
    }

    /** Si el modo oscuro está encendido (por defecto no). */
    public boolean oscuro() {
        JsonObject o = leerPreferencias();
        return o.has("oscuro") && o.get("oscuro").getAsBoolean();
    }

    public void oscuro(boolean oscuro) throws IOException {
        JsonObject o = leerPreferencias();
        o.addProperty("oscuro", oscuro);
        guardarPreferencias(o);
    }

    /** El cronómetro de esa sesión; {@code Crono.nuevo()} si no hay. */
    public EstadoCrono crono(long id, int micro, int dia) {
        String clave = id + "-" + micro + "-" + dia;
        JsonObject cronos = leerPreferencias().getAsJsonObject("cronos");
        if (cronos == null || !cronos.has(clave)) return Crono.nuevo();
        JsonObject c = cronos.getAsJsonObject(clave);
        Long desde = c.get("desde").isJsonNull() ? null : c.get("desde").getAsLong();
        return new EstadoCrono(c.get("acumuladoMs").getAsLong(), desde);
    }

    public void crono(long id, int micro, int dia, EstadoCrono estado) throws IOException {
        String clave = id + "-" + micro + "-" + dia;
        JsonObject o = leerPreferencias();
        JsonObject cronos = o.has("cronos") ? o.getAsJsonObject("cronos") : new JsonObject();
        JsonObject c = new JsonObject();
        c.addProperty("acumuladoMs", estado.acumuladoMs());
        c.addProperty("desde", estado.desde());
        cronos.add(clave, c);
        o.add("cronos", cronos);
        guardarPreferencias(o);
    }
}
