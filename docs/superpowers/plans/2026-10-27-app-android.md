# RUTINEITOR para Android — plan de implementación

> **Para agentes:** SUB-SKILL REQUERIDA: subagent-driven-development (recomendada) o executing-plans, tarea por tarea. Los pasos usan casillas (`- [ ]`) para el seguimiento.

**Objetivo:** app nativa de Android (APK propio, sin nada de Google) que hace lo mismo que la PWA `celular/`, con el estilo del RUTINEITOR de escritorio y modo oscuro.

**Arquitectura:** proyecto Gradle en `android/` dentro del repo. La lógica pura (notación, JSON, escalas) se **comparte** con el Maven de la PC apuntando un source set a `../src/main/java` filtrado; la lógica nueva (sesión, totales, cronómetro, almacenamiento) es Java puro en `com.rutina.app.logica` y `com.rutina.app.datos`, testeado con JUnit 5 en la JVM. Las Activities solo arman vistas y llaman a esa lógica.

**Tecnología:** Java 17 (lenguaje) con JDK 21, Android Gradle Plugin 8.x por línea de comandos, AndroidX AppCompat + Material Components (tema MaterialComponents, no Material 3), Gson, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-27-app-android-design.md`. Comportamiento de referencia: `celular/index.html`, `celular/logica.js`, `docs/llm/00-especificacion.md` (secciones 2, 5–8), `docs/registro-sesiones/00-especificacion.md`.

**Ejecución (pedido del usuario):** el código lo escriben qwen3.5-9b / Gemma (LM Studio) con el método de `~/Escritorio/Hobbies/programacion/Proyectos/qwen-aprendizajes.md` y las herramientas de `docs/llm/` (`pedir.py`, `modelo.sh`); un subagente Sonnet lleva el ciclo y frena una subtarea tras 2 correcciones fallidas. Las tareas marcadas **[modelo]** van al modelo local (Claude escribe esqueleto, tarea y tests ocultos); las **[Claude]** las hace el subagente (configuración, vistas XML, cableado de Activities).

## Restricciones globales
- Sin Google Play Services, Firebase, cuentas, analítica ni permiso `INTERNET`. Dependencias: solo `androidx.appcompat`, `com.google.android.material:material` (Material Components) y `com.google.code.gson`. Otra → preguntar al usuario.
- `minSdk 26`, `targetSdk`/`compileSdk` 35, `applicationId "com.rutina.app"`, nombre visible **RUTINEITOR**.
- Solo herramientas de línea de comandos del SDK en `~/Android/Sdk` (sin Android Studio). Compilar con `./gradlew` desde `android/`.
- Mismos formatos: plan `"rutineitor-plan"` v1, registro `"rutineitor-registro"` v1 con `nota` y `duracionMin`; se leen y escriben con `JsonRegistro` de la PC.
- Escalas con el texto exacto de `docs/registro-sesiones/00-especificacion.md` §2 (esfuerzo: 1 Muy liviano · 2 Liviano · 3 Justo · 4 Al límite · 5 Pesado). Descanso se muestra de 6 a 1.
- Estilo: grises FlatLaf Light (claro) / Darcula (oscuro), bordes de 1 dp, esquinas de 3 dp, botones planos, sin chips ni tarjetas flotantes ni animaciones; color solo en los cuatro discos (rojo `#E53935`, amarillo `#FDD835`, azul `#1E5BD8`, verde `#43A047`) y en el azul del campo con foco.
- Textos de la interfaz en español rioplatense, iguales a la PWA donde existan.
- Commits chicos con Conventional Commits en español; nunca `push`.

## Foco de revisión
1. **Plan que llega por WhatsApp sin tipo `.json`** (`application/octet-stream` o nombre sin extensión): tiene que cargar igual (la PWA tuvo ese arreglo en `0abc6c0`). → test en Tarea 3 (`cargarDesdeTexto` decide por el campo `formato`, no por el nombre) y filtros de intent en Tarea 5.
2. **Archivo de datos dañado en el celular** (JSON roto en `planes/` o `registros/`): no se cierra la app ni se pierden los demás; el roto se renombra a `<nombre>.roto-<epochms>` y se sigue. → test en Tarea 3.
3. **Girar el celular o que Android mate la app en medio de la sesión**: nada de lo escrito se pierde (se guarda en cada cambio y la pantalla se rearma desde `Almacen`). → test en Tarea 3 (guardar y releer) y prueba manual en Tarea 7.
4. **Cargar de nuevo el mismo plan** (mismo `mesocicloId`): reemplaza el plan y **conserva** su registro, como la PWA. → test en Tarea 3.
5. **Cronómetro corriendo cuando se reinicia el celular o se mata la app**: al volver, el tiempo es correcto (sale de `desde`) y la notificación se vuelve a mostrar. → test en Tarea 2 (`cronoMs` con `desde` viejo) y paso en Tarea 7.

---

### Tarea 1: SDK, proyecto Gradle y lógica compartida [Claude]

**Archivos:**
- Crear: `android/settings.gradle`, `android/build.gradle`, `android/app/build.gradle`, `android/gradle.properties`, wrapper (`android/gradlew`, `android/gradle/wrapper/*`), `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/java/com/rutina/app/ui/MainActivity.java` (vacía con la barra), `android/.gitignore` (`.gradle/`, `build/`, `local.properties`)
- Test: `android/app/src/test/java/com/rutina/app/CompartidoTest.java`

**Interfaces:**
- Produce: las clases de `com.rutina.registro` (salvo las que dependen de cálculo de la PC) y los enums `com.rutina.model.Tipo` y `com.rutina.model.Aproximacion` disponibles en la app sin copiar archivos.

- [ ] **Paso 1:** Instalar en `~/Android/Sdk` las command-line tools y, con `sdkmanager`, `platform-tools`, `platforms;android-35`, `build-tools;35.0.0`; aceptar licencias. Bajar una distribución de Gradle 8.x solo para generar el wrapper. Verificar: `~/Android/Sdk/platform-tools/adb version` responde.
- [ ] **Paso 2:** Source set compartido en `app/build.gradle`: `java.srcDir("../../src/main/java")` con `include "com/rutina/registro/**", "com/rutina/model/Tipo.java", "com/rutina/model/Aproximacion.java"` y `exclude "com/rutina/registro/ArmadorPlan.java", "com/rutina/registro/AnalisisRegistro.java", "com/rutina/registro/ComparacionTipo.java"` (dependen del cálculo de la PC). Si alguna clase incluida no compila en Android, ajustar el filtro, nunca copiar. JUnit 5 con `testOptions.unitTests.all { useJUnitPlatform() }`. `compileOptions` en Java 17.
- [ ] **Paso 3: Test que falla** `CompartidoTest.leeElPlanDeEjemplo()`: `JsonRegistro.planDeJson(<contenido de docs/registro-sesiones/ejemplos/plan-ejemplo.json>)` da `formato()` `"rutineitor-plan"` y al menos una sesión; `NotacionRegistro.parse("3-80.5", List.of(new SerieReal(3,85), new SerieReal(3,85)))` da `[SerieReal(3,80.0), SerieReal(5,85.0)]`. (Leer el ejemplo con la ruta relativa al módulo.)
- [ ] **Paso 4:** `./gradlew test assembleDebug` → BUILD SUCCESSFUL y `app/build/outputs/apk/debug/app-debug.apk` existe. `mvn -B -q -o test` en la raíz sigue en verde.
- [ ] **Paso 5:** Commit `chore: proyecto Android con la lógica del registro compartida`.

### Tarea 2: Lógica de la sesión, totales y cronómetro [modelo]

**Archivos:**
- Crear: `android/app/src/main/java/com/rutina/app/logica/Sesiones.java`, `Totales.java`, `Crono.java`, `EstadoCrono.java`
- Test: `android/app/src/test/java/com/rutina/app/logica/{Sesiones,Totales,Crono}Test.java`

**Interfaces** (traducción de `celular/logica.js`; mismos casos que `docs/registro-sesiones/evaluacion/web/pruebas.html` y `docs/llm/evaluacion/web/pruebas-mejoras.html`):
- `Sesiones.sesionDelPlan(PlanCelular, int micro, int dia) → SesionPlan` (o null)
- `Sesiones.sesionDelRegistro(RegistroCelular, int micro, int dia) → SesionRegistro` (o null)
- `Sesiones.registroVacio(PlanCelular) → RegistroCelular` (sesiones vacías, mismo perfil y mesocicloId)
- `Sesiones.limpiarSesion(SesionRegistro) → SesionRegistro` (filas con texto en blanco fuera, resto `strip`; nota `strip`, en blanco → null; ejercicio fuera si no tiene filas, ni esfuerzo, ni nota)
- `Sesiones.guardarSesion(RegistroCelular, SesionRegistro) → RegistroCelular` (limpia, reemplaza la de igual micro+día, ordena por micro y día)
- `Sesiones.pista(List<SerieReal>) → String` (`"3.3.3 · 120/127,5"`, `"12.12"`, `""`)
- `Sesiones.nombreArchivoRegistro(PlanCelular) → String` (`"registro-<perfil con _>-<inicio>.json"`)
- `Totales.totalReps(List<SerieReal>) → int`; `Totales.totalesEjercicio(List<FilaPlan>, Map<String,String> textos) → int[]{planificadas, hechas}` con `hechas = -1` si alguna casilla es inválida
- `record EstadoCrono(long acumuladoMs, Long desde)`; `Crono.nuevo()`, `iniciar(EstadoCrono, long ahora)`, `pausar(EstadoCrono, long ahora)`, `ms(EstadoCrono, long ahora) → long`, `texto(long ms) → String`, `minutosDe(long ms) → Integer` (0 → null), `duracionValida(String) → Integer` (1..600 o null)

- [ ] **Paso 1: Tests que fallan** con los ejemplos del contrato: totales `{}`→`{11,11}`, `{A:"3.2"}`→`{11,10}`, `{B:"0"}`→`{11,6}`, `{A:"x"}`→`{11,-1}`; `texto(0)`=`"0:00:00"`, `texto(75_500)`=`"0:01:15"`, `texto(3_725_000)`=`"1:02:05"`; `minutosDe(29_999)`=1, `(90_000)`=2, `(4_500_000)`=75, `(0)`=null; `duracionValida("75")`=75, `(" 5 ")`=5, `("0")`, `("601")`, `("7,5")`, `("")` = null; `ms(new EstadoCrono(60_000, 1_000L), 3_601_000)` = 3_660_000 (cronómetro que siguió corriendo, Foco 5); los de `limpiarSesion`, `guardarSesion` y `pista` copiados de `pruebas.html`.
- [ ] **Paso 2:** `./gradlew test` → FALLA por los `UnsupportedOperationException` del esqueleto.
- [ ] **Paso 3:** Subtareas al modelo (Gemma: `Totales`, `Crono`, `pista`, `nombreArchivoRegistro`; qwen: `limpiarSesion`, `guardarSesion`), un pedido por subtarea con `docs/llm/modelo.sh … pedir.py … --metodos`.
- [ ] **Paso 4:** `./gradlew test` → PASA.
- [ ] **Paso 5:** Commit `feat: lógica de la sesión, totales y cronómetro para Android`.

### Tarea 3: Almacenamiento en archivos [modelo]

**Archivos:**
- Crear: `android/app/src/main/java/com/rutina/app/datos/Almacen.java`
- Test: `android/app/src/test/java/com/rutina/app/datos/AlmacenTest.java` (con `@TempDir Path dir` y `throws IOException` ya en el esqueleto)

**Interfaces:**
- Consume: `JsonRegistro`, `Sesiones.registroVacio`, `EstadoCrono`.
- Produce: `new Almacen(Path raiz)`; `cargarDesdeTexto(String json) → Object` (devuelve el `PlanCelular` guardado o el `RegistroCelular` importado según `"formato"`; lanza `IllegalArgumentException` con mensaje en español si no es ninguno o no valida; un registro solo se importa si existe un plan con su `mesocicloId`); `planes() → List<PlanCelular>` (ordenados por `inicio`); `plan(long id)`; `borrarPlan(long id)` (borra plan, registro y cronos de ese plan); `registro(long id) → RegistroCelular` (vacío si no hay); `guardarRegistro(RegistroCelular)`; `activo() → Long` / `activo(Long)`; `crono(long id, int micro, int dia) → EstadoCrono` / `crono(…, EstadoCrono)`; `oscuro() → boolean` / `oscuro(boolean)`.
- Disposición: `planes/<id>.json`, `registros/<id>.json`, `preferencias.json` (activo, oscuro, cronos por `"<id>-<micro>-<dia>"`). Escritura atómica: temporal en la misma carpeta + `Files.move(…, ATOMIC_MOVE, REPLACE_EXISTING)`.

- [ ] **Paso 1: Tests que fallan:** guardar un plan y releerlo con otra instancia de `Almacen` sobre la misma carpeta (Foco 3); cargar dos veces el mismo plan conserva las sesiones de su registro (Foco 4); el texto del plan de ejemplo con espacios y sin nada que indique `.json` carga (Foco 1); `planes/9.json` con `"{roto"` → `planes()` devuelve los otros y existe `planes/9.json.roto-*` (Foco 2); importar un registro sin plan → `IllegalArgumentException` cuyo mensaje contiene `"no hay ningún plan"`; `borrarPlan` deja sin archivos de ese id; `oscuro()` por defecto `false`.
- [ ] **Paso 2:** `./gradlew test` → FALLA.
- [ ] **Paso 3:** Subtareas al modelo (qwen: escritura atómica y lectura con respaldo del roto; Gemma: preferencias y listados), con los pasos de código en la tarea.
- [ ] **Paso 4:** `./gradlew test` → PASA.
- [ ] **Paso 5:** Commit `feat: almacenamiento de planes y registros en el celular`.

### Tarea 4: Tema rústico claro/oscuro e ícono [Claude]

**Archivos:**
- Crear: `res/values/colors.xml`, `res/values-night/colors.xml`, `res/values/themes.xml` (padre `Theme.MaterialComponents.DayNight.NoActionBar`), `res/drawable/` (fondo de campo y botón con borde 1 dp y radio 3 dp, con estados de foco), `res/layout/barra_superior.xml` (nombre + 4 discos), `res/mipmap-*/ic_launcher.png` desde `docs/icono/rutineitor-512.png`
- Modificar: `MainActivity.java`

**Interfaces:**
- Produce: `App.aplicarTema(boolean oscuro)` (llama a `AppCompatDelegate.setDefaultNightMode` con `MODE_NIGHT_YES`/`NO`), usado al iniciar con `Almacen.oscuro()` y por el interruptor.
- Colores claros: fondo `#F2F2F2`, barra `#E4E4E4`, campo `#FFFFFF`, borde `#BDBDBD`, texto `#222222`, secundario `#555555`. Oscuros: fondo `#2B2D30`, barra `#3C3F41`, campo `#46494B`, borde `#5E6060`, texto `#DCDCDC`, secundario `#A9A9A9`. Foco: `#1E5BD8` claro / `#4A88F0` oscuro.

- [ ] **Paso 1:** Interruptor "Modo oscuro" en el menú de la barra superior (ítem con casilla), que guarda en `Almacen` y recrea la Activity.
- [ ] **Paso 2:** `./gradlew assembleDebug` → BUILD SUCCESSFUL; instalar con `adb install -r` y comparar con las maquetas aprobadas (claro y oscuro): grises, bordes finos, discos en la barra, ícono pixel art en el lanzador. Captura con `adb exec-out screencap -p`.
- [ ] **Paso 3:** Commit `feat: tema rústico claro y oscuro con el ícono de RUTINEITOR`.

### Tarea 5: Inicio con pestañas Sesión · Planes · Exportar, y archivos de entrada/salida [Claude]

**Archivos:**
- Crear: `ui/MainActivity.java` (TabLayout con tres fragments), `ui/InicioFragment.java`, `ui/PlanesFragment.java`, `ui/ExportarFragment.java`, layouts, `res/xml/rutas_archivos.xml`
- Modificar: `AndroidManifest.xml` (FileProvider; intent-filters `ACTION_SEND` y `ACTION_VIEW` para `application/json`, `text/plain`, `application/octet-stream` en `MainActivity`)

**Interfaces:**
- Consume: `Almacen` (todo), `Sesiones.nombreArchivoRegistro`, `JsonRegistro.registroAJson`.
- Produce: abre `SesionActivity` con extras `"plan"` (long), `"micro"` y `"dia"` (int).

- [ ] **Paso 1:** Sesión: desplegables Plan / Microciclo / Día (microciclos y días que existen en el plan), botón "Abrir sesión" y tabla Micro | Día | Anotada (fecha `dd/MM`) de las sesiones del registro. Sin planes: texto "Cargá un plan en la pestaña Planes".
- [ ] **Paso 2:** Planes: lista de planes (perfil · desde `inicio`), "Cargar archivo" (`ACTION_OPEN_DOCUMENT`, tipo `*/*`) que pasa por `Almacen.cargarDesdeTexto` y sirve para plan y para registro de la PWA; "Borrar plan" con confirmación igual a la PWA. Errores en un diálogo con el mensaje de la excepción.
- [ ] **Paso 3:** Un intent `SEND`/`VIEW` recibido lee el URI y llama al mismo `cargarDesdeTexto`, luego muestra la pestaña Sesión con ese plan activo.
- [ ] **Paso 4:** Exportar: "Compartir registro" (`ACTION_SEND` con URI de FileProvider, nombre de `nombreArchivoRegistro`) y "Guardar en Descargas" (`ACTION_CREATE_DOCUMENT` con ese nombre). Sin sesiones anotadas: aviso "Todavía no anotaste ninguna sesión".
- [ ] **Paso 5:** Verificar en el celular: compartir `docs/registro-sesiones/ejemplos/plan-ejemplo.json` desde WhatsApp o el administrador de archivos carga el plan; copiarlo con `adb push … /sdcard/Download/` y cargarlo con "Cargar archivo"; exportar y abrir el archivo en la PC con "Importar registro". Commit `feat: inicio con pestañas y carga/exportación de archivos`.

### Tarea 6: Pantalla de la sesión [Claude, con textos de lógica del modelo]

**Archivos:**
- Crear: `ui/SesionActivity.java`, `ui/VistaEjercicio.java` (arma el recuadro de un ejercicio), `res/layout/activity_sesion.xml`, `res/layout/vista_ejercicio.xml`

**Interfaces:**
- Consume: `Sesiones`, `Totales`, `NotacionRegistro`, escalas, `Almacen`.

- [ ] **Paso 1:** Barra: flecha atrás, `"Microciclo N · Día M"`, línea `"Anotada el …"` / `"Sin anotar todavía"`. Desplegable de descanso arriba (6 a 1) y de sensación final abajo; opción vacía "—" = sin anotar.
- [ ] **Paso 2:** Por ejercicio un recuadro: encabezado nombre + `"<hechas> / <plan> reps"` (`"— / <plan> reps"` si `hechas == -1`); tabla con grilla Zona | Planilla | Entrenado (Planilla = `pista`, Entrenado = campo con la pista como hint, teclado decimal); debajo del campo, mensaje en rojo con el error de `NotacionRegistro.parse`, o gris `"Ojo: N series (planificadas: M). Los decimales del peso van con coma."`; "Sensación" desplegable; "Notas" (máx. 500). Sin ejercicios: "Este día no tiene ejercicios".
- [ ] **Paso 3:** Cada cambio arma la `SesionRegistro` (fecha = la guardada o hoy `AAAA-MM-DD`) y la guarda con `Sesiones.guardarSesion` + `Almacen.guardarRegistro`. "Borrar lo anotado en esta sesión" con la misma confirmación que la PWA.
- [ ] **Paso 4:** Verificar en el celular con el plan de ejemplo: escribir `3.2`, `x`, una nota; girar el celular y volver a entrar: todo sigue (Foco 3). Commit `feat: pantalla de la sesión`.

### Tarea 7: Cronómetro con notificación [Claude]

**Archivos:**
- Modificar: `ui/SesionActivity.java`, `AndroidManifest.xml` (`POST_NOTIFICATIONS`)
- Crear: `ui/NotificacionCrono.java`

**Interfaces:**
- Consume: `Crono`, `EstadoCrono`, `Almacen.crono(…)`.

- [ ] **Paso 1:** En la barra el tiempo `Crono.texto` actualizado cada 1 s; botones Iniciar / Pausar / Reanudar y "Editar duración" (diálogo con campo numérico; inválido → aviso "Escribí los minutos, de 1 a 600" y no cambia nada). `duracionMin = Crono.minutosDe(…)` se guarda al pausar, al editar, al salir y cada 30 s mientras corre.
- [ ] **Paso 2:** Mientras corre, notificación en curso (canal "Cronómetro") con `setUsesChronometer(true)` y `setWhen(System.currentTimeMillis() - Crono.ms(…))`, título `"Microciclo N · Día M"`; tocarla abre la sesión; se quita al pausar. En Android 13+ pedir el permiso la primera vez; si lo niega, el cronómetro anda sin notificación.
- [ ] **Paso 3:** Verificar: iniciar, apagar la pantalla 2 min, la notificación sigue contando; forzar el cierre (`adb shell am force-stop com.rutina.app`), volver a abrir la sesión: el tiempo es correcto y la notificación vuelve (Foco 5). Commit `feat: cronómetro de la sesión con notificación`.

### Tarea 8: Instalación, documentación y retiro de la PWA [Claude + usuario]

**Archivos:**
- Modificar: `README.md` (compilar e instalar la app, dónde guarda los datos: almacenamiento interno de `com.rutina.app`), nota del vault `Trabajo/Proyectos/Programa de rutina de gimnasio/Cómo funciona.md` y `Vault Map.md`
- Borrar (solo con el visto bueno del usuario, en el paso 3): `celular/`

- [ ] **Paso 1:** El usuario activa la depuración USB; `adb devices` lo lista; `./gradlew installDebug`. Confirmar `minSdk` con `adb shell getprop ro.build.version.sdk` (≥ 26).
- [ ] **Paso 2:** Migración: el usuario exporta el registro desde la PWA y lo carga en la app con "Cargar archivo" (después de cargar el plan); comprobar que aparecen sus sesiones.
- [ ] **Paso 3:** Con el OK del usuario: dejar de publicar `gh-pages` (lo decide él: borrar la rama remota o desactivar Pages), borrar `celular/` y las secciones de la PWA del README. Commit `chore: retira la app web del celular`.

---

## Autorrevisión
- Cobertura del spec: funciones de la PWA → Tareas 2, 5, 6, 7; formato y PC sin cambios → Tarea 1 (lógica compartida) y `mvn test` en verde; independencia → restricciones globales (sin `INTERNET`), Tarea 3 (datos locales), Tarea 5 (archivos por cualquier camino); estilo y modo oscuro → Tarea 4; retiro de la PWA → Tarea 8; decisiones de Claude del spec (JSON en archivos, `setUsesChronometer`, minSdk 26, lógica compartida) → Tareas 3, 7, 8, 1.
- Firmas usadas en Tareas 5–7 coinciden con las de 2 y 3 (`hechas == -1`, `Almacen.crono(long,int,int)`).
