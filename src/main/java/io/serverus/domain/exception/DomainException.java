package io.serverus.domain.exception;

/**
 * Excepcion base de la jerarquia de excepciones del dominio.
 *
 * Todas las excepciones del dominio extienden esta clase, permitiendo
 * al REST adapter capturar cualquier excepcion de dominio con un
 * solo bloque catch y mapearla al codigo HTTP correspondiente.
 *
 * La jerarquia de excepciones del dominio es independiente de cualquier
 * framework — no extiende excepciones de Quarkus, Jakarta o Spring.
 *
 * @since 1.0.0
 */
public abstract class DomainException extends RuntimeException {

    /**
     * Codigo de error unico que identifica el tipo de excepcion.
     * Se utiliza en la respuesta HTTP para que el cliente pueda
     * identificar el error de forma programatica.
     */
    private final String errorCode;

    /**
     * Constructor principal.
     *
     * @param errorCode codigo de error unico del dominio
     * @param message   mensaje descriptivo del error
     */
    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor con causa raiz.
     *
     * @param errorCode codigo de error unico del dominio
     * @param message   mensaje descriptivo del error
     * @param cause     excepcion que origino este error
     */
    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Retorna el codigo de error unico del dominio.
     *
     * @return codigo de error como String
     */
    public String getErrorCode() {
        return errorCode;
    }
}