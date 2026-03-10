package io.serverus.domain.service;

import io.serverus.domain.exception.BusinessRuleException;
import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.BirthDate;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.FullName;
import io.serverus.domain.model.vo.PersonId;
import io.serverus.domain.model.vo.PersonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios del aggregate Person.
 *
 * Verifica el comportamiento del ciclo de vida, las transiciones de
 * estado y las reglas de negocio definidas en el aggregate raiz.
 * No requiere contenedor ni base de datos — corre en memoria.
 *
 * @since 1.0.0
 */
@DisplayName("Person")
class PersonTest {

    private static final Curp      CURP       = Curp.of("MOAA800101HDFRRN09");
    private static final FullName  FULL_NAME  = FullName.of("Rafael", "Morales", "Angoa");
    private static final BirthDate BIRTH_DATE = BirthDate.of(LocalDate.of(1980, 1, 1));

    private Person person;

    @BeforeEach
    void setUp() {
        person = Person.register(CURP, FULL_NAME, BIRTH_DATE);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("genera un identificador unico no nulo")
        void shouldGenerateNonNullId() {
            assertThat(person.getId()).isNotNull();
            assertThat(person.getId()).isNotEqualTo(PersonId.NONE);
        }

        @Test
        @DisplayName("asigna la CURP correctamente")
        void shouldAssignCurp() {
            assertThat(person.getCurp()).isEqualTo(CURP);
        }

        @Test
        @DisplayName("asigna el nombre completo correctamente")
        void shouldAssignFullName() {
            assertThat(person.getFullName()).isEqualTo(FULL_NAME);
        }

        @Test
        @DisplayName("asigna la fecha de nacimiento correctamente")
        void shouldAssignBirthDate() {
            assertThat(person.getBirthDate()).isEqualTo(BIRTH_DATE);
        }

        @Test
        @DisplayName("asigna el estado inicial ACTIVE")
        void shouldAssignActiveStatus() {
            assertThat(person.getStatus()).isEqualTo(PersonStatus.ACTIVE);
        }

        @Test
        @DisplayName("asigna la fecha de creacion")
        void shouldAssignCreatedAt() {
            assertThat(person.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("asigna la fecha de actualizacion igual a la de creacion")
        void shouldAssignUpdatedAtEqualToCreatedAt() {
            assertThat(person.getUpdatedAt()).isEqualTo(person.getCreatedAt());
        }

        @Test
        @DisplayName("genera identificadores distintos para cada registro")
        void shouldGenerateUniqueIds() {
            Person another = Person.register(CURP, FULL_NAME, BIRTH_DATE);
            assertThat(person.getId()).isNotEqualTo(another.getId());
        }

        @Test
        @DisplayName("lanza NullPointerException cuando la CURP es nula")
        void shouldThrowNullPointerExceptionForNullCurp() {
            assertThatThrownBy(() -> Person.register(null, FULL_NAME, BIRTH_DATE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el nombre es nulo")
        void shouldThrowNullPointerExceptionForNullFullName() {
            assertThatThrownBy(() -> Person.register(CURP, null, BIRTH_DATE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando la fecha de nacimiento es nula")
        void shouldThrowNullPointerExceptionForNullBirthDate() {
            assertThatThrownBy(() -> Person.register(CURP, FULL_NAME, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("suspend()")
    class Suspend {

        @Test
        @DisplayName("transiciona de ACTIVE a SUSPENDED")
        void shouldTransitionFromActiveToSuspended() {
            person.suspend("Proceso administrativo INE-2026-001");
            assertThat(person.getStatus()).isEqualTo(PersonStatus.SUSPENDED);
            assertThat(person.isSuspended()).isTrue();
        }

        @Test
        @DisplayName("actualiza updatedAt al suspender")
        void shouldUpdateUpdatedAt() {
            person.suspend("Proceso administrativo");
            assertThat(person.getUpdatedAt()).isAfterOrEqualTo(person.getCreatedAt());
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el motivo es nulo")
        void shouldThrowNullPointerExceptionForNullReason() {
            assertThatThrownBy(() -> person.suspend(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza BusinessRuleException cuando el estado es SUSPENDED")
        void shouldThrowBusinessRuleExceptionWhenAlreadySuspended() {
            person.suspend("Primera suspension");
            assertThatThrownBy(() -> person.suspend("Segunda suspension"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("SUSPENDED");
        }

        @Test
        @DisplayName("lanza BusinessRuleException cuando el estado es REVOKED")
        void shouldThrowBusinessRuleExceptionWhenRevoked() {
            person.revoke("Resolucion judicial");
            assertThatThrownBy(() -> person.suspend("Intento de suspension"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("REVOKED");
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        @Test
        @DisplayName("transiciona de SUSPENDED a ACTIVE")
        void shouldTransitionFromSuspendedToActive() {
            person.suspend("Suspension temporal");
            person.reactivate("Conclusion del proceso");
            assertThat(person.getStatus()).isEqualTo(PersonStatus.ACTIVE);
            assertThat(person.isActive()).isTrue();
        }

        @Test
        @DisplayName("actualiza updatedAt al reactivar")
        void shouldUpdateUpdatedAt() {
            person.suspend("Suspension temporal");
            person.reactivate("Conclusion del proceso");
            assertThat(person.getUpdatedAt()).isAfterOrEqualTo(person.getCreatedAt());
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el motivo es nulo")
        void shouldThrowNullPointerExceptionForNullReason() {
            person.suspend("Suspension temporal");
            assertThatThrownBy(() -> person.reactivate(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza BusinessRuleException cuando el estado es ACTIVE")
        void shouldThrowBusinessRuleExceptionWhenActive() {
            assertThatThrownBy(() -> person.reactivate("Intento de reactivacion"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("lanza BusinessRuleException cuando el estado es REVOKED")
        void shouldThrowBusinessRuleExceptionWhenRevoked() {
            person.revoke("Resolucion judicial");
            assertThatThrownBy(() -> person.reactivate("Intento de reactivacion"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("REVOKED");
        }
    }

    @Nested
    @DisplayName("revoke()")
    class Revoke {

        @Test
        @DisplayName("transiciona de ACTIVE a REVOKED")
        void shouldTransitionFromActiveToRevoked() {
            person.revoke("Resolucion judicial 2026-MX-001");
            assertThat(person.getStatus()).isEqualTo(PersonStatus.REVOKED);
            assertThat(person.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("transiciona de SUSPENDED a REVOKED")
        void shouldTransitionFromSuspendedToRevoked() {
            person.suspend("Suspension previa");
            person.revoke("Resolucion judicial 2026-MX-001");
            assertThat(person.getStatus()).isEqualTo(PersonStatus.REVOKED);
        }

        @Test
        @DisplayName("actualiza updatedAt al revocar")
        void shouldUpdateUpdatedAt() {
            person.revoke("Resolucion judicial");
            assertThat(person.getUpdatedAt()).isAfterOrEqualTo(person.getCreatedAt());
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el motivo es nulo")
        void shouldThrowNullPointerExceptionForNullReason() {
            assertThatThrownBy(() -> person.revoke(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza BusinessRuleException cuando el estado ya es REVOKED")
        void shouldThrowBusinessRuleExceptionWhenAlreadyRevoked() {
            person.revoke("Primera revocacion");
            assertThatThrownBy(() -> person.revoke("Segunda revocacion"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("REVOKED");
        }
    }

    @Nested
    @DisplayName("updateFullName()")
    class UpdateFullName {

        @Test
        @DisplayName("actualiza el nombre cuando el estado es ACTIVE")
        void shouldUpdateFullNameWhenActive() {
            FullName newName = FullName.of("Rafael", "Morales", "Garcia");
            person.updateFullName(newName);
            assertThat(person.getFullName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("actualiza el nombre cuando el estado es SUSPENDED")
        void shouldUpdateFullNameWhenSuspended() {
            person.suspend("Suspension temporal");
            FullName newName = FullName.of("Rafael", "Morales", "Garcia");
            person.updateFullName(newName);
            assertThat(person.getFullName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el nombre es nulo")
        void shouldThrowNullPointerExceptionForNullFullName() {
            assertThatThrownBy(() -> person.updateFullName(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza BusinessRuleException cuando el estado es REVOKED")
        void shouldThrowBusinessRuleExceptionWhenRevoked() {
            person.revoke("Resolucion judicial");
            assertThatThrownBy(() -> person.updateFullName(FULL_NAME))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("equals y hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("dos Person con el mismo id son iguales")
        void shouldBeEqualForSameId() {
            Person reconstituted = Person.reconstitute(
                    person.getId(),
                    person.getCurp(),
                    person.getFullName(),
                    person.getBirthDate(),
                    person.getStatus(),
                    person.getCreatedAt(),
                    person.getUpdatedAt()
            );
            assertThat(person).isEqualTo(reconstituted);
            assertThat(person.hashCode()).isEqualTo(reconstituted.hashCode());
        }

        @Test
        @DisplayName("dos Person con ids distintos no son iguales")
        void shouldNotBeEqualForDifferentIds() {
            Person another = Person.register(CURP, FULL_NAME, BIRTH_DATE);
            assertThat(person).isNotEqualTo(another);
        }
    }

    @Nested
    @DisplayName("isActive(), isSuspended(), isRevoked()")
    class StatusPredicates {

        @Test
        @DisplayName("isActive retorna true para una persona recien registrada")
        void shouldBeActiveAfterRegister() {
            assertThat(person.isActive()).isTrue();
            assertThat(person.isSuspended()).isFalse();
            assertThat(person.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("isSuspended retorna true despues de suspender")
        void shouldBeSuspendedAfterSuspend() {
            person.suspend("Motivo");
            assertThat(person.isActive()).isFalse();
            assertThat(person.isSuspended()).isTrue();
            assertThat(person.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("isRevoked retorna true despues de revocar")
        void shouldBeRevokedAfterRevoke() {
            person.revoke("Motivo");
            assertThat(person.isActive()).isFalse();
            assertThat(person.isSuspended()).isFalse();
            assertThat(person.isRevoked()).isTrue();
        }
    }
}