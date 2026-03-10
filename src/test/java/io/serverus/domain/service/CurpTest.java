package io.serverus.domain.service;

import io.serverus.domain.exception.ValidationException;
import io.serverus.domain.model.vo.Curp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios del Value Object Curp.
 *
 * Verifica las invariantes, factory methods, normalizacion y
 * comportamiento de extraccion definidos en el Value Object.
 * No requiere contenedor ni base de datos — corre en memoria.
 *
 * CURPs de prueba generadas con el formato oficial RENAPO.
 *
 * @since 1.0.0
 */
@DisplayName("Curp")
class CurpTest {

    // CURPs validas para pruebas — formato oficial RENAPO
    private static final String CURP_HOMBRE_CDMX   = "MOAA800101HDFRRN09";
    private static final String CURP_MUJER_JALISCO  = "GARE900215MJCRZL04";
    private static final String CURP_LOWERCASE      = "moaa800101hdfrrn09";
    private static final String CURP_WITH_SPACES    = "  MOAA800101HDFRRN09  ";

    @Nested
    @DisplayName("of(String)")
    class OfString {

        @Test
        @DisplayName("construye una CURP desde un valor valido")
        void shouldConstructFromValidCurp() {
            Curp curp = Curp.of(CURP_HOMBRE_CDMX);
            assertThat(curp.value()).isEqualTo(CURP_HOMBRE_CDMX);
        }

        @Test
        @DisplayName("normaliza a mayusculas en construccion")
        void shouldNormalizeToUpperCase() {
            Curp curp = Curp.of(CURP_LOWERCASE);
            assertThat(curp.value()).isEqualTo(CURP_HOMBRE_CDMX);
        }

        @Test
        @DisplayName("elimina espacios al inicio y al final en construccion")
        void shouldTrimSpaces() {
            Curp curp = Curp.of(CURP_WITH_SPACES);
            assertThat(curp.value()).isEqualTo(CURP_HOMBRE_CDMX);
        }

        @Test
        @DisplayName("lanza ValidationException cuando el formato no cumple el patron RENAPO")
        void shouldThrowValidationExceptionForInvalidFormat() {
            assertThatThrownBy(() -> Curp.of("INVALIDO123"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("INVALIDO123");
        }

        @Test
        @DisplayName("lanza ValidationException cuando el valor tiene longitud incorrecta")
        void shouldThrowValidationExceptionForWrongLength() {
            assertThatThrownBy(() -> Curp.of("MOAA800101"))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el valor es nulo")
        void shouldThrowNullPointerExceptionForNullValue() {
            assertThatThrownBy(() -> Curp.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza ValidationException cuando el sexo no es H ni M")
        void shouldThrowValidationExceptionForInvalidSex() {
            assertThatThrownBy(() -> Curp.of("MOAA800101XDFRRN09"))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("isValid(String)")
    class IsValid {

        @Test
        @DisplayName("retorna true para una CURP valida de hombre")
        void shouldReturnTrueForValidMaleCurp() {
            assertThat(Curp.isValid(CURP_HOMBRE_CDMX)).isTrue();
        }

        @Test
        @DisplayName("retorna true para una CURP valida de mujer")
        void shouldReturnTrueForValidFemaleCurp() {
            assertThat(Curp.isValid(CURP_MUJER_JALISCO)).isTrue();
        }

        @Test
        @DisplayName("retorna true para una CURP en minusculas")
        void shouldReturnTrueForLowerCaseCurp() {
            assertThat(Curp.isValid(CURP_LOWERCASE)).isTrue();
        }

        @Test
        @DisplayName("retorna false para un valor nulo")
        void shouldReturnFalseForNullValue() {
            assertThat(Curp.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("retorna false para una cadena vacia")
        void shouldReturnFalseForBlankString() {
            assertThat(Curp.isValid("   ")).isFalse();
        }

        @Test
        @DisplayName("retorna false para un formato incorrecto")
        void shouldReturnFalseForInvalidFormat() {
            assertThat(Curp.isValid("INVALIDO123")).isFalse();
        }
    }

    @Nested
    @DisplayName("extractSex()")
    class ExtractSex {

        @Test
        @DisplayName("extrae H para una CURP de hombre")
        void shouldExtractMaleSex() {
            Curp curp = Curp.of(CURP_HOMBRE_CDMX);
            assertThat(curp.extractSex()).isEqualTo('H');
        }

        @Test
        @DisplayName("extrae M para una CURP de mujer")
        void shouldExtractFemaleSex() {
            Curp curp = Curp.of(CURP_MUJER_JALISCO);
            assertThat(curp.extractSex()).isEqualTo('M');
        }
    }

    @Nested
    @DisplayName("extractStateCode()")
    class ExtractStateCode {

        @Test
        @DisplayName("extrae DF para una CURP de Ciudad de Mexico")
        void shouldExtractCdmxStateCode() {
            Curp curp = Curp.of(CURP_HOMBRE_CDMX);
            assertThat(curp.extractStateCode()).isEqualTo("DF");
        }

        @Test
        @DisplayName("extrae JC para una CURP de Jalisco")
        void shouldExtractJaliscoStateCode() {
            Curp curp = Curp.of(CURP_MUJER_JALISCO);
            assertThat(curp.extractStateCode()).isEqualTo("JC");
        }
    }

    @Nested
    @DisplayName("extractBirthDate()")
    class ExtractBirthDate {

        @Test
        @DisplayName("extrae la fecha de nacimiento en formato AAMMDD")
        void shouldExtractBirthDate() {
            Curp curp = Curp.of(CURP_HOMBRE_CDMX);
            assertThat(curp.extractBirthDate()).isEqualTo("800101");
        }
    }

    @Nested
    @DisplayName("equals y hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("dos Curp con el mismo valor son iguales")
        void shouldBeEqualForSameValue() {
            Curp first  = Curp.of(CURP_HOMBRE_CDMX);
            Curp second = Curp.of(CURP_HOMBRE_CDMX);
            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        @DisplayName("dos Curp con valores distintos no son iguales")
        void shouldNotBeEqualForDifferentValues() {
            Curp first  = Curp.of(CURP_HOMBRE_CDMX);
            Curp second = Curp.of(CURP_MUJER_JALISCO);
            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("una CURP en minusculas es igual a la misma en mayusculas")
        void shouldBeEqualRegardlessOfCase() {
            Curp fromLower = Curp.of(CURP_LOWERCASE);
            Curp fromUpper = Curp.of(CURP_HOMBRE_CDMX);
            assertThat(fromLower).isEqualTo(fromUpper);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("retorna la CURP en mayusculas")
        void shouldReturnUpperCaseCurp() {
            Curp curp = Curp.of(CURP_LOWERCASE);
            assertThat(curp.toString()).isEqualTo(CURP_HOMBRE_CDMX);
        }
    }
}