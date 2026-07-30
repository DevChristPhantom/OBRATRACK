# Política de seguridad

## Reportar una vulnerabilidad

Si encuentras una vulnerabilidad de seguridad en ObraTrack, repórtala de forma privada
abriendo un issue marcado como confidencial o contactando directamente a los mantenedores
del repositorio (`DevChristPhantom`) en vez de publicarla como issue público, para dar tiempo
a corregirla antes de que se conozca públicamente.

Incluye:
- Versión afectada (`pom.xml` / `CHANGELOG.md`).
- Pasos para reproducir.
- Impacto esperado (qué datos u operaciones compromete).

## Alcance

ObraTrack es una aplicación de escritorio con base de datos SQLite local (sin servidor
expuesto a red). Los vectores relevantes son: manejo de credenciales, inyección SQL,
integridad de los reportes exportados (Excel/PDF), y protección de la base de datos local.

Ver `docs/AUDITORIA.md` para el historial de hallazgos de seguridad ya revisados y su estado.

## Buenas prácticas ya implementadas

- Contraseñas con PBKDF2WithHmacSHA256 (120 000 iteraciones, salt aleatorio).
- Consultas SQL parametrizadas (`PreparedStatement`) en toda la capa de negocio.
- Bloqueo de cuenta tras intentos fallidos de login.
- Cambio de contraseña forzado en la cuenta admin sembrada por defecto.
- Sanitización de celdas de Excel exportadas contra inyección de fórmulas.
