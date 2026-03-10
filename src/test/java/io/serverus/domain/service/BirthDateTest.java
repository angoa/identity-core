package io.serverus.domain.service;

import io.serverus.domain.exception.ValidationException;
import io.serverus.domain.model.vo.BirthDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios del Value Object BirthDate.
 *
 * Verifica las invariantes, factory methods, validacion de rango
 * temporal y comportamiento de negocio definidos en el Value Object.
 * No requiere contenedor ni base de datos — corre en memoria.
 *
 * @since 1.0.0
 */
@DisplayName("BirthDate")
class BirthDateTest {

    private static final LocalDate VALID_DATE        = LocalDate.of(1980, 1, 1);
    private static final LocalDate MINOR_DATE        = LocalDate.now().minusYears(10);
    private static final LocalDate ADULT_DATE        = LocalDate.now().minusYears(25);
    private static final LocalDate EXACT_LEGAL_DATE  = LocalDate.now().minusYears(18);
    private static final LocalDate BEFORE_MIN_DATE   = LocalDate.of(1899, 12, 31);
    private static final String    VALID_DATE_STRING = "1980-01-01";
    private static final String    INVALID_FORMAT    = "01-01-1980";

    @Nested
    @DisplayName("of(LocalDate)")
    class OfLocalDate {

        @Test
        @DisplayName("construye un BirthDate desde una fecha valida")
        void shouldConstructFromValidDate() {
            BirthDate birthDate = BirthDate.of(VALID_DATE);
            assertThat(birthDate.value()).isEqualTo(VALID_DATE);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando la fecha es nula")
        void shouldThrowNullPointerExceptionForNullDate() {
            assertThatThrownBy(() -> BirthDate.of((LocalDate) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza ValidationException cuando la fecha es futura")
        void shouldThrowValidationExceptionForFutureDate() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            assertThatThrownBy(() -> BirthDate.of(tomorrow))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("birthDate");
        }

        @Test
        @DisplayName("lanza ValidationException cuando la fecha es anterior al minimo permitido")
        void shouldThrowValidationExceptionForDateBeforeMinimum() {
            assertThatThrownBy(() -> BirthDate.of(BEFORE_MIN_DATE))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("birthDate");
        }

        @Test
        @DisplayName("acepta la fecha de hoy como fecha de nacimiento valida")
        void shouldAcceptTodayAsValidDate() {
            BirthDate birthDate = BirthDate.of(LocalDate.now());
            assertThat(birthDate.value()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("acepta el minimo permitido como fecha de nacimiento valida")
        void shouldAcceptMinimumDateAsValid() {
            LocalDate minDate = LocalDate.of(1900, 1, 1);
            BirthDate birthDate = BirthDate.of(minDate);
            assertThat(birthDate.value()).isEqualTo(minDate);
        }
    }

    @Nested
    @DisplayName("of(String)")
    class OfString {

        @Test
        @DisplayName("construye un BirthDate desde una cadena ISO-8601 valida")
        void shouldConstructFromValidIsoString() {
            BirthDate birthDate = BirthDate.of(VALID_DATE_STRING);
            assertThat(birthDate.value()).isEqualTo(VALID_DATE);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando la cadena es nula")
        void shouldThrowNullPointerExceptionForNullString() {
            assertThatThrownBy(() -> BirthDate.of((String) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza ValidationException cuando el formato no es ISO-8601")
        void shouldThrowValidationExceptionForInvalidFormat() {
            assertThatThrownBy(() -> BirthDate.of(INVALID_FORMAT))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("birthDate");
        }

        @Test
        @DisplayName("lanza ValidationException cuando la fecha es futura en cadena")
        void shouldThrowValidationExceptionForFutureDateString() {
            String tomorrow = LocalDate.now().plusDays(1).toString();
            assertThatThrownBy(() -> BirthDate.of(tomorrow))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("isValid(String)")
    class IsValid {

        @Test
        @DisplayName("retorna true para una fecha valida en formato ISO-8601")
        void shouldReturnTrueForValidIsoDate() {
            assertThat(BirthDate.isValid(VALID_DATE_STRING)).isTrue();
        }

        @Test
        @DisplayName("retorna false para un valor nulo")
        void shouldReturnFalseForNullValue() {
            assertThat(BirthDate.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("retorna false para una cadena vacia")
        void shouldReturnFalseForBlankString() {
            assertThat(BirthDate.isValid("   ")).isFalse();
        }

        @Test
        @DisplayName("retorna false para un formato incorrecto")
        void shouldReturnFalseForInvalidFormat() {
            assertThat(BirthDate.isValid(INVALID_FORMAT)).isFalse();
        }

        @Test
        @DisplayName("retorna false para una fecha futura")
        void shouldReturnFalseForFutureDate() {
            String tomorrow = LocalDate.now().plusDays(1).toString();
            assertThat(BirthDate.isValid(tomorrow)).isFalse();
        }

        @Test
        @DisplayName("retorna false para una fecha anterior al minimo permitido")
        void shouldReturnFalseForDateBeforeMinimum() {
            assertThat(BirthDate.isValid(BEFORE_MIN_DATE.toString())).isFalse();
        }
    }

    @Nested
    @DisplayName("calculateAge()")
    class CalculateAge {

        @Test
        @DisplayName("calcula la edad correctamente para una fecha conocida")
        void shouldCalculateAgeCorrectly() {
            BirthDate birthDate = BirthDate.of(ADULT_DATE);
            assertThat(birthDate.calculateAge()).isEqualTo(25);
        }

        @Test
        @DisplayName("calcula edad cero para una persona nacida hoy")
        void shouldReturnZeroAgeForToday() {
            BirthDate birthDate = BirthDate.of(LocalDate.now());
            assertThat(birthDate.calculateAge()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("isAdult()")
    class IsAdult {

        @Test
        @DisplayName("retorna true para una persona mayor de 18 anos")
        void shouldReturnTrueForAdult() {
            BirthDate birthDate = BirthDate.of(ADULT_DATE);
            assertThat(birthDate.isAdult()).isTrue();
        }

        @Test
        @DisplayName("retorna true para una persona con exactamente 18 anos")
        void shouldReturnTrueForExactLegalAge() {
            BirthDate birthDate = BirthDate.of(EXACT_LEGAL_DATE);
            assertThat(birthDate.isAdult()).isTrue();
        }

        @Test
        @DisplayName("retorna false para una persona menor de 18 anos")
        void shouldReturnFalseForMinor() {
            BirthDate birthDate = BirthDate.of(MINOR_DATE);
            assertThat(birthDate.isAdult()).isFalse();
        }
    }

    @Nested
    @DisplayName("isMinor()")
    class IsMinor {

        @Test
        @DisplayName("retorna true para una persona menor de 18 anos")
        void shouldReturnTrueForMinor() {
            BirthDate birthDate = BirthDate.of(MINOR_DATE);
            assertThat(birthDate.isMinor()).isTrue();
        }

        @Test
        @DisplayName("retorna false para una persona mayor de 18 anos")
        void shouldReturnFalseForAdult() {
            BirthDate birthDate = BirthDate.of(ADULT_DATE);
            assertThat(birthDate.isMinor()).isFalse();
        }
    }

    @Nested
    @DisplayName("equals y hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("dos BirthDate con la misma fecha son iguales")
        void shouldBeEqualForSameDate() {
            BirthDate first  = BirthDate.of(VALID_DATE);
            BirthDate second = BirthDate.of(VALID_DATE);
            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        @DisplayName("dos BirthDate con fechas distintas no son iguales")
        void shouldNotBeEqualForDifferentDates() {
            BirthDate first  = BirthDate.of(VALID_DATE);
            BirthDate second = BirthDate.of(ADULT_DATE);
            assertThat(first).isNotEqualTo(second);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("retorna la fecha en formato ISO-8601")
        void shouldReturnIsoFormat() {
            BirthDate birthDate = BirthDate.of(VALID_DATE);
            assertThat(birthDate.toString()).isEqualTo(VALID_DATE_STRING);
        }
    }
}