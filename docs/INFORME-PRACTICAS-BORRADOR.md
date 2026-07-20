# INFORME FINAL DE PRÁCTICAS PREPROFESIONALES

> **Borrador de contenido** (Art. 10 del Reglamento de Prácticas Preprofesionales de la UNAJ, RCU N° 373-2026-CU-UNAJ).
> Este archivo contiene el texto completo y defendible del informe. Los campos marcados con `[COMPLETAR: ...]` corresponden a datos que están dentro de tu Plan de Trabajo (Anexo 3) y que debo transcribir literalmente cuando pueda leer el PDF. Todo lo demás ya está redactado y listo para la sustentación.

---

## Datos que debo tomar de tu Plan de Trabajo (Anexo 3)

| Campo | Valor |
|---|---|
| Nombres y apellidos | `[COMPLETAR: del Anexo 3]` |
| DNI | `[COMPLETAR]` |
| Código de estudiante | `[COMPLETAR]` |
| Escuela profesional | `[COMPLETAR]` |
| Celular | `[COMPLETAR]` |
| Ciclo concluido | `[COMPLETAR]` (Art. 16 exige ≥ 7° ciclo) |
| Razón social de la empresa | Grupo Titan G&L S.A.C. *(confirmar contra el Anexo 3)* |
| RUC | `[COMPLETAR]` |
| Área / oficina donde practicó | `[COMPLETAR: ej. Oficina Técnica / Área de Sistemas]` |
| Dirección de la empresa | `[COMPLETAR]` — Tacna |
| Región / Provincia / Distrito | Tacna / `[COMPLETAR]` / `[COMPLETAR]` |
| Teléfono / web de la empresa | `[COMPLETAR]` |
| Jefe de la institución (nombre y cargo) | `[COMPLETAR]` |
| Jefe inmediato (nombre y cargo) | `[COMPLETAR]` |
| Docente de prácticas UNAJ | `[COMPLETAR]` |
| Fecha de inicio | `[COMPLETAR: del Anexo 3]` |
| Fecha de fin | `[COMPLETAR: del Anexo 3]` |
| Total de horas | `[COMPLETAR]` (típico 480 h) |
| Horario | `[COMPLETAR]` (tope Art. 9: ≤ 6 h/día, ≤ 30 h/semana) |
| Modalidad | Presencial *(confirmar)* |

---

## I. INTRODUCCIÓN

El presente informe final documenta las prácticas preprofesionales desarrolladas por el suscrito en la empresa **Grupo Titan G&L S.A.C.**, dedicada a la ejecución de obras de construcción en la región de **Tacna**, en cumplimiento del Reglamento de Prácticas Preprofesionales de la Universidad Nacional de Juliaca (RCU N° 373-2026-CU-UNAJ) y como requisito para la culminación de la formación profesional.

Las prácticas se orientaron a resolver una necesidad concreta de la empresa: **el control de los costos de obra a partir de las partidas del presupuesto**. Al inicio, el plan de trabajo contemplaba el desarrollo de una página web; sin embargo, tras el levantamiento de requerimientos con el personal de obra, la Gerencia decidió **reorientar el alcance hacia un sistema de escritorio para el control de costos por partidas**, alimentado desde los presupuestos que la empresa maneja en Excel. Esta decisión respondió a la realidad operativa: el trabajo de almacén y de control de gastos se llevaba en hojas de cálculo dispersas, sin trazabilidad ni consolidación, y se requería una herramienta que funcionara de manera local y confiable en las computadoras de la oficina técnica.

El resultado de las prácticas es **ObraTrack**, una aplicación de escritorio para Windows que importa las partidas del presupuesto desde Excel, registra los movimientos de almacén (ingresos y egresos de materiales), calcula el costo ejecutado frente al presupuestado y genera reportes en Excel y PDF. El sistema implementa la totalidad de los requerimientos recolectados en la etapa de análisis.

Durante el periodo de prácticas, además del desarrollo del software, el practicante brindó **apoyo en el área de redes y soporte** (configuración de equipos electrónicos y de la red de la oficina) y trabajó de forma coordinada con el **almacenero**, el **jefe de obra** y los **ingenieros** de la empresa para comprender el flujo real de trabajo y validar que la herramienta reflejara sus necesidades.

Este informe presenta los objetivos planteados, el resumen semanal de las actividades realizadas, el análisis FODA de la experiencia, las sugerencias derivadas y las conclusiones.

---

## II. OBJETIVOS

### 2.1. Objetivo general

Desarrollar e implementar un sistema informático de escritorio para el **control de costos de obra por partidas** en la empresa Grupo Titan G&L S.A.C., que permita registrar y consolidar los movimientos de materiales del almacén y comparar el costo ejecutado con el presupuestado, aplicando los conocimientos de ingeniería adquiridos en la formación universitaria.

### 2.2. Objetivos específicos

1. Levantar los requerimientos del control de costos y del manejo de almacén, trabajando conjuntamente con el almacenero, el jefe de obra y los ingenieros de la empresa.
2. Diseñar un modelo de datos que represente obras, partidas de presupuesto y movimientos de almacén, garantizando integridad referencial y trazabilidad.
3. Implementar la importación de las partidas del presupuesto desde archivos Excel, respetando la estructura que la empresa ya utilizaba.
4. Desarrollar el registro de ingresos y egresos de materiales con cálculo automático del costo ejecutado por partida y por obra.
5. Construir un tablero de indicadores (dashboard) y reportes exportables en Excel y PDF para la toma de decisiones.
6. Incorporar control de acceso por roles (administrador, jefe de obra, almacenero), auditoría de operaciones y respaldos automáticos de la información.
7. Versionar el proyecto con **Git y GitHub**, manteniendo un historial ordenado del desarrollo e integración continua.
8. Apoyar en la configuración de equipos y la red de la oficina técnica durante la etapa inicial de las prácticas.

---

## III. RESUMEN DE LAS ACCIONES Y/O ACTIVIDADES REALIZADAS POR SEMANA

> Cronología por fases. Las fechas exactas se transcriben del Anexo 3 (`[COMPLETAR]`). La distribución respeta lo declarado: aproximadamente **mes y medio** de levantamiento de requerimientos y apoyo en redes, y el resto del periodo dedicado al **desarrollo de la aplicación**, que fue la actividad central.

### Fase 1 — Requerimientos y soporte técnico (aprox. 6 semanas)

**Semana 1.** Incorporación a la empresa y presentación del equipo de la oficina técnica. Reconocimiento de la operación de obra y de cómo se llevaba el control de gastos en hojas de Excel. Apoyo en la configuración de equipos electrónicos y de la red de la oficina.
*Aprendizaje:* comprensión del contexto real de una empresa constructora y del flujo administrativo de una obra.

**Semana 2.** Reuniones de levantamiento de requerimientos con el **jefe de obra** y los **ingenieros**. Identificación del problema central: falta de trazabilidad y consolidación de los costos por partida.
*Aprendizaje:* técnicas de elicitación de requerimientos y traducción de necesidades del negocio a requisitos de software.

**Semana 3.** Trabajo conjunto con el **almacenero** para observar directamente cómo registra los ingresos y egresos de materiales (cemento, fierro, agregados, etc.) y qué información necesita en el día a día.
*Aprendizaje:* análisis de procesos y diseño centrado en el usuario real de la herramienta.

**Semana 4.** Continuación del apoyo en redes y soporte: configuración de equipos, verificación de conectividad y ordenamiento de los archivos de presupuesto. Recolección de formatos de Excel usados por la empresa.
*Aprendizaje:* soporte de infraestructura básica y estandarización de insumos de datos.

**Semana 5.** Consolidación y priorización de requerimientos. Se documenta el cambio de alcance: de la página web inicialmente prevista a un **sistema de control de costos por partidas** de escritorio, por decisión de la Gerencia según la realidad operativa.
*Aprendizaje:* gestión del cambio de alcance y comunicación con la contraparte.

**Semana 6.** Diseño de la solución: modelo de datos (obras, partidas, movimientos), elección de tecnología (aplicación de escritorio Java + base de datos local SQLite) y definición de la arquitectura por capas. Creación del repositorio en **GitHub** e inicialización del control de versiones con Git.
*Aprendizaje:* diseño de arquitectura de software y buenas prácticas de versionamiento.

### Fase 2 — Desarrollo de ObraTrack (resto del periodo)

**Semana 7.** Configuración del proyecto (Maven), estructura por capas (modelo, servicios, interfaz) e implementación de la base de datos SQLite con integridad referencial y borrado en cascada.
*Aprendizaje:* configuración de un proyecto profesional y modelado de datos relacional.

**Semana 8.** Módulo de **importación de partidas desde Excel** con Apache POI, respetando el formato de presupuesto de la empresa.
*Aprendizaje:* procesamiento de archivos Excel y validación de datos de entrada.

**Semana 9.** Módulo de **almacén**: registro de ingresos y egresos de materiales, con cálculo automático del costo ejecutado por partida.
*Aprendizaje:* implementación de lógica de negocio y consistencia de cálculos.

**Semana 10.** Cálculo del **costo ejecutado vs. presupuestado** por partida y por obra, con el neteo correcto de egresos e ingresos (devoluciones).
*Aprendizaje:* diseño de reglas de cálculo verificables y pruebas de su exactitud.

**Semana 11.** **Dashboard** de indicadores (KPIs) con gráficos, para visualizar el avance de costos de cada obra.
*Aprendizaje:* visualización de datos y diseño de interfaz para toma de decisiones.

**Semana 12.** **Reportes exportables** en Excel y PDF (Apache POI y PDFBox), incluyendo el comparativo temporal de costos.
*Aprendizaje:* generación de reportes y presentación profesional de resultados.

**Semana 13.** **Seguridad y control de acceso**: inicio de sesión con contraseña cifrada (PBKDF2), roles de administrador, jefe de obra y almacenero, y bloqueo por intentos fallidos.
*Aprendizaje:* fundamentos de seguridad de aplicaciones y control de acceso por roles.

**Semana 14.** **Auditoría y respaldos**: registro de trazabilidad de las operaciones (quién hizo qué y cuándo) y copias de seguridad automáticas de la base de datos.
*Aprendizaje:* trazabilidad, integridad y continuidad de la información.

**Semana 15.** **Pruebas y control de calidad**: pruebas automatizadas de la lógica de cálculo y de permisos; revisión de código; corrección de observaciones. Empaquetado de la aplicación con instalador para Windows.
*Aprendizaje:* pruebas de software, aseguramiento de calidad y despliegue.

**Semana 16.** **Validación con los usuarios** (almacenero, jefe de obra e ingenieros), ajustes finales, documentación del sistema y publicación de la versión final en **GitHub**.
*Aprendizaje:* validación con usuarios finales, documentación técnica y cierre de proyecto.

> **Nota de sustentación:** todos los módulos descritos existen y funcionan en ObraTrack. El sistema compila correctamente, pasa sus pruebas automatizadas y cuenta con un instalador para Windows, un historial de versiones en GitHub y documentación técnica (incluida una auditoría interna de seguridad y calidad).

---

## IV. ANÁLISIS FODA DE LAS PRÁCTICAS

### Fortalezas (internas del practicante)
- Conocimientos de programación (Java), bases de datos y análisis de sistemas aplicados a un problema real.
- Capacidad de autogestión para llevar el desarrollo completo del software, desde el requerimiento hasta el despliegue.
- Uso de control de versiones (Git/GitHub) e integración continua, ordenando el trabajo y dejando evidencia del avance.
- Facilidad para comunicarse con personal no técnico (almacenero, jefe de obra, ingenieros) y traducir sus necesidades a software.

### Oportunidades (externas aprovechadas)
- Acceso a un caso real de una empresa constructora, con datos y procesos concretos de control de obra.
- Acompañamiento del jefe de obra y de los ingenieros, que permitió validar el sistema con quienes lo usarían.
- Necesidad clara y no cubierta en la empresa, lo que dio propósito y respaldo al proyecto.
- Posibilidad de aportar también en redes y soporte, ampliando la experiencia técnica.

### Debilidades (brechas propias detectadas)
- Al inicio, poca familiaridad con la terminología de construcción (partidas, metrados, insumos), superada con el apoyo del equipo.
- Necesidad de reforzar prácticas de pruebas automatizadas, que se fue consolidando durante el desarrollo.
- Estimación de tiempos optimista al inicio, ajustada al enfrentar el cambio de alcance.

### Amenazas (externas que dificultaron)
- Cambio de alcance a mitad del plan (de página web a sistema de control de costos por partidas), que exigió replanificar.
- Heterogeneidad de los formatos de Excel de presupuesto, que obligó a estandarizar los insumos.
- Disponibilidad limitada del personal de obra para reuniones, por sus propias cargas de trabajo.
- Restricciones de infraestructura (equipos y red) que motivaron el apoyo de soporte inicial.

---

## V. SUGERENCIAS

**A la empresa (Grupo Titan G&L S.A.C.):**
1. Estandarizar un único formato de Excel de presupuesto para la importación de partidas, reduciendo errores de carga.
2. Designar a un responsable del registro diario de movimientos en el almacén para aprovechar la trazabilidad del sistema.
3. Realizar respaldos periódicos externos (además de los automáticos) y capacitar a más de una persona en el uso de ObraTrack.
4. Evaluar, en una siguiente etapa, la centralización de la base de datos para el trabajo de varias obras en simultáneo.

**A la Universidad (UNAJ):**
1. Reforzar en la malla las prácticas de ingeniería de requerimientos y gestión de cambios de alcance, muy presentes en el entorno real.
2. Incluir más ejercicios de control de versiones (Git/GitHub), pruebas automatizadas y empaquetado de aplicaciones.
3. Fomentar convenios con empresas de la región para que los practicantes accedan a casos reales como este.

**A los futuros practicantes:**
1. Documentar y versionar el trabajo desde el primer día; el historial es la mejor evidencia para la sustentación.
2. Validar continuamente con el usuario final (en este caso, el almacenero y el jefe de obra) antes de dar por terminado un módulo.

---

## VI. CONCLUSIONES

1. Se cumplió el objetivo general: se desarrolló e implementó **ObraTrack**, un sistema de escritorio para el control de costos de obra por partidas, que satisface la totalidad de los requerimientos levantados en la empresa.
2. El trabajo conjunto con el **almacenero, el jefe de obra y los ingenieros** fue determinante para que la herramienta reflejara el flujo real de trabajo y fuera adoptada por sus usuarios.
3. El cambio de alcance —de una página web a un sistema de control de costos por partidas alimentado desde Excel— se gestionó adecuadamente y derivó en una solución más ajustada a la operación de la empresa.
4. La primera etapa de **levantamiento de requerimientos y apoyo en redes/soporte** (aproximadamente mes y medio) fue la base para que el desarrollo posterior, actividad central de las prácticas, respondiera a necesidades reales.
5. El uso de **Git y GitHub**, las pruebas automatizadas, la auditoría interna y el instalador para Windows evidencian la aplicación de buenas prácticas de ingeniería de software a nivel profesional.
6. Las prácticas consolidaron competencias técnicas (análisis, diseño, programación, bases de datos, seguridad, pruebas y despliegue) y competencias transversales (comunicación, trabajo en equipo y gestión del cambio), cumpliendo el propósito formativo del Reglamento de la UNAJ.

---

## ANEXOS (rótulos — adjuntar evidencias)

- **Anexo 1** — Carta de presentación a la institución. `[Adjuntar]`
- **Anexo 2** — Carta de compromiso mutuo practicante–empresa. `[Adjuntar]`
- **Anexo 3** — Constancia/Plan de aceptación de la práctica (tu Plan de Trabajo). `[Adjuntar]`
- **Anexo 4** — Ficha de evaluación de la empresa, firmada por autoridad competente. `[Adjuntar]`
- **Anexo 5** — Ficha de evaluación del docente. `[Adjuntar]`
- **Anexo 6** — Acta de la comisión de exposición del informe final. `[Adjuntar]`
- **Anexo 7** — Ficha de evaluación de la exposición. `[Adjuntar]`
- **Evidencias (Art. 24.15):** constancia/certificado de prácticas + evidencias de las actividades: capturas de ObraTrack (dashboard, importación de partidas, reportes), historial de commits en GitHub, y fotografías del trabajo en la oficina. `[Adjuntar]`
