package io.serverus.domain.model.vo;

/**
 * Enumeracion que representa el estado del ciclo de vida de una persona
 * dentro del sistema de identidad digital.
 *
 * El estado determina que operaciones pueden realizarse sobre el registro
 * y que credenciales pueden emitirse o utilizarse.
 *
 * Transiciones validas del ciclo de vida:
 *
 * ACTIVE -> SUSPENDED  : suspension temporal por proceso administrativo
 * ACTIVE -> REVOKED    : revocacion definitiva por resolucion legal
 * SUSPENDED -> ACTIVE  : reactivacion por conclusion del proceso administrativo
 * SUSPENDED -> REVOKED : revocacion durante suspension
 * REVOKED -> (ninguno) : estado terminal, no permite transicion
 *
 * @since 1.0.0
 */
public enum PersonStatus {

    /**
     * La persona esta activa en el sistema.
     *
     * Permite el uso de credenciales, emision de nuevas credenciales
     * y actualizacion de atributos verificados.
     */
    ACTIVE,

    /**
     * La persona esta suspendida temporalmente.
     *
     * No permite el uso ni emision de credenciales.
     * El registro permanece en el sistema y puede reactivarse
     * por conclusion del proceso administrativo correspondiente.
     */
    SUSPENDED,

    /**
     * La persona ha sido revocada de forma definitiva.
     *
     * Estado terminal. No permite transicion a ningun otro estado.
     * No permite el uso ni emision de credenciales.
     * El registro permanece en el sistema por requisitos de auditoria.
     */
    REVOKED;

    /**
     * Indica si el estado permite emitir nuevas credenciales.
     *
     * @return true si el estado es ACTIVE
     */
    public boolean allowsCredentialIssuance() {
        return this == ACTIVE;
    }

    /**
     * Indica si el estado permite el uso de credenciales existentes.
     *
     * @return true si el estado es ACTIVE
     */
    public boolean allowsCredentialUsage() {
        return this == ACTIVE;
    }

    /**
     * Indica si el estado permite la actualizacion de atributos.
     *
     * @return true si el estado es ACTIVE o SUSPENDED
     */
    public boolean allowsAttributeUpdate() {
        return this == ACTIVE || this == SUSPENDED;
    }

    /**
     * Indica si el estado es terminal.
     *
     * Un estado terminal no permite transicion a ningun otro estado.
     *
     * @return true si el estado es REVOKED
     */
    public boolean isTerminal() {
        return this == REVOKED;
    }

    /**
     * Valida si una transicion al estado destino es valida
     * desde el estado actual.
     *
     * @param target estado destino de la transicion
     * @return true si la transicion es valida
     */
    public boolean canTransitionTo(PersonStatus target) {
        return switch (this) {
            case ACTIVE    -> target == SUSPENDED || target == REVOKED;
            case SUSPENDED -> target == ACTIVE    || target == REVOKED;
            case REVOKED   -> false;
        };
    }
}