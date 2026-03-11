package io.serverus.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de la jerarquia de excepciones del dominio.
 *
 * Verifica constructores, getters y herencia de las cuatro clases
 * de excepcion. Estos tests son tests unitarios puros — no requieren
 * contenedor Quarkus ni base de datos.
 *
 * @since 1.0.0
 */
@DisplayName("Domain Exception hierarchy")
class DomainExceptionTest {

    // -------------------------------------------------------------------------
    // ValidationException
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ValidationException")
    class ValidationExceptionTests {

        @Test
        @DisplayName("construye con errorCode, field y message correctos")
        void shouldConstructWithAllFields() {
            ValidationException ex = new ValidationException(
                    "INVALID_CURP_FORMAT",
                    "curp",
                    "Curp value does not match RENAPO official format: INVALIDA"
            );

            assertThat(ex.getErrorCode()).isEqualTo("INVALID_CURP_FORMAT");
            assertThat(ex.getField()).isEqualTo("curp");
            assertThat(ex.getMessage()).isEqualTo("Curp value does not match RENAPO official format: INVALIDA");
        }

        @Test
        @DisplayName("es instancia de DomainException")
        void shouldExtendDomainException() {
            ValidationException ex = new ValidationException("CODE", "field", "msg");
            assertThat(ex).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("getField retorna null cuando el campo es null")
        void shouldAllowNullField() {
            ValidationException ex = new ValidationException("CODE", null, "msg");
            assertThat(ex.getField()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // BusinessRuleException
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("BusinessRuleException")
    class BusinessRuleExceptionTests {

        @Test
        @DisplayName("constructor simple preserva errorCode y message")
        void shouldConstructWithErrorCodeAndMessage() {
            BusinessRuleException ex = new BusinessRuleException(
                    "PERSON_ALREADY_REGISTERED",
                    "A person with CURP MOAA800101HDFRRN09 is already registered"
            );

            assertThat(ex.getErrorCode()).isEqualTo("PERSON_ALREADY_REGISTERED");
            assertThat(ex.getMessage()).isEqualTo("A person with CURP MOAA800101HDFRRN09 is already registered");
        }

        @Test
        @DisplayName("constructor con cause preserva la causa raiz")
        void shouldConstructWithCause() {
            RuntimeException cause = new RuntimeException("causa subyacente");
            BusinessRuleException ex = new BusinessRuleException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot transition from ACTIVE to ACTIVE",
                    cause
            );

            assertThat(ex.getErrorCode()).isEqualTo("INVALID_STATUS_TRANSITION");
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getMessage()).isEqualTo("Cannot transition from ACTIVE to ACTIVE");
        }

        @Test
        @DisplayName("es instancia de DomainException")
        void shouldExtendDomainException() {
            BusinessRuleException ex = new BusinessRuleException("CODE", "msg");
            assertThat(ex).isInstanceOf(DomainException.class);
        }
    }

    // -------------------------------------------------------------------------
    // ResourceNotFoundException
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("constructor preserva errorCode y message")
        void shouldConstructWithErrorCodeAndMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "PERSON_NOT_FOUND",
                    "Person with id 00000000-0000-0000-0000-000000000000 not found"
            );

            assertThat(ex.getErrorCode()).isEqualTo("PERSON_NOT_FOUND");
            assertThat(ex.getMessage()).contains("00000000-0000-0000-0000-000000000000");
        }

        @Test
        @DisplayName("es instancia de DomainException")
        void shouldExtendDomainException() {
            ResourceNotFoundException ex = new ResourceNotFoundException("CODE", "msg");
            assertThat(ex).isInstanceOf(DomainException.class);
        }
    }

    // -------------------------------------------------------------------------
    // DomainException — constructor con cause (compartido por BusinessRuleException)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DomainException")
    class DomainExceptionTests {

        @Test
        @DisplayName("getErrorCode retorna el codigo asignado en construccion")
        void shouldReturnErrorCode() {
            BusinessRuleException ex = new BusinessRuleException("MY_CODE", "msg");
            assertThat(ex.getErrorCode()).isEqualTo("MY_CODE");
        }

        @Test
        @DisplayName("constructor con cause propaga correctamente al RuntimeException padre")
        void shouldPropagateMessageAndCauseToParent() {
            IllegalStateException cause = new IllegalStateException("estado invalido");
            BusinessRuleException ex = new BusinessRuleException("CODE", "message", cause);

            assertThat(ex.getMessage()).isEqualTo("message");
            assertThat(ex.getCause()).isInstanceOf(IllegalStateException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("estado invalido");
        }
    }
}