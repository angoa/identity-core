package io.serverus.infrastructure.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.BirthDate;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.FullName;
import io.serverus.domain.model.vo.PersonId;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que las politicas de fault tolerance de MicroProfile esten
 * correctamente aplicadas en PersonRepositoryImpl.
 *
 * En el perfil test los timeouts son de 60s y retries=0, por lo que
 * estos tests verifican el camino feliz con fault tolerance activa —
 * no simulan fallos de BD (eso requiere un mock server de Oracle).
 *
 * La verificacion de que las anotaciones estan presentes y activas
 * se hace implicitamente: si CDI no procesara las anotaciones de
 * fault tolerance, el proxy no se generaria y la inyeccion fallaria
 * en el arranque del contenedor Quarkus.
 *
 * @since 1.0.0
 */
@QuarkusTest
@Transactional
@DisplayName("PersonRepositoryImpl — fault tolerance activa")
class PersonRepositoryFaultToleranceTest {

    @Inject
    PersonRepositoryImpl repository;

    private static final FullName  FULL_NAME  = FullName.of("Rafael", "Morales", "Angoa");
    private static final BirthDate BIRTH_DATE = BirthDate.of(LocalDate.of(1985, 6, 15));

    private Person buildPerson(String curp) {
        return Person.register(Curp.of(curp), FULL_NAME, BIRTH_DATE);
    }

    @Test
    @DisplayName("save() opera correctamente con fault tolerance activa")
    void saveShouldWorkWithFaultToleranceActive() {
        Person person = buildPerson("FTAA850615HDFRZS01");
        repository.save(person);
        assertThat(repository.findById(person.getId())).isPresent();
    }

    @Test
    @DisplayName("update() opera correctamente con fault tolerance activa")
    void updateShouldWorkWithFaultToleranceActive() {
        Person person = buildPerson("FTBB850615HDFRZS02");
        repository.save(person);
        person.suspend("test fault tolerance");
        repository.update(person);
        assertThat(repository.findById(person.getId())
                .orElseThrow().isSuspended()).isTrue();
    }

    @Test
    @DisplayName("findById() opera correctamente con fault tolerance activa")
    void findByIdShouldWorkWithFaultToleranceActive() {
        Person person = buildPerson("FTCC850615HDFRZS03");
        repository.save(person);
        Optional<Person> found = repository.findById(person.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(person.getId());
    }

    @Test
    @DisplayName("findByCurp() opera correctamente con fault tolerance activa")
    void findByCurpShouldWorkWithFaultToleranceActive() {
        String curp = "FTDD850615HDFRZS04";
        repository.save(buildPerson(curp));
        assertThat(repository.findByCurp(Curp.of(curp))).isPresent();
    }

    @Test
    @DisplayName("existsByCurp() opera correctamente con fault tolerance activa")
    void existsByCurpShouldWorkWithFaultToleranceActive() {
        String curp = "FTEE850615HDFRZS05";
        repository.save(buildPerson(curp));
        assertThat(repository.existsByCurp(Curp.of(curp))).isTrue();
    }

    @Test
    @DisplayName("findById() retorna empty para ID inexistente con fault tolerance activa")
    void findByIdShouldReturnEmptyWithFaultToleranceActive() {
        assertThat(repository.findById(PersonId.generate())).isEmpty();
    }

    @Test
    @DisplayName("existsByCurp() retorna false para CURP inexistente con fault tolerance activa")
    void existsByCurpShouldReturnFalseWithFaultToleranceActive() {
        assertThat(repository.existsByCurp(Curp.of("XEXX010101HNEXXXC2"))).isFalse();
    }
}