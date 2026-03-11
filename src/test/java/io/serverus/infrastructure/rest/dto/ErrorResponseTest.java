package io.serverus.infrastructure.rest.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Tests unitarios de ErrorResponse.
 *
 * ErrorResponse es un record inmutable con dos factory methods estaticos.
 * No requiere contenedor Quarkus — es un test unitario puro.
 *
 * @since 1.0.0
 */
@DisplayName("ErrorResponse")
class ErrorResponseTest {

    @Nested
    @DisplayName("of(errorCode, message, path)")
    class Of {

        @Test
        @DisplayName("construye con field nulo y timestamp cercano al momento actual")
        void shouldConstructWithNullField() {
            LocalDateTime before = LocalDateTime.now();

            ErrorResponse response = ErrorResponse.of(
                    "PERSON_NOT_FOUND",
                    "Person with id 00000000-0000-0000-0000-000000000000 not found",
                    "/persons/00000000-0000-0000-0000-000000000000"
            );

            assertThat(response.errorCode()).isEqualTo("PERSON_NOT_FOUND");
            assertThat(response.message()).contains("00000000-0000-0000-0000-000000000000");
            assertThat(response.field()).isNull();
            assertThat(response.path()).isEqualTo("/persons/00000000-0000-0000-0000-000000000000");
            assertThat(response.timestamp()).isCloseTo(before, within(2, SECONDS));
        }
    }

    @Nested
    @DisplayName("ofField(errorCode, message, field, path)")
    class OfField {

        @Test
        @DisplayName("construye con field no nulo para errores de validacion de dominio")
        void shouldConstructWithField() {
            LocalDateTime before = LocalDateTime.now();

            ErrorResponse response = ErrorResponse.ofField(
                    "INVALID_CURP_FORMAT",
                    "Curp value does not match RENAPO official format: INVALIDA",
                    "curp",
                    "/persons"
            );

            assertThat(response.errorCode()).isEqualTo("INVALID_CURP_FORMAT");
            assertThat(response.message()).contains("INVALIDA");
            assertThat(response.field()).isEqualTo("curp");
            assertThat(response.path()).isEqualTo("/persons");
            assertThat(response.timestamp()).isCloseTo(before, within(2, SECONDS));
        }

        @Test
        @DisplayName("field puede ser nulo cuando ofField se llama sin campo especifico")
        void shouldAllowNullField() {
            ErrorResponse response = ErrorResponse.ofField(
                    "SOME_ERROR", "mensaje", null, "/path"
            );
            assertThat(response.field()).isNull();
        }
    }

    @Nested
    @DisplayName("record — equals y toString")
    class RecordBehavior {

        @Test
        @DisplayName("dos instancias con los mismos valores son iguales")
        void shouldBeEqualWhenSameValues() {
            LocalDateTime ts = LocalDateTime.of(2026, 3, 11, 10, 0, 0);
            ErrorResponse a = new ErrorResponse("CODE", "msg", null, ts, "/path");
            ErrorResponse b = new ErrorResponse("CODE", "msg", null, ts, "/path");
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("toString contiene el errorCode")
        void shouldIncludeErrorCodeInToString() {
            LocalDateTime ts = LocalDateTime.now();
            ErrorResponse response = new ErrorResponse("MY_CODE", "msg", null, ts, "/p");
            assertThat(response.toString()).contains("MY_CODE");
        }
    }
}