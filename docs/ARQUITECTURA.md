# ObraTrack — Arquitectura y buenas prácticas

Aplicación de escritorio para gestión de obras: presupuesto vs. ejecutado, almacén,
comparativos temporales, reportes y control de usuarios.

## Stack

- **Lenguaje / plataforma:** Java 17, Swing (UI de escritorio nativa).
- **Look & feel:** FlatLaf (tema oscuro) + FlatSVGIcon/svgSalamander para iconos SVG.
- **Gráficos:** Java2D (dibujados a mano, sin librerías externas).
- **Base de datos:** SQLite embebida (sqlite-jdbc), un solo archivo, sin servidor.
- **Excel / PDF:** Apache POI y PDFBox.
- **Seguridad:** PBKDF2 (JDK) para el hash de contraseñas.
- **Build:** Maven. Empaquetado en un `.jar` único (maven-shade-plugin).

## Arquitectura por capas

```
com.obratrack
├── Main                 Punto de entrada (logging, errores, respaldo, login)
├── core/                Infraestructura transversal
│   ├── Database         Conexión SQLite, esquema, migraciones, pragmas (WAL)
│   ├── AppLog           Logging a archivo rotativo (java.util.logging)
│   ├── ManejadorErrores Captura global de excepciones (incl. EDT)
│   ├── RespaldoDB       Respaldo automático (VACUUM INTO)
│   └── AppInfo          Nombre y versión de la app
├── model/               Entidades de dominio (POJOs): Obra, Partida,
│                        MovimientoAlmacen, MovimientoAuditoria, Usuario,
│                        ResumenPeriodo
├── service/             Lógica de negocio y acceso a datos (una clase por área):
│                        ObraService, PartidaService, MovimientoService,
│                        UsuarioService, ExcelImporter, ReporteService, ReportePdf,
│                        IndicadorSalud, SesionActual. Los cuatro servicios centrales
│                        (Obra/Partida/Movimiento/Usuario) también exponen una interfaz
│                        `IXxxService` y una implementación `XxxServiceRemoto` (RPC) —
│                        ver "Modo anfitriona/cliente" abajo.
├── red/                 Transporte HTTP del modo anfitriona/cliente: ServidorHttp
│                        (host), RpcCliente, JsonUtil, Escritura (anotación de permisos)
├── util/                Utilidades puras (PasswordUtil)
└── ui/                  Swing: MainWindow, LoginView, Theme, Icons, ModoInicioView y views/
```

Regla de dependencias: **ui → service → core/model**, con `red` como infraestructura
paralela a `core` (`red → service/core`, ninguno de los dos depende de `red` ni de `ui`).
La capa `model` no depende de nada; `service` no conoce Swing; la UI no arma SQL. Esto
permite testear la lógica sin interfaz.

## Modo anfitriona/cliente (red local)

Cada obra puede operar en tres modos, elegidos en el primer arranque (`ModoInicioView`,
antes de tocar SQLite) y persistidos en `red.properties`:

- **Local** — el comportamiento de siempre, una PC con su propio archivo SQLite.
- **Anfitriona** — además de lo anterior, expone los servicios por HTTP (`ServidorHttp`,
  JDK `com.sun.net.httpserver`) en la red local para que otras PC de la obra se conecten.
- **Cliente** — no toca SQLite en absoluto (`Database.get()` se niega explícitamente si
  `RedEstado.modo() == CLIENTE`); todas las llamadas van por RPC (`RpcCliente`, JDK
  `java.net.http.HttpClient`) hacia la PC anfitriona.

El transporte es un único endpoint `POST /rpc` que despacha por reflexión hacia la clase
local real del servicio (usando el `Type` genérico de la interfaz `IXxxService` para
(de)serializar con Gson) — así ningún servicio existente cambia una línea de SQL. La
identidad del usuario remoto se "presta" a `SesionActual` solo durante cada request,
dentro de `Database.LOCK`. La anotación `@Escritura` en la interfaz marca qué métodos
mutan datos, para que el host exija `Permisos.puedeEscribir()` antes de ejecutarlos.

Los 13 servicios de datos ya funcionan en red (Usuario, Obra, Partida, Movimiento,
Cronograma, Cuaderno, Cumplimiento, Valorización, Metrado, APU, Fórmula Polinómica,
Documento y los dos generadores de reportes). Dos casos no encajan en el RPC genérico
porque mueven archivos, no JSON, y usan un transporte paralelo:

- **Documentos** (`IDocumentoService`): `subir` envía el archivo por streaming a
  `POST /archivos/subir` (metadatos por query string, cuerpo = bytes crudos);
  `archivoAbsoluto` descarga el archivo guardado en el host vía `POST /rpc-archivo`
  a una copia local en `Rutas.cache()`. El resto de sus métodos (`listarPorObra`,
  `listarVersiones`, `eliminar`) sí son RPC normales.
- **Reportes** (`IReporteService`/`IReportePdf`): generan el Excel/PDF en el host
  (reutilizando `PartidaService`/`MovimientoService` ya convertidos) y lo descargan
  también por `POST /rpc-archivo` a `Rutas.cache()`, sobrescribiendo la copia anterior
  en cada exportación (es una copia de trabajo, no un historial).

`POST /rpc-archivo` es como `/rpc` (mismo despacho por reflexión y mismo `registro`,
aunque en un mapa aparte `registroArchivos`) salvo que el método invocado debe devolver
un `Path`: el host transmite el CONTENIDO de ese archivo como respuesta binaria en vez
de intentar serializar el `Path` a JSON (no tendría sentido fuera de la máquina que lo
generó). Ningún caso quedó sin convertir.

## Patrones y decisiones

- **Service layer:** cada área de negocio tiene su servicio; la UI solo orquesta.
- **Singleton de conexión** (`Database`) para SQLite (un usuario local).
- **Objeto de resultado** (`ImportResult`) en vez de excepciones para el flujo del importador.
- **Migraciones idempotentes:** el esquema se crea con `IF NOT EXISTS` y se agregan columnas
  nuevas verificando `PRAGMA table_info` (no se pierden datos de bases existentes).
- **Auditoría append-only:** cada creación/edición/eliminación de movimiento se registra con
  usuario y fecha/hora; la tabla solo recibe INSERT.

## Seguridad

- Contraseñas con **PBKDF2WithHmacSHA256**, salt aleatorio, 120 000 iteraciones; verificación
  en tiempo constante. Nunca se guarda ni se registra la contraseña en claro.
- Todo el SQL con datos de usuario usa **PreparedStatement** (sin inyección).
- **Sesión** de usuario (`SesionActual`) que alimenta la auditoría con quién hizo cada acción.
- Intentos de login (correctos y fallidos) quedan en el log para revisión.

## Rendimiento

- SQLite en modo **WAL** + `synchronous=NORMAL` + caché de páginas.
- Índices en `obra_id`, `partida_id` y compuesto `(obra_id, fecha)`.
- La UI **refresca solo la vista visible** y difiere la primera carga tras el login.
- Iconos SVG **cacheados**; la verificación de login corre **fuera del hilo de UI** (SwingWorker).

## Cómo compilar, probar y empaquetar

```bash
mvn clean test        # compila y corre los tests unitarios
mvn clean verify      # tests + reporte de cobertura (target/site/jacoco/index.html)
mvn clean package     # genera dist/ObraTrack.jar (ejecutable, con dependencias)
mvn checkstyle:check  # análisis de estilo (Google), reporte en consola
mvn spotbugs:check    # detección de bugs por análisis estático
```

Ejecutar: `java -jar target/ObraTrack.jar` (o el ▶ sobre `Main` en IntelliJ).

## Convenciones de código

- Nombres en español para el dominio; consistencia por archivo.
- Métodos cortos y con una responsabilidad; comentarios que explican el *por qué*.
- Cambios cubiertos por tests cuando tocan lógica (importador, periodos, salud, auth, obras).

## Estándar de calidad

- **Tests:** JUnit 5 (ver `src/test`). Cobertura con **JaCoCo**.
- **Análisis estático:** Checkstyle (estilo) y SpotBugs (bugs), como pasos opcionales.
- **CI:** GitHub Actions (`.github/workflows/ci.yml`) compila, testea y publica cobertura en
  cada push/PR.
