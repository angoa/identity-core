package io.serverus.domain.model.vo;

import io.serverus.domain.exception.ValidationException;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object que representa el nombre completo de una persona
 * dentro del sistema de identidad digital.
 *
 * Encapsula el nombre conforme al estandar de registro civil mexicano,
 * que distingue entre nombre de pila, apellido paterno y apellido materno.
 * Garantiza inmutabilidad y validacion en el momento de construccion.
 *
 * Invariantes:
 * - El nombre de pila nunca es nulo ni vacio.
 * - El apellido paterno nunca es nulo ni vacio.
 * - El apellido materno es opcional — puede ser nulo en casos de
 *   registro civil con un solo apellido.
 * - Todos los valores se normalizan a mayusculas y sin espacios
 *   al inicio o al final en construccion.
 *
 * Uso:
 * <pre>{@code
 * FullName name = FullName.of("Rafael", "Morales", "Angoa");
 * FullName name = FullName.of("Rafael", "Morales", null);
 *
 * String display = name.toDisplayString();
 * }</pre>
 *
 * @since 1.0.0
 */
public record FullName(
        String givenName,
        String paternalSurname,
        String maternalSurname
) implements Serializable {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 80;

    public FullName {
        Objects.requireNonNull(givenName, "FullName givenName must not be null");
        Objects.requireNonNull(paternalSurname, "FullName paternalSurname must not be null");

        givenName = normalize(givenName);
        paternalSurname = normalize(paternalSurname);
        maternalSurname = maternalSurname != null ? normalize(maternalSurname) : null;

        validateLength("givenName", givenName);
        validateLength("paternalSurname", paternalSurname);
        if (maternalSurname != null) {
            validateLength("maternalSurname", maternalSurname);
        }
    }

    public static FullName of(String givenName, String paternalSurname, String maternalSurname) {
        return new FullName(givenName, paternalSurname, maternalSurname);
    }

    public static FullName of(String givenName, String paternalSurname) {
        return new FullName(givenName, paternalSurname, null);
    }

    public boolean hasMaternalSurname() {
        return maternalSurname != null;
    }

    public String toDisplayString() {
        if (hasMaternalSurname()) {
            return paternalSurname + " " + maternalSurname + ", " + givenName;
        }
        return paternalSurname + ", " + givenName;
    }

    public String toSearchString() {
        if (hasMaternalSurname()) {
            return givenName + " " + paternalSurname + " " + maternalSurname;
        }
        return givenName + " " + paternalSurname;
    }

    private static String normalize(String value) {
        return value.toUpperCase().trim();
    }

    private static void validateLength(String field, String value) {
        if (value.isBlank()) {
            throw new ValidationException(
                    "INVALID_NAME_BLANK",
                    field,
                    "FullName " + field + " must not be blank"
            );
        }
        if (value.length() < MIN_LENGTH) {
            throw new ValidationException(
                    "INVALID_NAME_TOO_SHORT",
                    field,
                    "FullName " + field + " must have at least " + MIN_LENGTH + " characters"
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new ValidationException(
                    "INVALID_NAME_TOO_LONG",
                    field,
                    "FullName " + field + " must not exceed " + MAX_LENGTH + " characters"
            );
        }
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}