# Auditoría de ObraTrack

**Fecha:** 2026-07-08
**Alcance:** seguridad, calidad de código y concurrencia, deuda técnica/build y cobertura de pruebas.
**Versión revisada:** `pom.xml` 2.0.0 · `BUILD SUCCESS`, 29 tests en verde.
**Método:** revisión estática del código fuente real (no genérica).

## Resumen ejecutivo

ObraTrack está en muy buen estado para una app de escritorio: las contraseñas usan PBKDF2 correctamente, el 100% del SQL de negocio es parametrizado, hay auditoría append-only, respaldos automáticos, roles y bloqueo por intentos fallidos. No se encontró ningún hallazgo **crítico**.

Hay tres puntos que conviene atender pronto porque son riesgos reales, no cosméticos: (1) el acceso concurrente a la **única conexión SQLite** desde el nuevo `SwingWorker` del Dashboard no es seguro entre hilos, (2) las operaciones de varios pasos (movimiento + auditoría, borrado de obra) **no son transaccionales**, y (3) los reportes Excel son vulnerables a **inyección de fórmulas** al escribir texto del usuario en las celdas.

| Severidad | Cantidad |
|-----------|----------|
| Crítico | 0 |
| Alto | 3 |
| Medio | 4 |
| Bajo | 6 |

---

## Hallazgos ALTOS

### A1 · Acceso concurrente inseguro a la conexión SQLite única
**Archivos:** `core/Database.java` (conexión estática compartida) · `ui/views/DashboardView.java` (SwingWorker) · todos los `*Service`.

`Database.get()` devuelve **una sola** `Connection` estática compartida por toda la app. El Dashboard ahora ejecuta sus consultas en un `SwingWorker` (hilo de fondo) mientras el EDT puede lanzar otras consultas (refresco de otra vista, respaldo al iniciar, etc.). Una `Connection` JDBC de SQLite **no es segura** para ejecutar sentencias en paralelo desde dos hilos: puede lanzar excepciones intermitentes o devolver resultados cruzados.

El `DB_LOCK` que agregamos en `DashboardView` solo serializa los workers *del Dashboard entre sí*; no coordina con las llamadas a servicios que corren en el EDT.

**Corrección recomendada (elige una):**
- **Opción simple y robusta:** que *todo* acceso a BD pase por un único candado global. Envuelve las llamadas en un método central, p. ej. en `Database`:
  ```java
  public static final Object LOCK = new Object();
  // en cada operación de servicio:  synchronized (Database.LOCK) { ... }
  ```
  y reemplaza el `DB_LOCK` local del Dashboard por `Database.LOCK`.
- **Opción más escalable:** abrir una conexión de vida corta por operación (SQLite lo soporta bien con WAL) o un pool pequeño (HikariCP), y quitar la conexión estática compartida.

### A2 · Operaciones de varios pasos sin transacción (atomicidad)
**Archivos:** `service/MovimientoService.java` (`registrar`, `actualizar`) · `service/ObraService.java` (`eliminar`).

Con `autoCommit = true`, estas operaciones ejecutan 2 sentencias independientes:
- `registrar()` → INSERT del movimiento **+** INSERT de auditoría.
- `eliminar(obraId)` → DELETE de auditoría **+** DELETE de la obra.

Si algo falla entre ambas (excepción, cierre abrupto), la base queda inconsistente: un movimiento sin su registro de auditoría, o auditoría borrada pero la obra no. Para una app cuyo valor es el **control anti-fraude**, la atomicidad importa.

**Corrección:** envolver en transacción.
```java
Connection c = Database.get();
synchronized (Database.LOCK) {
    boolean prev = c.getAutoCommit();
    c.setAutoCommit(false);
    try {
        // ... ambas sentencias ...
        c.commit();
    } catch (SQLException e) {
        c.rollback();
        throw e;
    } finally {
        c.setAutoCommit(prev);
    }
}
```

### A3 · Inyección de fórmulas en los reportes Excel (CSV/Excel injection)
**Archivo:** `service/ReporteService.java` (`crearCeldaTexto`, cabeceras) y cualquier celda con texto de usuario (nombre de obra, código/descripción de partida, observaciones).

Se escribe texto del usuario/importado directo con `setCellValue(String)`. Si un valor empieza por `=`, `+`, `-`, `@` (o tab/CR), Excel/LibreOffice puede **interpretarlo como fórmula** al abrir el reporte generado. Como los datos provienen de Excel importados o de entradas del usuario, es un vector real.

**Corrección:** neutralizar celdas de texto antes de escribir.
```java
private String celdaSegura(String v) {
    if (v == null || v.isEmpty()) return v;
    char c0 = v.charAt(0);
    if (c0 == '=' || c0 == '+' || c0 == '-' || c0 == '@' || c0 == '\t' || c0 == '\r') {
        return "'" + v;   // prefijo apóstrofo => Excel lo trata como texto literal
    }
    return v;
}
```
Aplícalo en `crearCeldaTexto(...)` para todas las celdas que contengan texto de origen externo.

---

## Hallazgos MEDIOS

### M1 · Rol desconocido cae en ADMIN (fail-open de privilegios)
**Archivo:** `service/UsuarioService.java` → `mapear(...)`, líneas ~215-219.

```java
try { u.setRol(Usuario.Rol.valueOf(rs.getString("rol"))); }
catch (Exception e) { u.setRol(Usuario.Rol.ADMIN); }   // <-- fail-open
```
Si el campo `rol` está corrupto o es un valor no reconocido, el usuario obtiene **ADMIN**. Un valor inesperado debería otorgar el **menor** privilegio, no el mayor.

**Corrección:** por defecto `ALMACENERO` (o marcar el usuario como inválido / desactivarlo y registrar el incidente).

### M2 · `maven-shade-plugin` machaca los `META-INF/services` de POI
**Archivo:** `pom.xml` (config del shade).

El build advierte solapamiento de `META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider`, `ExtractorProvider`, `ImageRenderer`, etc. Sin un transformador que los **fusione**, el uber-jar conserva solo uno y puede romper proveedores de POI en runtime (según qué función uses).

**Corrección:** añadir transformadores al shade.
```xml
<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
<transformer implementation="org.apache.maven.plugins.shade.resource.ApacheLicenseResourceTransformer"/>
<transformer implementation="org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformer"/>
```

### M3 · Contraseña de admin por defecto sin cambio forzado
**Archivo:** `service/UsuarioService.java` (`ADMIN_PASS_INICIAL = "admin123"`, `sembrarAdminSiVacio`).

Es correcto sembrar un admin inicial, pero nada obliga a cambiar `admin/admin123` en el primer ingreso. En una instalación real es la puerta de entrada más probable.

**Corrección:** marcar la cuenta sembrada con un flag `debe_cambiar_password` y, en el login, forzar el cambio antes de entrar al panel. Alternativa mínima: mostrar una advertencia persistente mientras la clave siga siendo la de fábrica.

### M4 · Cobertura de pruebas con huecos en la lógica de negocio central
**Archivos de test actuales:** ExcelImporter(8), IndicadorSalud(6), ObraService(2), PasswordUtil(4), ResumenPeriodo(4), UsuarioService(5) = 29.

Falta cobertura justo en lo más sensible:
- `MovimientoService`: `registrar`/`actualizar`/`eliminar` y, sobre todo, el **neteo EGRESO−INGRESO** de `totalEjecutadoPorPartida`/`totalEjecutadoObra`, y que se genere la fila de auditoría correcta.
- `Permisos`: matriz de roles (ADMIN/JEFE_OBRA/ALMACENERO).
- Una vez aplicada A3, un test de la sanitización de celdas.
- Regresión de A2: que un fallo a mitad de operación deje la BD consistente.

**Corrección:** agregar estos tests; son rápidos porque ya tienes el patrón con SQLite en memoria/temporal.

---

## Hallazgos BAJOS

- **B1 · Versión inconsistente:** `pom.xml` = 2.0.0 pero `CHANGELOG.md` = 2.1.0. Unifica (el artefacto se publica como `obratrack-2.0.0`).
- **B2 · Ruido de JaCoCo con JDK 21:** el proyecto es release 17 pero se compila/ejecuta con JDK 21 (class file v69) y JaCoCo 0.8.12 no puede instrumentar clases internas del JDK → cientos de stack traces inofensivos. Correr los tests con JDK 17, o subir JaCoCo, o asumirlo. No afecta el build.
- **B3 · Sin Maven Wrapper:** el `mvn` no está en el PATH (ya lo viste). Añadir `mvnw`/`mvnw.cmd` (`mvn -N wrapper:wrapper`) hace el build reproducible sin instalar Maven.
- **B4 · Contraseñas no se limpian de memoria en la capa de servicio:** `AjustesView` sí hace `Arrays.fill(...)`, pero `UsuarioService`/`PasswordUtil` reciben `char[]` y no lo borran tras usarlo. Menor en desktop, pero es buena práctica limpiar el arreglo al terminar.
- **B5 · Oráculo de enumeración por tiempo en login:** para un usuario inexistente `autenticar` retorna sin ejecutar PBKDF2 (respuesta más rápida que con clave incorrecta). Para igualar tiempos, calcula un hash "dummy" también cuando el usuario no existe.
- **B6 · Imports/constantes sin uso:** p. ej. `java.nio.file.Paths` en `Database.java` y `ReporteService.java`, y `CARPETA_EXPORTS` en `ReporteService`. SpotBugs/Checkstyle (ya configurados) los detectan; conviene correrlos en CI.

---

## Lo que está bien hecho (para no romperlo)

- **Hashing:** PBKDF2WithHmacSHA256, 120 000 iteraciones, salt aleatorio de 16 bytes, comparación en tiempo constante (`MessageDigest.isEqual`). Correcto y sin dependencias externas.
- **SQL:** `PreparedStatement` en todas las consultas de negocio; sin concatenación con datos de usuario.
- **Integridad referencial:** claves foráneas con `ON DELETE CASCADE` y `PRAGMA foreign_keys = ON`.
- **Auditoría:** tabla append-only con usuario y fecha/hora por acción.
- **Resiliencia:** manejador global de excepciones, logging a archivo rotativo, respaldo con `VACUUM INTO`, carpeta de datos escribible por usuario.
- **UX/robustez:** bloqueo por 5 intentos, protección del último admin activo, carga asíncrona del Dashboard con spinner.

---

## Plan sugerido (por orden de impacto)

1. **A1 + A2** juntos: introducir `Database.LOCK` global y envolver en transacción `registrar`/`actualizar`/`eliminar`. (Mismo cambio de base.)
2. **A3 + M1:** sanitizar celdas de reporte y cambiar el rol por defecto a menor privilegio.
3. **M2:** transformadores del shade (evita romper POI en runtime).
4. **M3:** cambio de contraseña forzado en el primer ingreso.
5. **M4:** tests de `MovimientoService`, `Permisos` y regresión de A2/A3.
6. **B1–B6:** limpieza (versión, wrapper, imports, JaCoCo, timing).

*Ninguno de estos cambios requiere reescribir la arquitectura; son ajustes acotados sobre una base sólida.*
