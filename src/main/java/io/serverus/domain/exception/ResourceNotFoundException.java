package io.serverus.domain.exception;

/**
 * Excepcion que representa la ausencia de un recurso del dominio.
 *
 * Se lanza cuando se busca una Entity por su identificador
 * y no existe en el sistema.
 *
 * El REST adapter mapea esta excepcion al codigo HTTP 404 Not Found.
 *
 * Uso:
 * <pre>{@code
 * throw new ResourceNotFoundException(
 *     "PERSON_NOT_FOUND",
 *     "Person with id " + id + " not found"
 * );
 * }</pre>
 *
 * @since 1.0.0
 */
public class ResourceNotFoundException extends DomainException {

    /**
     * Constructor principal.
     *
     * @param errorCode codigo de error unico del dominio
     * @param message   mensaje descriptivo del error
     */
    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}