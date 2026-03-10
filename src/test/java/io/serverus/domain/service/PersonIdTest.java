package io.serverus.domain.service;

import io.serverus.domain.exception.ValidationException;
import io.serverus.domain.model.vo.PersonId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios del Value Object PersonId.
 *
 * Verifica las invariantes, factory methods y comportamiento
 * definidos en el Value Object. No requiere contenedor ni
 * base de datos — corre en memoria.
 *
 * @since 1.0.0
 */
@DisplayName("PersonId")
class PersonIdTest {

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("genera un identificador unico no nulo")
        void shouldGenerateNonNullId() {
            PersonId id = PersonId.generate();
            assertThat(id).isNotNull();
            assertThat(id.value()).isNotNull();
        }

        @Test
        @DisplayName("genera identificadores distintos en cada llamada")
        void shouldGenerateUniqueIds() {
            PersonId first = PersonId.generate();
            PersonId second = PersonId.generate();
            assertThat(first).isNotEqualTo(second);
        }
    }

    @Nested
    @DisplayName("of(String)")
    class OfString {

        @Test
        @DisplayName("construye un PersonId desde un UUID valido en cadena")
        void shouldConstructFromValidString() {
            String uuid = UUID.randomUUID().toString();
            PersonId id = PersonId.of(uuid);
            assertThat(id.value().toString()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("lanza ValidationException cuando el valor no es un UUID valido")
        void shouldThrowValidationExceptionForInvalidUuid() {
            assertThatThrownBy(() -> PersonId.of("not-a-uuid"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not-a-uuid");
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el valor es nulo")
        void shouldThrowNullPointerExceptionForNullValue() {
            assertThatThrownBy(() -> PersonId.of((String) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("of(UUID)")
    class OfUuid {

        @Test
        @DisplayName("construye un PersonId desde un UUID valido")
        void shouldConstructFromValidUuid() {
            UUID uuid = UUID.randomUUID();
            PersonId id = PersonId.of(uuid);
            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("lanza NullPointerException cuando el UUID es nulo")
        void shouldThrowNullPointerExceptionForNullUuid() {
            assertThatThrownBy(() -> PersonId.of((UUID) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("isValid(String)")
    class IsValid {

        @Test
        @DisplayName("retorna true para un UUID valido")
        void shouldReturnTrueForValidUuid() {
            assertThat(PersonId.isValid(UUID.randomUUID().toString())).isTrue();
        }

        @Test
        @DisplayName("retorna false para una cadena invalida")
        void shouldReturnFalseForInvalidString() {
            assertThat(PersonId.isValid("not-a-uuid")).isFalse();
        }

        @Test
        @DisplayName("retorna false para una cadena nula")
        void shouldReturnFalseForNullString() {
            assertThat(PersonId.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("retorna false para una cadena vacia")
        void shouldReturnFalseForBlankString() {
            assertThat(PersonId.isValid("   ")).isFalse();
        }
    }

    @Nested
    @DisplayName("NONE")
    class None {

        @Test
        @DisplayName("NONE no es nulo")
        void shouldNotBeNull() {
            assertThat(PersonId.NONE).isNotNull();
        }

        @Test
        @DisplayName("isNone() retorna true para NONE")
        void shouldReturnTrueForNone() {
            assertThat(PersonId.NONE.isNone()).isTrue();
        }

        @Test
        @DisplayName("isNone() retorna false para un identificador generado")
        void shouldReturnFalseForGeneratedId() {
            assertThat(PersonId.generate().isNone()).isFalse();
        }
    }

    @Nested
    @DisplayName("equals y hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("dos PersonId con el mismo UUID son iguales")
        void shouldBeEqualForSameUuid() {
            UUID uuid = UUID.randomUUID();
            PersonId first = PersonId.of(uuid);
            PersonId second = PersonId.of(uuid);
            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        @DisplayName("dos PersonId con UUID distintos no son iguales")
        void shouldNotBeEqualForDifferentUuids() {
            PersonId first = PersonId.generate();
            PersonId second = PersonId.generate();
            assertThat(first).isNotEqualTo(second);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("retorna el UUID en formato estandar")
        void shouldReturnUuidString() {
            UUID uuid = UUID.randomUUID();
            PersonId id = PersonId.of(uuid);
            assertThat(id.toString()).isEqualTo(uuid.toString());
        }
    }
}