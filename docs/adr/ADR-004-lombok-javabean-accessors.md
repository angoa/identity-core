# ADR-004 — Lombok con JavaBean accessors sobre fluent accessors

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado               |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

El proyecto usa Lombok para reducir boilerplate en clases de dominio y entidades
JPA. Lombok ofrece dos estilos de accessors:

- **JavaBean style:** `getId()`, `getName()`, `setName(value)` — convención estándar Java
- **Fluent style:** `id()`, `name()`, `name(value)` — más compacto, estilo moderno

La elección afecta la compatibilidad con tres frameworks usados en el proyecto:
Jackson (serialización JSON), JPA/Hibernate (persistencia), y CDI (inyección
de dependencias de Quarkus).

---

## Decisión

**JavaBean accessors** — `@Getter` con convención `getX()` / `isX()`.

Configuración en `lombok.config` en la raíz del proyecto:

```properties
lombok.accessors.fluent=false
lombok.accessors.chain=false
```

---

## Alternativas evaluadas

### Alternativa A — Fluent accessors (`lombok.accessors.fluent=true`)

```java
person.id()           // en lugar de person.getId()
person.name("Rafael") // en lugar de person.setName("Rafael")
```

**Ventajas:**
- Código más compacto y legible para desarrolladores familiarizados con el estilo
- Menos verbosidad en el código de dominio

**Desventajas — críticas:**

1. **Jackson:** El `ObjectMapper` de Jackson descubre propiedades buscando métodos
   con prefijo `get`/`is`. Con fluent accessors, `id()` no es reconocido como
   propiedad JSON — la serialización produce `{}` en lugar del objeto completo.
   Requiere `@JsonProperty` en cada campo o configuración global de `MapperFeature`.

2. **JPA/Hibernate:** El spec de JPA requiere JavaBean accessors para el acceso
   por propiedad (`@Access(AccessType.PROPERTY)`). Con fluent accessors, Hibernate
   no puede detectar los campos para lazy loading y proxy generation — falla en
   runtime con `HibernateException: could not find getter`.

3. **CDI Quarkus:** Los interceptores de CDI (usados por `@WithSpan`, `@Timeout`,
   `@CircuitBreaker`) generan proxies que extienden la clase target. Los proxies
   CDI requieren el patrón JavaBean para la delegación de métodos.

**Resultado: descartada** — la combinación de Jackson + JPA + CDI hace que los
fluent accessors requieran configuración adicional no trivial en cada uno de los
tres frameworks. El coste supera el beneficio de legibilidad.

### Alternativa B — Records Java 21

```java
public record Person(PersonId id, Curp curp, FullName fullName) {}
```

**Ventajas:**
- Inmutabilidad garantizada por el compilador
- Accessors automáticos con nombre del campo (sin prefijo `get`)
- `equals()`, `hashCode()`, `toString()` generados automáticamente

**Desventajas para aggregates con estado mutable:**
- Los records son inmutables — no pueden representar un aggregate con ciclo de
  vida (ACTIVE → SUSPENDED → INACTIVE) sin crear nuevas instancias en cada transición
- JPA requiere constructor sin argumentos y campos mutables para el merge de entidades
- Los records no pueden ser extendidos — los proxies CDI no pueden generarse

**Resultado: records se usan para Value Objects** (Curp, FullName, BirthDate, PersonId,
PersonStatus) donde la inmutabilidad es la propiedad correcta. El aggregate `Person`
usa clase Lombok con JavaBean accessors porque necesita estado mutable.

---

## Justificación

JavaBean accessors son la convención estándar del ecosistema Java desde 1997.
Jackson, JPA, CDI, Bean Validation, y cualquier framework de reflexión los
espera por defecto — sin configuración adicional.

El coste de adoptar fluent accessors es la configuración explícita en cada
framework + la sorpresa para desarrolladores que ven `person.id()` y no saben
si están llamando un método de negocio o un accessor.

---

## Consecuencias

**Positivas:**
- Jackson serializa/deserializa `PersonEntity` y DTOs sin configuración adicional
- Hibernate detecta propiedades para proxy generation sin `@Access` explícito
- Los interceptores CDI (`@WithSpan`, `@Timeout`) funcionan sin configuración de proxy
- Código reconocible por cualquier desarrollador Java

**Negativas:**
- Más verbosidad: `person.getId()` en lugar de `person.id()`
- Los Value Objects como records usan `curp.value()` (accessor de record) mientras
  que el aggregate usa `person.getCurp()` — inconsistencia menor entre los dos estilos