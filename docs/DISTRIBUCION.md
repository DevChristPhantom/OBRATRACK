# ObraTrack — Distribución (instalador de Windows)

ObraTrack se empaqueta como una aplicación de escritorio nativa de Windows usando
**jpackage** (incluido en el JDK 17+). El instalador crea accesos directos en el menú Inicio y
en el escritorio, e incluye un runtime de Java propio, así que **el usuario final no necesita
tener Java instalado**.

## Requisitos (solo en la máquina que arma el instalador)

- **JDK 17 o superior** en el `PATH` (aporta `jpackage`).
- **Maven** (o el Maven embebido de IntelliJ) para compilar el `.jar`.
- Solo para generar `.exe`/`.msi`: **WiX Toolset 3.x** instalado
  (https://github.com/wixtoolset/wix3/releases). Si no lo tienes, usa la versión portable.

## Opción A — Instalador .exe (recomendada)

```bat
packaging\build-installer.bat
```

Genera `dist-installer\ObraTrack-2.0.0.exe`. Al ejecutarlo, instala la app, crea el acceso
directo y permite elegir carpeta de instalación.

## Opción B — Portable (sin WiX)

```bat
packaging\build-portable.bat
```

Genera `dist-portable\ObraTrack\ObraTrack.exe`. Esa carpeta es autocontenida: se puede copiar
tal cual (por ejemplo a un USB) y ejecutar `ObraTrack.exe`.

## Dónde guarda los datos la app

Para que funcione correctamente instalada en `C:\Program Files\...` (carpeta de solo lectura),
la app **no** guarda datos junto al ejecutable. Usa una carpeta escribible del usuario:

- **Instalada:** `%APPDATA%\ObraTrack\` (por ejemplo `data\obratrack.db`, `logs\`,
  `data\backups\`, `exports\`).
- **En desarrollo** (ejecutando desde el proyecto): la carpeta del proyecto, para no mover la
  base de datos existente.
- Se puede forzar la ubicación con `-Dobratrack.home=RUTA` (parámetro de la JVM).

El primer arranque crea el usuario administrador por defecto: **admin / admin123** (cámbialo
desde la pantalla Usuarios).

## Icono

El icono de la app está en `packaging\obratrack.ico` (multi-tamaño) y también embebido como
`src/main/resources/img/appicon.png` para la ventana y la barra de tareas.

## Alternativa: Launch4j

Si prefieres un solo `.exe` liviano que use el Java del sistema (en vez de empaquetar el
runtime), puedes envolver `target\ObraTrack.jar` con Launch4j apuntando a la clase
`com.obratrack.Main` y al icono `packaging\obratrack.ico`.
