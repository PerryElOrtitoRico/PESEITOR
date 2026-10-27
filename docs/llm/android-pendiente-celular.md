# Android: qué probar en el celular

Lo que no se pudo verificar sin celular conectado (no hay emulador). `./gradlew test assembleDebug` está en verde; esto es lo que falta mirar con el APK instalado (`adb install -r android/app/build/outputs/apk/debug/app-debug.apk`).

## Tarea 4 — tema e ícono
- [ ] El ícono pixel art aparece en el lanzador y el nombre es **RUTINEITOR**.
- [ ] Modo claro: fondo `#F2F2F2`, barra `#E4E4E4`, bordes finos grises; la barra superior muestra el nombre y a la derecha los cuatro discos (rojo, amarillo, azul, verde) antes del menú de tres puntos.
- [ ] Menú de tres puntos → "Modo oscuro" (casilla): al tildarlo la pantalla pasa a los grises de Darcula (`#2B2D30`, barra `#3C3F41`), se recuerda al cerrar y abrir la app, y destildarlo vuelve al claro.
- [ ] Los íconos de la barra de estado y de navegación se ven en los dos modos (oscuros sobre el claro, claros sobre el oscuro).
- [ ] Comparar con las maquetas aprobadas: bordes de 1 dp, esquinas de 3 dp, nada de sombras ni chips.

## Tarea 5 — inicio con pestañas y archivos
- [ ] Pestañas **Sesión · Planes · Exportar** (gris, subrayado azul en la elegida, sin mayúsculas). Sin planes, Sesión dice "Cargá un plan en la pestaña Planes".
- [ ] Los desplegables (Plan, Microciclo, Día) tienen borde fino y flechita; la lista que se abre se ve bien en claro y en oscuro.
- [ ] **Cargar el plan de ejemplo por cable:** `adb push docs/registro-sesiones/ejemplos/plan-ejemplo.json /sdcard/Download/` y en Planes → "Cargar archivo" elegirlo: aparece en la lista (con ✓ de activo) y en Sesión.
- [ ] **Plan por WhatsApp sin tipo `.json` (Foco 1):** mandarse el archivo por WhatsApp (también renombrado sin extensión) y desde WhatsApp tocar Compartir → RUTINEITOR (o "Abrir con"): carga igual y muestra la pestaña Sesión con ese plan.
- [ ] Cargar dos veces el mismo plan: no se duplica y conserva lo anotado (Foco 4).
- [ ] Un archivo que no es plan ni registro (una foto, un PDF, un `.json` cualquiera): diálogo "No se pudo cargar" con el mensaje, sin cerrar la app.
- [ ] Cargar el registro exportado de la app web (`registro-….json`) después de cargar el plan: aparecen sus sesiones en la tabla Micro | Día | Anotada; sin plan cargado da el aviso "Primero cargá el plan de este registro…".
- [ ] "Borrar plan" pide confirmación con el mismo texto de la app web y borra también lo anotado.
- [ ] Exportar sin sesiones anotadas: aviso "Todavía no anotaste ninguna sesión". Con sesiones: "Compartir registro" abre el menú de Compartir (probar mandárselo por WhatsApp o guardarlo en Drive) y "Guardar en Descargas" abre el selector de carpeta con el nombre `registro-<perfil>-<inicio>.json`.
- [ ] El archivo exportado se abre en el programa de la PC con "Importar registro".

## Tarea 6 — pantalla de la sesión
Con el plan de ejemplo (Franco, mesociclo 3: Sentadilla con filas `70` y `80-85`, Bíceps con `4x12`):
- [ ] Sesión → Abrir sesión (microciclo 1, día 1): barra con flecha atrás, "Microciclo 1 · Día 1" y debajo "Sin anotar todavía". Descanso previo arriba (6 a 1, con "—" = sin anotar) y "Sensación al terminar" abajo.
- [ ] Cada ejercicio es un recuadro con encabezado (nombre + `11 / 11 reps`) y una **tabla con grilla** Zona | Planilla | Entrenado; la casilla tiene la pista como texto gris (hint). Planilla de Sentadilla `80-85`: `3.3.3 · 120/127,5`.
- [ ] **Teclado:** la casilla Entrenado muestra un teclado numérico donde se pueden escribir `.`, `,`, `-` y espacio (si el teclado del celular no los trae, avisar: habría que cambiar a teclado de texto).
- [ ] Escribir `3.2` → el encabezado pasa a `10 / 11 reps`. Escribir `x` → borde rojo y mensaje `serie inválida: "x" (reps de 1 a 99, kg con coma)` y el encabezado `— / 11 reps`. Escribir `3.3.3.3-50` (4 series) → aviso gris "Ojo: 4 series (planificadas: 3)…".
- [ ] Sensación del ejercicio (desplegable 1 a 5: Muy liviano … Al límite, Pesado) y Notas (máximo 500 caracteres).
- [ ] **Foco 3:** escribir `3.2`, `x` y una nota; girar el celular (queda todo) y salir con la flecha y volver a entrar (queda todo). Después, con la sesión abierta, cerrar la app desde recientes y abrirla de nuevo: lo escrito sigue.
- [ ] Solo abrir una sesión y volver NO la crea: en la pestaña Sesión la tabla sigue en "0 de 4".
- [ ] La línea de la barra pasa a "Anotada el dd/mm/aaaa" al escribir lo primero. La tabla de la pestaña Sesión muestra Micro | Día | Anotada (`dd/MM`).
- [ ] Día sin ejercicios (microciclo 1, día 2 del ejemplo): "Este día no tiene ejercicios".
- [ ] "Borrar lo anotado en esta sesión": pide confirmación y deja la pantalla vacía; sin nada anotado avisa "Esta sesión no tiene nada anotado."
- [ ] Modo oscuro: los recuadros, la grilla y los bordes rojos se ven bien en los dos modos.
