# ADR-002 — Arquitectura hexagonal para identity-core

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado               |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

`identity-core` es un microservicio de identidad digital que debe:

- Persistir en Oracle en producción y H2 en desarrollo (sin Docker disponible)
- Exponerse via REST pero potencialmente también via gRPC o mensajería en el futuro
- Ser testeable con cobertura alta sin depender de infraestructura real
- Evolucionar independientemente de decisiones de framework (Quarkus, JPA, etc.)

La elección de arquitectura interna determina directamente la testabilidad,
la mantenibilidad y el coste de cambiar tecnologías de infraestructura.

---

## Decisión

**Arquitectura hexagonal** (Ports & Adapters, Alistair Cockburn 2005).

El dominio define interfaces (ports). La infraestructura implementa esas
interfaces (adapters). El dominio nunca importa clases de infraestructura.

```
domain/
  model/        — aggregates, value objects
  exception/    — excepciones de dominio
  port/in/      — casos de uso (interfaces de entrada)
  port/out/     — repositorios (interfaces de salida)
  service/      — orquestación de dominio

infrastructure/
  persistence/  — adapter JPA (implementa port/out)
  rest/         — adapter REST (implementa port/in via HTTP)
  health/       — adapter de observabilidad
```

---

## Alternativas evaluadas

### Alternativa A — MVC en capas (Controller → Service → Repository)

El patrón estándar de Spring/Quarkus con anotaciones directamente en los
servicios de negocio.

**Ventajas:** Familiaridad, menos boilerplate, generación automática de código.

**Desventajas:**
- El servicio de negocio importa `@Entity`, `EntityManager`, `@Transactional`
  — el dominio queda contaminado por JPA
- Testear el servicio requiere levantar el contenedor JPA o mockear EntityManager
- Cambiar de JPA a JDBC o a un cliente HTTP requiere modificar el dominio

**Resultado: descartada** — la contaminación del dominio con dependencias de
infraestructura hace inviable alcanzar cobertura alta sin levantar el contenedor.

### Alternativa B — Clean Architecture (Uncle Bob)

Capas concéntricas: Entities → Use Cases → Interface Adapters → Frameworks.

**Ventajas:** Separación estricta, independencia de frameworks.

**Desventajas:**
- Más capas y más clases que hexagonal para el mismo resultado
- La nomenclatura (Interactors, Presenters, Gateways) es menos estándar
  en el ecosistema Java/Quarkus

**Resultado: descartada** — hexagonal logra la misma separación con menos
ceremonial y es más reconocible para equipos Java.

---

## Justificación

**Testabilidad:** El dominio no tiene dependencias de framework. `PersonService`
se puede instanciar en un test unitario con un `InMemoryPersonRepository` fake
sin levantar Quarkus ni H2.

**Sustituibilidad de adaptadores:** El repositorio Oracle en producción y el
repositorio H2 en desarrollo implementan el mismo port `PersonRepository`.
Cambiar de Oracle a PostgreSQL requiere solo un nuevo adapter, sin tocar el dominio.

**Aislamiento de decisiones:** La decisión de usar JPA, de exponer REST o gRPC,
de usar Flyway o Liquibase, son todas decisiones de la capa de infraestructura.
El dominio no sabe nada de ellas.

---

## Consecuencias

**Positivas:**
- 100% de cobertura en el dominio sin contenedor Quarkus
- Mutation coverage 96% (techo real) — los tests ejercitan comportamiento real
- Los adapters son intercambiables sin modificar el dominio

**Negativas:**
- Más interfaces y más clases que MVC clásico
- Los nuevos miembros del equipo necesitan entender el patrón antes de contribuir
- Quarkus genera algunos adapters automáticamente (REST, JPA) que hexagonal
  requiere escribir manualmente