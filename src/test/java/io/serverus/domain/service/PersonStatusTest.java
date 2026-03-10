package io.serverus.domain.service;

import io.serverus.domain.model.vo.PersonStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios del Value Object PersonStatus.
 *
 * Verifica el comportamiento de negocio y las transiciones validas
 * del ciclo de vida definidas en el enum.
 * No requiere contenedor ni base de datos — corre en memoria.
 *
 * @since 1.0.0
 */
@DisplayName("PersonStatus")
class PersonStatusTest {

    @Nested
    @DisplayName("allowsCredentialIssuance()")
    class AllowsCredentialIssuance {

        @Test
        @DisplayName("retorna true cuando el estado es ACTIVE")
        void shouldReturnTrueForActive() {
            assertThat(PersonStatus.ACTIVE.allowsCredentialIssuance()).isTrue();
        }

        @Test
        @DisplayName("retorna false cuando el estado es SUSPENDED")
        void shouldReturnFalseForSuspended() {
            assertThat(PersonStatus.SUSPENDED.allowsCredentialIssuance()).isFalse();
        }

        @Test
        @DisplayName("retorna false cuando el estado es REVOKED")
        void shouldReturnFalseForRevoked() {
            assertThat(PersonStatus.REVOKED.allowsCredentialIssuance()).isFalse();
        }
    }

    @Nested
    @DisplayName("allowsCredentialUsage()")
    class AllowsCredentialUsage {

        @Test
        @DisplayName("retorna true cuando el estado es ACTIVE")
        void shouldReturnTrueForActive() {
            assertThat(PersonStatus.ACTIVE.allowsCredentialUsage()).isTrue();
        }

        @Test
        @DisplayName("retorna false cuando el estado es SUSPENDED")
        void shouldReturnFalseForSuspended() {
            assertThat(PersonStatus.SUSPENDED.allowsCredentialUsage()).isFalse();
        }

        @Test
        @DisplayName("retorna false cuando el estado es REVOKED")
        void shouldReturnFalseForRevoked() {
            assertThat(PersonStatus.REVOKED.allowsCredentialUsage()).isFalse();
        }
    }

    @Nested
    @DisplayName("allowsAttributeUpdate()")
    class AllowsAttributeUpdate {

        @Test
        @DisplayName("retorna true cuando el estado es ACTIVE")
        void shouldReturnTrueForActive() {
            assertThat(PersonStatus.ACTIVE.allowsAttributeUpdate()).isTrue();
        }

        @Test
        @DisplayName("retorna true cuando el estado es SUSPENDED")
        void shouldReturnTrueForSuspended() {
            assertThat(PersonStatus.SUSPENDED.allowsAttributeUpdate()).isTrue();
        }

        @Test
        @DisplayName("retorna false cuando el estado es REVOKED")
        void shouldReturnFalseForRevoked() {
            assertThat(PersonStatus.REVOKED.allowsAttributeUpdate()).isFalse();
        }
    }

    @Nested
    @DisplayName("isTerminal()")
    class IsTerminal {

        @Test
        @DisplayName("retorna true cuando el estado es REVOKED")
        void shouldReturnTrueForRevoked() {
            assertThat(PersonStatus.REVOKED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("retorna false cuando el estado es ACTIVE")
        void shouldReturnFalseForActive() {
            assertThat(PersonStatus.ACTIVE.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("retorna false cuando el estado es SUSPENDED")
        void shouldReturnFalseForSuspended() {
            assertThat(PersonStatus.SUSPENDED.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("canTransitionTo()")
    class CanTransitionTo {

        @Nested
        @DisplayName("desde ACTIVE")
        class FromActive {

            @Test
            @DisplayName("permite transicion a SUSPENDED")
            void shouldAllowTransitionToSuspended() {
                assertThat(PersonStatus.ACTIVE.canTransitionTo(PersonStatus.SUSPENDED)).isTrue();
            }

            @Test
            @DisplayName("permite transicion a REVOKED")
            void shouldAllowTransitionToRevoked() {
                assertThat(PersonStatus.ACTIVE.canTransitionTo(PersonStatus.REVOKED)).isTrue();
            }

            @Test
            @DisplayName("no permite transicion a ACTIVE")
            void shouldNotAllowTransitionToActive() {
                assertThat(PersonStatus.ACTIVE.canTransitionTo(PersonStatus.ACTIVE)).isFalse();
            }
        }

        @Nested
        @DisplayName("desde SUSPENDED")
        class FromSuspended {

            @Test
            @DisplayName("permite transicion a ACTIVE")
            void shouldAllowTransitionToActive() {
                assertThat(PersonStatus.SUSPENDED.canTransitionTo(PersonStatus.ACTIVE)).isTrue();
            }

            @Test
            @DisplayName("permite transicion a REVOKED")
            void shouldAllowTransitionToRevoked() {
                assertThat(PersonStatus.SUSPENDED.canTransitionTo(PersonStatus.REVOKED)).isTrue();
            }

            @Test
            @DisplayName("no permite transicion a SUSPENDED")
            void shouldNotAllowTransitionToSuspended() {
                assertThat(PersonStatus.SUSPENDED.canTransitionTo(PersonStatus.SUSPENDED)).isFalse();
            }
        }

        @Nested
        @DisplayName("desde REVOKED")
        class FromRevoked {

            @ParameterizedTest
            @EnumSource(PersonStatus.class)
            @DisplayName("no permite transicion a ningun estado")
            void shouldNotAllowTransitionToAnyStatus(PersonStatus target) {
                assertThat(PersonStatus.REVOKED.canTransitionTo(target)).isFalse();
            }
        }
    }
}