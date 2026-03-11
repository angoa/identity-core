# ADR-007 — JaCoCo offline instrumentation sobre agente en runtime

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado               |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

El proyecto requiere medición de cobertura de código con JaCoCo. Quarkus
utiliza CDI (Contexts and Dependency Injection) que genera proxies dinámicos
en runtime para implementar interceptores (`@Timeout`, `@CircuitBreaker`,
`@WithSpan`, `@Transactional`).

El mecanismo estándar de JaCoCo usa un agente Java (`-javaagent:jacoco.jar`)
que instrumenta las clases en tiempo de carga. Este mecanismo falla con los
proxies CDI de Quarkus porque:

1. Quarkus genera los proxies CDI en tiempo de build (Augmentation Phase),
   no en tiempo de carga de clase
2. El agente instrumenta las clases originales, pero los proxies CDI son
   subclases generadas que delegan al objeto real
3. Los métodos del objeto real se llaman a través del proxy — JaCoCo ve
   el proxy como clase no instrumentada y no registra la cobertura de los
   métodos interceptados

El resultado sin instrumentación offline: clases como `PersonRepositoryImpl`
y `PersonService` aparecen con 0% de cobertura a pesar de tener tests que
las ejercitan completamente.

---

## Decisión

**JaCoCo offline instrumentation** — instrumentación en tiempo de compilación,
antes de que Quarkus genere los proxies CDI.

Configuración en `pom.xml`:

```xml
<!-- Dependencia del agente como artifact de runtime en tests -->
<dependency>
    <groupId>org.jacoco</groupId>
    <artifactId>org.jacoco.agent</artifactId>
    <version>${jacoco.version}</version>
    <classifier>runtime</classifier>
    <scope>test</scope>
</dependency>

<!-- Executions del plugin:
     1. instrument   — en process-classes, antes de la augmentation de Quarkus
     2. restore      — en prepare-package, restaura las clases originales
     3. report       — en verify, genera el reporte HTML/XML              -->
```

```xml
<!-- Surefire pasa la ubicación del archivo de datos al proceso de test -->
<systemPropertyVariables>
    <jacoco-agent.destfile>${project.build.directory}/jacoco.exec</jacoco-agent.destfile>
</systemPropertyVariables>
```

---

## Alternativas evaluadas

### Alternativa A — Agente JaCoCo en runtime (`prepare-agent`)

El mecanismo estándar: `-javaagent:jacocoagent.jar` en los flags de la JVM.

**Ventajas:**
- Configuración más simple — un solo `<execution>` en el plugin
- No requiere la dependencia del agente como artifact

**Desventajas:**
- No instrumenta las clases que Quarkus procesa durante la Augmentation Phase
- Los proxies CDI generados por Quarkus no son visibles al agente
- Resultado: `PersonRepositoryImpl`, `PersonService` y otros beans CDI
  muestran 0% de cobertura

**Resultado: descartada** — producía reportes de cobertura incorrectos que
ocultaban gaps reales en la cobertura de la capa de infraestructura.

### Alternativa B — Quarkus Test Coverage con JaCoCo extension

Quarkus ofrece `quarkus-jacoco` como extensión experimental que integra
JaCoCo dentro del ciclo de augmentation.

**Ventajas:**
- Integración nativa con el ciclo de build de Quarkus
- Sin necesidad de configuración manual de fases Maven

**Desventajas:**
- Estado experimental en Quarkus 3.x — la documentación advierte sobre
  incompatibilidades con ciertas extensiones
- Interfiere con el reporte cuando se combinan tests unitarios puros
  (sin contenedor) y tests `@QuarkusTest`
- Menos control sobre las fases de instrumentación y restauración

**Resultado: descartada** — el estado experimental y la interferencia con
tests unitarios puros introduciría inestabilidad en el pipeline de CI.

---

## Justificación

La instrumentación offline ocurre en la fase `process-classes` de Maven,
antes de que Quarkus ejecute su Augmentation Phase. Las clases instrumentadas
son las que Quarkus ve cuando genera los proxies CDI, por lo que los contadores
de cobertura están presentes tanto en las clases originales como en los proxies.

La fase `restore-instrumented-classes` en `prepare-package` restaura las clases
originales no instrumentadas antes del empaquetado, evitando que el bytecode
de JaCoCo llegue a la imagen de producción.

---

## Consecuencias

**Positivas:**
- Cobertura real visible para todos los beans CDI — `PersonRepositoryImpl`,
  `PersonService`, `PersonResource`
- Resultado final: 100% instrucciones, 100% branches
- El reporte refleja la cobertura real ejercitada por los tests, no artefactos
  de los proxies CDI

**Negativas:**
- Configuración Maven más compleja que el agente estándar
- El paso `restore-instrumented-classes` es necesario para no contaminar el JAR
- Si se añaden nuevas fases al ciclo Maven, hay que verificar que la instrumentación
  y restauración estén en el orden correcto