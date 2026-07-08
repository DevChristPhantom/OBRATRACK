# Changelog — ObraTrack

## [2.1.0] - 2026-07-05

### UX — carga asíncrona del Dashboard
- El **Dashboard ahora carga sus datos en segundo plano** (`SwingWorker`): al abrir la vista o
  cambiar de obra ya no se congela la ventana mientras se consultan partidas, movimientos y
  ejecución acumulada. Se muestra un **spinner con velo** ("Cargando datos de la obra…") y la
  interfaz vuelve a responder de inmediato, como en las apps de escritorio modernas.
- Las consultas de fondo se **serializan** (una sola conexión SQLite) y los resultados obsoletos
  se descartan si el usuario cambia rápidamente de obra, evitando parpadeos y datos cruzados.

## [2.0.0] - 2026-07-02

### Estándar profesional — Pilar 5: UX profesional
- **Pantalla de Ajustes:** cambiar la propia contraseña (verificando la actual), **crear un
  respaldo al instante**, y abrir las carpetas de datos, respaldos, exportaciones y logs.
- **Atajos de teclado:** Ctrl+1..6 para cambiar de vista, F1 abre "Acerca de", Ctrl+L cierra
  sesión y Ctrl+Q sale de la app.
- **Barra de estado** inferior con la versión, el usuario/rol y la obra activa.
- "Acerca de" también disponible desde el menú lateral y con F1.

### Estándar profesional — Pilar 4: distribución como app de Windows
- **Instalador nativo con jpackage:** `packaging/build-installer.bat` genera un `.exe` que
  instala la app con su propio runtime de Java (el usuario final no necesita tener Java),
  crea accesos directos en el menú Inicio y el escritorio. `build-portable.bat` genera una
  versión portable (carpeta con `ObraTrack.exe`, sin necesidad de WiX).
- **Icono de la aplicación** (`packaging/obratrack.ico` + `appicon.png`): se ve en la ventana,
  la barra de tareas y el ejecutable instalado.
- **Ventana "Acerca de"** (`AcercaDe`) con logo, versión y datos de Grupo Titan, accesible desde
  el menú lateral.
- **Datos en carpeta escribible** (`Rutas`): instalada bajo "Archivos de programa" (solo
  lectura), la app guarda base de datos, logs, respaldos y exportaciones en
  `%APPDATA%\ObraTrack`; en desarrollo sigue usando la carpeta del proyecto (no mueve tu base
  actual). Configurable con `-Dobratrack.home`.
- Documentación en `docs/DISTRIBUCION.md`.

### Estándar profesional de escritorio — Pilar 1: fiabilidad y observabilidad
- **Logging a archivo** (`AppLog`, java.util.logging, sin dependencias): registra a consola y a
  un archivo rotativo en `logs/obratrack.*.log` (5 archivos de ~2 MB), formato de una línea.
- **Manejo global de errores** (`ManejadorErrores`): captura excepciones no controladas —
  incluidas las del hilo de UI (EDT)— las registra en el log y muestra un diálogo claro, en
  vez de que la app se cierre en silencio.
- **Respaldo automático de la base** (`RespaldoDB`): en cada arranque genera una copia
  consistente con `VACUUM INTO` en `data/backups/` (compatible con WAL) y conserva los últimos
  10 respaldos.
- **Registro de accesos:** los intentos de login (correctos y fallidos) quedan en el log, útil
  para auditoría de seguridad. Nunca se registra la contraseña.
- **Versión de la app** (`AppInfo` + `version.properties`) disponible en código para el "Acerca
  de" y el empaquetado. Marca el salto a **2.0.0**.
- `.gitignore` actualizado para excluir `logs/`, `data/backups/` y los archivos WAL.

### Estándar profesional — Pilar 3: seguridad y control de usuarios
- **Pantalla de gestión de usuarios** (`UsuariosView`, solo visible para ADMIN): crear usuarios,
  editar (nombre, rol, activo), activar/desactivar y **resetear contraseña**.
- **Permisos por rol** (`Permisos`): ADMIN administra todo; ADMIN/JEFE_OBRA gestionan obras;
  ALMACENERO registra almacén y consulta. La opción "Usuarios" del menú solo aparece para ADMIN,
  y los botones de crear/editar/eliminar obra se desactivan si el rol no tiene permiso.
- **Bloqueo por intentos fallidos:** tras 5 intentos, la cuenta se bloquea 5 minutos; el login
  muestra el tiempo restante. Evita ataques de fuerza bruta al inicio de sesión.
- **Protección del último admin:** no se puede desactivar ni quitar el rol al último
  administrador activo.
- `UsuarioService`: nuevos `actualizar`, `contarAdminsActivos`, `estaBloqueado`,
  `segundosBloqueoRestantes`.
- Nuevos tests de bloqueo y de actualización de usuarios.

### Estándar profesional — Pilar 2: calidad, métricas y CI
- **Cobertura de código con JaCoCo**: `mvn clean verify` genera el reporte en
  `target/site/jacoco/index.html`.
- **Análisis estático**: Checkstyle (estilo Google) y SpotBugs configurados como pasos
  opcionales (`mvn checkstyle:check`, `mvn spotbugs:check`) que no rompen el build normal.
- **Integración continua** (GitHub Actions, `.github/workflows/ci.yml`): compila, corre los
  tests con cobertura y publica los reportes en cada push/PR.
- **Más tests**: `ObraServiceTest` valida crear/actualizar/eliminar y el borrado en cascada de
  partidas y movimientos. Total del proyecto: **27 tests**.
- **Documentación de arquitectura y buenas prácticas** en `docs/ARQUITECTURA.md` (capas,
  patrones, seguridad, rendimiento, cómo compilar/probar/empaquetar, convenciones).
- Versión del `pom.xml` alineada a **2.0.0**.

## [1.9.2] - 2026-07-02

### Login → panel mas rapido y fluido
- **Verificacion de credenciales en segundo plano** (SwingWorker): el hasheo PBKDF2 ya no
  congela la ventana de login. El boton muestra "Verificando..." y se desactiva mientras
  comprueba, evitando doble envio.
- **El panel aparece al instante:** la primera carga de datos del Dashboard se difiere un
  tick (via `invokeLater`) para que la ventana principal se pinte de inmediato tras el login
  y el contenido entre justo despues (sensacion de app moderna).
- Se evita cualquier refresco de vista durante la construccion inicial (flag `listo`), de modo
  que abrir la app hace una sola carga en vez de varias.

## [1.9.1] - 2026-07-02

### Editar y eliminar obras
- En la lista "Obras registradas" ahora se puede **seleccionar una obra** y usar los botones
  **Editar** y **Eliminar**.
- **Editar:** dialogo para cambiar nombre, descripcion, fecha de inicio, fecha fin estimada y
  estado (ACTIVA/PAUSADA/FINALIZADA), con validacion de fechas.
- **Eliminar:** con confirmacion; borra la obra y **en cascada** sus partidas y movimientos de
  almacen (FK ON DELETE CASCADE) y limpia su historial de auditoria.
- `ObraService`: nuevos `actualizar(Obra)` y `eliminar(obraId)`.
- El selector de obra activa (barra superior) se recarga automaticamente tras editar/eliminar.

## [1.9.0] - 2026-07-02

### Rendimiento / fluidez (respuesta como apps actuales)
- **Solo se refresca la vista visible.** Antes, cada clic en el menu o cambio de obra
  refrescaba las 5 vistas (5x consultas a la base por accion), lo que hacia lenta la
  navegacion. Ahora `MainWindow` refresca unicamente la pantalla actual; las demas se
  actualizan de forma perezosa al mostrarse. Es la mejora mas notoria entre botones.
- **Cache de iconos SVG:** `Icons` cachea cada icono por (nombre, tamano, color); el SVG se
  parsea una sola vez y se reutiliza, agilizando el repintado del sidebar y las tarjetas.
- **SQLite mas rapido:** se activaron `journal_mode=WAL`, `synchronous=NORMAL`,
  `temp_store=MEMORY` y ~8 MB de cache de paginas — escrituras mas rapidas y lecturas sin
  bloqueo.
- **Almacen:** el combo de partidas (cientos de items) solo se reconstruye cuando cambia la
  obra, no cada vez que se abre la pantalla.

## [1.8.1] - 2026-07-02

### Login mas fiel al diseno de referencia
- **Panel izquierdo claro** (tipo tarjeta blanca) con avatar circular azul, titulo
  "¡Bienvenido de nuevo!", campos redondeados con **icono dentro** (usuario con icono de
  persona, contrasena con candado) y **placeholder**, fila "Recordarme / ¿Olvidaste tu
  contrasena?", boton azul "Iniciar sesion" y pie "Tu informacion esta protegida". Estilo de
  campos redondeados via FlatLaf (leadingIcon, trailingComponent, placeholderText).
- **Panel derecho** con la **foto de obra** (`fondoobra.jpg`) de fondo y capa azul oscura,
  logo de Grupo Titan G&L, tagline y los 4 modulos en recuadros redondeados.
- El campo de contrasena usa el **icono de ojo** (`eye.svg`) para mostrar/ocultar (se tinta de
  azul al mostrar). Nuevo icono `lock.svg` para el candado.
- Imagenes/iconos empaquetados en `src/main/resources/img/` e `.../icons/`.
- Nota: los botones Windows/Google y el toggle "Modo oscuro" del mockup no se implementaron
  (OAuth externo / cambio de tema global); el acceso es por usuario y contrasena.

## [1.8.0] - 2026-07-02

### Control de usuarios / login
- **Pantalla de inicio de sesion** (`LoginView`): panel partido con el formulario a la
  izquierda (usuario, contrasena con ver/ocultar, "Recordarme") y la marca a la derecha con
  el logo de Grupo Titan G&L y los cuatro modulos del sistema. La app ahora arranca en el
  login y solo abre la ventana principal tras autenticar.
- **Contrasenas cifradas** (`PasswordUtil`): hashing PBKDF2WithHmacSHA256 con salt aleatorio
  y 120 000 iteraciones (incluido en el JDK, sin dependencias). Nunca se guarda la clave en
  claro; la verificacion es en tiempo constante.
- **Tabla `usuario`** en la base (username unico, nombre, hash, rol, activo, creado_en) y
  **`UsuarioService`** (crear, autenticar, listar, cambiar contrasena). En el primer arranque
  se crea automaticamente el administrador por defecto: **usuario `admin`, contrasena
  `admin123`** (conviene cambiarla).
- **Sesion en la auditoria:** el historial de Almacen ahora registra el **usuario que
  inicio sesion** (via `SesionActual`) en cada creacion/edicion/eliminacion, en vez de un
  nombre fijo.
- **Sidebar:** la tarjeta de usuario muestra el nombre y rol reales; se agrego **"Cerrar
  sesion"** (vuelve al login) junto a "Salir".
- El logo se empaqueta en `src/main/resources/img/logoTitan.png` (classpath, va dentro del jar).
- Tests: `PasswordUtilTest` (4) y `UsuarioServiceTest` (3) — total del proyecto ahora 25 tests.

Nota: los botones de inicio con Windows/Google del mockup no se implementaron (requieren
OAuth/servicios externos); el login real es por usuario y contrasena.

## [1.7.1] - 2026-07-02

### Verificación integral (test estático + seguridad)
- Cross-check completo: todas las llamadas de la UI corresponden a métodos existentes de los
  servicios; todas las constantes de `Theme` y los iconos referenciados existen; balance de
  llaves correcto en los 29 archivos `.java`.
- Corregido: se detectaron y eliminaron bytes NUL (``) que se habían colado en
  `ObrasView`, `ComparativoView` y `ReportesView` (habrían impedido compilar). La constante
  centinela de selección de hoja pasó a `"__CANCELADA__"`.
- Revisión de seguridad: todo el SQL con datos usa `PreparedStatement` con placeholders; la
  única concatenación SQL (migración de columnas) usa identificadores literales, no input de
  usuario; sin accesos de red, procesos externos, reflexión ni secretos embebidos.
- Se limpiaron los últimos glifos (✓ / líneas) de mensajes que podían verse como cuadros.
- Revalidada la lógica clave: importador (total = Costo Directo S/. 3,865,615.16), semáforo
  de salud (6/6 escenarios), comparativo temporal y auditoría crear/editar/eliminar.

## [1.7.0] - 2026-07-02

### Almacén — editar, eliminar e historial de auditoría (anti-fraude)
- Los movimientos de almacén ahora se pueden **editar** y **eliminar** desde la tabla
  (botones bajo el listado). Al editar, el formulario entra en "modo edición" y el botón
  pasa a "Guardar cambios".
- **Auditoría con fecha y hora:** cada movimiento guarda `creado_en` y `actualizado_en`, y
  toda creación / edición / eliminación queda registrada en una tabla `movimiento_auditoria`
  (append-only) con acción, detalle del cambio, usuario y timestamp — para control anti-fraude.
- **Historial de cambios:** nuevo botón que abre un diálogo con todo el registro de auditoría
  de la obra (del más reciente al más antiguo), incluyendo qué cambió en cada edición.
- `MovimientoService`: nuevos `actualizar`, `obtenerPorId`, `listarAuditoria` y registro
  automático de auditoría en `registrar` / `actualizar` / `eliminar`; el detalle de edición
  lista los campos que cambiaron (fecha, tipo, cantidad, costo, nota).
- **Migración segura:** `Database` agrega las columnas de auditoría a bases de datos ya
  existentes (vía `PRAGMA table_info` + `ALTER TABLE`) sin perder los datos actuales.
- Se reemplazaron los glifos que salían como cuadros (✓) en botones y mensajes por iconos SVG.
- Validado en SQLite (Python): migración de una BD antigua + crear/editar/eliminar generan
  los tres registros de auditoría con timestamp.

## [1.6.0] - 2026-07-01

### Semáforo de salud de la obra (ritmo de gasto) + gráfico de línea
- Nueva clase `IndicadorSalud`: evalúa la salud de la obra con una lógica de **ritmo**, no de
  simple % consumido. Combina dos señales y toma la más severa:
  - **A) Ritmo gasto vs. tiempo:** desvío = %presupuesto_gastado − %tiempo_transcurrido
    (según fecha de inicio y fin estimada de la obra).
  - **B) Sobregiro de partidas:** cuánto se pasaron las partidas que superaron su tope, como
    % del presupuesto total.
  - Umbrales aprobados: 🟢 verde (desvío ≤ +5 pts, sin sobregiros), 🟡 ámbar (+5 a +15 pts, o
    ≥85% gastado, o alguna partida sobregirada), 🔴 rojo (ejecutado ≥ presupuesto total, o
    desvío ≥ +15 pts, o sobregiro ≥ 5%). Sin fecha fin usa "modo sin fecha" (solo señal B +
    % gastado) y lo avisa.
- **Banner de salud** en el Dashboard: color, título ("En control" / "Requiere atención" /
  "Fuera de control") y una frase con los números reales.
- **Formulario Nueva obra:** se agregaron los campos "Fecha de inicio" y "Fecha fin estimada"
  (con validación), necesarios para el análisis de ritmo. `ObraService.crear` ya los persiste.
- **Gráfico de línea** en el Dashboard: ejecución acumulada (por mes, desde `resumenPorPeriodo`)
  vs. la línea guía de ritmo esperado. Los gráficos ahora se muestran en fila de tres
  (distribución, ejecución acumulada, presupuesto vs ejecutado).
- Tests JUnit `IndicadorSaludTest` (6 casos). Lógica validada además en Python contra los
  nueve escenarios de referencia.

## [1.5.0] - 2026-07-01

### UI/UX — Adaptabilidad, legibilidad, gráficos y botón salir
- **Ventana adaptable:** la app ahora abre maximizada (`MAXIMIZED_BOTH`) para ajustarse al
  tamaño de la pantalla; se conserva un tamaño de restauración de 1280x800 y un mínimo de
  1024x640. Los gráficos se redibujan según el ancho/alto disponible.
- **Montos legibles:** las tarjetas KPI auto-ajustan el tamaño de la fuente del monto según
  su longitud, para que ya no se corten (antes se veía "S/. 3,63…").
- **Glifos corregidos:** se reemplazaron los caracteres que salían como cuadros (✓/⚠/⚡) por
  texto legible ("Sin alertas…", "SUPERADO", "POR AGOTARSE").
- **Botón Salir** en el sidebar (con icono y confirmación) que cierra la aplicación.
- **Dos gráficos reales en el Dashboard** (dibujados con Java2D, sin librerías externas, a
  partir de los datos de la obra):
  - *Dona de distribución del presupuesto por grupo* (agrupa las partidas por su código de
    nivel 1 —ESTRUCTURAS, ARQUITECTURA, etc.— con leyenda, montos y %).
  - *Barras Presupuesto vs Ejecutado por grupo*, con el avance coloreado verde/amarillo/rojo.
  - Validado contra el consolidado real: 7 grupos que suman exactamente el Costo Directo
    (S/. 3,865,615.16); ESTRUCTURAS 37.8%, ARQUITECTURA 32.8%, etc.

## [1.4.0] - 2026-07-01

### UI/UX — Iconografía y rediseño del sidebar
- Integrados los 17 iconos SVG (Material Symbols) que aportó el usuario. Se copiaron a
  `src/main/resources/icons/` con nombres limpios (home, obras, partidas, almacen,
  comparativo, reportes, add, account, download, search, money, etc.) para que se
  empaqueten dentro del `.jar`/`.exe` y se carguen por classpath.
- Nueva utilidad `Icons` (usa `FlatSVGIcon` de flatlaf-extras) que carga cada SVG y lo
  recolorea al tono del tema oscuro mediante un `ColorFilter`.
- Se añadió la dependencia `com.formdev:svgSalamander:1.1.4` (motor de render que usa
  FlatSVGIcon) para garantizar que los iconos rendericen al recompilar.
- **Sidebar rediseñado** (`MainWindow`): cada ítem de navegación ahora lleva su icono; el
  ítem activo se resalta con fondo y texto/icono destacados (igual que la referencia). Se
  agregó una sección "Atajos rápidos" (Nueva obra / Nueva partida / Nuevo reporte) y una
  tarjeta de usuario al pie. Barra superior con logo e icono.
- **Tarjetas KPI** del Dashboard: cada una muestra su icono dentro de un recuadro
  redondeado tintado con su color de acento (azul/verde/morado/ámbar).
- Iconos en los botones de acción: exportar (↓) en Reportes y Comparativo, buscar archivo
  y crear obra en Obras.
- Nuevos colores de tema: `PRIMARY` (azul de acción), `PURPLE`, `NAV_HOVER`, `NAV_ACTIVE`
  y fuente `FONT_SMALL`.

Nota: la app seguía siendo funcional; estos cambios son solo de presentación. Requiere
recompilar en IntelliJ para que Maven descargue `svgSalamander`.

## [1.3.0] - 2026-06-30

### Validado contra Excel real de consolidado (obra CENTRO DE CONSERVACIÓN, Tacna)
- Probado el `ExcelImporter` contra `1Ppto. consolidado con partidas...xls` (formato S10,
  3 hojas, 1998 filas con filas en blanco intercaladas). Detección correcta del encabezado
  (fila 11) y de todas las columnas (Item, Descripción, Und., Metrado, Precio S/, Parcial S/).
- La suma de partidas ejecutables de la hoja `Sheet1` da **S/. 3,865,615.16**, que coincide
  EXACTAMENTE con el "COSTO DIRECTO" declarado en el archivo. 976 partidas (288 padre,
  688 ejecutables), 0 códigos duplicados, y `total == cantidad × precio` en el 100% de las hojas.

### Corregido
- **Acceso a un Workbook ya cerrado (bug latente):** `importar()` leía celdas después de que
  el `try-with-resources` cerrara el libro. Ahora toda la importación ocurre con el libro
  abierto. Se extrajo `procesarFilas()` para reutilizar la lógica.
- **Selección de hoja ambigua:** un consolidado suele traer varias hojas de presupuesto
  (`Sheet1`, `PPTO`, `PPTO 95%`) con totales distintos e internamente consistentes, así que
  ninguna heurística puede adivinar "la correcta". Nuevos `listarHojasImportables()` e
  `importar(ruta, nombreHoja)`: si el libro trae más de una hoja válida, `ObrasView` muestra
  un selector con partidas y total de cada hoja para que el usuario elija.
- Selección por defecto más robusta: la primera hoja con estructura reconocible (o una llamada
  Presupuesto/Resumen), en vez de la hoja 0 a ciegas.
- Se omiten también filas "COSTO DIRECTO"/"COSTO TOTAL" sin unidad, y se normalizan más
  unidades (glb→global, jgo→juego, pto→punto, vje→viaje) para un control de gastos más limpio.
- Lectura numérica: las celdas de fórmula ahora intentan primero su valor numérico calculado.

### Escalabilidad
- Nuevo índice compuesto `idx_mov_obra_fecha (obra_id, fecha)` para acelerar el comparativo
  temporal y los reportes por fecha cuando la obra acumula miles de movimientos.
- Confirmado que `PartidaService.guardarTodas` inserta en lote dentro de una transacción
  (rápido incluso con ~1000 partidas).

### Tests
- `ExcelImporterTest`: nuevos casos `listaVariasHojasYPermiteImportarLaElegida` y
  `hojaInexistenteDevuelveError`.

## [1.2.0] - 2026-06-27

### Agregado — Comparativo temporal diario/semanal/mensual (Sprint 6)
- **`Granularidad`** (enum): niveles de agrupación DIARIO / SEMANAL / MENSUAL.
- **`ResumenPeriodo`** (modelo): bloque agregado de un periodo con egresos, ingresos,
  neto (egresos − ingresos), acumulado corrido y número de movimientos.
- **`MovimientoService.resumenPorPeriodo(obraId, granularidad)`**: agrega primero por día en
  SQL (exacto y rápido) y luego agrupa los días en Java por día, semana ISO (lunes a domingo)
  o mes calendario, cubriendo toda la duración de la obra. Devuelve los periodos en orden
  cronológico con el acumulado corrido y el % del presupuesto consumido.
- **`ReporteService.exportarComparativoPeriodicoExcel`** y
  **`ReportePdf.exportarComparativoPeriodicoPdf`**: exportan el comparativo temporal a Excel y
  PDF con totales, línea de referencia del presupuesto y % coloreado verde/amarillo/rojo.
- **`ComparativoView`**: nueva pantalla con selector Diario/Semanal/Mensual, tabla coloreada
  por avance, resumen global (periodos, movimientos, ejecutado vs presupuesto) y botones de
  exportación a Excel/PDF. Integrada en el sidebar de `MainWindow` y en el ciclo de refresco.

### Validado
- Lógica de bucketing y acumulados replicada en Python sobre SQLite con datos sintéticos
  (semanas ISO, meses, filtrado por obra, devoluciones): todas las aserciones correctas
  (neto semana 26 = 1900, junio = 2300, julio = 600, acumulado final = neto global = 2900).
- Test JUnit `ResumenPeriodoTest` (4 casos: diario, semanal ISO, mensual, obra sin movimientos)
  con creación y limpieza de datos propios para no contaminar la base real.

## [1.1.0] - 2026-06-27

### Agregado — Reportes exportables (Sprint 5)
- **`ReporteService`**: exporta a Excel (`.xlsx`) tres reportes, con estilos profesionales
  (encabezado de obra, header azul, formato de moneda, fila de totales, % de avance coloreado
  en verde/amarillo/rojo):
  - Comparativo presupuesto vs ejecutado por partida.
  - Reporte diario de almacén (movimientos de una fecha).
  - Reporte acumulado de almacén (todos los movimientos).
- **`ReportePdf`**: genera los mismos tres reportes en PDF usando Apache PDFBox (licencia
  Apache 2.0), con paginación automática y encabezado de tabla repetido en cada página.
- **`EstilosExcel`**: helper que centraliza los estilos de celda de POI.
- **`ReportesView`**: nueva pantalla con una tarjeta por reporte y botones "Excel" / "PDF".
  Al generar, abre el archivo automáticamente y muestra la ruta. Integrada en el sidebar
  (`📄 Reportes`) y en el ciclo de refresco de `MainWindow`.
- Nueva dependencia en el `pom.xml`: `org.apache.pdfbox:pdfbox:2.0.31`.

### Notas
- Los archivos se guardan en la carpeta `exports/` del proyecto, con nombre
  `ObraTrack_<tipo>_<obra>_<fechahora>.xlsx|pdf`.
- En el PDF, las tildes y caracteres especiales se omiten (limitación de las fuentes estándar
  de PDFBox); el Excel sí conserva todos los caracteres.
- La lógica de totales del comparativo se validó contra el Excel real: solo las partidas
  "hoja" suman al total, evitando el doble conteo con las partidas agrupadoras.

---

## [1.0.0-MVP] - 2026-06-27

### Cambio de stack respecto a la skill por defecto
El usuario pidió explícitamente **Java + IntelliJ + generación de .exe**, en lugar del stack
Python/CustomTkinter sugerido por defecto en la skill `obra-track`. Se documenta este cambio
para que sesiones futuras lo mantengan.

### Agregado
- Proyecto Maven completo (`pom.xml`) con SQLite JDBC, Apache POI, FlatLaf y Maven Shade Plugin
  (para generar un único `.jar` ejecutable con todas las dependencias incluidas).
- Modelos de dominio: `Obra`, `Partida`, `MovimientoAlmacen`.
- Capa de persistencia SQLite con creación automática de esquema (`Database.java`).
- `ObraService`, `PartidaService`, `MovimientoService`: CRUD completo y cálculos de
  presupuestado vs. ejecutado.
- `ExcelImporter`: detección automática de estructura de Excel (sin formato fijo), soporta
  `.xlsx` y `.xls`, jerarquía de partidas padre/hijo, normalización de unidades, exclusión
  de filas de subtotal/total, parseo numérico flexible (punto o coma decimal), detección de
  códigos de partida duplicados, mensajes de error amigables (archivo bloqueado, no encontrado,
  vacío, sin estructura reconocible), advertencia cuando el libro tiene varias hojas.
- UI completa en Swing + FlatLaf (tema oscuro): `MainWindow` con sidebar de navegación y
  selector de obra activa, `DashboardView` con 4 KPIs y alertas automáticas, `ObrasView`
  para crear obras y cargar su Excel, `PartidasView` con colores por % de avance,
  `AlmacenView` con formulario de registro diario optimizado para uso rápido (Enter para
  saltar entre campos, confirmación al registrar costo en cero o fechas fuera de lo normal).
- Tests JUnit (`ExcelImporterTest`) cubriendo los casos del `qa-checklist.md`: header en
  fila distinta de la primera, filas de subtotal/vacías, Excel sin estructura reconocible,
  precios con símbolo "S/.", Excel completamente vacío.
- `README.md` con instrucciones detalladas de cómo abrir el proyecto en IntelliJ y generar
  el `.exe` (vía Launch4j o jpackage).

### Validado
- La lógica del `ExcelImporter` fue portada a Python y probada contra el Excel real
  proporcionado por el usuario (presupuesto tipo S10/Crystal Reports, ~2000 filas):
  detectó correctamente 976 partidas (290 agrupadoras, 686 ejecutables) y un presupuesto
  total de S/. 3,788,609.89, omitiendo 1010 filas vacías/separadoras sin intervención manual.
- Se verificó el balance de paréntesis y llaves (excluyendo comentarios y strings) en los
  17 archivos `.java` del proyecto: todos correctos.

### Limitaciones conocidas de esta entrega
- No se pudo compilar/ejecutar el `.jar` real en este entorno de desarrollo (sin Maven ni
  JDK completo disponibles, sin acceso a internet para descargar dependencias). El usuario
  deberá compilarlo en IntelliJ, donde sí tendrá Maven embebido y conexión a internet.
- Sin reportes exportables todavía (Excel/PDF).
- Sin edición/eliminación de movimientos ya registrados.
