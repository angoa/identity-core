package io.serverus.infrastructure.rest.dto;

import io.serverus.domain.model.Person;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de salida para operaciones de cambio de estado de una persona.
 *
 * Se retorna en los endpoints de suspension, reactivacion y revocacion.
 * Expone unicamente los campos relevantes para confirmar el cambio
 * de estado sin retornar el aggregate completo.
 *
 * @param id        identificador unico
 * @param curp      CURP del registro
 * @param status    nuevo estado del ciclo de vida
 * @param updatedAt fecha y hora de la actualizacion
 *
 * @since 1.0.0
 */
@Schema(name = "StatusResponse", description = "Confirmacion de cambio de estado de una persona")
public record StatusResponse(

        @Schema(description = "Identificador unico UUID")
        String id,

        @Schema(description = "CURP en formato oficial RENAPO")
        String curp,

        @Schema(description = "Nuevo estado del ciclo de vida")
        String status,

        @Schema(description = "Fecha y hora de la actualizacion")
        LocalDateTime updatedAt

) {
    /**
     * Factory method. Construye un StatusResponse desde el aggregate Person.
     *
     * @param person aggregate con el estado actualizado
     * @return DTO de confirmacion
     */
    public static StatusResponse from(Person person) {
        return new StatusResponse(
                person.getId().toString(),
                person.getCurp().toString(),
                person.getStatus().name(),
                person.getUpdatedAt()
        );
    }
}