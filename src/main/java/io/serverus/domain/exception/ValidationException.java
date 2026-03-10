package io.serverus.domain.exception;

/**
 * Excepcion que representa una violacion de las invariantes de un
 * Value Object o Entity del dominio.
 *
 * Se lanza cuando un valor de entrada no cumple las reglas de negocio
 * definidas en el dominio — formato incorrecto, rango invalido,
 * valor nulo donde no se permite.
 *
 * El REST adapter mapea esta excepcion al codigo HTTP 422
 * Unprocessable Entity.
 *
 * Uso:
 * <pre>{@code
 * throw new ValidationException(
 *     "INVALID_CURP_FORMAT",
 *     "Curp value does not match RENAPO official format: " + value
 * );
 * }</pre>
 *
 * @since 1.0.0
 */
public class ValidationException extends DomainException {

    /**
     * Nombre del campo que origino la violacion de invariante.
     * Permite al cliente identificar que campo corregir.
     */
    private final String field;

    /**
     * Constructor principal.
     *
     * @param errorCode codigo de error unico del dominio
     * @param field     nombre del campo que origino el error
     * @param message   mensaje descriptivo del error
     */
    public ValidationException(String errorCode, String field, String message) {
        super(errorCode, message);
        this.field = field;
    }

    /**
     * Retorna el nombre del campo que origino la violacion de invariante.
     *
     * @return nombre del campo como String
     */
    public String getField() {
        return field;
    }
}