package io.serverus.domain.model.vo;

import io.serverus.domain.exception.ValidationException;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Value Object que representa la fecha de nacimiento de una persona
 * dentro del sistema de identidad digital.
 *
 * Encapsula un {@link LocalDate} como representacion de la fecha de
 * nacimiento, garantizando que el valor sea valido y no futuro en el
 * momento de construccion.
 *
 * Invariantes:
 * - El valor nunca es nulo.
 * - La fecha no puede ser una fecha futura.
 * - La fecha no puede ser anterior al 1 de enero de 1900.
 *
 * Uso:
 * <pre>{@code
 * BirthDate birthDate = BirthDate.of(LocalDate.of(1980, 1, 1));
 * BirthDate birthDate = BirthDate.of("1980-01-01");
 *
 * int age = birthDate.calculateAge();
 * boolean isAdult = birthDate.isAdult();
 * }</pre>
 *
 * @since 1.0.0
 */
public record BirthDate(LocalDate value) implements Serializable {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final int LEGAL_AGE = 18;

    public BirthDate {
        Objects.requireNonNull(value, "BirthDate value must not be null");
        if (value.isAfter(LocalDate.now())) {
            throw new ValidationException(
                    "INVALID_BIRTH_DATE_FUTURE",
                    "birthDate",
                    "BirthDate value must not be a future date: " + value
            );
        }
        if (value.isBefore(MIN_DATE)) {
            throw new ValidationException(
                    "INVALID_BIRTH_DATE_TOO_OLD",
                    "birthDate",
                    "BirthDate value must not be before " + MIN_DATE + ": " + value
            );
        }
    }

    public static BirthDate of(LocalDate value) {
        return new BirthDate(value);
    }

    public static BirthDate of(String value) {
        Objects.requireNonNull(value, "BirthDate string value must not be null");
        try {
            return new BirthDate(LocalDate.parse(value));
        } catch (Exception e) {
            throw new ValidationException(
                    "INVALID_BIRTH_DATE_FORMAT",
                    "birthDate",
                    "BirthDate value is not a valid ISO-8601 date: " + value
            );
        }
    }

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            LocalDate date = LocalDate.parse(value);
            return !date.isAfter(LocalDate.now()) && !date.isBefore(MIN_DATE);
        } catch (Exception e) {
            return false;
        }
    }

    public int calculateAge() {
        return Period.between(value, LocalDate.now()).getYears();
    }

    public boolean isAdult() {
        return calculateAge() >= LEGAL_AGE;
    }

    public boolean isMinor() {
        return !isAdult();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}