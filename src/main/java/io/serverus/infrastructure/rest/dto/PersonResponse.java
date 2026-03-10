package io.serverus.infrastructure.rest.dto;

import io.serverus.domain.model.Person;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de salida para la representacion de una persona en el API.
 *
 * @param id              identificador unico
 * @param curp            CURP del registro
 * @param givenName       nombre de pila
 * @param paternalSurname apellido paterno
 * @param maternalSurname apellido materno, puede ser nulo
 * @param displayName     nombre completo en formato de presentacion oficial
 * @param birthDate       fecha de nacimiento
 * @param age             edad calculada en anos completos
 * @param status          estado del ciclo de vida
 * @param createdAt       fecha y hora de creacion
 * @param updatedAt       fecha y hora de ultima actualizacion
 *
 * @since 1.0.0
 */
@Schema(name = "PersonResponse", description = "Datos de una persona registrada")
public record PersonResponse(

        @Schema(description = "Identificador unico UUID")
        String id,

        @Schema(description = "CURP en formato oficial RENAPO")
        String curp,

        @Schema(description = "Nombre de pila")
        String givenName,

        @Schema(description = "Apellido paterno")
        String paternalSurname,

        @Schema(description = "Apellido materno, puede ser nulo")
        String maternalSurname,

        @Schema(description = "Nombre completo en formato oficial")
        String displayName,

        @Schema(description = "Fecha de nacimiento en formato ISO-8601")
        LocalDate birthDate,

        @Schema(description = "Edad en anos completos")
        int age,

        @Schema(description = "Estado del ciclo de vida")
        String status,

        @Schema(description = "Fecha y hora de creacion")
        LocalDateTime createdAt,

        @Schema(description = "Fecha y hora de ultima actualizacion")
        LocalDateTime updatedAt

) {
    /**
     * Factory method. Construye un PersonResponse desde el aggregate Person.
     *
     * @param person aggregate del dominio
     * @return DTO de respuesta
     */
    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId().toString(),
                person.getCurp().toString(),
                person.getFullName().givenName(),
                person.getFullName().paternalSurname(),
                person.getFullName().maternalSurname(),
                person.getFullName().toDisplayString(),
                person.getBirthDate().value(),
                person.getBirthDate().calculateAge(),
                person.getStatus().name(),
                person.getCreatedAt(),
                person.getUpdatedAt()
        );
    }
}