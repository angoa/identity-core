# identity-core

[![Build](https://img.shields.io/github/actions/workflow/status/angoa/identity-core/ci.yml?branch=main&label=build)](https://github.com/angoa/identity-core/actions)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)](#ejecutar-tests)
[![Tests](https://img.shields.io/badge/tests-230%2B-brightgreen)](#ejecutar-tests)
[![Java](https://img.shields.io/badge/java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Quarkus](https://img.shields.io/badge/quarkus-3.32.2-purple)](https://quarkus.io)
[![License](https://img.shields.io/badge/license-proprietary-lightgrey)](#)

Microservicio de identidad digital construido con **Quarkus 3.32.2**, **Java 21** y **arquitectura hexagonal**. Expone un API REST para el registro y gestión del ciclo de vida de personas, con observabilidad completa y preparado para despliegue en Kubernetes y OpenShift.

---

## Características

- Arquitectura hexagonal (Ports & Adapters) — dominio desacoplado de infraestructura
- Quarkus 3.32.2 sobre Java 21 con compilación JVM y Native (Mandrel)
- Persistencia JPA con Oracle en producción, H2 en desarrollo
- Migraciones automáticas con Flyway
- Health checks SmallRye (`/q/health/live`, `/q/health/ready`, `/q/health/started`)
- Métricas custom con Micrometer + Prometheus (`/q/metrics`)
- Trazas distribuidas con OpenTelemetry OTLP/gRPC
- Resiliencia: `@Timeout` + `@CircuitBreaker` + `@Retry` (MicroProfile Fault Tolerance)
- Imágenes UBI9 hardened — `ubi9/openjdk-21-runtime` (JVM) y `ubi9/ubi-micro` (Native)
- Despliegue Kubernetes / OpenShift con Pod Security Standards: **Restricted**
- Cobertura 100% instrucciones y branches · Mutation coverage 96%

---

## Tabla de contenidos

- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Setup local](#setup-local)
- [Ejecutar tests](#ejecutar-tests)
- [Build de imagen](#build-de-imagen)
- [Despliegue en Kubernetes](#despliegue-en-kubernetes)
- [API — Endpoints](#api--endpoints)
- [Variables de entorno](#variables-de-entorno)
- [Observabilidad](#observabilidad)
- [Decisiones de arquitectura (ADRs)](#decisiones-de-arquitectura-adrs)

---

## Arquitectura

```
identity-core/
│
├── domain/                         # Núcleo — sin dependencias de framework
│   ├── model/                      # Aggregate Person + Value Objects
│   │   └── vo/                     # Curp, FullName, BirthDate, PersonId, PersonStatus
│   ├── exception/                  # DomainException, ValidationException (422),
│   │                               # BusinessRuleException (409), ResourceNotFoundException (404)
│   ├── port/
│   │   ├── in/                     # RegisterPersonUseCase (entrada)
│   │   └── out/                    # PersonRepository (salida)
│   └── service/                    # PersonService — orquesta casos de uso
│
└── infrastructure/                 # Adaptadores — implementan los ports
    ├── persistence/                # PersonRepositoryImpl (JPA + Oracle)
    │   ├── entity/                 # PersonEntity
    │   └── mapper/                 # PersonMapper
    ├── rest/                       # PersonResource — 6 endpoints REST
    │   └── dto/                    # PersonRequest/Response, StatusRequest/Response, ErrorResponse
    ├── health/                     # PersonRepositoryHealthCheck (@Readiness)
    └── config/                     # Configuración de infraestructura
```

### Flujo de una petición

```
HTTP Request
    │
    ▼
PersonResource          (REST adapter — infrastructure)
    │  @Valid + mapeo DTO → Command
    ▼
PersonService           (domain service)
    │  orquesta · emite métricas · genera trazas (@WithSpan)
    ▼
PersonRepository        (port — domain interface)
    │
    ▼
PersonRepositoryImpl    (JPA adapter — infrastructure)
    │  @Timeout + @CircuitBreaker + @Retry
    ▼
Oracle (prod) / H2 (dev)
```

---

## Requisitos

| Herramienta | Versión mínima | Notas |
|---|---|---|
| Java | 21 | `JAVA_HOME` configurado |
| Maven | 3.9+ | o usar `./mvnw` incluido en el repo |
| Docker / Podman | cualquiera reciente | solo para build de imagen |
| Mandrel / GraalVM | jdk-21 | **solo** para build nativo — vive en el builder Docker, no se requiere local |

```bash
# Verificar Java
java -version   # debe mostrar 21

# Si JAVA_HOME no está configurado (Windows):
set JAVA_HOME=C:\Program Files\Java\jdk-21
```

> **Base de datos:** H2 en memoria en desarrollo y tests. Oracle en producción — configurar via variables de entorno.

---

## Setup local

```bash
# Clonar
git clone https://github.com/angoa/identity-core.git
cd identity-core

# Compilar y ejecutar en modo dev (hot reload)
./mvnw quarkus:dev
```

El API queda disponible en:

| URL | Descripción |
|---|---|
| `http://localhost:8080/persons` | API REST |
| `http://localhost:8080/q/swagger-ui` | Documentación interactiva |
| `http://localhost:8080/q/health` | Health checks |
| `http://localhost:8080/q/metrics` | Métricas Prometheus |

> En modo `dev`, Quarkus levanta H2 en memoria y ejecuta las migraciones Flyway automáticamente. No se requiere base de datos externa.

---

## Ejecutar tests

```bash
# Tests + cobertura JaCoCo
./mvnw verify

# Abrir reporte de cobertura
# Windows:  start target\site\jacoco\index.html
# macOS:    open target/site/jacoco/index.html

# Mutation testing (PIT) — tarda ~5 minutos
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

**Métricas actuales:**

| Métrica | Valor | Referencia |
|---|---|---|
| Tests | 230+ | — |
| Cobertura instrucciones | 100% | — |
| Cobertura branches | 100% | — |
| Mutation coverage | 96% | >90% se considera nivel alto |
| Mutation strength | 97% | — |

> El mutation coverage del 96% es el techo real del proyecto — las 4 mutaciones restantes son equivalentes estructurales en los `null checks` de los Value Objects.

---

## Build de imagen

### JVM — desarrollo, staging, debugging

```bash
# 1. Compilar el artefacto
./mvnw package -DskipTests

# 2. Construir imagen
docker build -f src/main/docker/Dockerfile.jvm -t identity-core:jvm .
# o con Podman:
podman build -f src/main/docker/Dockerfile.jvm -t identity-core:jvm .

# 3. Ejecutar localmente
docker run -p 8080:8080 identity-core:jvm
```

### Native — producción

```bash
# Mandrel vive dentro del builder — no se requiere instalación local.
docker build -f src/main/docker/Dockerfile.native -t identity-core:native .

# Ejecutar localmente
docker run -p 8080:8080 identity-core:native
```

> El build nativo tarda ~10-15 minutos la primera vez. Las rebuilds incrementales son más rápidas gracias al cache de capas de dependencias Maven.

**Comparación:**

| Característica | JVM | Native |
|---|---|---|
| Imagen base | `ubi9/openjdk-21-runtime:1.24` | `ubi9/ubi-micro:9.4` |
| Tamaño final aprox. | ~280MB | ~65MB |
| Tiempo de arranque | ~3-5s | <100ms |
| Memoria RSS en reposo | ~150MB | ~50MB |
| Uso recomendado | Dev / staging / debug | Producción |

---

## Despliegue en Kubernetes

```bash
# Aplicar todos los manifiestos
kubectl apply -f k8s/deployment.yaml

# Verificar pods
kubectl -n identity-core get pods -w

# Verificar servicio
kubectl -n identity-core get svc

# Ver logs
kubectl -n identity-core logs -l app=identity-core -f
```

Los manifiestos incluyen: `Namespace`, `ServiceAccount`, `Secret` (placeholder),
`ConfigMap`, `Deployment`, `Service`, `HorizontalPodAutoscaler` e `Ingress`.

> **Antes del apply:** reemplazar las credenciales Oracle en el `Secret` y la URL
> de la imagen en el `Deployment`. En producción gestionar el Secret via Vault o
> External Secrets Operator — nunca commitear credenciales reales.

**Hardening aplicado — cumple Pod Security Standards: Restricted:**

- `runAsNonRoot: true` + `runAsUser: 1001`
- `allowPrivilegeEscalation: false`
- `readOnlyRootFilesystem: true`
- `capabilities.drop: ALL`
- `seccompProfile: RuntimeDefault`
- `automountServiceAccountToken: false`
- Namespace con Pod Security Admission `restricted` (enforce + audit + warn)
- `topologySpreadConstraints` — réplicas distribuidas en nodos distintos
- `terminationGracePeriodSeconds: 30` — graceful shutdown de Quarkus

---

## API — Endpoints

Base URL: `http://localhost:8080`

Documentación interactiva: [`/q/swagger-ui`](http://localhost:8080/q/swagger-ui)

### Registrar persona

```bash
curl -X POST http://localhost:8080/persons \
  -H "Content-Type: application/json" \
  -d '{
    "curp": "MOAA850615HDFRZS09",
    "firstName": "Rafael",
    "lastName": "Morales",
    "secondLastName": "Angoa",
    "birthDate": "1985-06-15"
  }'

# 201 Created
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "curp": "MOAA850615HDFRZS09",
  "fullName": "Rafael Morales Angoa",
  "birthDate": "1985-06-15",
  "status": "ACTIVE"
}
```

### Buscar por ID

```bash
curl http://localhost:8080/persons/550e8400-e29b-41d4-a716-446655440000

# 200 OK · 400 UUID inválido · 404 Not Found
```

### Buscar por CURP

```bash
curl http://localhost:8080/persons/curp/MOAA850615HDFRZS09

# 200 OK · 404 Not Found
```

### Suspender persona

```bash
curl -X PATCH http://localhost:8080/persons/550e8400-e29b-41d4-a716-446655440000/suspend \
  -H "Content-Type: application/json" \
  -d '{"reason": "Documentación vencida"}'

# 200 OK · 404 Not Found · 409 Conflict (ya suspendida)
```

### Reactivar persona

```bash
curl -X PATCH http://localhost:8080/persons/550e8400-e29b-41d4-a716-446655440000/reactivate \
  -H "Content-Type: application/json" \
  -d '{"reason": "Documentación renovada"}'

# 200 OK · 404 Not Found · 409 Conflict (ya activa)
```

### Dar de baja

```bash
curl -X PATCH http://localhost:8080/persons/550e8400-e29b-41d4-a716-446655440000/deactivate \
  -H "Content-Type: application/json" \
  -d '{"reason": "Solicitud del titular"}'

# 200 OK · 404 Not Found · 409 Conflict (ya inactiva)
```

### Estructura de error

Todos los errores retornan:

```json
{
  "errorCode": "PERSON_NOT_FOUND",
  "message": "Person with id 550e8400... not found",
  "field": null,
  "timestamp": "2026-03-11T10:30:00",
  "path": "/persons/550e8400..."
}
```

---

## Variables de entorno

### Base de datos — **obligatorias en producción**

| Variable | Descripción | Default (dev) |
|---|---|---|
| `QUARKUS_DATASOURCE_DB_KIND` | Tipo de BD | `h2` |
| `QUARKUS_DATASOURCE_JDBC_URL` | URL JDBC | H2 en memoria |
| `QUARKUS_DATASOURCE_USERNAME` | Usuario | `identity` |
| `QUARKUS_DATASOURCE_PASSWORD` | Password | `identity_pass` |

Ejemplo producción Oracle:

```bash
QUARKUS_DATASOURCE_DB_KIND=oracle
QUARKUS_DATASOURCE_JDBC_URL=jdbc:oracle:thin:@oracle-svc:1521/identitydb
QUARKUS_DATASOURCE_USERNAME=identity_user
QUARKUS_DATASOURCE_PASSWORD=<secret>
```

### Observabilidad — **obligatorias en producción**

| Variable | Descripción | Default |
|---|---|---|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTel Collector gRPC | `http://localhost:4317` |
| `OTEL_SERVICE_NAME` | Nombre del servicio en trazas | `identity-core` |
| `QUARKUS_LOG_LEVEL` | Nivel de log global | `INFO` |

### Fault tolerance — opcionales (tienen defaults razonables)

| Variable | Descripción | Default |
|---|---|---|
| `REPO_READ_TIMEOUT_MS` | Timeout lecturas Oracle (ms) | `2000` |
| `REPO_WRITE_TIMEOUT_MS` | Timeout escrituras Oracle (ms) | `5000` |
| `REPO_CB_DELAY_MS` | Delay half-open del circuit breaker (ms) | `30000` |

---

## Observabilidad

### Health checks

| Endpoint | Tipo | Descripción |
|---|---|---|
| `/q/health/live` | Liveness | El proceso está vivo |
| `/q/health/ready` | Readiness | BD disponible y lista para tráfico |
| `/q/health/started` | Startup | Arranque completado (Flyway incluido) |

### Métricas (Micrometer + Prometheus)

```bash
curl http://localhost:8080/q/metrics
```

Métricas custom expuestas por `PersonService`:

| Métrica | Tipo | Descripción |
|---|---|---|
| `persons.registered.total` | Counter | Registros exitosos |
| `persons.registration.errors.total` | Counter | Errores de registro |
| `persons.status.changes.total` | Counter | Cambios de estado |
| `persons.registration.duration` | Timer | Duración del registro |

### Trazas distribuidas (OpenTelemetry)

El servicio exporta trazas vía OTLP/gRPC. Los spans incluyen `traceId` y
`spanId` en todos los logs estructurados para correlación en Grafana.

### Stack recomendado

```
identity-core /q/metrics  →  Prometheus      →  Grafana  (métricas)
identity-core stdout       →  Fluentd / Loki →  Grafana  (logs)
identity-core OTLP :4317   →  OTel Collector →  Jaeger   (trazas)
```

---

## Decisiones de arquitectura (ADRs)

Los ADRs documentan el contexto, alternativas evaluadas y justificación de las
decisiones técnicas del proyecto. Se encuentran en `docs/adr/`.

| ADR | Decisión | Estado |
|---|---|---|
| [ADR-001](docs/adr/ADR-001-imagen-base-contenedor.md) | Imagen base UBI9 sobre Distroless/Alpine | Aceptado |
| [ADR-002](docs/adr/ADR-002-arquitectura-hexagonal.md) | Arquitectura hexagonal sobre MVC en capas | Aceptado |
| [ADR-003](docs/adr/ADR-003-quarkus-sobre-spring-boot.md) | Quarkus sobre Spring Boot y Micronaut | Aceptado |
| [ADR-004](docs/adr/ADR-004-lombok-javabean-accessors.md) | Lombok JavaBean accessors sobre fluent | Aceptado |
| [ADR-005](docs/adr/ADR-005-h2-desarrollo-oracle-produccion.md) | H2 en desarrollo, Oracle en producción | Aceptado (temporal) |
| [ADR-006](docs/adr/ADR-006-application-yaml.md) | `application.yaml` sobre `application.properties` | Aceptado |
| [ADR-007](docs/adr/ADR-007-jacoco-offline-instrumentation.md) | JaCoCo offline instrumentation sobre agente runtime | Aceptado |
| [ADR-008](docs/adr/ADR-008-inmemory-repository-fake.md) | `InMemoryPersonRepository` fake sobre Mockito | Aceptado |
| [ADR-009](docs/adr/ADR-009-opentelemetry-withspan.md) | OpenTelemetry via `@WithSpan`, fuera del dominio | Aceptado |
| [ADR-010](docs/adr/ADR-010-microprofile-fault-tolerance.md) | MicroProfile Fault Tolerance sobre Resilience4j | Aceptado |

---

## Estructura del repositorio

```
identity-core/
├── src/
│   ├── main/
│   │   ├── java/io/serverus/
│   │   │   ├── domain/
│   │   │   └── infrastructure/
│   │   ├── resources/
│   │   │   ├── application.yaml
│   │   │   └── db/migration/           # Flyway migrations
│   │   └── docker/
│   │       ├── Dockerfile.jvm          # JVM — dev / staging
│   │       └── Dockerfile.native       # Native — producción
│   └── test/
│       └── java/io/serverus/
├── k8s/
│   └── deployment.yaml                 # Namespace + SA + CM + Deployment + SVC + HPA + Ingress
├── docs/
│   └── adr/                            # ADR-001 al ADR-010
├── .dockerignore
├── pom.xml
├── mvnw
└── README.md
```