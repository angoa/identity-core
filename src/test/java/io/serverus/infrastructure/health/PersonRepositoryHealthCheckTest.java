package io.serverus.infrastructure.health;

import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.PersonId;
import io.serverus.domain.port.out.PersonRepository;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de PersonRepositoryHealthCheck.
 *
 * Ejercita ambos branches del metodo call():
 * - UP  cuando el repositorio responde sin excepcion
 * - DOWN cuando el repositorio lanza una excepcion
 *
 * Test unitario puro — no requiere contenedor Quarkus.
 * La inyeccion CDI se reemplaza con fakes manuales para
 * controlar el comportamiento del repositorio.
 *
 * @since 1.0.0
 */
@DisplayName("PersonRepositoryHealthCheck")
class PersonRepositoryHealthCheckTest {

    // -------------------------------------------------------------------------
    // Fakes manuales — evitan dependencia en Mockito
    // -------------------------------------------------------------------------

    /** Repositorio que siempre responde sin excepcion. */
    private static final PersonRepository HEALTHY_REPOSITORY = new PersonRepository() {
        @Override public void save(Person p) {}
        @Override public void update(Person p) {}
        @Override public Optional<Person> findById(PersonId id) { return Optional.empty(); }
        @Override public Optional<Person> findByCurp(Curp c) { return Optional.empty(); }
        @Override public boolean existsByCurp(Curp c) { return false; }
    };

    /** Repositorio que lanza RuntimeException en existsByCurp. */
    private static final PersonRepository FAILING_REPOSITORY = new PersonRepository() {
        @Override public void save(Person p) {}
        @Override public void update(Person p) {}
        @Override public Optional<Person> findById(PersonId id) { return Optional.empty(); }
        @Override public Optional<Person> findByCurp(Curp c) { return Optional.empty(); }
        @Override public boolean existsByCurp(Curp c) {
            throw new RuntimeException("Connection refused: Oracle no disponible");
        }
    };

    private PersonRepositoryHealthCheck buildCheck(PersonRepository repository) {
        PersonRepositoryHealthCheck check = new PersonRepositoryHealthCheck();
        check.personRepository = repository;
        return check;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("call() — branch UP")
    class Up {

        @Test
        @DisplayName("retorna UP cuando el repositorio responde sin excepcion")
        void shouldReturnUpWhenRepositoryIsHealthy() {
            HealthCheckResponse response = buildCheck(HEALTHY_REPOSITORY).call();

            assertThat(response.getStatus())
                    .isEqualTo(HealthCheckResponse.Status.UP);
            assertThat(response.getName())
                    .isEqualTo("identity-core/person-repository");
        }
    }

    @Nested
    @DisplayName("call() — branch DOWN")
    class Down {

        @Test
        @DisplayName("retorna DOWN cuando el repositorio lanza excepcion")
        void shouldReturnDownWhenRepositoryThrows() {
            HealthCheckResponse response = buildCheck(FAILING_REPOSITORY).call();

            assertThat(response.getStatus())
                    .isEqualTo(HealthCheckResponse.Status.DOWN);
            assertThat(response.getName())
                    .isEqualTo("identity-core/person-repository");
        }

        @Test
        @DisplayName("el mensaje de error queda registrado en los datos del check")
        void shouldIncludeErrorMessageInData() {
            HealthCheckResponse response = buildCheck(FAILING_REPOSITORY).call();

            assertThat(response.getData())
                    .isPresent()
                    .get()
                    .satisfies(data -> assertThat(data.get("error").toString())
                            .contains("Oracle no disponible"));
        }
    }
}