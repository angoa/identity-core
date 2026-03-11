# ADR-008 — InMemoryPersonRepository fake sobre Mockito para tests de dominio

| Campo      | Valor                  |
|------------|------------------------|
| Estado     | Aceptado               |
| Fecha      | 2026-03-11             |
| Autores    | Equipo Arquitectura    |

---

## Contexto

`PersonService` depende del port `PersonRepository`. Para los tests unitarios
del servicio necesitamos una implementación de ese port que no requiera JPA
ni base de datos.

Las opciones principales son un mock generado por Mockito o un fake escrito
manualmente. La elección afecta la calidad de los tests y su capacidad de
detectar regresiones mediante mutation testing (PIT).

---

## Decisión

**`InMemoryPersonRepository`** — implementación fake manual con un `HashMap`
interno. Los métodos de lectura retornan snapshots (copias) del objeto
almacenado, no referencias directas.

```java
public class InMemoryPersonRepository implements PersonRepository {
    private final Map<PersonId, Person> store = new HashMap<>();

    @Override
    public void save(Person person) {
        store.put(person.getId(), Person.reconstitute(...));  // snapshot
    }

    @Override
    public Optional<Person> findById(PersonId id) {
        return Optional.ofNullable(store.get(id))
                .map(p -> Person.reconstitute(...));           // snapshot en lectura
    }
    // ...
}
```

---

## Alternativas evaluadas

### Alternativa A — Mockito (`@Mock`, `when(...).thenReturn(...)`)

```java
@Mock PersonRepository repository;

when(repository.existsByCurp(curp)).thenReturn(false);
when(repository.findById(id)).thenReturn(Optional.of(person));
```

**Ventajas:**
- Sin código adicional que mantener
- Flexible — cada test configura exactamente el comportamiento que necesita
- Estándar en el ecosistema Java

**Desventajas críticas para mutation testing:**

PIT (mutation testing) modifica el bytecode de `PersonService` para crear
mutantes. Por ejemplo, cambia:

```java
if (repository.existsByCurp(curp)) throw new BusinessRuleException(...)
// mutante: if (!repository.existsByCurp(curp)) throw new BusinessRuleException(...)
```

Con Mockito, `when(repository.existsByCurp(curp)).thenReturn(false)` siempre
retorna `false` independientemente del estado. El test con el mutante pasa
igual que el test original — el mutante no es detectado.

Con `InMemoryPersonRepository`, el repositorio tiene estado real. Si el servicio
no llama a `save()` correctamente, `findById()` retorna `empty()`. Los tests
que verifican el estado final del repositorio detectan mutantes que Mockito
no detectaría.

**Resultado: descartada** — producía una tasa de mutantes no detectados
artificialmente alta en `PersonService`.

### Alternativa B — `InMemoryPersonRepository` sin snapshots

```java
public void save(Person person) {
    store.put(person.getId(), person);  // referencia directa, no copia
}
```

**Ventajas:**
- Implementación más simple

**Desventajas:**
- Si el test modifica el objeto `person` después de `save()`, el objeto
  en el store también se modifica — el repositorio no tiene estado independiente
- No simula el comportamiento real de JPA donde `persist()` desconecta el
  objeto del contexto de persistencia
- PIT puede generar mutantes que no son detectados porque el test y el store
  comparten la misma referencia

**Resultado: descartada** — la ausencia de snapshots introduce falsos negativos
en mutation testing.

---

## Justificación

Un fake con snapshots simula el comportamiento real de un repositorio de
persistencia: una vez que guardas un objeto, el repositorio tiene su propia
copia independiente. Modificar el objeto original después de guardarlo no
afecta lo que está en el store.

Este comportamiento es esencial para mutation testing: PIT necesita que los
tests fallen cuando el código de producción tiene un defecto. Con un fake
stateful, los tests verifican el estado final del sistema — no solo que se
llamó a un método con ciertos argumentos.

Resultado: mutation coverage 96% en `PersonService` (techo real considerando
mutaciones equivalentes).

---

## Consecuencias

**Positivas:**
- Mutation coverage máximo alcanzable en `PersonService`
- Tests que verifican comportamiento observable, no interacciones internas
- `InMemoryPersonRepository` sirve como documentación ejecutable del contrato
  del port `PersonRepository`

**Negativas:**
- Requiere mantener `InMemoryPersonRepository` actualizado cuando el port
  `PersonRepository` evoluciona con nuevos métodos
- Más código que un mock Mockito — aunque el fake es simple (~50 líneas)