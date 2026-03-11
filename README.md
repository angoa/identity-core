# identity-core

![Build](https://img.shields.io/github/actions/workflow/status/angoa/identity-core/ci.yml?branch=main&label=build)
![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)
![Tests](https://img.shields.io/badge/tests-230%2B-brightgreen)
![Java](https://img.shields.io/badge/java-21-blue)
![Quarkus](https://img.shields.io/badge/quarkus-3.32.2-purple)
![License](https://img.shields.io/badge/license-proprietary-lightgrey)

Microservicio de identidad digital construido con **Quarkus 3.32.2**, **Java 21** y **arquitectura hexagonal**. Expone un API REST para el registro y gestión del ciclo de vida de personas, con observabilidad completa y preparado para despliegue en Kubernetes y OpenShift.

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
├── domain/                         # Núcleo — sin dependencias de frameworks
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
    │  @Valid, mapeo DTO → Command
    ▼
PersonService           (domain service)
    │  orquesta, emite métricas y trazas
    ▼
PersonRepository        (port — domain)
    │
    ▼
PersonRepositoryImpl    (JPA adapter — infrastructure)
    │  @Timeout + @CircuitBreaker + @Retry
    ▼
Oracle / H2
```

---

## Requisitos

| Herramienta | Versión mínima | Notas |
|---|---|---|
| Java | 21 | JAVA_HOME configurado |
| Maven | 3.9+ | o usar `./mvnw` incluido en el repo |
| Docker / Podman | cualquiera reciente | solo para build de imagen |
| Mandrel / GraalVM | jdk-21 | **solo** para build nativo — vive en el builder Docker, no se requiere local |

> **Base de datos:** H2 en memoria en desarrollo y tests. Oracle en producción — configurar via variables de entorno.

---

## Setup local

```bash
# Clonar
git clone https://github.com/angoa/identity-core.git
cd identity-core

# Compilar y ejecutar en modo dev (hot reload)
./mvnw quarkus:dev

# El API queda disponible en:
#   http://localhost:8080/persons
#   http://localhost:8080/q/swagger-ui
#   http://localhost:8080/q/health
```

> En modo `dev`, Quarkus levanta H2 en memoria y ejecuta las migraciones Flyway automáticamente.

---

## Ejecutar tests

```bash
# Tests + cobertura JaCoCo
./mvnw verify

# Reporte de cobertura
# Windows:  start target\site\jacoco\index.html
# macOS:    open target/site/jacoco/index.html

# Mutation testing (PIT) — tarda ~5 minutos
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

**Métricas actuales:**

| Métrica | Valor |
|---|---|
| Tests | 230+ |
| Cobertura instrucciones | 100% |
| Cobertura branches | 100% |
| Mutation coverage | 96% (techo alcanzable) |

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
# El build nativo ocurre dentro del contenedor builder con Mandrel.
# No se requiere GraalVM instalado localmente.

docker build -f src/main/docker/Dockerfile.native -t identity-core:native .
# o con Podman:
podman build -f src/main/docker/Dockerfile.native -t identity-core:native .

# Ejecutar localmente
docker run -p 8080:8080 identity-core:native
```

> El build nativo tarda ~10-15 minutos la primera vez. Las rebuilds incrementales son más rápidas gracias al cache de capas de dependencias Maven.

**Comparación de imágenes:**

| | JVM | Native |
|---|---|---|
| Imagen base | `ubi9/openjdk-21-runtime:1.24` | `ubi9/ubi-micro:9.4` |
| Tamaño final aprox. | ~280MB | ~65MB |
| Tiempo de arranque | ~3-5s | <100ms |
| Memoria RSS en reposo | ~150MB | ~50MB |

---

## Despliegue en Kubernetes

```bash
# Aplicar todos los manifiestos
kubectl apply -f k8s/deployment.yaml

# Verificar pods
kubectl -n identity-core get pods -w

# Ver logs
kubectl -n identity-core logs -l app=identity-core -f
```

Los manifiestos incluyen: `Namespace`, `ServiceAccount`, `Secret` (placeholder), `ConfigMap`, `Deployment`, `Service`, `HorizontalPodAutoscaler` e `Ingress`.

> **Antes del apply:** reemplazar las credenciales Oracle en el `Secret` y la URL de la imagen en el `Deployment`. En producción gestionar el Secret via Vault o External Secrets Operator — nunca commitear credenciales reales.

**Hardening aplicado:**

- `runAsNonRoot: true` + `runAsUser: 1001`
- `allowPrivilegeEscalation: false`
- `readOnlyRootFilesystem: true`
- `capabilities.drop: ALL`
- `seccompProfile: RuntimeDefault`
- `automountServiceAccountToken: false`
- Namespace con Pod Security Admission `restricted`
- `topologySpreadConstraints` — distribuye réplicas en nodos distintos

---

## API — Endpoints

Base URL: `http://localhost:8080`

Documentación interactiva: [`/q/swagger-ui`](http://localhost:8080/q/swagger-ui)

### Registrar persona

```bash
POST /persons
Content-Type: application/json

{
  "curp": "MOAA850615HDFRZS09",
  "firstName": "Rafael",
  "lastName": "Morales",
  "secondLastName": "Angoa",
  "birthDate": "1985-06-15"
}

# Respuesta 201 Created
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
GET /persons/{id}

# 200 OK — PersonResponse
# 400 Bad Request — UUID con formato inválido
# 404 Not Found
```

### Buscar por CURP

```bash
GET /persons/curp/{curp}

# 200 OK — PersonResponse
# 404 Not Found
```

### Suspender persona

```bash
PATCH /persons/{id}/suspend
Content-Type: application/json

{"reason": "Documentación vencida"}

# 200 OK — StatusResponse
# 404 Not Found
# 409 Conflict — ya está suspendida
```

### Reactivar persona

```bash
PATCH /persons/{id}/reactivate
Content-Type: application/json

{"reason": "Documentación renovada"}

# 200 OK — StatusResponse
# 404 Not Found
# 409 Conflict — ya está activa
```

### Dar de baja

```bash
PATCH /persons/{id}/deactivate
Content-Type: application/json

{"reason": "Solicitud del titular"}

# 200 OK — StatusResponse
# 404 Not Found
# 409 Conflict — ya está inactiva
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

### Base de datos

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

### Observabilidad

| Variable | Descripción | Default |
|---|---|---|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTel Collector gRPC | `http://localhost:4317` |
| `OTEL_SERVICE_NAME` | Nombre del servicio en trazas | `identity-core` |
| `QUARKUS_LOG_LEVEL` | Nivel de log global | `INFO` |

### Fault tolerance (repositorio)

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
| `/q/health/ready` | Readiness | BD disponible y lista |
| `/q/health/started` | Startup | Arranque completado |

### Métricas (Micrometer + Prometheus)

```bash
GET /q/metrics
```

Métricas custom expuestas por `PersonService`:

- `persons.registered.total` — contador de registros exitosos
- `persons.registration.errors.total` — contador de errores de registro
- `persons.status.changes.total` — contador de cambios de estado
- `persons.registration.duration` — timer de duración del registro

### Trazas distribuidas (OpenTelemetry)

El servicio exporta trazas vía OTLP/gRPC al endpoint configurado en `OTEL_EXPORTER_OTLP_ENDPOINT`. Los spans incluyen `traceId` y `spanId` en todos los logs estructurados.

Stack de observabilidad recomendado:

```
identity-core → Prometheus     → Grafana  (métricas)
identity-core → Loki           → Grafana  (logs)
identity-core → OTel Collector → Jaeger   (trazas)
```

---

## Decisiones de arquitectura (ADRs)

| ADR | Decisión | Estado |
|---|---|---|
| [ADR-001](docs/adr/ADR-001-imagen-base-contenedor.md) | Imagen base UBI9 sobre Distroless/Alpine | Aceptado |

> Los ADRs documentan el contexto, alternativas evaluadas y justificación de las decisiones técnicas relevantes del proyecto.

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
│   │   │   └── db/migration/        # Flyway migrations
│   │   └── docker/
│   │       ├── Dockerfile.jvm
│   │       └── Dockerfile.native
│   └── test/
│       └── java/io/serverus/
├── k8s/
│   └── deployment.yaml
├── docs/
│   └── adr/
│       └── ADR-001-imagen-base-contenedor.md
├── .dockerignore
├── pom.xml
└── README.md
```