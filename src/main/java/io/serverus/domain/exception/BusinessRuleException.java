package io.serverus.domain.exception;

/**
 * Excepcion que representa una violacion de una regla de negocio
 * del dominio.
 *
 * Se lanza cuando una operacion es tecnicamente valida pero viola
 * una regla de negocio — intentar emitir una credencial a una persona
 * suspendida, intentar registrar una CURP duplicada, intentar
 * transicionar a un estado no permitido.
 *
 * El REST adapter mapea esta excepcion al codigo HTTP 409 Conflict
 * o 422 Unprocessable Entity segun el contexto.
 *
 * Uso:
 * <pre>{@code
 * throw new BusinessRuleException(
 *     "PERSON_ALREADY_REGISTERED",
 *     "A person with CURP " + curp + " is already registered"
 * );
 *
 * throw new BusinessRuleException(
 *     "INVALID_STATUS_TRANSITION",
 *     "Cannot transition from " + current + " to " + target
 * );
 * }</pre>
 *
 * @since 1.0.0
 */
public class BusinessRuleException extends DomainException {

    /**
     * Constructor principal.
     *
     * @param errorCode codigo de error unico del dominio
     * @param message   mensaje descriptivo del error
     */
    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructor con causa raiz.
     *
     * @param errorCode codigo de error unico del dominio
     * @param message   mensaje descriptivo del error
     * @param cause     excepcion que origino este error
     */
    public BusinessRuleException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}