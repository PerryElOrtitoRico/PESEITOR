# RUTINEITOR

Programa de escritorio para programar mesociclos de sobrecarga con el **método cubano de distribución de repeticiones** (H. Anselmi, *Actualizaciones sobre el entrenamiento de la potencia*, cap. 8). Reemplaza la planilla `Rutina.xlsx`: a partir del volumen y la intensidad que planificás para cada mesociclo, calcula solo las series × repeticiones × kg de cada ejercicio, con series parejas (escalera de peso) y contadores que muestran cuánto se aparta de la teoría. Exporta un PDF con el formato de la planilla del entrenador para llevar al gimnasio.

## Qué tiene

- **Ejercicios:** potencia, fuerza, hipertrofia y pliometría. Cada ejercicio tiene una ficha con su 1RM, el objetivo, el techo de intensidad, las repeticiones por serie propias de cada zona y un gráfico de cómo evoluciona su 1RM.
- **Plantillas:** las tablas del método (microciclos, días, distribución por intensidad, repeticiones por serie, plantillas de fuerza básica e hipertrofia, volúmenes de pliometría). Son editables, con un botón para restaurar los valores originales.
- **Macrociclo:** calendario del año con los mesociclos y los planes de pliometría, más el gráfico anual de volumen e intensidad.
- **Mesociclo:** la grilla de días y ejercicios, con las series que se calculan solas, los contadores, el gráfico teoría vs planilla y el botón **Exportar PDF**.
- **Perfiles:** cada persona tiene sus propios ejercicios, 1RM y mesociclos. Las plantillas son compartidas.
- **Progreso y Registro:** lo que entrenaste de verdad (anotado en el celular) comparado con la planilla.

## Registro desde el celular

La app del celular está en <https://perryelortitorico.github.io/RUTINEITOR/> (código en `celular/`). No necesita cuenta ni conexión una vez abierta.

1. En la PC, pestaña **Mesociclo → Plan al celular**: guarda un `.json` con el plan. Pasalo al celular (WhatsApp a vos mismo, Drive, cable).
2. En el celular, abrí el link en Chrome y, en el menú ⋮, **Agregar a pantalla principal** (queda como app).
3. En la app: **Cargar plan** → elegí el `.json`. Después elegís microciclo y día y anotás la sesión; se guarda sola.
4. Al terminar el mesociclo (o cuando quieras), **Exportar registro** y pasá ese `.json` a la PC.
5. En la PC, **Mesociclo → Importar registro**. La pestaña **Registro** muestra Planilla vs Entrenado.

## Requisitos

- Java 21
- Maven 3.9 (solo para compilar)

## Compilar, ejecutar y testear

```bash
mvn package
```

```bash
java -jar target/rutineitor.jar
```

```bash
mvn test
```

## Dónde guarda los datos

En `~/.local/share/rutina/rutina.db` (o `$XDG_DATA_HOME/rutina/rutina.db` si esa variable está definida). Es un único archivo SQLite: para hacer una copia de seguridad alcanza con copiarlo. Si el archivo se daña, el programa lo renombra a `rutina.db.bak-<fecha>` y empieza uno nuevo; nunca lo pisa.

Los PDF se guardan donde elijas al exportar (por defecto, en `~/Documentos`).

## Cómo calcula

```mermaid
flowchart LR
    A[Macrociclo:<br>semanas, volumen e IMR<br>de potencia y fuerza] --> B[Mesociclo:<br>variante A/B/C, días,<br>ejercicios por día]
    B --> C[Volumen por microciclo<br>y por día]
    C --> D[Reps por zona<br>tabla cubana]
    D --> E[Reparto entre ejercicios<br>% de la bolsa y techo]
    E --> F[Series de tamaño fijo<br>con arrastre entre zonas]
    F --> G[PDF + contadores + gráficos]
    B --> H[Fuerza básica e hipertrofia:<br>plantillas]
    H --> G
    I[Plan de pliometría<br>nivel, 4 semanas] --> G
```

El detalle de cada regla está en [`docs/2026-10-21-generador-rutinas-design.md`](docs/2026-10-21-generador-rutinas-design.md).

## Estructura

- `com.rutina.model`: datos (ejercicio, mesociclo, metodología…).
- `com.rutina.service.calculo`: el motor de cálculo, sin interfaz ni base de datos, con tests.
- `com.rutina.service.datos`: SQLite y los datos iniciales de las planillas.
- `com.rutina.pdf`: el exportador.
- `com.rutina.ui`: la interfaz Swing.
