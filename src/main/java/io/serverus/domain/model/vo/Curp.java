package io.serverus.domain.model.vo;

import io.serverus.domain.exception.ValidationException;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object que representa la Clave Unica de Registro de Poblacion.
 *
 * La CURP es un codigo alfanumerico de 18 caracteres asignado por RENAPO
 * a cada persona registrada en Mexico. Es el identificador primario de
 * identidad en el sistema nacional de registro de poblacion.
 *
 * Invariantes:
 * - El valor nunca es nulo.
 * - Longitud exacta de 18 caracteres.
 * - Formato validado contra el patron oficial RENAPO.
 * - El valor se normaliza a mayusculas en construccion.
 *
 * Uso:
 * <pre>{@code
 * Curp curp = Curp.of("MOAA800101HDFRRN09");
 *
 * if (Curp.isValid(input)) {
 *     Curp curp = Curp.of(input);
 * }
 * }</pre>
 *
 * @see <a href="https://www.gob.mx/curp">Portal CURP - RENAPO</a>
 * @since 1.0.0
 */
public record Curp(String value) implements Serializable {

    private static final Pattern CURP_PATTERN = Pattern.compile(
            "^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$"
    );

    private static final int CURP_LENGTH = 18;

    public Curp {
        Objects.requireNonNull(value, "Curp value must not be null");
        value = value.toUpperCase().trim();
        if (!CURP_PATTERN.matcher(value).matches()) {
            throw new ValidationException(
                    "INVALID_CURP_FORMAT",
                    "curp",
                    "Curp value does not match RENAPO official format: " + value
            );
        }
    }

    public static Curp of(String value) {
        return new Curp(value);
    }

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) return false;
        return CURP_PATTERN.matcher(value.toUpperCase().trim()).matches();
    }

    public char extractSex() {
        return value.charAt(10);
    }

    public String extractStateCode() {
        return value.substring(11, 13);
    }

    public String extractBirthDate() {
        return value.substring(4, 10);
    }

    @Override
    public String toString() {
        return value;
    }
}