# ObraTrack — Documento Técnico Completo

**Sistema de control de costos de obra por partidas**
Empresa: Grupo Titan G&L S.A.C. — Tacna, Perú
Documento de sustentación de las prácticas preprofesionales (aspecto técnico del desarrollo).

Este documento describe **todo lo que se desarrolló y se usó** en ObraTrack: tecnologías, arquitectura, base de datos, programación orientada a objetos, metodología de desarrollo, código limpio, aseguramiento de calidad (QA), pruebas (testing), seguridad, empaquetado y las **dificultades presentadas** durante el proyecto. Está redactado para poder defenderse punto por punto ante la comisión.

---

## 1. Resumen del sistema

ObraTrack es una **aplicación de escritorio para Windows** que permite a una empresa constructora controlar el costo de sus obras a partir de las **partidas del presupuesto**. El presupuesto se importa desde Excel; el almacén registra los **ingresos y egresos de materiales**; el sistema calcula el **costo ejecutado** por partida y por obra, lo compara con lo **presupuestado** y genera **reportes en Excel y PDF**. Incorpora control de acceso por **roles**, **auditoría** de operaciones, **respaldos automáticos** e **instalador** para Windows.

| Característica | Valor |
|---|---|
| Tipo de aplicación | Escritorio (desktop), monousuario por equipo |
| Sistema operativo | Windows |
| Lenguaje | Java 17 |
| Interfaz gráfica | Java Swing + FlatLaf (tema oscuro) |
| Base de datos | SQLite (archivo local, embebida) |
| Empaquetado | JAR ejecutable (fat jar) + instalador con jpackage |
| Control de versiones | Git + GitHub |
| Versión actual | 2.1.0 |

---

## 2. Tecnologías utilizadas

### 2.1. Lenguaje y plataforma
- **Java 17 (LTS).** Lenguaje robusto, tipado y multiplataforma; versión con soporte a largo plazo. Se usaron características modernas del lenguaje (var, switch expressions, text blocks, records donde correspondía).
- **JVM / JDK 17+.** La aplicación corre sobre la máquina virtual de Java.

### 2.2. Interfaz gráfica (UI)
- **Java Swing.** Framework nativo de Java para interfaces de escritorio; no requiere dependencias externas de UI.
- **FlatLaf 3.4.1** + **flatlaf-extras 3.4.1.** "Look and Feel" moderno de aspecto plano y **tema oscuro/claro**, que da a la aplicación una apariencia profesional y actual sobre Swing. `flatlaf-extras` aporta utilidades adicionales (entre ellas el soporte de íconos SVG).
- **svgSalamander 1.1.4.** Motor de renderizado SVG que usa `FlatSVGIcon` para dibujar los **íconos vectoriales** de la aplicación (clases `Icons`, `Theme`).
- **Java2D.** Para el dibujo de los **gráficos del dashboard** (barras/indicadores) sin librerías de charting externas.

### 2.3. Persistencia de datos
- **SQLite** mediante el driver **xerial sqlite-jdbc 3.45.3.0**. Base de datos **embebida** en un solo archivo; ideal para una app de escritorio porque no requiere instalar un servidor de base de datos.
- Se configuró **WAL (Write-Ahead Logging)** y **PRAGMA** de afinamiento para mejorar la concurrencia y el rendimiento de escritura/lectura.
- **Claves foráneas activadas** con **ON DELETE CASCADE**, garantizando integridad referencial (al borrar una obra se borran sus partidas y movimientos).

### 2.4. Importación / exportación de archivos
- **Apache POI 5.2.5** (`poi`, `poi-ooxml`, `poi-scratchpad`). Lectura de las **partidas del presupuesto desde Excel**, tanto **.xlsx (moderno)** como **.xls (antiguo)** gracias a `poi-scratchpad`, y **exportación de reportes a Excel** (clases `ExcelImporter`, `EstilosExcel`, `ImportResult`).
- **Apache PDFBox 2.0.31.** Generación de **reportes en PDF** (clase `ReportePdf`).

### 2.5. Seguridad
- **PBKDF2WithHmacSHA256** (del JDK) para el **hashing de contraseñas** con sal por usuario; nunca se almacena la contraseña en texto plano.

### 2.6. Construcción y dependencias
- **Apache Maven.** Gestión de dependencias y ciclo de construcción.
- **maven-shade-plugin.** Empaqueta la aplicación y todas sus dependencias en un **fat jar** ejecutable (`ObraTrack.jar`), con el `ServicesResourceTransformer` para fusionar los service loaders y los avisos de licencia.
- **jpackage.** Genera un **instalador nativo para Windows** a partir del jar.

### 2.7. Control de versiones e integración continua
- **Git** para el versionamiento y **GitHub** como repositorio remoto (historial de commits como evidencia del avance).
- **GitHub Actions (CI)** para compilar y ejecutar las pruebas automáticamente en cada cambio.

### 2.8. Calidad de código (herramientas)
- **JUnit 5 (5.10.2)** — pruebas automatizadas.
- **maven-compiler-plugin 3.13.0**, **maven-surefire-plugin 3.2.5** — compilación y ejecución de pruebas.
- **JaCoCo 0.8.12** — medición de cobertura de pruebas.
- **Checkstyle 3.3.1** (perfil `google_checks`) — verificación de estilo de código.
- **SpotBugs 4.8.6.4** (esfuerzo Max) — análisis estático para detección de posibles bugs.

### 2.9. Componentes/clases internas propias (no dependen de librerías externas)
- **`ManejadorErrores`** — manejo centralizado de errores/excepciones de la aplicación.
- **`AppLog`** — registro de eventos (logging) de la aplicación.
- **`Rutas`** — resolución de rutas de datos en una ubicación con permisos de escritura del usuario.
- **`RespaldoDB`** — respaldos automáticos de la base de datos.
- **`AppInfo`** — información/metadatos de la aplicación (nombre, versión).
- **`PasswordUtil`** — utilidades de hashing/verificación de contraseñas (PBKDF2).
- **`SesionActual`** / **`Permisos`** — sesión del usuario y matriz de permisos por rol.
- **Servicios de dominio:** `ObraService`, `PartidaService`, `MovimientoService`, `UsuarioService`, `ReporteService`, `ExcelImporter`, `ReportePdf`.
- **Vistas (UI):** `LoginView`, `MainWindow`, `DashboardView`, `ObrasView`, `PartidasView`, `AlmacenView`, `ReportesView`, `ComparativoView`, `UsuariosView`, `AjustesView`, `AcercaDe`, `CambioPasswordObligatorio`.
- **Modelos:** `Obra`, `Partida`, `MovimientoAlmacen`, `MovimientoAuditoria`, `Usuario`, `ResumenPeriodo`; enums/tipos de apoyo `Granularidad`, `IndicadorSalud`.

---

## 3. Arquitectura del software

ObraTrack sigue una **arquitectura en capas** que separa responsabilidades, facilitando el mantenimiento y las pruebas:

```
┌─────────────────────────────────────────────┐
│  Capa de Presentación (UI - Swing/FlatLaf)   │  Ventanas, vistas, dashboard
├─────────────────────────────────────────────┤
│  Capa de Servicios (lógica de negocio)       │  ObraService, PartidaService,
│                                              │  MovimientoService, UsuarioService,
│                                              │  ReporteService, Permisos, SesionActual
├─────────────────────────────────────────────┤
│  Capa de Modelo (entidades del dominio)      │  Obra, Partida, MovimientoAlmacen,
│                                              │  Usuario, MovimientoAuditoria
├─────────────────────────────────────────────┤
│  Capa de Datos (Database + SQLite/JDBC)      │  Conexión única, transacciones, migraciones
└─────────────────────────────────────────────┘
```

**Principios aplicados:**
- **Separación de responsabilidades:** la UI no habla directamente con la base de datos; siempre pasa por la capa de servicios.
- **Bajo acoplamiento y alta cohesión:** cada servicio agrupa la lógica de una parte del dominio (obras, partidas, movimientos, usuarios, reportes).
- **Punto único de acceso a datos:** la clase `Database` centraliza la conexión, las transacciones y las migraciones de esquema.

---

## 4. Programación Orientada a Objetos (POO)

El sistema está diseñado íntegramente con POO. Se aplicaron sus cuatro pilares y varios patrones de diseño:

### 4.1. Pilares de la POO
- **Abstracción.** Cada entidad del dominio (Obra, Partida, MovimientoAlmacen, Usuario) modela un concepto real de la obra. Los servicios abstraen las operaciones ("registrar movimiento", "importar partidas") ocultando el detalle SQL.
- **Encapsulamiento.** Los atributos de las entidades son privados y se acceden mediante getters/setters; las reglas de negocio viven en los servicios, no expuestas a la UI.
- **Herencia / jerarquías.** Uso de enumeraciones y tipos (por ejemplo `Usuario.Rol` con ADMIN, JEFE_OBRA, ALMACENERO; `MovimientoAlmacen.Tipo` con INGRESO/EGRESO; `MovimientoAuditoria.Accion` con CREACION/ACTUALIZACION/ELIMINACION; `Obra.Estado` con ACTIVA, etc.).
- **Polimorfismo.** El manejo uniforme de movimientos (ingreso/egreso) y de acciones de auditoría mediante enums y métodos que operan sobre el tipo base.

### 4.2. Patrones de diseño empleados
- **Service Layer (capa de servicio):** `ObraService`, `PartidaService`, `MovimientoService`, `UsuarioService`, `ReporteService`.
- **DAO / Repository (implícito):** el acceso a datos se aísla en la capa `Database` y en los servicios, no en la UI.
- **Singleton / estado de sesión:** `SesionActual` mantiene el usuario autenticado; `Database.LOCK` centraliza la sincronización.
- **Template para transacciones:** método `enTransaccion(Trabajo)` en `Database` que ejecuta un bloque de trabajo dentro de una transacción (commit/rollback automáticos) — patrón *Execute Around*.
- **MVC aproximado:** modelo (entidades), vista (Swing) y control (servicios/handlers) separados.

### 4.3. Modelo de dominio (entidades principales)
- **Obra** — el proyecto de construcción (nombre, fechas, estado).
- **Partida** — ítem del presupuesto (código, descripción, unidad, cantidad, costo unitario, parcial presupuestado).
- **MovimientoAlmacen** — ingreso o egreso de material asociado a una partida (fecha, tipo, cantidad, costo unitario real, costo total real).
- **Usuario** — con rol y contraseña cifrada; controla el acceso.
- **MovimientoAuditoria** — registro de trazabilidad (quién, qué acción, cuándo).

---

## 5. Base de datos

### 5.1. Motor y justificación
Se eligió **SQLite** por ser una base de datos **embebida en un archivo**, sin servidor, perfecta para una aplicación de escritorio que debe funcionar de forma local y confiable en las computadoras de la oficina técnica.

### 5.2. Modelo relacional (resumen)
- `obra` (1) ── (N) `partida` ── (N) `movimiento_almacen`
- `usuario` (control de acceso, con columna `debe_cambiar_password` para forzar el cambio del password inicial)
- `movimiento_auditoria` (trazabilidad de operaciones)

**Integridad:** claves foráneas con **ON DELETE CASCADE**; al eliminar una obra se eliminan en cascada sus partidas y movimientos, evitando datos huérfanos.

### 5.3. Cálculo del costo ejecutado
El **ejecutado** de una partida se calcula neteando correctamente: **EGRESOS − INGRESOS** (los ingresos representan devoluciones al almacén). Este neteo está cubierto por pruebas automatizadas para garantizar su exactitud.

### 5.4. Rendimiento y concurrencia
- Modo **WAL** y **PRAGMAs** de afinamiento.
- **Conexión única compartida**, con todas las operaciones **serializadas mediante un candado global (`Database.LOCK`)** para evitar condiciones de carrera al escribir desde la UI (que usa hilos de Swing).
- Escrituras dentro de **transacciones** (commit/rollback) para mantener la consistencia.

### 5.5. Migraciones y respaldos
- El esquema se crea y **migra automáticamente** al iniciar (por ejemplo, la incorporación de la columna `debe_cambiar_password`).
- **Respaldos automáticos** de la base de datos para continuidad de la información.

---

## 6. Módulos funcionales desarrollados

1. **Autenticación y sesión** — login con contraseña cifrada (PBKDF2), bloqueo por intentos fallidos y cambio de contraseña obligatorio en el primer ingreso.
2. **Gestión de obras** — alta/edición/eliminación de obras con su estado.
3. **Importación de partidas desde Excel** — carga del presupuesto respetando el formato de la empresa (Apache POI).
4. **Gestión de partidas** — listado y mantenimiento de las partidas por obra.
5. **Movimientos de almacén** — registro de ingresos y egresos de materiales con cálculo del costo ejecutado.
6. **Dashboard** — indicadores (KPIs) y gráficos (Java2D) de costo presupuestado vs. ejecutado; carga **asíncrona** con SwingWorker para no congelar la interfaz.
7. **Reportes** — exportación a **Excel** (POI) y **PDF** (PDFBox), incluyendo el **comparativo temporal** de costos.
8. **Control de acceso por roles** — ADMIN, JEFE_OBRA, ALMACENERO, con una matriz de permisos.
9. **Auditoría** — bitácora *append-only* de las operaciones (creación, actualización, eliminación).
10. **Respaldos** — copias de seguridad automáticas.

---

## 7. Metodología de desarrollo

### 7.1. Enfoque
Se trabajó con un **enfoque ágil e iterativo-incremental**, adaptado a un practicante trabajando de cerca con los usuarios:
- **Levantamiento de requerimientos** con el jefe de obra, los ingenieros y el almacenero.
- **Iteraciones cortas**: cada módulo (importación, almacén, reportes, seguridad) se desarrolló, se probó y se validó con el usuario antes de pasar al siguiente.
- **Validación continua** con el usuario final (especialmente el almacenero) para asegurar que la herramienta reflejara su trabajo real.
- **Gestión del cambio de alcance**: cuando la Gerencia cambió el requerimiento de una página web a un sistema de control de costos por partidas, se replanificó el trabajo manteniendo el objetivo de negocio.

### 7.2. Prácticas de ingeniería
- **Control de versiones con Git/GitHub**: commits frecuentes y descriptivos como historial y evidencia del avance.
- **Integración continua (GitHub Actions)**: compilación y pruebas automáticas ante cada cambio.
- **Versionado semántico** del producto (versión 2.1.0).
- **Documentación técnica** del sistema (incluida una auditoría interna).

---

## 8. Código limpio (Clean Code)

Prácticas aplicadas para mantener el código legible y mantenible:
- **Nombres significativos** en español coherentes con el dominio (`registrar`, `totalEjecutadoObra`, `celdaSegura`), evitando abreviaturas oscuras.
- **Funciones cortas y con una sola responsabilidad**; la lógica compleja se divide en métodos con nombre autoexplicativo.
- **Separación de capas** (UI / servicios / modelo / datos) para no mezclar responsabilidades.
- **Manejo de recursos** con *try-with-resources* en las operaciones JDBC (cierre garantizado de statements y result sets).
- **Consultas parametrizadas** (PreparedStatement) en todo el acceso a datos: nunca se concatena SQL con datos del usuario.
- **Constantes y enums** en lugar de "números/textos mágicos" (roles, tipos de movimiento, estados).
- **Comentarios donde aportan valor** (explican el *por qué*, no lo obvio).
- **Eliminación de código muerto** y de imports sin usar (verificado con las herramientas de análisis).

---

## 9. Aseguramiento de la calidad (QA) y pruebas (Testing)

### 9.1. Estrategia de pruebas
- **Pruebas unitarias con JUnit 5** sobre la lógica de negocio crítica:
  - `MovimientoServiceTest` — verifica el **neteo EGRESO − INGRESO** del costo ejecutado, que **cada movimiento deje su rastro de auditoría** y que la **eliminación** funcione. Usa una obra de nombre único y la limpia al terminar (`@AfterEach`).
  - `PermisosTest` — verifica la **matriz de permisos por rol** (ADMIN puede todo; JEFE_OBRA gestiona obras pero no usuarios; ALMACENERO solo consulta/registra; sin sesión no hay permisos).
- **Pruebas de integración** ligeras contra la base de datos SQLite real (creación y borrado en cascada).
- Total: conjunto de pruebas que pasa en verde (**BUILD SUCCESS**) e integrado en CI.

### 9.2. Herramientas de QA
- **JaCoCo** — cobertura de pruebas.
- **Checkstyle** — estilo consistente.
- **SpotBugs** — detección estática de posibles defectos.
- **GitHub Actions** — ejecuta compilación y pruebas automáticamente.

### 9.3. Auditoría interna de seguridad y calidad
Se realizó una **auditoría interna** del sistema (documentada aparte) que revisó seguridad, concurrencia, transacciones, manejo de roles y build. Resultado: **0 hallazgos críticos**; los hallazgos altos y medios detectados fueron **corregidos** (ver sección de dificultades). Esto demuestra un ciclo de mejora continua propio de un estándar profesional.

---

## 10. Seguridad

- **Contraseñas cifradas** con PBKDF2WithHmacSHA256 y sal por usuario; jamás en texto plano ni en logs.
- **Bloqueo por intentos fallidos** de inicio de sesión.
- **Cambio de contraseña obligatorio** en el primer ingreso (el usuario semilla `admin` no puede seguir con la clave por defecto).
- **Control de acceso por roles** con una matriz de permisos aplicada en la capa de servicios (no solo ocultando botones en la UI).
- **Protección contra inyección SQL** con consultas parametrizadas.
- **Protección contra inyección de fórmulas en Excel** (`celdaSegura`): se neutralizan valores que empiezan con `=`, `+`, `-`, `@` al exportar.
- **Protección del último administrador activo**: no se permite eliminar/inhabilitar al único admin, evitando dejar el sistema sin acceso administrativo.
- **Auditoría append-only**: las operaciones quedan registradas y no se sobrescriben.
- **Respaldos automáticos** de la base de datos.

---

## 11. Empaquetado y despliegue

- **Fat jar** (`ObraTrack.jar`) construido con maven-shade-plugin: incluye todas las dependencias en un único archivo ejecutable.
- **Instalador para Windows** generado con **jpackage**, para una instalación sencilla en las máquinas de la oficina.
- **Datos de la aplicación** (base de datos y respaldos) escritos en una ubicación con permisos de escritura del usuario.

---

## 12. Dificultades presentadas y cómo se resolvieron

Sección clave para la sustentación: problemas reales del proyecto y su solución.

### 12.1. Cambio de alcance (de página web a sistema de escritorio)
**Dificultad:** el plan inicial contemplaba una página web, pero tras el levantamiento la Gerencia pidió un sistema de **control de costos por partidas** de escritorio, alimentado desde Excel.
**Solución:** se replanificó el proyecto manteniendo el objetivo de negocio; se eligió una arquitectura de escritorio (Java + SQLite) más acorde a la operación local de la oficina.

### 12.2. Heterogeneidad de los formatos de Excel
**Dificultad:** los presupuestos venían en formatos de Excel no uniformes, lo que dificultaba la importación de partidas.
**Solución:** se estandarizó el formato de entrada y se construyó un importador tolerante con validación de datos (Apache POI).

### 12.3. Concurrencia con la base de datos (condiciones de carrera)
**Dificultad:** al operar desde los hilos de Swing con una conexión SQLite compartida, podían darse condiciones de carrera.
**Solución:** se **serializaron todas las operaciones de datos con un candado global (`Database.LOCK`)** y se movió la carga del dashboard a **SwingWorker** (asíncrono) para no congelar la interfaz.

### 12.4. Consistencia de las operaciones (transacciones)
**Dificultad:** operaciones que tocaban varias tablas podían dejar datos inconsistentes si fallaban a medias.
**Solución:** se introdujo el helper **`enTransaccion(Trabajo)`** en `Database` (commit/rollback automáticos) y se envolvieron en transacción los registros/actualizaciones/eliminaciones de movimientos y obras.

### 12.5. Inyección de fórmulas al exportar a Excel
**Dificultad:** un texto que empezara con `=` podía convertirse en fórmula ejecutable en el Excel exportado (riesgo de seguridad).
**Solución:** función **`celdaSegura`** que neutraliza los prefijos peligrosos (`= + - @`) antes de escribir la celda.

### 12.6. Seguridad de credenciales y roles
**Dificultad:** evitar contraseñas por defecto persistentes, enumeración de usuarios y accesos indebidos.
**Solución:** **cambio de contraseña obligatorio** del admin semilla, hash **PBKDF2**, comparación con *hash dummy* para no revelar si un usuario existe, y **matriz de permisos** aplicada en la capa de servicios (fallo seguro por defecto).

### 12.7. Construcción del fat jar (service loaders y licencias)
**Dificultad:** al empaquetar todas las dependencias, se podían perder los *service loaders* (POI/PDFBox) o duplicar avisos de licencia.
**Solución:** configuración del **`ServicesResourceTransformer`** y de los transformadores de licencia/aviso en maven-shade-plugin.

### 12.8. Ruido de herramientas con JDK nuevo
**Dificultad:** JaCoCo mostraba `Unsupported class file major version` al instrumentar clases internas del JDK más reciente.
**Solución:** se verificó que era **cosmético** (la build seguía en **BUILD SUCCESS** y las pruebas pasaban); se documentó y no afectó al producto.

### 12.9. Entorno de compilación (Maven no estaba en el PATH)
**Dificultad:** `mvn` no se encontraba en la terminal.
**Solución:** se usó el **Maven incluido en el IDE** apuntando su `JAVA_HOME` al JDK del propio IDE, logrando compilar y empaquetar (`BUILD SUCCESS`).

### 12.10. Curva de aprendizaje del dominio de construcción
**Dificultad:** al inicio, poca familiaridad con la terminología (partidas, metrados, insumos).
**Solución:** trabajo directo con el **almacenero, el jefe de obra y los ingenieros** para entender el flujo real y validar la herramienta.

---

## 13. Competencias técnicas demostradas

Análisis y diseño de sistemas · Modelado de datos relacional · Programación en Java · Programación orientada a objetos y patrones de diseño · Interfaces de escritorio (Swing/FlatLaf) · Manejo de archivos Excel y PDF · Bases de datos embebidas (SQLite) · Concurrencia y transacciones · Seguridad de aplicaciones · Pruebas automatizadas y QA · Control de versiones e integración continua (Git/GitHub/Actions) · Empaquetado y despliegue (Maven, jpackage).

---

## 14. Resumen de todo lo que se usó (checklist)

- **Lenguaje:** Java 17
- **UI:** Swing, FlatLaf 3.4.1 + flatlaf-extras, svgSalamander 1.1.4 (íconos SVG), Java2D (gráficos)
- **Base de datos:** SQLite (sqlite-jdbc 3.45.3.0), WAL, PRAGMAs, FK ON DELETE CASCADE
- **Archivos:** Apache POI 5.2.5 (poi + poi-ooxml + poi-scratchpad → Excel .xlsx y .xls), PDFBox 2.0.31 (PDF)
- **Seguridad:** PBKDF2WithHmacSHA256, roles, auditoría, respaldos
- **Build:** Maven, maven-compiler-plugin 3.13.0, maven-shade-plugin 3.5.3 (fat jar), maven-surefire-plugin 3.2.5, jpackage (instalador)
- **VCS/CI:** Git, GitHub, GitHub Actions
- **Calidad:** JUnit 5.10.2, JaCoCo 0.8.12, Checkstyle 3.3.1, SpotBugs 4.8.6.4
- **Arquitectura:** por capas (UI / Servicios / Modelo / Datos)
- **POO:** abstracción, encapsulamiento, herencia (enums/jerarquías), polimorfismo; patrones Service Layer, DAO/Repository, Singleton, Execute-Around (transacciones)
- **Metodología:** ágil iterativa-incremental con validación continua del usuario
- **Clean Code:** nombres significativos, funciones cortas, try-with-resources, consultas parametrizadas, sin código muerto
