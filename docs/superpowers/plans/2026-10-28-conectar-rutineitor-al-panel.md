---
tipo: proyecto
creado: 2026-10-28
tags:
  - proyecto
---
# Conectar RUTINEITOR al panel: plan de implementación

> **Para quien implemente:** usar subagent-driven-development (recomendado) o executing-plans, tarea por tarea. Los pasos usan casillas `- [ ]`.

**Objetivo:** que RUTINEITOR (escritorio) mande al panel las sesiones de gimnasio al importar el registro del celular y la evolución de los 1RM, sin cambiar nada de su funcionamiento cuando el panel no está configurado.

**Arquitectura:** RUTINEITOR usa la librería `sync-core` del panel. Al importar el registro, convierte cada sesión (plan + registro) en un `Registro` del panel y lo publica con el `Sincronizador` (cola local, reintentos en segundo plano). Cada 1RM guardado se manda como `resultado`. Una ventana "Conexión con el panel" guarda la URL y el token.

**Tecnología:** Java 21, Maven, Swing + FlatLaf, Gson (ya en RUTINEITOR), JUnit 5, `panel:sync-core:1.0.0-SNAPSHOT` (Jackson va dentro). Sin cambios en los JSON de plan y registro del celular.

**Spec:** [Diseño](Dise%C3%B1o.md) del panel, [Plan 1](Plan%201%20-%20Panel%20y%20sincronizaci%C3%B3n.md) (ya ejecutado; su `README.md` y `docs/detalle-json.md` en el repo `Panel`) y las decisiones de abajo.

**Repos:** `~/Escritorio/Hobbies/programacion/Proyectos/Panel` (Tarea 1) y `~/Escritorio/Hobbies/programacion/Proyectos/ProyectoRutina` (Tareas 2 a 7, en la rama `feature/panel`). Los dos ya son repositorios git. Al empezar, copiar este plan a `docs/superpowers/plans/` del repo `ProyectoRutina`.

## Decisiones de Franco
- Se manda **al importar el registro en la PC** (botón "Importar registro"), no directo desde el celular.
- Se mandan **todas las sesiones del archivo**, aunque sean anteriores al día de la conexión.
- Una sesión **sin duración** no se manda; se avisa cuántas quedaron afuera.
- Los **1RM** se mandan cada vez que se guardan.

## Restricciones globales
- Sin configuración del panel (sin URL o sin token, o `enviar=false`) RUTINEITOR se comporta exactamente como hoy: no crea la cola, no hace red, no muestra errores ni avisos.
- Ningún error del panel (servidor caído, token inválido, disco lleno) interrumpe, congela ni bloquea la importación ni el guardado local.
- El token nunca se escribe en logs ni en mensajes; en la ventana va en un campo de contraseña; el archivo de configuración queda con permisos 600 cuando el sistema lo permite.
- Formato de registro del panel: `fuente = "rutineitor"`, `area = "gimnasio"`, `dispositivo` el de la configuración (el servidor lo reemplaza por el del token), `actualizadoEn` = milisegundos del momento del envío (la última importación gana).
- Sesión: `tipo = "esfuerzo"`, `idEnFuente = "ses:<perfilId>:<mesocicloId>:<microciclo>:<dia>"` (estable: reimportar actualiza sin duplicar), `fecha` = fecha de la sesión, `duracionMin` = minutos del registro (1 a 600), `esfuerzo` = "cómo terminé" (`sensacionFinal`, 1 destruido … 5 con energía; null si no se anotó), `resumen = "Macro <año> · Meso <n> · Micro <m> · Día <d>"` (macro = año de `inicio` del mesociclo; meso = posición 1-based entre los mesociclos del perfil con ese mismo año, ordenados por `inicio`).
- Resultado de 1RM: `tipo = "resultado"`, `idEnFuente = "rm:<ejercicioId>:<fecha ISO>"` (si se guarda otro 1RM el mismo día, el último gana), `metrica = "1rm:<clave>"`, `valor` = kg, sin duración ni esfuerzo. La `<clave>` del ejercicio se arma así: minúsculas, sin tildes, todo lo que no sea letra o número pasa a un guion, sin guiones en los extremos (`Press banca` → `press-banca`).
- Solo se manda el **primer perfil** (el de menor id, el del usuario). Los demás perfiles (por ejemplo el de un amigo) nunca se mandan.
- Contrato del `detalleJson` de gimnasio (Tarea 1 lo amplía): `{"dia":"Macro 2026 · Meso 2 · Micro 3 · Día 1","ejercicios":[{"nombre","planificado","hecho","esfuerzo":n,"esfuerzoTexto":"Al límite","aviso":"bajó series"}]}`. `planificado` y `hecho` agrupan series iguales: `3 × 5 @ 85 kg · 3 × 3 @ 90 kg` (el `@ kg` se omite si el peso es 0). `esfuerzo` es el valor de `EscalaEsfuerzo` del ejercicio (1 muy liviano … 5 pesado); `esfuerzoTexto` es su texto.
- `aviso` (el primero que corresponda): menos series que lo planificado → `bajó series`; mismas series pero menos repeticiones en total → `bajó reps`; mismas repeticiones pero menor peso máximo → `bajó peso`; casilla ilegible → `casilla ilegible`.
- Código existente: seguir el estilo de RUTINEITOR (español en nombres de dominio, 4 espacios, sin tocar los formatos JSON del celular).

## Foco de revisión
- Importar dos veces el mismo archivo: mismos `idEnFuente`, sin duplicados, gana el `actualizadoEn` más nuevo (Tarea 4).
- Una casilla con notación inválida (hoy `AnalisisRegistro` lanza `IllegalArgumentException`) no debe impedir mandar el resto de las sesiones ni romper la importación (Tarea 3).
- Sesión sin duración, con duración fuera de 1 a 600, que no está en el plan, o con lista de ejercicios vacía (Tareas 3 y 4).
- Panel apagado, inalcanzable o con token inválido: la importación guarda igual y el resumen lo dice sin dramatizar (Tarea 4).
- Nombres de ejercicio con tildes, espacios y signos, y dos ejercicios cuya clave sale igual (Tarea 5).
- Datos de otro perfil nunca llegan al panel (Tareas 5 y 6).

---

### Task 1: Panel: probar conexión y esfuerzo por ejercicio en la web (repo `Panel`)

**Files:**
- Modify: `sync-core/src/main/java/panel/core/PanelClient.java`, `sync-core/src/test/java/panel/core/PanelClientTest.java`
- Modify: `panel-server/src/main/resources/web/dia.js`, `docs/detalle-json.md`

**Interfaces:**
- Produces: `PanelClient.probar()` devuelve `PanelClient.Conexion` (`enum { OK, NO_AUTORIZADO, SIN_RESPUESTA }`): GET `<url>/api/totales` con el token; 200 → `OK`; 401 o 403 → `NO_AUTORIZADO`; cualquier otro estado, error de red o configuración inactiva → `SIN_RESPUESTA`. Nunca lanza ni deja el token en mensajes.
- Produces: en la web, si un ejercicio de gimnasio trae `esfuerzoTexto`, la tabla muestra `<n>/5 <esfuerzoTexto>` en lugar de la etiqueta común; sin `esfuerzoTexto` sigue como hoy.

- [ ] **Step 1:** tests en `PanelClientTest` con el servidor falso: `probarConTokenValidoDevuelveOk`, `probarConTokenInvalidoDevuelveNoAutorizado`, `probarConServidorCaidoDevuelveSinRespuesta`, `probarConConfiguracionInactivaDevuelveSinRespuesta`, `probarMandaElBearer`.
- [ ] **Step 2:** `mvn -q -pl sync-core test -Dtest=PanelClientTest` falla.
- [ ] **Step 3:** implementar `probar()` con los mismos timeouts que `enviar`.
- [ ] **Step 4:** `dia.js`: el renderizado de la columna Esfuerzo del detalle de gimnasio usa `esfuerzoTexto` si es una cadena no vacía (siempre con `textContent`). Documentarlo en `docs/detalle-json.md` (campos `dia` y `esfuerzoTexto`).
- [ ] **Step 5:** verificar en el navegador con `scripts/demo.sh` mandando con `curl` una sesión con `esfuerzoTexto`, y una sin él; decir qué se vio.
- [ ] **Step 6:** `mvn -q test` limpio; instalar la librería: `mvn -q -pl sync-core -am install -DskipTests`; commit `feat: probar conexión y texto de esfuerzo por ejercicio`.

### Task 2: RUTINEITOR: conexión con el panel (config, cola, sincronizador)

**Files:**
- Modify: `pom.xml` (dependencia `panel:sync-core:1.0.0-SNAPSHOT`)
- Create: `src/main/java/com/rutina/panel/ConexionPanel.java`
- Test: `src/test/java/com/rutina/panel/ConexionPanelTest.java`

**Interfaces:**
- Consume: `panel.core.ConfigPanel`, `ColaLocal`, `PanelClient`, `Sincronizador`, `Registro` (Tarea 1).
- Produces: `ConexionPanel implements AutoCloseable`:
  - `static ConexionPanel abrir(Path carpetaDatos)`: lee `carpetaDatos/panel-cliente.properties`, usa la cola `carpetaDatos/panel-cola.jsonl` y arranca el `Sincronizador` solo si la configuración está activa.
  - `boolean activo()`, `void publicar(Registro r)` (no hace nada si está inactiva), `void vaciarAhora()`, `PanelClient.Conexion probar()`, `String url()`, `String dispositivo()`, `close()`.
  - `static void guardarConfig(Path carpetaDatos, String url, String token, String dispositivo, boolean enviar)`: escribe el archivo en UTF-8 y, si el sistema lo permite, con permisos 600.

- [ ] **Step 1:** tests con `HttpServer` falso y `@TempDir`: `inactivaNoCreaLaColaNiHaceRed`, `activaEntregaLoPublicado` (tras `vaciarAhora()` el servidor recibió el registro con el Bearer), `guardarConfigYReabrirLaActiva`, `guardarConfigConEnviarFalseQuedaInactiva`, `guardarConfigDejaPermisos600` (omitir si el sistema de archivos no es POSIX), `elTokenNoApareceEnToString`.
- [ ] **Step 2:** `mvn -q test -Dtest=ConexionPanelTest` falla.
- [ ] **Step 3:** implementar `ConexionPanel` y agregar la dependencia; confirmar que `mvn -q package` sigue armando el jar con dependencias y que contiene las clases de `panel.core` (`jar tf … | grep panel/core/Registro`).
- [ ] **Step 4:** `mvn -q test` completo limpio. **Step 5:** commit `feat: conexión con el panel`.

### Task 3: RUTINEITOR: de una sesión del celular a un registro del panel

**Files:**
- Create: `src/main/java/com/rutina/panel/SesionAPanel.java`, `FormatoSeries.java`
- Test: `src/test/java/com/rutina/panel/SesionAPanelTest.java`, `FormatoSeriesTest.java`

**Interfaces:**
- Consume: `PlanCelular`, `SesionPlan`, `EjercicioPlan`, `FilaPlan`, `SesionRegistro`, `EjercicioRegistro`, `SerieReal`, `NotacionRegistro`, `EscalaEsfuerzo`, `panel.core.Registro`.
- Produces:
  - `FormatoSeries.texto(List<SerieReal>) -> String` (agrupa series iguales consecutivas, `3 × 5 @ 85 kg`, unidas con ` · `; lista vacía → `—`; kg con coma decimal si no es entero).
  - `record SesionAPanel.Datos(long perfilId, long mesocicloId, int macro, int meso, String dispositivo)`.
  - `record SesionAPanel.Resultado(Registro registro, String omitida)`: exactamente uno de los dos es no nulo.
  - `static Resultado convertir(Datos datos, PlanCelular plan, SesionRegistro sesion, long ahoraMs)`. No lanza nunca.

- [ ] **Step 1:** tests de `FormatoSeriesTest`: `agrupaSeriesIguales`, `omiteElKgCuandoEsCero`, `kgConDecimalesUsaComa`, `listaVaciaEsRaya`. Tests de `SesionAPanelTest` con un plan y registros armados a mano: `convierteUnaSesionCompleta` (tipo, área, fuente, id, fecha, duración, esfuerzo igual a `sensacionFinal`, resumen exacto, `actualizadoEn` = `ahoraMs`, `detalleJson` con `dia` y los ejercicios en el orden del plan), `planificadoYHechoSalenDelPlanYDeLaCasilla`, `casillaVaciaSignificaComoPlanificado`, `avisoBajoSeries`, `avisoBajoReps`, `avisoBajoPeso`, `sinAvisoSiHizoLoPlanificado`, `casillaIlegibleNoLanzaYAvisa` (el `hecho` muestra el texto escrito), `esfuerzoTextoViajaConElTextoDeLaEscala`, `sinDuracionSeOmite`, `duracionFueraDeRangoSeOmite` (0 y 601), `sesionQueNoEstaEnElPlanSeOmite`, `sinSensacionFinalDejaEsfuerzoNulo`, `sesionSinEjerciciosAnotadosUsaLoPlanificado`.
- [ ] **Step 2:** correr los tests y ver que fallan. **Step 3:** implementar; el `detalleJson` se arma con Gson y la salida de `convertir` debe pasar `Registro.errores()` (agregar esa aserción a cada test que espera un registro). **Step 4:** tests pasan, suite completa limpia. **Step 5:** commit `feat: convertir sesiones del celular en registros del panel`.

### Task 4: RUTINEITOR: enviar las sesiones al importar el registro

**Files:**
- Create: `src/main/java/com/rutina/panel/EnvioRegistro.java`, `NumeracionMeso.java`
- Modify: `src/main/java/com/rutina/ui/PanelMesociclo.java` (método `importarRegistro`, y el acceso a la conexión), `ui/Contexto.java`, `ui/VentanaPrincipal.java`
- Test: `src/test/java/com/rutina/panel/EnvioRegistroTest.java`, `NumeracionMesoTest.java`

**Interfaces:**
- Consume: `ConexionPanel` (T2), `SesionAPanel` (T3), `ArmadorPlan.armar(perfilNombre, mesociclo, resultado, metodologia)`.
- Produces:
  - `NumeracionMeso.de(List<Mesociclo> delPerfil, Mesociclo m) -> int[]{macro, meso}`.
  - `record EnvioRegistro.Resumen(boolean panelActivo, int enviadas, int sinDuracion, int fueraDelPlan)` y `static Resumen enviar(ConexionPanel panel, long perfilId, boolean esPerfilDelPanel, PlanCelular plan, RegistroCelular registro, int macro, int meso, long ahoraMs)`: publica cada sesión convertible y devuelve las cuentas. Si `!esPerfilDelPanel` o el panel está inactivo no publica nada y devuelve `panelActivo=false`.
  - `Contexto.panel()` devuelve la `ConexionPanel` compartida; `VentanaPrincipal.iniciar()` la abre con `Database.rutaPorDefecto().getParent()` y la cierra junto con la base. `esPerfilDelPanel` = el perfil actual tiene el menor id de `PerfilRepo.listar()`.

- [ ] **Step 1:** tests: `numeracionCuentaPorAnioYOrdenDeInicio`, `numeracionDeOtroAnioReiniciaMeso`, `enviaLasSesionesConvertibles` (servidor falso), `cuentaSinDuracionYFueraDelPlan`, `reimportarDaLosMismosIdYPisaConLoMasNuevo`, `panelInactivoNoPublicaNada`, `otroPerfilNoPublicaNada`, `servidorCaidoDejaLasSesionesEnLaCola` (nada se pierde, nada lanza), `unaCasillaIlegibleNoImpideLasDemas`.
- [ ] **Step 2:** tests fallan. **Step 3:** implementar. En `importarRegistro`: después de `ctx.registros.guardar(r)` y sin cambiar nada de lo existente, si hay `resultado` calculado armar el plan con `ArmadorPlan.armar`, llamar a `EnvioRegistro.enviar` y, solo si `panelActivo`, mostrar un `JOptionPane` informativo: "Se mandaron N sesiones al panel." más ", M quedaron sin mandar por no tener duración" cuando corresponda (y, si el panel no responde, "se mandarán cuando vuelva la conexión"). Si no hay `resultado`, se omite el envío con el aviso "No se pudo armar el plan: no se mandó nada al panel." solo cuando el panel está activo. Cualquier excepción del envío se captura y no impide el guardado ni la recarga de la pestaña.
- [ ] **Step 4:** suite completa limpia. **Step 5:** commit `feat: mandar las sesiones al panel al importar el registro`.

### Task 5: RUTINEITOR: 1RM al panel

**Files:**
- Modify: `src/main/java/com/rutina/service/datos/EjercicioRepo.java`, `ui/Contexto.java`
- Create: `src/main/java/com/rutina/panel/RmAPanel.java`, `ClaveEjercicio.java`, `HistorialRm.java`
- Test: `src/test/java/com/rutina/panel/RmAPanelTest.java`, `ClaveEjercicioTest.java`, `HistorialRmTest.java`; ampliar los tests de `EjercicioRepo` si existen en `service/datos`.

**Interfaces:**
- Produces:
  - `EjercicioRepo.alRegistrarRm(Consumer<RmRegistrado> oyente)` y `record EjercicioRepo.RmRegistrado(long ejercicioId, long perfilId, String nombre, LocalDate fecha, double rm)`: se llama después de insertar en `historial_rm` (por `guardar` y por `actualizarRm`), nunca ante errores, y las excepciones del oyente no afectan al guardado.
  - `ClaveEjercicio.de(String nombre) -> String` (regla de las restricciones globales).
  - `RmAPanel.aRegistro(RmRegistrado, String dispositivo, long ahoraMs) -> Registro`.
  - `record HistorialRm.Envio(int enviados, List<String> advertencias)` y `HistorialRm.enviarTodo(ConexionPanel, EjercicioRepo, long perfilId, String dispositivo, long ahoraMs) -> Envio` (un registro por `(ejercicio, fecha)` con el último valor de ese día).
  - `Contexto` conecta el oyente al panel solo si el perfil es el del panel.

- [ ] **Step 1:** tests: `claveSinTildesNiEspacios` (`Press banca` → `press-banca`, `Sentadilla  trasera (barra)` → `sentadilla-trasera-barra`, `Dominadas` → `dominadas`), `claveDeSoloSignosEsVacia`; `elOyenteSeLlamaAlCambiarElRm`, `noSeLlamaSiElRmNoCambia`, `noSeLlamaSiElRmEsNulo`, `unaExcepcionDelOyenteNoRompeElGuardado`; `rmARegistroTieneIdMetricaYValor` (pasa `Registro.errores()`); `dosRmDelMismoDiaComparteId`; `enviarTodoMandaUnoPorEjercicioYFecha`; `dosEjerciciosConLaMismaClaveSeAvisan` (el envío agrega la clave repetida a `advertencias` y manda solo el del menor `ejercicioId`).
- [ ] **Step 2:** fallan. **Step 3:** implementar. **Step 4:** suite completa limpia. **Step 5:** commit `feat: mandar los 1RM al panel`.

### Task 6: RUTINEITOR: ventana "Conexión con el panel"

**Files:**
- Create: `src/main/java/com/rutina/ui/DialogoPanel.java`, `src/main/java/com/rutina/panel/ValidacionConexion.java`
- Modify: `ui/VentanaPrincipal.java` (menú "Panel"), `ui/Contexto.java` (poder reabrir la conexión)
- Test: `src/test/java/com/rutina/panel/ValidacionConexionTest.java`

**Interfaces:**
- Produces: `ValidacionConexion.errores(String url, String token) -> List<String>` (la URL debe empezar con `http://` o `https://`, tener host, y no terminar en `/api`; el token no puede estar en blanco ni tener espacios, `;`, `,`, `"` o `\`).
- Produces: menú "Panel → Conexión con el panel…" que abre `DialogoPanel`: URL, token (campo de contraseña), nombre del dispositivo, casilla "Enviar al panel", botones **Probar conexión** (llama `ConexionPanel.probar()` y muestra "Conectado", "Token inválido" o "No responde"), **Enviar historial de 1RM** (llama `HistorialRm.enviarTodo` y muestra la cantidad y las advertencias; deshabilitado si la conexión no está activa), **Guardar** y **Cancelar**. Debajo, una lista de solo lectura con las claves de 1RM del perfil (`1rm:press-banca`, …) para usarlas al crear metas en el panel. Al guardar: `guardarConfig`, cerrar la conexión vieja y abrir la nueva en `Contexto`.

- [ ] **Step 1:** tests de `ValidacionConexionTest`: URL sin esquema, URL vacía, URL con `/api` al final, URL válida con puerto, token en blanco, token con espacio, token con `;`, token válido.
- [ ] **Step 2:** fallan. **Step 3:** implementar validación y diálogo; los cuadros de aviso no muestran el token; el diálogo no bloquea el hilo de la interfaz mientras prueba o envía (usar `SwingWorker`).
- [ ] **Step 4:** verificación manual (la hace Franco, hay que ver la ventana): abrir el diálogo sin configuración; guardar con datos inválidos (errores); con el panel de demo (`scripts/demo.sh` del repo `Panel`) probar con token correcto, token malo y servidor apagado; enviar el historial de 1RM y ver las curvas en el panel; desmarcar "Enviar al panel" y confirmar que ya no se manda nada.
- [ ] **Step 5:** suite completa limpia; commit `feat: ventana de conexión con el panel`.

### Task 7: RUTINEITOR: documentación, empaquetado y prueba de extremo a extremo

**Files:**
- Modify: `README.md`, `LEEME.txt`
- Create: `docs/ejemplos/registro-ejemplo.json` y `docs/ejemplos/plan-ejemplo.json` (tomados de los fixtures de `JsonRegistroTest`/`JsonPlanTest`)

- [ ] **Step 1:** `README.md`: sección "Panel" (qué se manda, cuándo, cómo configurarlo, dónde queda `panel-cliente.properties` y `panel-cola.jsonl`, que sin configuración todo sigue igual, que solo el primer perfil se manda, cómo se arma la clave de 1RM). `LEEME.txt`: dos líneas para la versión de Windows.
- [ ] **Step 2:** `mvn -q package`; comprobar que el jar con dependencias contiene `panel/core/Registro.class`; revisar cómo se arma `RUTINEITOR-windows.zip` (script o pasos en el repo) y dejar anotado en el README cómo regenerarlo con la dependencia nueva. No regenerar ni mandar nada a nadie.
- [ ] **Step 3:** prueba de extremo a extremo con datos de ejemplo (no con los de Franco): levantar `scripts/demo.sh` del repo `Panel`; configurar RUTINEITOR con una carpeta de datos temporal (`XDG_DATA_HOME`) apuntando a `http://localhost:8099` y token `demo`; importar el registro de ejemplo; ver las sesiones en el calendario del panel y el detalle (planificado contra hecho, texto del esfuerzo por ejercicio) en el navegador; reimportar y confirmar que no hay duplicados; apagar el servidor, reimportar y confirmar que no hay errores y que al volver se entrega. Decir qué se vio y qué no.
- [ ] **Step 4:** commit `docs: conexión con el panel`.

---

## Revisión del plan frente al diseño
- **Cubierto:** datos de gimnasio del Diseño (macro·meso·micro·día, tiempo, cómo terminé, hecho contra planificado, esfuerzo por ejercicio) en T3 y T4; 1RM para las metas en T5; panel inactivo en otras PCs en T2; configuración por URL y token en T2 y T6; no importar historial viejo: reemplazado por la decisión de Franco de mandar todas las sesiones del archivo (T4).
- **Fuera de este plan:** mandar directo desde la app de Android; conectar PLANIFICADOR (plan 3) y running (plan 4); borrar en el panel una sesión que se quitó del archivo al reimportar; sueño.
- **Decisiones mías:** macro = año del mesociclo y meso = posición dentro de ese año; solo se manda el primer perfil; la clave de 1RM y su regla; ids estables de sesión y de 1RM; reglas del `aviso`; casilla ilegible se muestra tal cual en vez de abortar; `probar()` en `PanelClient` y `esfuerzoTexto` en el contrato; el diálogo ofrece enviar el historial de 1RM existente (acción explícita); la cola vive junto a `rutina.db`.
