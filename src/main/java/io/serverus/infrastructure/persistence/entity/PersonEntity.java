package io.serverus.infrastructure.persistence.entity;

import io.serverus.domain.model.vo.PersonStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa el registro de una persona en la base de datos.
 *
 * Es la representacion de persistencia del aggregate Person. No contiene
 * logica de negocio — es exclusivamente un contenedor de datos para
 * la capa de persistencia.
 *
 * La traduccion entre PersonEntity y Person es responsabilidad
 * de PersonMapper.
 *
 * Tabla: PERSONS
 * Schema: definido por Flyway en V1__create_persons_table.sql
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "PERSONS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "curp", "status"})
public class PersonEntity {

    /**
     * Identificador unico del registro.
     * Corresponde a PersonId.value() en el dominio.
     * Tipo VARCHAR2(36) en Oracle para almacenar UUID como cadena.
     */
    @Id
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    /**
     * Clave Unica de Registro de Poblacion.
     * Restriccion UNIQUE garantizada a nivel de base de datos.
     */
    @Column(name = "CURP", length = 18, nullable = false, unique = true)
    private String curp;

    /**
     * Nombre de pila de la persona.
     */
    @Column(name = "GIVEN_NAME", length = 80, nullable = false)
    private String givenName;

    /**
     * Apellido paterno de la persona.
     */
    @Column(name = "PATERNAL_SURNAME", length = 80, nullable = false)
    private String paternalSurname;

    /**
     * Apellido materno de la persona. Puede ser nulo.
     */
    @Column(name = "MATERNAL_SURNAME", length = 80)
    private String maternalSurname;

    /**
     * Fecha de nacimiento de la persona en formato ISO-8601.
     * Almacenada como DATE en Oracle.
     */
    @Column(name = "BIRTH_DATE", nullable = false)
    private java.time.LocalDate birthDate;

    /**
     * Estado del ciclo de vida de la persona.
     * Almacenado como VARCHAR2 con el nombre del enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20, nullable = false)
    private PersonStatus status;

    /**
     * Fecha y hora de creacion del registro.
     * Inmutable — no se actualiza despues de la insercion.
     */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la ultima actualizacion del registro.
     */
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}