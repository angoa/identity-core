# ADR-006 — application.yaml sobre application.properties

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado               |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

Quarkus soporta dos formatos de configuración: `application.properties`
(formato clave=valor plano) y `application.yaml` (formato YAML jerárquico,
requiere extensión `quarkus-config-yaml`).

El proyecto tiene una configuración con múltiples dimensiones:
- Perfiles (`%dev`, `%test`, `%prod`)
- Subsistemas (datasource, ORM, Flyway, OTel, logging, Swagger)
- Fault tolerance por método y por clase (`mp.fault-tolerance.*`)

---

## Decisión

**`application.yaml`** como formato principal de configuración.

Dependencia añadida al `pom.xml`:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-config-yaml</artifactId>
</dependency>
```

---

## Alternativas evaluadas

### Alternativa A — `application.properties`

Formato nativo de Quarkus — no requiere dependencia adicional.

**Ventajas:**
- Sin dependencia extra
- Formato conocido por todos los desarrolladores Java/Quarkus
- Soporte nativo sin configuración

**Desventajas:**
- La configuración de fault tolerance por método genera claves muy largas
  y repetitivas:

```properties
"io.serverus.infrastructure.persistence.repository.PersonRepositoryImpl/findById/Timeout/value"=2000
"io.serverus.infrastructure.persistence.repository.PersonRepositoryImpl/findByCurp/Timeout/value"=2000
"io.serverus.infrastructure.persistence.repository.PersonRepositoryImpl/existsByCurp/Timeout/value"=2000
```

- Los perfiles anidados (`%prod.quarkus.otel.exporter.otlp.traces.endpoint`)
  generan claves largas difíciles de leer
- La jerarquía de configuración no es visible sin leer el nombre completo de la clave

**Resultado: descartada** — la configuración de fault tolerance con 5 métodos
y 3 políticas por método genera ~75 líneas de claves repetitivas en `.properties`.
En YAML la misma configuración es una sección jerárquica legible.

---

## Justificación

YAML permite expresar la jerarquía de configuración de forma natural:

```yaml
"io.serverus.infrastructure.persistence.repository.PersonRepositoryImpl":
  findById/Timeout/value: 2000
  findById/Retry/maxRetries: 2
  findByCurp/Timeout/value: 2000
  findByCurp/Retry/maxRetries: 2
```

Los perfiles de Quarkus en YAML usan la sintaxis `"%prod":` como clave de
nivel superior, lo que permite agrupar toda la configuración de un perfil
en un bloque contiguo y legible.

La dependencia `quarkus-config-yaml` es un coste mínimo (sin overhead en
runtime — el YAML se parsea en tiempo de arranque) por un beneficio claro
en legibilidad y mantenibilidad de la configuración.

---

## Consecuencias

**Positivas:**
- La configuración de fault tolerance por método es legible y mantenible
- Los perfiles de entorno (`%dev`, `%test`, `%prod`) están agrupados
  visualmente
- La jerarquía de propiedades Quarkus (`quarkus.datasource.jdbc.url`) es
  evidente en la estructura anidada

**Negativas:**
- Requiere `quarkus-config-yaml` como dependencia adicional
- YAML es sensible a la indentación — errores de indentación producen
  fallos de arranque difíciles de diagnosticar
- Los desarrolladores acostumbrados a `.properties` necesitan familiarizarse
  con la sintaxis de perfiles YAML de Quarkus (`"%prod":` con comillas)