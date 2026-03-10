package io.serverus.domain.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.serverus.domain.exception.BusinessRuleException;
import io.serverus.domain.exception.ResourceNotFoundException;
import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.BirthDate;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.FullName;
import io.serverus.domain.model.vo.PersonId;
import io.serverus.domain.port.in.RegisterPersonUseCase;
import io.serverus.domain.port.out.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios del servicio de dominio PersonService.
 *
 * Utiliza un fake en memoria del repositorio y un SimpleMeterRegistry
 * de Micrometer — sin Mockito, sin base de datos, sin contenedor.
 * Verifica la orquestacion de casos de uso y el cumplimiento de
 * reglas de negocio a nivel de servicio.
 *
 * @since 1.0.0
 */
@DisplayName("PersonService")
class PersonServiceTest {

    // -------------------------------------------------------------------------
    // Fake en memoria del repositorio
    // Distingue save() de update() para que PIT detecte si update() es eliminado.
    // -------------------------------------------------------------------------

    static class InMemoryPersonRepository implements PersonRepository {

        private final Map<PersonId, Person> store = new HashMap<>();

        @Override
        public void save(Person person) {
            if (store.containsKey(person.getId())) {
                throw new IllegalStateException(
                        "Person already exists, use update(): " + person.getId()
                );
            }
            store.put(person.getId(), person);
        }

        @Override
        public void update(Person person) {
            if (!store.containsKey(person.getId())) {
                throw new IllegalStateException(
                        "Person not found for update: " + person.getId()
                );
            }
            store.put(person.getId(), person);
        }

        @Override
        public Optional<Person> findById(PersonId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Person> findByCurp(Curp curp) {
            return store.values().stream()
                    .filter(p -> p.getCurp().equals(curp))
                    .findFirst();
        }

        @Override
        public boolean existsByCurp(Curp curp) {
            return store.values().stream()
                    .anyMatch(p -> p.getCurp().equals(curp));
        }

        public int size() {
            return store.size();
        }
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final Curp      CURP       = Curp.of("MOAA800101HDFRRN09");
    private static final Curp      CURP_2     = Curp.of("GARE900215MJCRZL04");
    private static final FullName  FULL_NAME  = FullName.of("Rafael", "Morales", "Angoa");
    private static final BirthDate BIRTH_DATE = BirthDate.of(LocalDate.of(1980, 1, 1));

    private InMemoryPersonRepository repository;
    private SimpleMeterRegistry       meterRegistry;
    private PersonService             service;

    @BeforeEach
    void setUp() {
        repository    = new InMemoryPersonRepository();
        meterRegistry = new SimpleMeterRegistry();
        service       = new PersonService(repository, meterRegistry);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("execute() — RegisterPersonUseCase")
    class Execute {

        @Test
        @DisplayName("registra una nueva persona y la persiste en el repositorio")
        void shouldRegisterAndPersistPerson() {
            RegisterPersonUseCase.Command command = new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            );

            Person person = service.execute(command);

            assertThat(person).isNotNull();
            assertThat(person.getCurp()).isEqualTo(CURP);
            assertThat(person.getFullName()).isEqualTo(FULL_NAME);
            assertThat(person.getBirthDate()).isEqualTo(BIRTH_DATE);
            assertThat(person.isActive()).isTrue();
            assertThat(repository.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("lanza BusinessRuleException cuando la CURP ya esta registrada")
        void shouldThrowBusinessRuleExceptionForDuplicateCurp() {
            RegisterPersonUseCase.Command command = new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            );

            service.execute(command);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("CURP")
                    .hasMessageContaining(CURP.toString());
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el comando es nulo")
        void shouldThrowNullPointerExceptionForNullCommand() {
            assertThatThrownBy(() -> service.execute(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("permite registrar dos personas con CURPs distintas")
        void shouldAllowTwoPeopleWithDifferentCurps() {
            service.execute(new RegisterPersonUseCase.Command(CURP,   FULL_NAME, BIRTH_DATE));
            service.execute(new RegisterPersonUseCase.Command(CURP_2, FULL_NAME, BIRTH_DATE));

            assertThat(repository.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("incrementa el contador de registros")
        void shouldIncrementRegisteredCounter() {
            service.execute(new RegisterPersonUseCase.Command(CURP, FULL_NAME, BIRTH_DATE));

            assertThat(meterRegistry
                    .counter("identity.persons.registered", "service", "identity-core")
                    .count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("suspend()")
    class Suspend {

        @Test
        @DisplayName("suspende una persona activa")
        void shouldSuspendActivePerson() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));

            Person suspended = service.suspend(registered.getId(), "Proceso administrativo");

            assertThat(suspended.isSuspended()).isTrue();
        }

        @Test
        @DisplayName("persiste el estado suspendido en el repositorio")
        void shouldPersistSuspendedState() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));

            service.suspend(registered.getId(), "Proceso administrativo");

            Person persisted = repository.findById(registered.getId()).orElseThrow();
            assertThat(persisted.isSuspended()).isTrue();
        }

        @Test
        @DisplayName("incrementa el contador de suspensiones")
        void shouldIncrementSuspendedCounter() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));
            service.suspend(registered.getId(), "Motivo");

            assertThat(meterRegistry
                    .counter("identity.persons.suspended", "service", "identity-core")
                    .count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la persona no existe")
        void shouldThrowResourceNotFoundExceptionForUnknownId() {
            assertThatThrownBy(() -> service.suspend(PersonId.generate(), "Motivo"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el id es nulo")
        void shouldThrowNullPointerExceptionForNullId() {
            assertThatThrownBy(() -> service.suspend(null, "Motivo"))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        @Test
        @DisplayName("reactiva una persona suspendida")
        void shouldReactivateSuspendedPerson() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));
            service.suspend(registered.getId(), "Suspension temporal");

            Person reactivated = service.reactivate(registered.getId(), "Conclusion del proceso");

            assertThat(reactivated.isActive()).isTrue();
        }

        @Test
        @DisplayName("persiste el estado reactivado en el repositorio")
        void shouldPersistReactivatedState() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));
            service.suspend(registered.getId(), "Suspension temporal");

            service.reactivate(registered.getId(), "Conclusion del proceso");

            Person persisted = repository.findById(registered.getId()).orElseThrow();
            assertThat(persisted.isActive()).isTrue();
        }

        @Test
        @DisplayName("incrementa el contador de reactivaciones")
        void shouldIncrementReactivatedCounter() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));
            service.suspend(registered.getId(), "Suspension");
            service.reactivate(registered.getId(), "Reactivacion");

            assertThat(meterRegistry
                    .counter("identity.persons.reactivated", "service", "identity-core")
                    .count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la persona no existe")
        void shouldThrowResourceNotFoundExceptionForUnknownId() {
            assertThatThrownBy(() -> service.reactivate(PersonId.generate(), "Motivo"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("lanza BusinessRuleException cuando la persona esta activa")
        void shouldThrowBusinessRuleExceptionWhenPersonIsActive() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));

            assertThatThrownBy(() -> service.reactivate(registered.getId(), "Motivo"))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("revoke()")
    class Revoke {

        @Test
        @DisplayName("revoca una persona activa")
        void shouldRevokeActivePerson() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));

            Person revoked = service.revoke(registered.getId(), "Resolucion judicial");

            assertThat(revoked.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("revoca una persona suspendida")
        void shouldRevokeSuspendedPerson() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));
            service.suspend(registered.getId(), "Suspension previa");

            Person revoked = service.revoke(registered.getId(), "Resolucion judicial");

            assertThat(revoked.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("persiste el estado revocado en el repositorio")
        void shouldPersistRevokedState() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));

            service.revoke(registered.getId(), "Resolucion judicial");

            Person persisted = repository.findById(registered.getId()).orElseThrow();
            assertThat(persisted.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("incrementa el contador de revocaciones")
        void shouldIncrementRevokedCounter() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));
            service.revoke(registered.getId(), "Resolucion");

            assertThat(meterRegistry
                    .counter("identity.persons.revoked", "service", "identity-core")
                    .count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la persona no existe")
        void shouldThrowResourceNotFoundExceptionForUnknownId() {
            assertThatThrownBy(() -> service.revoke(PersonId.generate(), "Motivo"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("retorna la persona cuando existe")
        void shouldReturnPersonWhenExists() {
            Person registered = service.execute(new RegisterPersonUseCase.Command(
                    CURP, FULL_NAME, BIRTH_DATE
            ));

            Person found = service.findById(registered.getId());

            assertThat(found).isEqualTo(registered);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando no existe")
        void shouldThrowResourceNotFoundExceptionWhenNotExists() {
            assertThatThrownBy(() -> service.findById(PersonId.generate()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el id es nulo")
        void shouldThrowNullPointerExceptionForNullId() {
            assertThatThrownBy(() -> service.findById(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("findByCurp()")
    class FindByCurp {

        @Test
        @DisplayName("retorna la persona cuando existe")
        void shouldReturnPersonWhenExists() {
            service.execute(new RegisterPersonUseCase.Command(CURP, FULL_NAME, BIRTH_DATE));

            Person found = service.findByCurp(CURP);

            assertThat(found.getCurp()).isEqualTo(CURP);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando no existe")
        void shouldThrowResourceNotFoundExceptionWhenNotExists() {
            assertThatThrownBy(() -> service.findByCurp(CURP))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("lanza NullPointerException cuando la CURP es nula")
        void shouldThrowNullPointerExceptionForNullCurp() {
            assertThatThrownBy(() -> service.findByCurp(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("lanza NullPointerException cuando el repositorio es nulo")
        void shouldThrowNullPointerExceptionForNullRepository() {
            assertThatThrownBy(() -> new PersonService(null, new SimpleMeterRegistry()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el MeterRegistry es nulo")
        void shouldThrowNullPointerExceptionForNullMeterRegistry() {
            assertThatThrownBy(() -> new PersonService(repository, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}