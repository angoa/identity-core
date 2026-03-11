# ADR-010 — MicroProfile Fault Tolerance sobre Resilience4j

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado               |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

`PersonRepositoryImpl` conecta con Oracle en producción. Una BD degradada
o no disponible puede bloquear threads del pool de conexiones JDBC de forma
indefinida, causando que el microservicio deje de responder a peticiones
nuevas — efecto cascada hacia los clientes del API.

Se necesita una estrategia de resiliencia que proteja el servicio ante:
1. **Latencia alta:** Oracle responde pero lentamente (query plan degradado,
   contención de locks, red lenta)
2. **Fallo total:** Oracle no responde (pod caído, red particionada)
3. **Fallo transitorio:** Error puntual de red o timeout de conexión del pool

---

## Decisión

**MicroProfile Fault Tolerance** — extensión nativa de Quarkus, implementada
por SmallRye Fault Tolerance.

Políticas aplicadas en `PersonRepositoryImpl`:

| Método | `@Timeout` | `@CircuitBreaker` | `@Retry` |
|--------|-----------|-------------------|---------|
| `save` | 5000ms | ✅ | ❌ |
| `update` | 5000ms | ✅ | ❌ |
| `findById` | 2000ms | ✅ | 2 reintentos |
| `findByCurp` | 2000ms | ✅ | 2 reintentos |
| `existsByCurp` | 2000ms | ✅ | 2 reintentos |

`@Retry` no se aplica a escrituras (`save`, `update`) para evitar
inserciones o actualizaciones duplicadas si el primer intento persistió
pero el ACK se perdió.

---

## Alternativas evaluadas

### Alternativa A — Resilience4j

Biblioteca de resiliencia para Java, ampliamente usada con Spring Boot.

**Ventajas:**
- API programática muy flexible
- Métricas detalladas por circuit breaker
- Soporte de bulkhead y rate limiter además de circuit breaker y timeout

**Desventajas:**
- No es una extensión nativa de Quarkus — requiere integración manual
- La configuración via anotaciones requiere AOP (Aspect-Oriented Programming)
  que en Quarkus necesita el módulo `quarkus-arc` con configuración adicional
- La configuración de propiedades no sigue el estándar MicroProfile Config —
  no es sobreescribible con el mismo patrón de variables de entorno que el
  resto de la configuración del proyecto
- Añade una dependencia no certificada por Red Hat para OpenShift

**Resultado: descartada** — MicroProfile Fault Tolerance logra el mismo
resultado con APIs nativas del ecosistema Quarkus y configuración estándar.

### Alternativa B — Timeouts en el pool de conexiones JDBC

Configurar `quarkus.datasource.jdbc.acquisition-timeout` y
`quarkus.datasource.jdbc.idle-removal-interval` para que el pool de
conexiones libere conexiones bloqueadas.

**Ventajas:**
- Sin dependencias adicionales
- Afecta a todas las operaciones JDBC automáticamente

**Desventajas:**
- El timeout del pool no es un `@Timeout` de MicroProfile — no lanza
  `TimeoutException` que el circuit breaker pueda contar como fallo
- No hay circuit breaker — si Oracle está caída, cada petición agota
  su timeout antes de fallar, bloqueando threads durante el tiempo de espera
- No hay retry — los fallos transitorios de red no se reintentan
- La granularidad es de pool completo, no por operación

**Resultado: descartada** — complementaria pero insuficiente como única
estrategia. Se usará junto con MicroProfile Fault Tolerance.

### Alternativa C — MicroProfile Fault Tolerance (decisión adoptada)

**Ventajas:**
- Extensión nativa de Quarkus (`quarkus-smallrye-fault-tolerance`)
- Las políticas se configuran via MicroProfile Config — sobreescribibles
  con variables de entorno en Kubernetes/OpenShift sin recompilar
- `@Timeout`, `@CircuitBreaker`, `@Retry` son interceptores CDI — coherentes
  con el resto de las políticas cross-cutting del proyecto (`@WithSpan`,
  `@Transactional`)
- Los valores por perfil (`%test`, `%prod`) se configuran en `application.yaml`
  — timeout de 60s en tests para evitar falsos fallos por lentitud del CI

**Desventajas:**
- Menos flexible que Resilience4j para casos avanzados (bulkhead por semáforo,
  rate limiter, hedge requests)
- El `@CircuitBreaker` de MicroProfile es por instancia CDI, no compartido
  entre pods — en un cluster de N pods, cada pod tiene su propio estado del
  circuit breaker

---

## Justificación

MicroProfile Fault Tolerance es la elección natural para un proyecto Quarkus
con destino OpenShift. Las anotaciones son interceptores CDI como los demás,
la configuración sigue el mismo patrón que el resto del `application.yaml`,
y las variables de entorno `REPO_READ_TIMEOUT_MS`, `REPO_WRITE_TIMEOUT_MS`,
`REPO_CB_DELAY_MS` permiten al equipo de operaciones ajustar los umbrales en
producción sin tocar el código.

---

## Consecuencias

**Positivas:**
- Si Oracle tarda >2s en responder una lectura, el thread se libera inmediatamente
- Si el 50% de las últimas 10 peticiones fallan, el circuit breaker abre y
  retorna fallo rápido — sin acumular threads bloqueados
- Los fallos transitorios de red en lecturas se reintentan 2 veces antes de fallar
- Los umbrales son configurables por entorno sin recompilar la imagen

**Negativas:**
- El estado del circuit breaker no se comparte entre pods — en un rolling
  update, los pods nuevos empiezan con el circuito cerrado aunque los pods
  anteriores lo tuvieran abierto
- Para circuit breaker distribuido se necesitaría un store compartido (Redis)
  — fuera del alcance del proyecto actual