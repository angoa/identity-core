# ADR-003 — Quarkus sobre Spring Boot como framework principal

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado               |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

El equipo necesita elegir un framework Java para un microservicio de identidad
digital que se desplegará en OpenShift y Kubernetes. Los criterios principales son:

- Arranque rápido para pods efímeros y autoescalado horizontal
- Bajo consumo de memoria para densidad de pods en el cluster
- Soporte nativo de MicroProfile (Health, Metrics, OpenAPI, Fault Tolerance, OpenTelemetry)
- Certificación Red Hat para entornos OpenShift corporativos
- Compatibilidad con compilación nativa (GraalVM/Mandrel) para producción

---

## Decisión

**Quarkus 3.32.2** con Java 21 como framework principal.

---

## Alternativas evaluadas

### Alternativa A — Spring Boot 3.x

El framework más utilizado en el ecosistema Java empresarial.

**Ventajas:**
- Mayor adopción en el mercado — más desarrolladores familiarizados
- Ecosistema más amplio de integraciones y starters
- Spring Native (GraalVM) maduro en Spring Boot 3.x
- Mayor cantidad de documentación y ejemplos disponibles

**Desventajas:**
- Tiempo de arranque ~3-8s en JVM vs ~0.5-1s de Quarkus en JVM
- Consumo de memoria mayor en reposo — Spring carga todo el contexto al arrancar
- MicroProfile no es el modelo nativo de Spring (usa Actuator, Micrometer, etc.)
  — las integraciones son equivalentes pero con APIs diferentes
- La certificación Red Hat para OpenShift prioriza Quarkus como framework oficial

**Resultado: descartada** — el perfil de despliegue en OpenShift con arranque
rápido y bajo footprint favorece Quarkus. La certificación Red Hat es un
requisito del entorno corporativo.

### Alternativa B — Micronaut

Framework cloud-native con compilación AOT y arranque rápido comparable a Quarkus.

**Ventajas:**
- Arranque muy rápido, similar a Quarkus
- Inyección de dependencias AOT sin reflexión

**Desventajas:**
- Menor adopción en el mercado que Spring Boot y Quarkus
- Sin certificación Red Hat para OpenShift
- Ecosistema de extensiones más pequeño
- El soporte de MicroProfile es parcial

**Resultado: descartada** — menor ecosistema y ausencia de certificación Red Hat.

### Alternativa C — Helidon (Oracle)

Framework MicroProfile nativo de Oracle, especialmente relevante dado que
el proyecto usa Oracle como BD de producción.

**Ventajas:**
- Implementación de referencia de MicroProfile
- Soporte oficial de Oracle
- Helidon Níma (virtual threads) muy eficiente para I/O intensivo

**Desventajas:**
- Menor adopción que Quarkus en el ecosistema corporativo
- Sin certificación Red Hat
- Tooling de desarrollo menos maduro (sin dev mode equivalente a Quarkus)

**Resultado: descartada** — Quarkus tiene mejor integración con Oracle
(extensión JDBC Oracle oficial) con mayor adopción y mejor tooling.

---

## Justificación

Quarkus cumple todos los criterios:

- **Arranque JVM:** ~500ms-1s vs ~5s de Spring Boot
- **Arranque Native:** <100ms con Mandrel
- **MicroProfile nativo:** Health, Metrics, OpenAPI, Fault Tolerance, JWT
  son extensiones de primera clase en Quarkus
- **Certificación Red Hat:** Quarkus es el framework oficial de Red Hat
  para OpenShift — soporte comercial disponible
- **Dev experience:** `quarkus:dev` con hot reload y Dev UI acelera el
  ciclo de desarrollo sin levantar contenedores adicionales

---

## Consecuencias

**Positivas:**
- Imagen nativa ~65MB vs ~280MB JVM — reduce costes de registry y transferencia
- Tiempo de arranque <100ms — pods listos antes de que el readinessProbe falle
- MicroProfile Fault Tolerance con `@Timeout`, `@CircuitBreaker`, `@Retry`
  disponible como extensión oficial sin dependencias adicionales

**Negativas:**
- La curva de aprendizaje de Quarkus es diferente a Spring Boot
- Algunos patrones de Spring (ApplicationContext, BeanFactory) no existen en Quarkus
- La compilación nativa con Mandrel requiere configuración adicional para
  reflexión, recursos y proxies dinámicos