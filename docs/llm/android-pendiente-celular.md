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
