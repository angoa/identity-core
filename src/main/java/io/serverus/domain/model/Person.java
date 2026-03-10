package io.serverus.domain.model;

import io.serverus.domain.exception.BusinessRuleException;
import io.serverus.domain.model.vo.BirthDate;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.FullName;
import io.serverus.domain.model.vo.PersonId;
import io.serverus.domain.model.vo.PersonStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate raiz que representa a una persona dentro del sistema
 * de identidad digital.
 *
 * Encapsula el estado y el comportamiento del ciclo de vida de una
 * persona — registro, suspension, reactivacion y revocacion.
 * Garantiza que todas las transiciones de estado cumplan las reglas
 * de negocio del dominio antes de ejecutarse.
 *
 * Los domain events generados por las operaciones son responsabilidad
 * del service que invoca el comportamiento del aggregate.
 *
 * Invariantes:
 * - El identificador nunca es nulo.
 * - La CURP nunca es nula.
 * - El nombre completo nunca es nulo.
 * - La fecha de nacimiento nunca es nula.
 * - El estado nunca es nulo.
 * - La fecha de creacion nunca es nula.
 *
 * Uso:
 * <pre>{@code
 * Person person = Person.register(curp, fullName, birthDate);
 *
 * person.suspend("Proceso administrativo INE-2026-001");
 * person.reactivate("Conclusion proceso administrativo");
 * person.revoke("Resolucion judicial 2026-MX-001");
 * }</pre>
 *
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(of = {"id", "curp", "status"})
public class Person {

    @EqualsAndHashCode.Include
    private final PersonId      id;
    private final Curp          curp;
    private       FullName      fullName;
    private       BirthDate     birthDate;
    private       PersonStatus  status;
    private final LocalDateTime createdAt;
    private       LocalDateTime updatedAt;

    /**
     * Constructor privado. La construccion se realiza exclusivamente
     * a traves de factory methods para garantizar invariantes.
     */
    private Person(
            PersonId      id,
            Curp          curp,
            FullName      fullName,
            BirthDate     birthDate,
            PersonStatus  status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id        = Objects.requireNonNull(id,        "Person id must not be null");
        this.curp      = Objects.requireNonNull(curp,      "Person curp must not be null");
        this.fullName  = Objects.requireNonNull(fullName,  "Person fullName must not be null");
        this.birthDate = Objects.requireNonNull(birthDate, "Person birthDate must not be null");
        this.status    = Objects.requireNonNull(status,    "Person status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Person createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Person updatedAt must not be null");
    }

    /**
     * Factory method. Registra una nueva persona en el sistema.
     *
     * @param curp      CURP del registro
     * @param fullName  nombre completo
     * @param birthDate fecha de nacimiento
     * @return nueva instancia de Person en estado ACTIVE
     * @throws NullPointerException si cualquier parametro es nulo
     */
    public static Person register(Curp curp, FullName fullName, BirthDate birthDate) {
        LocalDateTime now = LocalDateTime.now();
        return new Person(
                PersonId.generate(),
                curp,
                fullName,
                birthDate,
                PersonStatus.ACTIVE,
                now,
                now
        );
    }

    /**
     * Factory method. Reconstruye una persona desde la capa de persistencia.
     *
     * @param id        identificador unico
     * @param curp      CURP del registro
     * @param fullName  nombre completo
     * @param birthDate fecha de nacimiento
     * @param status    estado actual
     * @param createdAt fecha y hora de creacion original
     * @param updatedAt fecha y hora de ultima actualizacion
     * @return instancia de Person reconstruida desde persistencia
     */
    public static Person reconstitute(
            PersonId      id,
            Curp          curp,
            FullName      fullName,
            BirthDate     birthDate,
            PersonStatus  status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Person(id, curp, fullName, birthDate, status, createdAt, updatedAt);
    }

    /**
     * Suspende a la persona de forma temporal.
     *
     * @param reason motivo de la suspension — requerido para auditoria
     * @throws NullPointerException  si reason es nulo
     * @throws BusinessRuleException si el estado actual no permite
     *                               la transicion a SUSPENDED
     */
    public void suspend(String reason) {
        Objects.requireNonNull(reason, "Suspension reason must not be null");
        if (!status.canTransitionTo(PersonStatus.SUSPENDED)) {
            throw new BusinessRuleException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot transition from " + status + " to SUSPENDED"
            );
        }
        this.status    = PersonStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reactiva a la persona despues de una suspension temporal.
     *
     * @param reason motivo de la reactivacion — requerido para auditoria
     * @throws NullPointerException  si reason es nulo
     * @throws BusinessRuleException si el estado actual no permite
     *                               la transicion a ACTIVE
     */
    public void reactivate(String reason) {
        Objects.requireNonNull(reason, "Reactivation reason must not be null");
        if (!status.canTransitionTo(PersonStatus.ACTIVE)) {
            throw new BusinessRuleException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot transition from " + status + " to ACTIVE"
            );
        }
        this.status    = PersonStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Revoca a la persona de forma definitiva.
     *
     * @param reason motivo de la revocacion — requerido para auditoria
     * @throws NullPointerException  si reason es nulo
     * @throws BusinessRuleException si el estado actual es terminal
     */
    public void revoke(String reason) {
        Objects.requireNonNull(reason, "Revocation reason must not be null");
        if (!status.canTransitionTo(PersonStatus.REVOKED)) {
            throw new BusinessRuleException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot transition from " + status + " to REVOKED"
            );
        }
        this.status    = PersonStatus.REVOKED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Actualiza el nombre completo de la persona.
     *
     * @param fullName nuevo nombre completo
     * @throws NullPointerException  si fullName es nulo
     * @throws BusinessRuleException si el estado actual no permite
     *                               la actualizacion de atributos
     */
    public void updateFullName(FullName fullName) {
        Objects.requireNonNull(fullName, "FullName must not be null");
        if (!status.allowsAttributeUpdate()) {
            throw new BusinessRuleException(
                    "ATTRIBUTE_UPDATE_NOT_ALLOWED",
                    "Cannot update attributes when status is " + status
            );
        }
        this.fullName  = fullName;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Indica si la persona esta activa en el sistema.
     *
     * @return true si el estado es ACTIVE
     */
    public boolean isActive() {
        return status == PersonStatus.ACTIVE;
    }

    /**
     * Indica si la persona esta suspendida.
     *
     * @return true si el estado es SUSPENDED
     */
    public boolean isSuspended() {
        return status == PersonStatus.SUSPENDED;
    }

    /**
     * Indica si la persona ha sido revocada.
     *
     * @return true si el estado es REVOKED
     */
    public boolean isRevoked() {
        return status == PersonStatus.REVOKED;
    }
}