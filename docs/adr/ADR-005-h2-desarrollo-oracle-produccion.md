# ADR-005 — H2 en desarrollo, Oracle en producción

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado (temporal)    |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

`identity-core` usa Oracle como base de datos de producción. Durante el
desarrollo inicial del proyecto, la máquina de desarrollo no tiene:

- Acceso a Docker (sin derechos de administrador)
- Acceso a una instancia Oracle de desarrollo (pendiente de provisionamiento)
- WSL disponible

Se necesita una estrategia de base de datos para desarrollo y tests que permita
avanzar en la implementación sin bloquear el progreso del proyecto.

---

## Decisión

**H2 en memoria** para desarrollo local y tests de integración (`@QuarkusTest`).
**Oracle** para producción — configurado via variables de entorno.

La BD se selecciona por perfil de Quarkus:
- `%dev` / `%test` → H2 (`jdbc:h2:mem:identitydb`)
- `%prod` → Oracle (`jdbc:oracle:thin:@host:1521/sid`)

Esta decisión se revisará cuando se disponga de Docker o de una instancia Oracle
de desarrollo.

---

## Alternativas evaluadas

### Alternativa A — Oracle XE en Docker

Oracle Express Edition disponible como imagen oficial en Docker Hub.

**Ventajas:**
- Paridad exacta con producción — mismo dialecto SQL, mismo comportamiento
  de tipos de datos, mismas constraints
- Los tests de integración validan contra el motor real

**Desventajas:**
- Requiere Docker instalado con derechos de administrador
- La imagen Oracle XE pesa ~2GB
- No disponible en la máquina de desarrollo actual

**Resultado: bloqueada** por restricciones del entorno actual. Se adoptará
cuando se disponga de acceso a Docker.

### Alternativa B — Testcontainers + Oracle XE

Levanta un contenedor Oracle XE por cada ejecución de tests.

**Ventajas:**
- Tests con paridad exacta a producción
- El contenedor se destruye al terminar — sin estado residual entre runs

**Desventajas:**
- Requiere Docker — mismo bloqueo que la alternativa A
- Tiempo de arranque del contenedor Oracle (~30-60s) alarga el ciclo de feedback

**Resultado: bloqueada** — misma restricción. Se considerará para el pipeline
de CI cuando Docker esté disponible.

### Alternativa C — H2 con modo compatibilidad Oracle

H2 ofrece un modo de compatibilidad: `MODE=Oracle`.

**Ventajas:**
- Disponible sin Docker
- Intenta emular tipos y comportamiento Oracle

**Desventajas:**
- La compatibilidad es parcial — secuencias, tipos LOB, paquetes PL/SQL y
  algunas funciones Oracle no están implementadas
- Puede enmascarar problemas que solo aparecen en Oracle real
- Genera falsa confianza en los tests

**Resultado: descartada** — la compatibilidad parcial es peor que aceptar
explícitamente que el dev environment es diferente a producción. H2 estándar
es más honesto en sus limitaciones.

---

## Justificación

H2 en memoria es la opción pragmática dado el entorno actual. Las limitaciones
son conocidas y documentadas:

1. El dialecto SQL de H2 difiere de Oracle en tipos (`VARCHAR2` vs `VARCHAR`,
   `NUMBER` vs `DECIMAL`), secuencias y funciones analíticas
2. Las migraciones Flyway deben escribirse en SQL ANSI compatible con ambos,
   o usar scripts separados por BD (`V1__*.sql` vs `V1__oracle__*.sql`)
3. Algunos comportamientos de locking y transacciones de Oracle no se reproducen en H2

Los tests de integración actuales validan la lógica de negocio y el mapeo
objeto-relacional básico — no el comportamiento específico de Oracle.

---

## Consecuencias

**Positivas:**
- El proyecto puede avanzar sin bloquear en infraestructura
- Tests de integración rápidos (~50ms por test vs ~500ms con contenedor)
- Sin dependencias externas en el entorno de desarrollo

**Negativas:**
- Riesgo de regresiones al conectar Oracle real — SQL que funciona en H2
  puede fallar en Oracle por diferencias de dialecto
- Las migraciones Flyway deben validarse contra Oracle antes de producción
- Algunos tipos de datos Oracle (CLOB, BLOB, TIMESTAMP WITH TIME ZONE)
  se comportan diferente en H2

**Decisión de revisión:** Cuando Docker esté disponible, migrar a
Testcontainers + Oracle XE para tests de integración. El código de dominio
y servicio no requiere cambios — solo la configuración de tests.