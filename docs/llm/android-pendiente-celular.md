# Android: qué probar en el celular

Lo que no se pudo verificar sin celular conectado (no hay emulador). `./gradlew test assembleDebug` está en verde; esto es lo que falta mirar con el APK instalado (`adb install -r android/app/build/outputs/apk/debug/app-debug.apk`).

## Tarea 4 — tema e ícono
- [ ] El ícono pixel art aparece en el lanzador y el nombre es **RUTINEITOR**.
- [ ] Modo claro: fondo `#F2F2F2`, barra `#E4E4E4`, bordes finos grises; la barra superior muestra el nombre y a la derecha los cuatro discos (rojo, amarillo, azul, verde) antes del menú de tres puntos.
- [ ] Menú de tres puntos → "Modo oscuro" (casilla): al tildarlo la pantalla pasa a los grises de Darcula (`#2B2D30`, barra `#3C3F41`), se recuerda al cerrar y abrir la app, y destildarlo vuelve al claro.
- [ ] Los íconos de la barra de estado y de navegación se ven en los dos modos (oscuros sobre el claro, claros sobre el oscuro).
- [ ] Comparar con las maquetas aprobadas: bordes de 1 dp, esquinas de 3 dp, nada de sombras ni chips.
