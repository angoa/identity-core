package io.serverus.infrastructure.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.BirthDate;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.FullName;
import io.serverus.domain.model.vo.PersonId;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integracion del adaptador de persistencia PersonRepositoryImpl.
 *
 * Levanta el contenedor Quarkus completo con H2 en memoria y Flyway.
 * {@code @Transactional} a nivel de clase aplica a todos los metodos —
 * CDI solo intercepta metodos de la clase raiz, no de clases {@code @Nested}.
 *
 * Cada test usa CURPs unicas para garantizar aislamiento sin rollback.
 *
 * @since 1.0.0
 */
@QuarkusTest
@Transactional
@DisplayName("PersonRepositoryImpl — integracion JPA")
class PersonRepositoryImplTest {

    @Inject
    PersonRepositoryImpl repository;

    private static final FullName  FULL_NAME  = FullName.of("Rafael", "Morales", "Angoa");
    private static final BirthDate BIRTH_DATE = BirthDate.of(LocalDate.of(1980, 1, 1));

    private Person buildPerson(String curpValue) {
        return Person.register(Curp.of(curpValue), FULL_NAME, BIRTH_DATE);
    }

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("persiste una persona nueva en la base de datos")
        void shouldPersistPerson() {
            Person person = buildPerson("REPJ810315HDFRZS04");
            repository.save(person);
            assertThat(repository.findById(person.getId())).isPresent();
        }

        @Test
        @DisplayName("la persona persistida tiene estado ACTIVE")
        void shouldPersistWithActiveStatus() {
            Person person = buildPerson("LOPM920618HDFRZS07");
            repository.save(person);
            assertThat(repository.findById(person.getId()).orElseThrow().isActive()).isTrue();
        }

        @Test
        @DisplayName("la persona persistida conserva el ID original")
        void shouldPreserveId() {
            Person person = buildPerson("GUZR850901HDFRZS02");
            PersonId originalId = person.getId();
            repository.save(person);
            assertThat(repository.findById(originalId).orElseThrow().getId()).isEqualTo(originalId);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("actualiza el estado de una persona suspendida")
        void shouldUpdateSuspendedStatus() {
            Person person = buildPerson("HEMJ760424HDFRZS01");
            repository.save(person);
            person.suspend("Proceso administrativo");
            repository.update(person);
            assertThat(repository.findById(person.getId()).orElseThrow().isSuspended()).isTrue();
        }

        @Test
        @DisplayName("actualiza el estado de una persona reactivada")
        void shouldUpdateReactivatedStatus() {
            Person person = buildPerson("NUJR910712HDFRZS09");
            repository.save(person);
            person.suspend("Suspension temporal");
            repository.update(person);
            person.reactivate("Proceso concluido");
            repository.update(person);
            assertThat(repository.findById(person.getId()).orElseThrow().isActive()).isTrue();
        }

        @Test
        @DisplayName("actualiza el estado de una persona revocada")
        void shouldUpdateRevokedStatus() {
            Person person = buildPerson("ROMJ840530HDFRZS06");
            repository.save(person);
            person.revoke("Resolucion judicial");
            repository.update(person);
            assertThat(repository.findById(person.getId()).orElseThrow().isRevoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("retorna Optional vacio cuando el ID no existe")
        void shouldReturnEmptyWhenNotFound() {
            assertThat(repository.findById(PersonId.generate())).isEmpty();
        }

        @Test
        @DisplayName("retorna la persona correcta cuando el ID existe")
        void shouldReturnPersonWhenFound() {
            Person person = buildPerson("SANM780215HDFRZS03");
            repository.save(person);
            assertThat(repository.findById(person.getId()).orElseThrow().getId()).isEqualTo(person.getId());
        }
    }

    @Nested
    @DisplayName("findByCurp()")
    class FindByCurp {

        @Test
        @DisplayName("retorna la persona cuando la CURP existe")
        void shouldReturnPersonWhenCurpExists() {
            String curp = "JIMR950320HDFRZS08";
            repository.save(buildPerson(curp));
            assertThat(repository.findByCurp(Curp.of(curp))).isPresent();
        }

        @Test
        @DisplayName("retorna Optional vacio cuando la CURP no existe")
        void shouldReturnEmptyWhenCurpNotFound() {
            assertThat(repository.findByCurp(Curp.of("XEXX010101HNEXXXA4"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByCurp()")
    class ExistsByCurp {

        @Test
        @DisplayName("retorna true cuando la CURP existe")
        void shouldReturnTrueWhenCurpExists() {
            String curp = "VEGR881104HDFRZS05";
            repository.save(buildPerson(curp));
            assertThat(repository.existsByCurp(Curp.of(curp))).isTrue();
        }

        @Test
        @DisplayName("retorna false cuando la CURP no existe")
        void shouldReturnFalseWhenCurpNotFound() {
            assertThat(repository.existsByCurp(Curp.of("XEXX010101HNEXXXB3"))).isFalse();
        }
    }
}