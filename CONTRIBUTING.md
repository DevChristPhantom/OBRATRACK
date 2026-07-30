# Contribuir a ObraTrack

## Requisitos

- Java 17+
- No necesitas Maven instalado: usa `mvnw.cmd` (Windows) / `./mvnw` (Linux/Mac).

## Flujo de trabajo

1. Crea una rama a partir de `main`: `feature/nombre-corto` o `fix/nombre-corto`.
2. Antes de hacer commit, corre la suite completa:
   ```bat
   mvnw.cmd clean verify
   ```
   Todos los tests deben pasar (`BUILD SUCCESS`). Opcionalmente:
   ```bat
   mvnw.cmd checkstyle:check
   mvnw.cmd spotbugs:check
   ```
3. Abre un Pull Request contra `main`. El workflow de CI (`.github/workflows/ci.yml`) corre
   automáticamente tests, checkstyle y spotbugs, y publica los reportes como artifacts.
4. Describe en el PR **qué** cambia y **por qué**, no solo el detalle técnico.

## Convenciones de código

- Java 17, sin dependencias nuevas salvo que sea claramente necesario.
- Los accesos a la única conexión SQLite deben pasar por `Database.LOCK` /
  `Database.enTransaccion(...)` (ver `docs/AUDITORIA.md` para el porqué).
- Sin comentarios que expliquen "qué" hace el código si el nombre ya lo dice; sí explicar
  decisiones no obvias (workarounds, invariantes).

## Reportar bugs o pedir features

Abre un issue en GitHub describiendo el comportamiento actual vs. el esperado, y pasos para
reproducir si aplica.
