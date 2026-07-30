# ObraTrack

[![CI](https://github.com/DevChristPhantom/OBRATRACK/actions/workflows/ci.yml/badge.svg)](https://github.com/DevChristPhantom/OBRATRACK/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](pom.xml)

Sistema de escritorio para gestionar la ejecución de obras: importa el presupuesto desde
un Excel de licitación, registra ingresos/egresos de materiales por partida, y muestra un
dashboard de presupuestado vs. ejecutado en tiempo real.

Pensado para usarse con **varias obras**: cada vez que cargas un Excel nuevo, creas una
obra nueva y el sistema importa sus partidas sin mezclarlas con las demás.

---

## Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 17 |
| UI Desktop | Swing + FlatLaf (tema oscuro moderno) |
| Base de datos | SQLite (archivo único `data/obratrack.db`, sin servidor) |
| Lectura de Excel | Apache POI (`.xlsx` y `.xls`) |
| Build | Maven (`pom.xml` incluido) |

---

## Cómo abrir el proyecto en IntelliJ IDEA

1. Abre IntelliJ → **File → Open...** → selecciona la carpeta `ObraTrack` (la que contiene `pom.xml`).
2. IntelliJ detecta el `pom.xml` automáticamente y te preguntará si quieres importarlo como proyecto Maven → **sí**.
3. Espera a que descargue las dependencias (SQLite JDBC, Apache POI, FlatLaf) — necesita internet la primera vez.
4. Abre `src/main/java/com/obratrack/Main.java`.
5. Click derecho → **Run 'Main.main()'**. Eso ya levanta la ventana de la aplicación.

Si IntelliJ marca el proyecto en rojo al abrirlo, ve a la pestaña **Maven** (lateral derecho) →
ícono de refrescar (🔄 *Reload All Maven Projects*).

---

## Build por línea de comandos

El proyecto incluye Maven Wrapper, así que no hace falta tener Maven instalado:

```bat
mvnw.cmd clean verify
```

Esto compila, corre los 36 tests unitarios (JUnit 5) y genera el reporte de cobertura JaCoCo en
`target/site/jacoco/index.html`. Para solo generar el jar ejecutable sin correr tests:

```bat
mvnw.cmd -DskipTests package
```

## Cómo generar el `.exe` (Windows)

Hay dos caminos. El recomendado es el primero porque no requiere instalar nada adicional
en las PCs donde luego se use el programa.

### Opción A — jar ejecutable + Launch4j (recomendada)

1. En IntelliJ, abre la pestaña **Maven** (lateral derecho) → `Lifecycle` → doble click en **package**.
   Esto genera `target/ObraTrack.jar`, que ya incluye TODAS las dependencias (gracias al plugin
   `maven-shade-plugin` que está configurado en el `pom.xml`).
2. Prueba que funciona: `java -jar target/ObraTrack.jar` (abre la app igual que el botón Run).
3. Descarga **Launch4j** (gratuito, https://launch4j.sourceforge.net) — convierte el `.jar` en `.exe`.
4. En Launch4j:
   - **Output file**: `ObraTrack.exe`
   - **Jar**: selecciona `target/ObraTrack.jar`
   - Pestaña **JRE** → Min JRE version: `17.0.0`
   - Click en el ícono de engranaje para generar el `.exe`.
5. El `ObraTrack.exe` resultante es portable: se copia a cualquier PC con Windows que tenga
   Java 17+ instalado (o puedes empaquetar un JRE embebido en la misma carpeta para que no
   dependa de tener Java instalado — Launch4j lo permite con la opción "Bundled JRE path").

### Opción B — jpackage (sin Launch4j, requiere JDK con módulos)

```bash
# Desde la carpeta del proyecto, después de "mvn package"
jpackage --input target/ --name ObraTrack --main-jar ObraTrack.jar ^
  --main-class com.obratrack.Main --type exe --win-shortcut --win-dir-chooser
```

`jpackage` viene incluido en el JDK 17 de Oracle/Adoptium para Windows.

---

## Flujo de uso

1. **Obras** → crea una obra nueva, ponle nombre y carga su Excel de presupuesto.
   El sistema detecta automáticamente las columnas (Item, Descripción, Unidad, Metrado, Precio, Total)
   sea cual sea el formato del Excel — no necesitas que tenga una estructura fija.
2. **Partidas** → revisa lo que se importó: partidas "padre" (agrupadoras, en gris, sin montos)
   y partidas "hoja" (las que se ejecutan, con presupuestado/ejecutado/% avance coloreado).
3. **Almacén** → pantalla de uso diario: eliges la obra activa (arriba a la derecha), seleccionas
   la partida, ingresas cantidad y costo real, y el sistema acumula todo automáticamente.
4. **Dashboard** → KPIs en tiempo real (presupuesto, ejecutado, diferencia, % avance) y alertas
   automáticas cuando una partida supera el 80% o el 100% de su presupuesto.
5. **Reportes** → exporta a Excel o PDF: el comparativo presupuesto vs ejecutado, el reporte
   diario (de una fecha) y el acumulado. Los archivos se guardan en la carpeta `exports/` y
   se abren automáticamente al generarse.

### Cargar varias obras

Cada obra es independiente. Para una obra nueva: **Obras → Nueva obra → cargas su propio Excel**.
El selector "Obra activa" en la barra superior cambia el contexto de todas las pantallas
(Partidas, Almacén, Dashboard) a la obra que elijas.

---

## Importación de Excel — cómo funciona la detección automática

Los presupuestos de obra en Perú no tienen un formato fijo (cada entidad/contratista usa el suyo).
`ExcelImporter` busca en las primeras 20 filas la que tenga más coincidencias de palabras clave
(Item, Descripción, Unidad, Metrado, Precio, Total, en español con o sin tildes) y a partir de
ahí mapea las columnas automáticamente. Las filas sin unidad se tratan como partidas "padre"
(agrupadoras tipo "01 OBRAS PRELIMINARES"); las que sí tienen unidad son partidas ejecutables.

Esta lógica fue **probada contra un presupuesto real tipo S10/Crystal Reports** (formato común
en licitaciones MEF/SEACE) de ~2000 filas, detectando correctamente 976 partidas reales,
290 agrupadoras y 686 ejecutables, omitiendo automáticamente 1010 filas vacías/separadoras.

---

## Estructura del proyecto

```
ObraTrack/
├── pom.xml
├── src/main/java/com/obratrack/
│   ├── Main.java                  # Punto de entrada
│   ├── core/Database.java         # Conexión SQLite + esquema
│   ├── model/                     # Obra, Partida, MovimientoAlmacen
│   ├── service/                   # ObraService, PartidaService, MovimientoService, ExcelImporter
│   └── ui/
│       ├── MainWindow.java        # Ventana principal + navegación
│       ├── Theme.java             # Colores y fuentes
│       └── views/                 # DashboardView, ObrasView, PartidasView, AlmacenView
├── data/                           # Aquí se crea obratrack.db (no se versiona)
└── exports/                       # Para reportes futuros
```

## Próximos pasos (no incluidos en este MVP)

- Reportes exportables a Excel/PDF (presupuesto vs ejecutado)
- Edición/eliminación de movimientos ya registrados
- Múltiples usuarios con roles (almacenero vs jefe de obra)
- Gráficos de avance en el dashboard
# OBRATRACK
