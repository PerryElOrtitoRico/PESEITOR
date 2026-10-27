# RUTINEITOR para Android — diseño

Fecha: 27/10/2026. Origen: idea 2 de `Agregar.md` (vault de Obsidian, `Trabajo/Proyectos/Programa de rutina de gimnasio/`) y la conversación de diseño de ese día.

## Por qué
La app del celular hoy es una PWA (`celular/`) publicada en GitHub Pages. Funciona bien; el problema es la **dependencia**: necesita Chrome y la página publicada. El usuario quiere que sea **suya**, como el RUTINEITOR de escritorio y el Planificador: una app que se abra y funcione aunque mañana desaparezcan Google, Chrome o GitHub. Además quiere que se vea como el RUTINEITOR de escritorio (estilo "rústico"), no como una app moderna genérica.

## Objetivos
- App nativa de Android en **Java + Material Components (vistas XML)**, que hace **lo mismo que la PWA actual**, con el mismo comportamiento:
  - cargar uno o varios planes (`.json` "rutineitor-plan", versión 1) y elegir el activo;
  - elegir microciclo y día, abrir la sesión;
  - descanso previo (6 niveles), sensación por ejercicio (5: Muy liviano, Liviano, Justo, Al límite, Pesado), sensación final (5) — textos exactos en `docs/registro-sesiones/00-especificacion.md` sección 2;
  - casillas con la notación de lo realizado (`3.3-85.3-80`, `0`, vacía = como la planilla), con la pista gris y la validación en vivo (borde y mensaje de error, aviso "Ojo: N series");
  - total de repeticiones por ejercicio, notas por ejercicio (máx. 500), cronómetro de la sesión con Iniciar/Pausar/Reanudar y "Editar duración";
  - borrar lo anotado en una sesión, borrar un plan;
  - exportar el registro (`.json` "rutineitor-registro", versión 1, con `nota` y `duracionMin`).
  La referencia de comportamiento es la PWA: `celular/index.html` + `celular/logica.js`, el contrato `docs/llm/00-especificacion.md` (secciones 2, 5–8) y `docs/registro-sesiones/00-especificacion.md`. Las pruebas `docs/registro-sesiones/evaluacion/web/pruebas.html` y `docs/llm/evaluacion/web/pruebas-mejoras.html` son los casos que la lógica Java tiene que cumplir igual.
- **Mismo formato de archivos** de plan y registro: el programa de PC no cambia.
- **Independiente**: instalada como APK desde la PC (por cable, `adb install`), sin Play Store, **sin Google Play Services, Firebase ni cuentas**, sin red. Todos los datos en el celular.
- **Estilo del escritorio** (aprobado con maquetas):
  - grises claros tipo FlatLaf Light, bordes finos de 1 px, esquinas casi rectas (≈3 dp), botones planos;
  - barra superior con el nombre RUTINEITOR y los cuatro discos del ícono (rojo, amarillo, azul, verde); el ícono de la app es el pixel art `docs/icono/rutineitor-512.png`;
  - pestañas arriba: **Sesión** · **Planes** · **Exportar**;
  - Inicio/Sesión: desplegables Plan, Microciclo y Día, botón "Abrir sesión" y una tabla de sesiones anotadas (Micro, Día, Anotada);
  - pantalla de la sesión: barra con flecha atrás, "Microciclo N · Día M" y el cronómetro; botones Pausar/Editar duración debajo; cada ejercicio es un recuadro con encabezado (nombre + "33 / 36 reps") y una **tabla con grilla** Zona | Planilla | Entrenado (la casilla editable en Entrenado, con la pista gris), debajo "Sensación" como desplegable y el campo Notas;
  - color solo en los discos y en el azul de la casilla con foco; nada de chips, tarjetas flotantes ni animaciones llamativas.
- **Modo oscuro** con un **interruptor dentro de la app** (Claro/Oscuro), que se recuerda. El oscuro usa los grises de FlatLaf Darcula con los mismos acentos.
- **Archivos de entrada y salida, por cualquier camino**:
  - cargar plan: desde el menú **Compartir** y **"Abrir con"** de Android (WhatsApp, Drive, administrador de archivos) y desde la app con el selector de archivos del sistema (sirve para un archivo copiado por cable a Descargas);
  - exportar registro: **Compartir** (cualquier app) y **Guardar en Descargas** (para llevarlo por cable).
- **Cronómetro con la pantalla apagada**: mientras corre, una notificación muestra el tiempo; al volver a la app sigue en el valor correcto (se calcula con la hora del sistema, como en la PWA).

## No-objetivos
- Funciones nuevas más allá de la PWA (la PWA "funciona excelente"; el cambio es de independencia y estética).
- Publicar en Play Store o en cualquier tienda; firmar para distribución.
- Sincronización automática o servidor (los archivos viajan a mano).
- Cambios en el programa de PC o en el formato de los `.json`.

## Anti-objetivos (sería un fracaso aunque ande)
- Que dependa de algo de Google o de internet para abrirse o funcionar.
- Que se vea como una app moderna "de catálogo" (Material 3 con chips, tarjetas redondeadas, colores tonales) en vez de como el RUTINEITOR de escritorio.
- Que se pierdan registros: al migrar de la PWA o porque el sistema borre datos.

## Restricciones
- Herramientas: **solo las herramientas de línea de comandos del SDK de Android** (sin Android Studio), compilando con Gradle desde la terminal. JDK 21 instalado. El usuario activa la depuración USB en su celular.
- Código: Java; el modelo local (qwen3.5-9b / Gemma en LM Studio) escribe el código con el método de `~/Escritorio/Hobbies/programacion/Proyectos/qwen-aprendizajes.md` (Claude planifica: esqueletos, tareas, tests ocultos; un subagente Sonnet lleva el ciclo). La lógica tiene que quedar separada de las Activities para poder testearla con JUnit sin emulador.
- Dependencias: solo AndroidX/Material Components y Gson (ya usado en la PC). Cualquier otra, consultarla.
- Convenciones del usuario: `~/Escritorio/Hobbies/programacion/Proyectos/CLAUDE.md` (español, Conventional Commits, nunca push sin pedirlo).
- Vive en el mismo repo, en una carpeta `android/` (proyecto Gradle propio, al lado del Maven de la PC).

## Retiro de la PWA (cuando la app ande)
1. El usuario exporta desde la PWA lo anotado y lo importa en la PC (o en la app nativa: ver "a cargo del constructor").
2. Se deja de publicar `gh-pages` (lo hace o autoriza el usuario: es un cambio en GitHub).
3. `celular/` sale del repo (queda en la historia de git) y el README y la nota `Cómo funciona.md` pasan a explicar la app nativa.

## Decisiones de Claude (no las eligió el usuario; puede cambiarlas)
- **Datos guardados como archivos JSON** en el almacenamiento interno de la app (planes y registros en el mismo formato que se exporta, escritura atómica), en vez de una base SQLite: el formato ya existe y es lo que se importa/exporta. "Base local" se cumple igual: no depende de Chrome.
- **Notificación del cronómetro** con `setUsesChronometer` (el sistema la actualiza solo), sin servicio en primer plano.
- **minSdk 26** (Android 8) si el celular del usuario lo permite.
- **Reutilizar la lógica Java de la PC** que no depende de Swing ni SQLite (`com.rutina.registro`: `NotacionRegistro`, `JsonRegistro`, records del plan y del registro, escalas) en vez de reescribirla: idealmente compartida sin copiar archivos; si no es práctico, copiada con tests que garanticen que dan lo mismo.

## A cargo del constructor
- Estructura de Activities/Fragments, nombre del paquete Android, versión de Gradle/AGP.
- Cómo compartir las clases con el proyecto Maven (source set apuntando a `src/main/java` filtrado, módulo común, o copia verificada).
- Si la app nativa también puede importar el registro guardado en la PWA (un `.json` de registro exportado) para continuar con lo ya anotado.
- Cómo se ve exactamente el interruptor de modo oscuro (barra superior u opciones), siempre dentro del estilo descrito.
- Orden de las tareas y cuáles van al modelo local.
