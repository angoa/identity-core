package io.serverus.domain.model.vo;

import io.serverus.domain.exception.ValidationException;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object que representa el identificador unico de una persona
 * dentro del sistema de identidad digital.
 *
 * Encapsula un UUID como identificador interno del sistema,
 * desacoplado de identificadores externos como CURP o RFC.
 * Garantiza inmutabilidad y validacion en el momento de construccion.
 *
 * Invariantes:
 * - El valor nunca es nulo.
 * - Una vez construido, el valor no puede modificarse.
 *
 * Uso:
 * <pre>{@code
 * PersonId id = PersonId.generate();
 * PersonId id = PersonId.of(uuid);
 * PersonId id = PersonId.of("550e8400-e29b-41d4-a716-446655440000");
 *
 * if (PersonId.isValid(input)) {
 *     PersonId id = PersonId.of(input);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public record PersonId(UUID value) implements Serializable {

    public static final PersonId NONE = new PersonId(new UUID(0L, 0L));

    public PersonId {
        Objects.requireNonNull(value, "PersonId value must not be null");
    }

    public static PersonId generate() {
        return new PersonId(UUID.randomUUID());
    }

    public static PersonId of(String value) {
        Objects.requireNonNull(value, "PersonId string value must not be null");
        try {
            return new PersonId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "INVALID_PERSON_ID_FORMAT",
                    "id",
                    "PersonId value is not a valid UUID: " + value
            );
        }
    }

    public static PersonId of(UUID value) {
        return new PersonId(value);
    }

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isNone() {
        return NONE.equals(this);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}