package io.serverus.domain.service;

import io.serverus.domain.exception.ValidationException;
import io.serverus.domain.model.vo.FullName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios del Value Object FullName.
 *
 * Verifica las invariantes, factory methods, normalizacion y
 * comportamiento de presentacion definidos en el Value Object.
 * No requiere contenedor ni base de datos — corre en memoria.
 *
 * @since 1.0.0
 */
@DisplayName("FullName")
class FullNameTest {

    private static final String GIVEN_NAME        = "Rafael";
    private static final String PATERNAL_SURNAME  = "Morales";
    private static final String MATERNAL_SURNAME  = "Angoa";

    @Nested
    @DisplayName("of(String, String, String)")
    class OfThreeArgs {

        @Test
        @DisplayName("construye un FullName con los tres componentes")
        void shouldConstructWithAllComponents() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(name.givenName()).isEqualTo(GIVEN_NAME.toUpperCase());
            assertThat(name.paternalSurname()).isEqualTo(PATERNAL_SURNAME.toUpperCase());
            assertThat(name.maternalSurname()).isEqualTo(MATERNAL_SURNAME.toUpperCase());
        }

        @Test
        @DisplayName("construye un FullName sin apellido materno cuando es nulo")
        void shouldConstructWithNullMaternalSurname() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, null);
            assertThat(name.maternalSurname()).isNull();
        }

        @Test
        @DisplayName("normaliza todos los componentes a mayusculas en construccion")
        void shouldNormalizeToUpperCase() {
            FullName name = FullName.of("rafael", "morales", "angoa");
            assertThat(name.givenName()).isEqualTo("RAFAEL");
            assertThat(name.paternalSurname()).isEqualTo("MORALES");
            assertThat(name.maternalSurname()).isEqualTo("ANGOA");
        }

        @Test
        @DisplayName("elimina espacios al inicio y al final en construccion")
        void shouldTrimSpaces() {
            FullName name = FullName.of("  Rafael  ", "  Morales  ", "  Angoa  ");
            assertThat(name.givenName()).isEqualTo("RAFAEL");
            assertThat(name.paternalSurname()).isEqualTo("MORALES");
            assertThat(name.maternalSurname()).isEqualTo("ANGOA");
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el nombre de pila es nulo")
        void shouldThrowNullPointerExceptionForNullGivenName() {
            assertThatThrownBy(() -> FullName.of(null, PATERNAL_SURNAME, MATERNAL_SURNAME))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el apellido paterno es nulo")
        void shouldThrowNullPointerExceptionForNullPaternalSurname() {
            assertThatThrownBy(() -> FullName.of(GIVEN_NAME, null, MATERNAL_SURNAME))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza ValidationException cuando el nombre de pila esta vacio")
        void shouldThrowValidationExceptionForBlankGivenName() {
            assertThatThrownBy(() -> FullName.of("   ", PATERNAL_SURNAME, MATERNAL_SURNAME))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("givenName");
        }

        @Test
        @DisplayName("lanza ValidationException cuando el nombre de pila es demasiado corto")
        void shouldThrowValidationExceptionForShortGivenName() {
            assertThatThrownBy(() -> FullName.of("A", PATERNAL_SURNAME, MATERNAL_SURNAME))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("givenName");
        }

        @Test
        @DisplayName("acepta un nombre de pila con exactamente la longitud minima (2 caracteres)")
        void shouldAcceptGivenNameAtMinimumLength() {
            FullName name = FullName.of("Al", PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(name.givenName()).isEqualTo("AL");
        }

        @Test
        @DisplayName("lanza ValidationException cuando el apellido paterno es demasiado largo")
        void shouldThrowValidationExceptionForLongPaternalSurname() {
            String longName = "A".repeat(81);
            assertThatThrownBy(() -> FullName.of(GIVEN_NAME, longName, MATERNAL_SURNAME))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("paternalSurname");
        }

        @Test
        @DisplayName("acepta un apellido paterno con exactamente la longitud maxima (80 caracteres)")
        void shouldAcceptPaternalSurnameAtMaximumLength() {
            String maxName = "A".repeat(80);
            FullName name = FullName.of(GIVEN_NAME, maxName, MATERNAL_SURNAME);
            assertThat(name.paternalSurname()).isEqualTo(maxName);
        }

        @Test
        @DisplayName("acepta un nombre de pila con exactamente la longitud maxima (80 caracteres)")
        void shouldAcceptGivenNameAtMaximumLength() {
            String maxName = "A".repeat(80);
            FullName name = FullName.of(maxName, PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(name.givenName()).isEqualTo(maxName);
        }

        @Test
        @DisplayName("lanza ValidationException cuando el nombre de pila supera la longitud maxima")
        void shouldThrowValidationExceptionForTooLongGivenName() {
            String longName = "A".repeat(81);
            assertThatThrownBy(() -> FullName.of(longName, PATERNAL_SURNAME, MATERNAL_SURNAME))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("givenName");
        }

        @Test
        @DisplayName("lanza ValidationException cuando el apellido paterno esta vacio")
        void shouldThrowValidationExceptionForBlankPaternalSurname() {
            assertThatThrownBy(() -> FullName.of(GIVEN_NAME, "   ", MATERNAL_SURNAME))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("paternalSurname");
        }

        @Test
        @DisplayName("acepta un apellido paterno con exactamente la longitud minima (2 caracteres)")
        void shouldAcceptPaternalSurnameAtMinimumLength() {
            FullName name = FullName.of(GIVEN_NAME, "Lu", MATERNAL_SURNAME);
            assertThat(name.paternalSurname()).isEqualTo("LU");
        }

        @Test
        @DisplayName("lanza ValidationException cuando el apellido paterno es demasiado corto")
        void shouldThrowValidationExceptionForShortPaternalSurname() {
            assertThatThrownBy(() -> FullName.of(GIVEN_NAME, "A", MATERNAL_SURNAME))
                    .isInstanceOf(ValidationException.class)
                    .extracting("field")
                    .isEqualTo("paternalSurname");
        }
    }

    @Nested
    @DisplayName("of(String, String)")
    class OfTwoArgs {

        @Test
        @DisplayName("construye un FullName sin apellido materno")
        void shouldConstructWithoutMaternalSurname() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME);
            assertThat(name.givenName()).isEqualTo(GIVEN_NAME.toUpperCase());
            assertThat(name.paternalSurname()).isEqualTo(PATERNAL_SURNAME.toUpperCase());
            assertThat(name.maternalSurname()).isNull();
        }
    }

    @Nested
    @DisplayName("hasMaternalSurname()")
    class HasMaternalSurname {

        @Test
        @DisplayName("retorna true cuando el apellido materno esta presente")
        void shouldReturnTrueWhenMaternalSurnameIsPresent() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(name.hasMaternalSurname()).isTrue();
        }

        @Test
        @DisplayName("retorna false cuando el apellido materno es nulo")
        void shouldReturnFalseWhenMaternalSurnameIsNull() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, null);
            assertThat(name.hasMaternalSurname()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDisplayString()")
    class ToDisplayString {

        @Test
        @DisplayName("retorna formato oficial con apellido materno")
        void shouldReturnOfficialFormatWithMaternalSurname() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(name.toDisplayString()).isEqualTo("MORALES ANGOA, RAFAEL");
        }

        @Test
        @DisplayName("retorna formato oficial sin apellido materno")
        void shouldReturnOfficialFormatWithoutMaternalSurname() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME);
            assertThat(name.toDisplayString()).isEqualTo("MORALES, RAFAEL");
        }
    }

    @Nested
    @DisplayName("toSearchString()")
    class ToSearchString {

        @Test
        @DisplayName("retorna formato de busqueda con apellido materno")
        void shouldReturnSearchFormatWithMaternalSurname() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(name.toSearchString()).isEqualTo("RAFAEL MORALES ANGOA");
        }

        @Test
        @DisplayName("retorna formato de busqueda sin apellido materno")
        void shouldReturnSearchFormatWithoutMaternalSurname() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME);
            assertThat(name.toSearchString()).isEqualTo("RAFAEL MORALES");
        }
    }

    @Nested
    @DisplayName("equals y hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("dos FullName con los mismos componentes son iguales")
        void shouldBeEqualForSameComponents() {
            FullName first  = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, MATERNAL_SURNAME);
            FullName second = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        @DisplayName("dos FullName con componentes distintos no son iguales")
        void shouldNotBeEqualForDifferentComponents() {
            FullName first  = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, MATERNAL_SURNAME);
            FullName second = FullName.of("Juan", PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("un FullName en minusculas es igual al mismo en mayusculas")
        void shouldBeEqualRegardlessOfCase() {
            FullName fromLower = FullName.of("rafael", "morales", "angoa");
            FullName fromUpper = FullName.of("RAFAEL", "MORALES", "ANGOA");
            assertThat(fromLower).isEqualTo(fromUpper);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("retorna el formato de presentacion oficial")
        void shouldReturnDisplayFormat() {
            FullName name = FullName.of(GIVEN_NAME, PATERNAL_SURNAME, MATERNAL_SURNAME);
            assertThat(name.toString()).isEqualTo("MORALES ANGOA, RAFAEL");
        }
    }
}