# ADR-009 — OpenTelemetry via @WithSpan, fuera del modelo de dominio

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado               |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

`identity-core` requiere trazabilidad distribuida con OpenTelemetry para
correlacionar peticiones a través de los servicios del sistema. Se necesita
decidir dónde y cómo añadir instrumentación de trazas en el código.

Las opciones son:
1. Instrumentación manual con `Tracer` / `Span` directamente en las clases
2. Instrumentación declarativa con `@WithSpan` (interceptor CDI)
3. Instrumentación automática de Quarkus (solo para frameworks conocidos)

La decisión afecta directamente la pureza del modelo de dominio y la
testabilidad del servicio.

---

## Decisión

**`@WithSpan` como interceptor CDI** en `PersonService` y `PersonRepositoryImpl`.
El modelo de dominio (`Person`, Value Objects, excepciones) no tiene ninguna
referencia a OpenTelemetry.

```java
// PersonService — service layer (application scoped)
@WithSpan("PersonService.execute")
public Person execute(RegisterPersonCommand command) { ... }

// PersonRepositoryImpl — infrastructure adapter
@WithSpan("PersonRepository.findById")
public Optional<Person> findById(PersonId id) { ... }
```

El dominio (`domain/`) no tiene ningún import de `io.opentelemetry.*`.

---

## Alternativas evaluadas

### Alternativa A — Instrumentación manual con `Tracer` en el dominio

```java
// PersonService con Tracer inyectado
@Inject Tracer tracer;

public Person execute(RegisterPersonCommand command) {
    Span span = tracer.spanBuilder("PersonService.execute").startSpan();
    try (Scope scope = span.makeCurrent()) {
        // lógica de negocio
    } finally {
        span.end();
    }
}
```

**Ventajas:**
- Control total sobre el span — nombre, atributos, eventos, errores
- No depende de CDI para la instrumentación

**Desventajas:**
- El dominio importa `io.opentelemetry.api.trace.Tracer` — una dependencia
  de infraestructura de observabilidad en el núcleo de negocio
- Los tests unitarios de `PersonService` deben proveer un `Tracer` — ya sea
  real (OpenTelemetry SDK completo) o un no-op manual
- El boilerplate `try-with-scope` se repite en cada método instrumentado
- Viola la regla de arquitectura hexagonal: el dominio no debe conocer
  detalles de infraestructura

**Resultado: descartada** — introduce una dependencia de infraestructura en
el modelo de dominio, lo que compromete la portabilidad y testabilidad.

### Alternativa B — Instrumentación automática de Quarkus solamente

Quarkus instrumenta automáticamente los endpoints JAX-RS, los métodos JPA
y las llamadas a clientes HTTP sin ninguna anotación adicional.

**Ventajas:**
- Sin código de instrumentación en absoluto
- El dominio permanece completamente limpio

**Desventajas:**
- La instrumentación automática no crea spans para la capa de servicio
  — el trace solo muestra el span del endpoint REST y el span JPA,
  sin el span intermedio de `PersonService`
- No permite añadir atributos de negocio al span (ej. `person.curp`,
  `operation.type`)
- La granularidad es insuficiente para diagnosticar problemas en producción

**Resultado: descartada** — la visibilidad limitada hace inviable el diagnóstico
de problemas de rendimiento en la capa de servicio.

### Alternativa C — `@WithSpan` en service e infrastructure (decisión adoptada)

`@WithSpan` es un interceptor CDI que crea un span automáticamente al entrar
al método y lo cierra al salir. Captura excepciones automáticamente.

**Ventajas:**
- El dominio permanece completamente limpio de dependencias de OTel
- Sin boilerplate `try-with-scope` en el código de producción
- Los tests unitarios de `PersonService` con `InMemoryPersonRepository`
  no necesitan ningún stub de OTel — `@WithSpan` solo actúa cuando CDI
  está activo (en `@QuarkusTest`), no en tests unitarios puros
- Coherente con el patrón de interceptores CDI del proyecto (`@Timeout`,
  `@CircuitBreaker` también son interceptores)

**Desventajas:**
- Menor control sobre el span que la instrumentación manual — añadir
  atributos custom requiere inyectar `Span.current()` en el método
- Depende de CDI para funcionar — no actúa en tests unitarios puros
  (lo cual es una ventaja para la testabilidad pero podría sorprender)

---

## Justificación

La arquitectura hexagonal requiere que el dominio sea independiente de
decisiones de infraestructura. La observabilidad (OTel, Micrometer, logging)
es infraestructura — el dominio no debe saber que está siendo observado.

`@WithSpan` aplica la instrumentación en la capa correcta (service e
infrastructure) sin contaminar el dominio, usando el mismo mecanismo de
interceptores CDI que el resto de las políticas cross-cutting del proyecto.

---

## Consecuencias

**Positivas:**
- El dominio (`domain/`) no tiene imports de `io.opentelemetry.*`
- `PersonService` es testeable con un test unitario puro sin necesidad
  de stubs de OpenTelemetry
- Los spans aparecen automáticamente en Jaeger para todos los métodos
  anotados en service e infrastructure

**Negativas:**
- Para añadir atributos custom al span desde dentro de un método, se
  requiere `Span.current().setAttribute(...)` — introduce una referencia
  puntual a OTel que no es un interceptor
- `@WithSpan` no funciona en métodos privados — la instrumentación se
  limita a métodos públicos de beans CDI